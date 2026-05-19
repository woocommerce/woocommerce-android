package com.woocommerce.android.aiassistant.architecture

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class FeatureModuleBoundaryTest {
    @Test
    fun `when scanning feature production sources, then Woo analytics imports are absent`() {
        val sourceDir = repoRoot().resolve("libs/ai-assistant/feature/src/main/kotlin")
        assertThat(sourceDir).isDirectory()

        val forbiddenImport = Regex("""import\s+com\.woocommerce\.android\.analytics(\b|\.)""")
        val violations = sourceDir.kotlinFiles()
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (forbiddenImport.containsMatchIn(line)) {
                        "${file.relativeTo(repoRoot())}:${index + 1}:$line"
                    } else {
                        null
                    }
                }
            }

        assertThat(violations).isEmpty()
    }

    @Test
    fun `when scanning Woo assistant telemetry package, then generated assistant events are not constructed`() {
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

    @Test
    fun `when scanning assistant ViewModel boundary, then model messages are not persistent session history`() {
        val root = repoRoot()
        val viewModel = root.resolve(
            "libs/ai-assistant/feature/src/main/kotlin/com/woocommerce/android/aiassistant/ui/AssistantViewModel.kt"
        )
        val runtime = root.resolve(
            "libs/ai-assistant/feature/src/main/kotlin/com/woocommerce/android/aiassistant/runtime/AssistantRuntime.kt"
        )
        assertThat(viewModel).isFile
        assertThat(runtime).isFile

        val violations = buildList {
            addAll(
                viewModel.forbiddenMatches(
                    Regex("""import\s+com\.woocommerce\.android\.aiassistant\.core\.chat\.AssistantMessage\b""")
                )
            )
            addAll(
                viewModel.forbiddenMatches(
                    Regex(
                        """\b(history|lastTurnBaseHistory|committedSessionHistory|currentTurnBaseHistory):""" +
                            """\s*List<AssistantMessage>"""
                    )
                )
            )
            addAll(
                runtime.forbiddenMatches(
                    Regex(
                        """AssistantTurnRequest\([^)]*history:\s*List<AssistantMessage>""",
                        RegexOption.DOT_MATCHES_ALL,
                    )
                )
            )
            addAll(
                runtime.forbiddenMatches(
                    Regex("""updatedHistory:\s*List<AssistantMessage>""")
                )
            )
        }

        assertThat(violations).isEmpty()
    }

    @Test
    fun `when scanning assistant ViewModel boundary, then ViewModel does not construct session messages directly`() {
        val viewModel = repoRoot().resolve(
            "libs/ai-assistant/feature/src/main/kotlin/com/woocommerce/android/aiassistant/ui/AssistantViewModel.kt"
        )
        assertThat(viewModel).isFile

        val violations = buildList {
            addAll(
                viewModel.forbiddenMatches(
                    Regex(
                        """import\s+com\.woocommerce\.android\.aiassistant\.core\.history\.AssistantSessionMessage\b"""
                    )
                )
            )
            addAll(viewModel.forbiddenMatches(Regex("""AssistantSessionMessage\.User\(""")))
            addAll(viewModel.forbiddenMatches(Regex("""AssistantSessionMessage\.Assistant\(""")))
        }

        assertThat(violations).isEmpty()
    }

    private fun File.kotlinFiles(): List<File> =
        walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    private fun File.forbiddenMatches(regex: Regex): List<String> =
        if (RegexOption.DOT_MATCHES_ALL in regex.options) {
            listOfNotNull(
                if (regex.containsMatchIn(readText())) {
                    "${relativeTo(repoRoot())}: matched ${regex.pattern}"
                } else {
                    null
                }
            )
        } else {
            readLines().mapIndexedNotNull { index, line ->
                if (regex.containsMatchIn(line)) {
                    "${relativeTo(repoRoot())}:${index + 1}:$line"
                } else {
                    null
                }
            }
        }

    private fun repoRoot(): File {
        val userDir = requireNotNull(System.getProperty("user.dir")) { "user.dir is not set" }
        var current: File? = File(userDir).absoluteFile
        while (current != null && !current.resolve("settings.gradle").isFile) {
            current = current.parentFile
        }
        return requireNotNull(current) { "Unable to locate repository root from $userDir" }
    }
}
