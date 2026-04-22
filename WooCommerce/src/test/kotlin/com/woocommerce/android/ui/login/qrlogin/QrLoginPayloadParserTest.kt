package com.woocommerce.android.ui.login.qrlogin

import com.google.gson.Gson
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginPayloadParserTest : BaseUnitTest() {
    private val parser = QrLoginPayloadParser(Gson())

    @Test
    fun `given app_password payload, when parsed, then returns SiteAppPassword`() {
        val raw = """
            {"v":1,"type":"app_password","url":"https://store.example","username":"admin","password":"abcd efgh ijkl"}
        """.trimIndent()

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(
            QrLoginPayload.SiteAppPassword(
                siteUrl = "https://store.example",
                username = "admin",
                appPassword = "abcd efgh ijkl"
            )
        )
    }

    @Test
    fun `given wpcom_token payload, when parsed, then returns WpComToken`() {
        val raw = """{"v":1,"type":"wpcom_token","token":"abc123"}"""

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(QrLoginPayload.WpComToken(token = "abc123"))
    }

    @Test
    fun `given url_only payload, when parsed, then returns UrlOnly`() {
        val raw = """{"v":1,"type":"url_only","url":"https://store.example"}"""

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(QrLoginPayload.UrlOnly(siteUrl = "https://store.example"))
    }

    @Test
    fun `given bare url string, when parsed, then returns UrlOnly`() {
        val result = parser.parse("https://store.example")

        assertThat(result).isEqualTo(QrLoginPayload.UrlOnly(siteUrl = "https://store.example"))
    }

    @Test
    fun `given bare non-url string, when parsed, then returns Invalid`() {
        val result = parser.parse("not-a-url")

        assertThat(result).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given null raw payload, when parsed, then returns Invalid`() {
        val result = parser.parse(null)

        assertThat(result).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given blank raw payload, when parsed, then returns Invalid`() {
        val result = parser.parse("   ")

        assertThat(result).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given malformed JSON payload, when parsed, then returns Invalid`() {
        val result = parser.parse("{not json}")

        assertThat(result).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given unsupported version, when parsed, then returns Invalid`() {
        val raw = """{"v":2,"type":"url_only","url":"https://store.example"}"""

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given missing version, when parsed, then returns Invalid`() {
        val raw = """{"type":"url_only","url":"https://store.example"}"""

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given unknown type, when parsed, then returns Invalid`() {
        val raw = """{"v":1,"type":"magic_link","url":"https://store.example"}"""

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given app_password payload without url, when parsed, then returns Invalid`() {
        val raw = """{"v":1,"type":"app_password","password":"abc"}"""

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given app_password payload with non-http url, when parsed, then returns Invalid`() {
        val raw = """{"v":1,"type":"app_password","url":"javascript:alert(1)","password":"abc"}"""

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given app_password payload without password, when parsed, then returns Invalid`() {
        val raw = """{"v":1,"type":"app_password","url":"https://store.example"}"""

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given app_password payload without username, when parsed, then username is null`() {
        val raw = """{"v":1,"type":"app_password","url":"https://store.example","password":"abc"}"""

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(
            QrLoginPayload.SiteAppPassword(
                siteUrl = "https://store.example",
                username = null,
                appPassword = "abc"
            )
        )
    }

    @Test
    fun `given wpcom_token payload without token, when parsed, then returns Invalid`() {
        val raw = """{"v":1,"type":"wpcom_token"}"""

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given url_only payload with invalid url, when parsed, then returns Invalid`() {
        val raw = """{"v":1,"type":"url_only","url":"ftp://store.example"}"""

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(QrLoginPayload.Invalid)
    }
}
