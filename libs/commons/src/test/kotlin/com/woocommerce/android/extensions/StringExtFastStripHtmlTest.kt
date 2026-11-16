package com.woocommerce.android.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StringExtFastStripHtmlTest {
    @Test
    fun `given an empty string, when html is stripped, then an empty string is returned`() {
        assertThat("".fastStripHtml()).isEmpty()
    }

    @Test
    fun `given plain text, when html is stripped, then the text is returned unchanged`() {
        assertThat("Blue cotton shirt".fastStripHtml()).isEqualTo("Blue cotton shirt")
    }

    @Test
    fun `given a single empty tag pair, when html is stripped, then an empty string is returned`() {
        assertThat("<p></p>".fastStripHtml()).isEmpty()
    }

    @Test
    fun `given only nested and self closing tags, when html is stripped, then an empty string is returned`() {
        assertThat("<div><span class=\"note\"></span><img src=\"a.png\" /></div>".fastStripHtml()).isEmpty()
    }

    @Test
    fun `given several empty paragraphs, when html is stripped, then no markup is returned`() {
        assertThat("<p></p><p></p>".fastStripHtml()).doesNotContain("<", ">").isBlank()
    }

    @Test
    fun `given tags around text, when html is stripped, then only the text is returned`() {
        assertThat("<strong>Leave</strong> at the <em>front door</em>".fastStripHtml())
            .isEqualTo("Leave at the front door")
    }

    @Test
    fun `given a tag with attributes spanning lines, when html is stripped, then only the text is returned`() {
        assertThat("<a\nhref=\"https://example.com\">Link</a>".fastStripHtml()).isEqualTo("Link")
    }

    @Test
    fun `given a single paragraph at the start, when html is stripped, then no line break is added`() {
        assertThat("<p>Only paragraph</p>".fastStripHtml()).isEqualTo("Only paragraph")
    }

    @Test
    fun `given a paragraph after text, when html is stripped, then a line break precedes the paragraph`() {
        assertThat("First line<p class=\"second\">Second line</p>".fastStripHtml())
            .isEqualTo("First line\nSecond line")
    }

    @Test
    fun `given br tags, when html is stripped, then each one becomes a line break`() {
        assertThat("One<br>Two<br/>Three<br />Four".fastStripHtml()).isEqualTo("One\nTwo\nThree\nFour")
    }

    @Test
    fun `given html entities, when html is stripped, then they are unescaped`() {
        assertThat("Salt &amp; pepper &lt;3 &quot;spicy&quot; &#8211; 5&euro;".fastStripHtml())
            .isEqualTo("Salt & pepper <3 \"spicy\" – 5€")
    }

    @Test
    fun `given entities inside tags, when html is stripped, then tags are removed and entities unescaped`() {
        assertThat("<b>Tom &amp; Jerry</b>".fastStripHtml()).isEqualTo("Tom & Jerry")
    }

    @Test
    fun `given text with trailing whitespace, when html is stripped, then the trailing whitespace is kept`() {
        assertThat("<p>Fragile </p>\n".fastStripHtml()).isEqualTo("Fragile \n")
    }

    @Test
    fun `given an html comment, when html is stripped, then the comment is removed`() {
        assertThat("Before<!-- hidden -->After".fastStripHtml()).isEqualTo("BeforeAfter")
    }
}
