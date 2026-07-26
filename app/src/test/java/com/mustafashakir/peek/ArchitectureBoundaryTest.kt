package com.mustafashakir.peek

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureBoundaryTest {
    private val sourceRoot = File("src/main/java/com/mustafashakir/peek")
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
}
