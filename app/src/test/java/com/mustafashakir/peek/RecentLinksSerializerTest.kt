package com.mustafashakir.peek

import androidx.datastore.core.CorruptionException
import com.mustafashakir.peek.data.recent.RecentLinkRecord
import com.mustafashakir.peek.data.recent.RecentLinksDocument
import com.mustafashakir.peek.data.recent.RecentLinksSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentLinksSerializerTest {
    @Test
    fun persistedDocumentRoundTripsOnlyUrlAndTimestamp() = runTest {
        val document = RecentLinksDocument(listOf(RecentLinkRecord("https://example.test", 42L)))
        val serializer = RecentLinksSerializer(RecentLinksDocument())
        val output = ByteArrayOutputStream()

        serializer.writeTo(document, output)
        val restored = serializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(document, restored)
    }

    @Test(expected = CorruptionException::class)
    fun malformedStorageIsReportedAsCorruptionForReplacementHandler() = runTest {
        val serializer = RecentLinksSerializer(RecentLinksDocument())
        serializer.readFrom(ByteArrayInputStream("not-json".encodeToByteArray()))
    }
}
