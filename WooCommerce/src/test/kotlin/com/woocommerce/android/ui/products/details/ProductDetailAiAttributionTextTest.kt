package com.woocommerce.android.ui.products.details

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductDetailAiAttributionTextTest {
    @Test
    fun `given localized attribution HTML, when styled, then onVariant text precedes primary underlined link`() {
        // GIVEN
        val parsedHtml = AnnotatedString.fromHtml(
            "Powered by AI. <a href=''><u>Learn more</u></a>."
        )

        // WHEN
        val result = productDetailAiAttributionText(
            parsedHtml = parsedHtml,
            onVariantColor = ON_VARIANT_COLOR,
            linkColor = LINK_COLOR,
        )

        // THEN
        val linkStart = result.text.indexOf(LINK_TEXT)
        val linkEnd = linkStart + LINK_TEXT.length
        val onVariantStyle = result.spanStyles.single { it.item.color == ON_VARIANT_COLOR }
        val linkStyle = result.spanStyles.single { it.item.color == LINK_COLOR }
        val onVariantStyleIndex = result.spanStyles.indexOf(onVariantStyle)
        val linkStyleIndex = result.spanStyles.indexOf(linkStyle)
        assertThat(result.text).isEqualTo("Powered by AI. Learn more.")
        assertThat(onVariantStyle.start).isZero()
        assertThat(onVariantStyle.end).isEqualTo(result.length)
        assertThat(linkStyle.start).isEqualTo(linkStart)
        assertThat(linkStyle.end).isEqualTo(linkEnd)
        assertThat(linkStyle.item.textDecoration).isEqualTo(TextDecoration.Underline)
        assertThat(linkStyleIndex).isGreaterThan(onVariantStyleIndex)
        assertThat(linkStyle.start).isGreaterThan(0)
        assertThat(linkStyle.end).isLessThan(result.length)
    }

    @Test
    fun `given RTL attribution HTML, when styled, then localized order and link range are preserved`() {
        // GIVEN
        val prefix = "مدعوم من الذكاء الاصطناعي. "
        val link = "تعرّف على المزيد"
        val suffix = "."

        // WHEN
        val result = productDetailAiAttributionText(
            parsedHtml = givenParsedAttribution(prefix, link, suffix),
            onVariantColor = ON_VARIANT_COLOR,
            linkColor = LINK_COLOR,
        )

        // THEN
        val linkStyle = result.spanStyles.single { it.item.color == LINK_COLOR }
        assertThat(result.text).isEqualTo(prefix + link + suffix)
        assertThat(result.text.substring(linkStyle.start, linkStyle.end)).isEqualTo(link)
        assertThat(linkStyle.item.textDecoration).isEqualTo(TextDecoration.Underline)
    }

    private fun givenParsedAttribution(
        prefix: String,
        link: String,
        suffix: String,
    ): AnnotatedString = buildAnnotatedString {
        append(prefix)
        pushLink(LinkAnnotation.Url(url = "learn-more"))
        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
            append(link)
        }
        pop()
        append(suffix)
    }

    private companion object {
        const val LINK_TEXT = "Learn more"
        val ON_VARIANT_COLOR = Color(0x991E1E1E)
        val LINK_COLOR = Color(0xFF720EEC)
    }
}
