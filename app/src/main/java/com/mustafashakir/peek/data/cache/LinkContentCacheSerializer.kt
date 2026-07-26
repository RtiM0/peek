package com.mustafashakir.peek.data.cache

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class LinkContentCacheSerializer(
    override val defaultValue: LinkContentCacheDocument,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : Serializer<LinkContentCacheDocument> {
    override suspend fun readFrom(input: InputStream): LinkContentCacheDocument = try {
        json.decodeFromString(input.readBytes().decodeToString())
    } catch (error: SerializationException) {
        throw CorruptionException("Unable to read link content cache", error)
    }

    override suspend fun writeTo(t: LinkContentCacheDocument, output: OutputStream) {
        output.write(json.encodeToString(t).encodeToByteArray())
    }
}
