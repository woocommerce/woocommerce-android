package com.woocommerce.android.aiassistant.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantMarkdownParserTest {
    @Test
    fun `given markdown link, when parsed, then label text and url annotation are emitted`() {
        // WHEN
        val parsed = AssistantMarkdownParser.parse(
            "Read [docs](https://woocommerce.com/documentation/woocommerce/mobile/) today."
        )

        // THEN
        assertThat(parsed.text).isEqualTo("Read docs today.")
        val link = parsed.getLinkAnnotations(0, parsed.length).single().item as LinkAnnotation.Url
        assertThat(link.url).isEqualTo("https://woocommerce.com/documentation/woocommerce/mobile/")
    }

    @Test
    fun `given markdown link with unsupported scheme, when parsed, then source text is preserved`() {
        // WHEN
        val parsed = AssistantMarkdownParser.parse("Open [docs](javascript:alert) today.")

        // THEN
        assertThat(parsed.text).isEqualTo("Open [docs](javascript:alert) today.")
        assertThat(parsed.getLinkAnnotations(0, parsed.length)).isEmpty()
    }

    @Test
    fun `given bold and italic markdown, when parsed, then markers are removed and styles are applied`() {
        // WHEN
        val parsed = AssistantMarkdownParser.parse("This is ***bold italic***, **bold**, and *italic*.")

        // THEN
        assertThat(parsed.text).isEqualTo("This is bold italic, bold, and italic.")
        assertThat(
            parsed.spanStyles.any {
                it.start == 8 &&
                    it.end == 19 &&
                    it.item.fontWeight == FontWeight.Bold &&
                    it.item.fontStyle == FontStyle.Italic
            }
        ).isTrue()
        assertThat(parsed.spanStyles.any { it.item.fontWeight == FontWeight.Bold }).isTrue()
        assertThat(parsed.spanStyles.any { it.item.fontStyle == FontStyle.Italic }).isTrue()
    }

    @Test
    fun `given malformed markdown, when parsed, then source text is preserved`() {
        // WHEN
        val parsed = AssistantMarkdownParser.parse("Broken [docs]( and **bold")

        // THEN
        assertThat(parsed.text).isEqualTo("Broken [docs]( and **bold")
        assertThat(parsed.getLinkAnnotations(0, parsed.length)).isEmpty()
        assertThat(parsed.spanStyles).isEmpty()
    }

    @Test
    fun `given link style, when parsed, then link annotation carries visible style`() {
        // GIVEN
        val linkStyles = TextLinkStyles(
            style = SpanStyle(
                color = Color(0xFF7F54B3),
                textDecoration = TextDecoration.Underline,
            )
        )

        // WHEN
        val parsed = AssistantMarkdownParser.parse("[docs](https://example.com)", linkStyles = linkStyles)

        // THEN
        val link = parsed.getLinkAnnotations(0, parsed.length).single().item as LinkAnnotation.Url
        assertThat(link.styles).isEqualTo(linkStyles)
    }

    @Test
    fun `given bold and italic markdown in link label, when parsed, then label markers are removed`() {
        // WHEN
        val parsed = AssistantMarkdownParser.parse("[***docs***](https://example.com)")

        // THEN
        assertThat(parsed.text).isEqualTo("docs")
        assertThat(parsed.getLinkAnnotations(0, parsed.length)).hasSize(1)
        assertThat(
            parsed.spanStyles.any {
                it.start == 0 &&
                    it.end == 4 &&
                    it.item.fontWeight == FontWeight.Bold &&
                    it.item.fontStyle == FontStyle.Italic
            }
        ).isTrue()
    }
}
