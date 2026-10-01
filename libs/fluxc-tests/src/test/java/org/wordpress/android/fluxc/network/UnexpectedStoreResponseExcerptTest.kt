package org.wordpress.android.fluxc.network

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UnexpectedStoreResponseExcerptTest {
    @Test
    fun `given a web page, when the excerpt is built, then it has the title and the start of the visible text`() {
        val body = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Access Denied - Website Firewall</title>
                <style>body { color: red; }</style>
                <script>var blocked = true;</script>
            </head>
            <body>
                <!-- firewall v2 -->
                <h1>Blocked</h1>
                <p>This request was blocked by the website firewall.</p>
                <script>trackBlock();</script>
            </body>
            </html>
        """.trimIndent()

        val excerpt = UnexpectedStoreResponseExcerpt.from(body)

        assertThat(excerpt)
            .isEqualTo("Access Denied - Website Firewall | Blocked This request was blocked by the website firewall.")
    }

    @Test
    fun `given the page text starts with the title, when the excerpt is built, then the title is not repeated`() {
        val body = "<html><head><title>Just a moment...</title></head>" +
            "<body><h1>Just a moment...</h1><p>Checking your browser.</p></body></html>"

        val excerpt = UnexpectedStoreResponseExcerpt.from(body)

        assertThat(excerpt).isEqualTo("Just a moment... Checking your browser.")
    }

    @Test
    fun `given a page with HTML entities, when the excerpt is built, then they are decoded`() {
        val body = "<html><head><title>WordPress &rsaquo; Error</title></head>" +
            "<body><p>There has been a critical error on this website.</p></body></html>"

        val excerpt = UnexpectedStoreResponseExcerpt.from(body)

        assertThat(excerpt).isEqualTo("WordPress › Error | There has been a critical error on this website.")
    }

    @Test
    fun `given an invalid numeric entity, when the excerpt is built, then the text is kept as it is`() {
        val excerpt = UnexpectedStoreResponseExcerpt.from("<p>Error &#1234567; on this page</p>")

        assertThat(excerpt).isEqualTo("Error &#1234567; on this page")
    }

    @Test
    fun `given a page with emails and IP addresses, when the excerpt is built, then they are masked`() {
        val body = "<p>Contact admin@example.com. Your IP: 203.0.113.7, or 2001:db8::1 on IPv6.</p>"

        val excerpt = UnexpectedStoreResponseExcerpt.from(body)

        assertThat(excerpt).isEqualTo("Contact [email]. Your IP: [ip], or [ip] on IPv6.")
    }

    @Test
    fun `given a JSON critical error, when the excerpt is built, then the HTML in the message is removed`() {
        val body = """{"code":"internal_server_error","message":"<p>There has been a critical error.</p>"}"""

        val excerpt = UnexpectedStoreResponseExcerpt.from(body)

        assertThat(excerpt)
            .isEqualTo("""{"code":"internal_server_error","message":" There has been a critical error. "}""")
    }

    @Test
    fun `given a long page, when the excerpt is built, then it is cut to 300 characters`() {
        val body = "<p>${"Maintenance ".repeat(40)}</p>"

        val excerpt = UnexpectedStoreResponseExcerpt.from(body)

        assertThat(excerpt).hasSize(300).endsWith("…")
    }

    @Test
    fun `given an email at the cut point, when the excerpt is built, then no part of it is kept`() {
        val body = "a".repeat(290) + " john.doe@example.com"

        val excerpt = UnexpectedStoreResponseExcerpt.from(body)

        assertThat(excerpt).doesNotContain("john")
    }

    @Test
    fun `given a script that is never closed, when the excerpt is built, then the rest of the page is left out`() {
        val body = "<p>Blocked</p><script>var page = 'not closed'; <p>Hidden</p>"

        val excerpt = UnexpectedStoreResponseExcerpt.from(body)

        assertThat(excerpt).isEqualTo("Blocked")
    }

    @Test
    fun `given a head that is never closed, when the excerpt is built, then the body text is kept`() {
        val body = "<html><head><title>Forbidden</title><body><p>Access denied.</p></body></html>"

        val excerpt = UnexpectedStoreResponseExcerpt.from(body)

        assertThat(excerpt).isEqualTo("Forbidden | Access denied.")
    }

    @Test
    fun `given a page without readable text, when the excerpt is built, then it is null`() {
        val excerpt = UnexpectedStoreResponseExcerpt.from("<html><head><script>run();</script></head></html>")

        assertThat(excerpt).isNull()
    }
}
