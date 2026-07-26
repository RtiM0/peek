package com.mustafashakir.peek.data.recent

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class RecentLinksSerializer(
    override val defaultValue: RecentLinksDocument,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : Serializer<RecentLinksDocument> {
    override suspend fun readFrom(input: InputStream): RecentLinksDocument = try {
        json.decodeFromString(input.readBytes().decodeToString())
    } catch (error: SerializationException) {
        throw CorruptionException("Unable to read recent links", error)
    }

    override suspend fun writeTo(t: RecentLinksDocument, output: OutputStream) {
        output.write(json.encodeToString(t).encodeToByteArray())
    }
}
