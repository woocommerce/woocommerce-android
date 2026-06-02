package com.woocommerce.android.aiassistant.headless

import java.io.File

data class WooAiSmokeRunExit(
    val artifactsDirectory: File,
    val sourceArtifactsDirectory: File = artifactsDirectory,
    val failureMessage: String?,
) {
    fun artifactDirectories(): List<File> = listOf(sourceArtifactsDirectory, artifactsDirectory)
        .distinctBy { it.absolutePath }
}
