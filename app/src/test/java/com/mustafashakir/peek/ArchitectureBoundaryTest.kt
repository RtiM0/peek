package com.mustafashakir.peek

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureBoundaryTest {
    private val sourceRoot = File("src/main/java/com/mustafashakir/peek")
    private val uiSourceRoot = File(sourceRoot, "ui")
    private val dataSourceRoot = File(sourceRoot, "data")
    private val domainSourceRoot = File(sourceRoot, "domain")
    private val viewFiles = listOf(
        File(sourceRoot, "ui/home/HomeView.kt"),
        File(sourceRoot, "ui/viewer/ViewerView.kt"),
    )

    @Test
    fun productionViewsDoNotDependOnStateOrDataInfrastructure() {
        val forbidden = listOf(
            ".data.",
            ".domain.",
            "ViewModel",
            "Repository",
            "DataStore",
            "NavDisplay",
            "NavKey",
            "LocalClipboard",
            "LocalContext",
            "ClipData",
        )

        viewFiles.forEach { file ->
            assertTrue("Missing view file: $file", file.isFile)
            val source = file.readText()
            forbidden.forEach { token ->
                assertFalse("${file.name} must not reference $token", source.contains(token))
            }
        }
    }

    @Test
    fun fixtureContentDoesNotLeakIntoProductionViews() {
        val forbiddenFixtures = listOf(
            "A quiet morning in Kyoto",
            "A24 — Material studies",
            "Field notes, volume 04",
            "Mara Chen",
            "The pacing feels like taking a breath.",
            "instagram.com/reel/peek-kyoto",
        )
        val productionViewSource = viewFiles.joinToString("\n") { it.readText() }

        forbiddenFixtures.forEach { fixture ->
            assertFalse("Fixture leaked into production view: $fixture", productionViewSource.contains(fixture))
        }
    }

    @Test
    fun uiDoesNotImplementNetworkOrPersistenceIo() {
        val forbidden = listOf(
            "import android.os.Environment",
            "import android.provider.MediaStore",
            "import java.net.HttpURLConnection",
            "Dispatchers.IO",
            ".openConnection()",
            ".openStream()",
            ".inputStream()",
            ".outputStream()",
        )

        kotlinFiles(uiSourceRoot).forEach { file ->
            val source = file.readText()
            forbidden.forEach { token ->
                assertFalse("${file.name} must not perform data-layer I/O via $token", source.contains(token))
            }
        }
    }

    @Test
    fun lowerLayersDoNotDependOnUi() {
        (kotlinFiles(dataSourceRoot) + kotlinFiles(domainSourceRoot)).forEach { file ->
            assertFalse(
                "${file.name} must not depend on UI types",
                file.readText().contains("com.mustafashakir.peek.ui."),
            )
        }
    }

    @Test
    fun domainDoesNotDependOnAndroidOrData() {
        kotlinFiles(domainSourceRoot).forEach { file ->
            val source = file.readText()
            assertFalse("${file.name} must not depend on Android", source.contains("import android."))
            assertFalse("${file.name} must not depend on data", source.contains("com.mustafashakir.peek.data."))
        }
    }

    private fun kotlinFiles(root: File): List<File> =
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
}
