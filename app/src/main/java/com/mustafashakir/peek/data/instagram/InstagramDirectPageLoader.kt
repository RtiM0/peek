package com.mustafashakir.peek.data.instagram

import com.mustafashakir.peek.domain.model.LoadProgress
import com.mustafashakir.peek.domain.model.LoadStage
import com.mustafashakir.peek.domain.repository.LoadProgressListener
import java.io.IOException
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Fetches the same logged-out post payload Kittygram uses. The response includes the post's
 * complete media collection and Instagram's initial comments connection.
 */
class InstagramDirectPageLoader(
    private val parser: InstagramHtmlParser = InstagramHtmlParser(),
    private val connectionFactory: (String) -> HttpURLConnection = { url ->
        URI(url).toURL().openConnection() as HttpURLConnection
    },
) : InstagramPageLoader {
    private val json = Json { ignoreUnknownKeys = true }
    private val secureRandom = SecureRandom()

    override val resolverId: String = "instagram-graphql"

    override suspend fun resolve(url: String): ParsedInstagramMedia {
        val shortcode = extractShortcode(url)
            ?: throw IllegalArgumentException("Unsupported Instagram post URL: $url")
        val listener = coroutineContext[PageLoadProgressElement]?.listener ?: LoadProgressListener {}
        listener.report(0.05f, LoadStage.Connecting)

        val variables = buildJsonObject {
            put("media_id", shortcodeToMediaId(shortcode).toString())
        }
        val response = request(url) { lsd -> mediaQueryBody(variables, lsd) }
        listener.report(0.85f, LoadStage.ExtractingContent)
        val media = parser.parse(response)
            ?: throw IOException(graphQlErrorMessage(response) ?: "Instagram response contained no public post")
        listener.report(1f, LoadStage.ExtractingContent)
        return media
    }

    override suspend fun loadComments(postId: String, cursor: String): ParsedInstagramCommentsPage {
        require(postId.isNotBlank()) { "Instagram post id is required" }
        require(cursor.isNotBlank()) { "Instagram comment cursor is required" }
        val variables = buildJsonObject {
            put("after", cursor)
            put("first", COMMENTS_PAGE_SIZE)
            put("media_id", postId)
        }
        val response = request("https://www.instagram.com/p/$postId/") { lsd ->
            mediaQueryBody(variables, lsd, COMMENTS_PAGINATION_DOC_ID)
        }
        return parser.parseCommentsPage(response)
            ?: throw IOException(graphQlErrorMessage(response) ?: "Instagram response contained no comments page")
    }

    private suspend fun request(sourceUrl: String, body: (String) -> String): String {
        val lsd = randomLsd()
        return withContext(Dispatchers.IO) { executeRequest(body(lsd), lsd, sourceUrl) }
    }

    private fun mediaQueryBody(
        variables: kotlinx.serialization.json.JsonObject,
        lsd: String,
        docId: String = MEDIA_ID_POST_DOC_ID,
    ): String {
        return formBody(
            "av" to "0",
            "__d" to "www",
            "__user" to "0",
            "__a" to "1",
            "__comet_req" to "7",
            "lsd" to lsd,
            "server_timestamps" to "true",
            "variables" to json.encodeToString(variables),
            "doc_id" to docId,
        )
    }

    private fun executeRequest(body: String, lsd: String, sourceUrl: String): String {
        val connection = connectionFactory(GRAPHQL_ENDPOINT)
        try {
            connection.instanceFollowRedirects = false
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setRequestProperty("Accept", "*/*")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Origin", "https://www.instagram.com")
            connection.setRequestProperty("Referer", sourceUrl)
            connection.setRequestProperty("Sec-CH-Prefers-Color-Scheme", "dark")
            connection.setRequestProperty(
                "Sec-CH-UA",
                "\"Chromium\";v=\"135\", \"Not)A;Brand\";v=\"24\"",
            )
            connection.setRequestProperty("Sec-CH-UA-Mobile", "?0")
            connection.setRequestProperty("Sec-CH-UA-Model", "\"\"")
            connection.setRequestProperty("Sec-CH-UA-Platform-Version", "\"10.0.19045\"")
            connection.setRequestProperty("Sec-Fetch-Dest", "empty")
            connection.setRequestProperty("Sec-Fetch-Mode", "cors")
            connection.setRequestProperty("Sec-Fetch-Site", "same-origin")
            connection.setRequestProperty("X-IG-App-ID", INSTAGRAM_WEB_APP_ID)
            connection.setRequestProperty("X-CSRFToken", "exampletexthere")
            connection.setRequestProperty("X-IG-Max-Touch-Points", "0")
            connection.setRequestProperty("X-FB-LSD", lsd)
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }

            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == 429) {
                throw IOException("Instagram temporarily rate-limited the request (HTTP $status)")
            }
            if (status !in 200..299) {
                throw IOException("Instagram returned HTTP $status")
            }
            if (response.isBlank()) throw IOException("Instagram returned an empty response")
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun extractShortcode(url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        return POST_PATH.matchEntire(uri.path.orEmpty().trimEnd('/'))?.groupValues?.get(2)
    }

    private fun shortcodeToMediaId(shortcode: String): BigInteger =
        shortcode.fold(BigInteger.ZERO) { value, character ->
            val digit = SHORTCODE_ALPHABET.indexOf(character)
            require(digit >= 0) { "Invalid Instagram shortcode" }
            value * BASE_64 + digit.toBigInteger()
        }

    private fun randomLsd(): String {
        val bytes = ByteArray(12)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun formBody(vararg entries: Pair<String, String>): String =
        entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun graphQlErrorMessage(response: String): String? {
        val root = runCatching { json.parseToJsonElement(response) }.getOrNull() ?: return null
        val text = root.toString()
        return when {
            "\"require_login\":true" in text -> "Instagram requires a login for this post"
            "\"errors\"" in text -> "Instagram rejected the post query"
            else -> null
        }
    }

    private fun LoadProgressListener.report(fraction: Float, stage: LoadStage) {
        onProgress(LoadProgress(fraction, stage))
    }

    private companion object {
        const val GRAPHQL_ENDPOINT = "https://www.instagram.com/api/graphql"
        const val MEDIA_ID_POST_DOC_ID = "27130156389949648"
        const val COMMENTS_PAGINATION_DOC_ID = "27261273046856309"
        const val COMMENTS_PAGE_SIZE = 10
        const val INSTAGRAM_WEB_APP_ID = "936619743392459"
        const val USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
        const val SHORTCODE_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val BASE_64: BigInteger = BigInteger.valueOf(64)
        val POST_PATH = Regex("/(p|reel|reels)/([A-Za-z0-9_-]+)")
    }
}
