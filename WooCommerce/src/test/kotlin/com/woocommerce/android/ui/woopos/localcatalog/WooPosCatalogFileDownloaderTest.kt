package com.woocommerce.android.ui.woopos.localcatalog

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WooPosCatalogFileDownloaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `given file starts with doctype html, when startsWithHtmlMarker, then returns true`() {
        val file = fileWithContent("<!DOCTYPE html><html><body>Forbidden</body></html>")

        assertThat(file.startsWithHtmlMarker()).isTrue()
    }

    @Test
    fun `given file starts with leading whitespace then html tag, when startsWithHtmlMarker, then returns true`() {
        val file = fileWithContent("\n\t  <html><head></head></html>")

        assertThat(file.startsWithHtmlMarker()).isTrue()
    }

    @Test
    fun `given file is a json array, when startsWithHtmlMarker, then returns false`() {
        val file = fileWithContent("""[ { "type": "simple", "data": {} } ]""")

        assertThat(file.startsWithHtmlMarker()).isFalse()
    }

    @Test
    fun `given file is empty, when startsWithHtmlMarker, then returns false`() {
        val file = fileWithContent("")

        assertThat(file.startsWithHtmlMarker()).isFalse()
    }

    private fun fileWithContent(content: String): File =
        temporaryFolder.newFile().apply { writeText(content) }
}
