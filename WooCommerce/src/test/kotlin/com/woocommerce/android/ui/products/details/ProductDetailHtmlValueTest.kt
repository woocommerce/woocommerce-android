package com.woocommerce.android.ui.products.details

import androidx.compose.ui.text.font.FontWeight
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductDetailHtmlValueTest {
    @Test
    fun `given formatted HTML, when parsed, then text keeps its styles`() {
        // WHEN
        val result = productDetailHtmlValue("Plain <b>bold</b>")

        // THEN
        assertThat(result.text).isEqualTo("Plain bold")
        assertThat(result.spanStyles.single().item.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `given list item starting with an aligned heading, when parsed, then plain text is returned`() {
        // GIVEN
        val html = "<ul>\n<li>\n<h3 style=\"text-align: center;\">Prima</h3>\n<p>Seconda</li>\n</ul>\n"

        // WHEN
        val result = productDetailHtmlValue(html)

        // THEN
        assertThat(result.text).contains("Prima").contains("Seconda")
        assertThat(result.paragraphStyles).isEmpty()
    }

    @Test
    fun `given list item starting with an aligned paragraph, when parsed, then plain text is returned`() {
        // WHEN
        val result = productDetailHtmlValue("<ul><li><p style=\"text-align:center\">Prima</p>Seconda</li></ul>")

        // THEN
        assertThat(result.text).contains("Prima").contains("Seconda")
        assertThat(result.paragraphStyles).isEmpty()
    }
}
