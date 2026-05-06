package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginPayloadParserTest : BaseUnitTest() {
    private val parser = QrLoginPayloadParser()

    @Test
    fun `given valid deep link, when parsed, then returns Ticket`() {
        val raw = "woocommerce://qr-login?token=$VALID_TOKEN&siteUrl=https%3A%2F%2Fstore.example"

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(
            QrLoginPayload.Ticket(token = VALID_TOKEN, siteUrl = "https://store.example")
        )
    }

    @Test
    fun `given uppercase scheme and host, when parsed, then returns Ticket`() {
        val raw = "WOOCOMMERCE://QR-LOGIN?token=$VALID_TOKEN&siteUrl=https%3A%2F%2Fstore.example"

        val result = parser.parse(raw)

        assertThat(result).isInstanceOf(QrLoginPayload.Ticket::class.java)
    }

    @Test
    fun `given trailing slash before query, when parsed, then returns Ticket`() {
        val raw = "woocommerce://qr-login/?token=$VALID_TOKEN&siteUrl=https%3A%2F%2Fstore.example"

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(
            QrLoginPayload.Ticket(token = VALID_TOKEN, siteUrl = "https://store.example")
        )
    }

    @Test
    fun `given prefix but no query, when parsed, then returns Invalid`() {
        assertThat(parser.parse("woocommerce://qr-login")).isEqualTo(QrLoginPayload.Invalid)
        assertThat(parser.parse("woocommerce://qr-login/")).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given null raw payload, when parsed, then returns Invalid`() {
        assertThat(parser.parse(null)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given blank raw payload, when parsed, then returns Invalid`() {
        assertThat(parser.parse("   ")).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given wrong scheme, when parsed, then returns Invalid`() {
        val raw = "https://qr-login?token=$VALID_TOKEN&siteUrl=https%3A%2F%2Fstore.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given wrong host, when parsed, then returns Invalid`() {
        val raw = "woocommerce://login?token=$VALID_TOKEN&siteUrl=https%3A%2F%2Fstore.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given missing token, when parsed, then returns Invalid`() {
        val raw = "woocommerce://qr-login?siteUrl=https%3A%2F%2Fstore.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given missing siteUrl, when parsed, then returns Invalid`() {
        val raw = "woocommerce://qr-login?token=$VALID_TOKEN"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given blank token, when parsed, then returns Invalid`() {
        val raw = "woocommerce://qr-login?token=&siteUrl=https%3A%2F%2Fstore.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given token shorter than 64 chars, when parsed, then returns Invalid`() {
        val raw = "woocommerce://qr-login?token=abc123&siteUrl=https%3A%2F%2Fstore.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given token longer than 512 chars, when parsed, then returns Invalid`() {
        val token = "a".repeat(513)
        val raw = "woocommerce://qr-login?token=$token&siteUrl=https%3A%2F%2Fstore.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given token with non-alphanumeric chars, when parsed, then returns Invalid`() {
        val token = "a".repeat(63) + "!"
        val raw = "woocommerce://qr-login?token=$token&siteUrl=https%3A%2F%2Fstore.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given non-https siteUrl, when parsed, then returns Invalid`() {
        val raw = "woocommerce://qr-login?token=$VALID_TOKEN&siteUrl=http%3A%2F%2Fstore.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given javascript scheme as siteUrl, when parsed, then returns Invalid`() {
        val raw = "woocommerce://qr-login?token=$VALID_TOKEN&siteUrl=javascript%3Aalert(1)"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given malformed URI, when parsed, then returns Invalid`() {
        val raw = "not a uri"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given long real-world token, when parsed, then returns Ticket with full token`() {
        val token = "8a3f5b9e2c4d7168f0a2b4c6e8f1a3b5c7d9e1f3a5b7c9d1e3f5a7b9c1d3e5f7"
        val raw = "woocommerce://qr-login?token=$token&siteUrl=https%3A%2F%2Feasyclothes.example"

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(
            QrLoginPayload.Ticket(token = token, siteUrl = "https://easyclothes.example")
        )
    }

    private companion object {
        const val VALID_TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCD"
    }
}
