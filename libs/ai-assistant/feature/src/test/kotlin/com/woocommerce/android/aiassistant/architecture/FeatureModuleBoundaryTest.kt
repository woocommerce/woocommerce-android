package com.woocommerce.android.aiassistant.architecture

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class FeatureModuleBoundaryTest {
    @Test
    fun `feature production sources do not import Woo analytics infrastructure`() {
        val sourceDir = repoRoot().resolve("libs/ai-assistant/feature/src/main/kotlin")
        assertThat(sourceDir).isDirectory()

        val forbiddenImport = Regex("""import\s+com\.woocommerce\.android\.analytics\b""")
        val violations = sourceDir.kotlinFiles()
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    when {
                        forbiddenImport.containsMatchIn(line) -> "${file.relativeTo(repoRoot())}:${index + 1}:$line"
                        "AnalyticsTracker" in line -> "${file.relativeTo(repoRoot())}:${index + 1}:$line"
                        "AnalyticsEvent" in line -> "${file.relativeTo(repoRoot())}:${index + 1}:$line"
                        else -> null
                    }
                }
            }

        assertThat(violations).isEmpty()
    }

    @Test
    fun `Woo assistant telemetry package does not construct generated assistant events`() {
        val sourceDir = repoRoot()
            .resolve("WooCommerce/src/main/kotlin/com/woocommerce/android/ui/aiassistant/telemetry")
        assertThat(sourceDir).isDirectory()

        val generatedAssistantConstruction = Regex("""\bAiAssistant\w+(Event|Value)\b""")
        val violations = sourceDir.kotlinFiles()
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (generatedAssistantConstruction.containsMatchIn(line)) {
                        "${file.relativeTo(repoRoot())}:${index + 1}:$line"
                    } else {
                        null
                    }
                }
            }

        assertThat(violations).isEmpty()
    }

    private fun File.kotlinFiles(): List<File> =
        walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    private fun repoRoot(): File {
        val userDir = requireNotNull(System.getProperty("user.dir")) { "user.dir is not set" }
        var current: File? = File(userDir).absoluteFile
        while (current != null && !current.resolve("settings.gradle").isFile) {
            current = current.parentFile
        }
        return requireNotNull(current) { "Unable to locate repository root from $userDir" }
    }
}
