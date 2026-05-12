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
    fun `given legacy app login deep link with username, when parsed, then returns AppLogin Credentials`() {
        val raw = "woocommerce://app-login?siteUrl=https%3A%2F%2Fstore.example&username=admin"

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(
            QrLoginPayload.AppLogin.Credentials(siteUrl = "https://store.example", username = "admin")
        )
    }

    @Test
    fun `given legacy app login deep link with http siteUrl, when parsed, then returns AppLogin Credentials`() {
        val raw = "woocommerce://app-login?siteUrl=http%3A%2F%2Fstore.example&username=admin"

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(
            QrLoginPayload.AppLogin.Credentials(siteUrl = "http://store.example", username = "admin")
        )
    }

    @Test
    fun `given legacy app login deep link with wpcomEmail, when parsed, then returns AppLogin WpComEmail`() {
        val raw = "woocommerce://app-login?siteUrl=https%3A%2F%2Fstore.example&wpcomEmail=admin%40example.com"

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(
            QrLoginPayload.AppLogin.WpComEmail(siteUrl = "https://store.example", wpComEmail = "admin@example.com")
        )
    }

    @Test
    fun `given legacy app login with both wpcomEmail and username, when parsed, then wpcomEmail wins`() {
        val raw = "woocommerce://app-login?siteUrl=https%3A%2F%2Fstore.example" +
            "&wpcomEmail=admin%40example.com&username=admin"

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(
            QrLoginPayload.AppLogin.WpComEmail(siteUrl = "https://store.example", wpComEmail = "admin@example.com")
        )
    }

    @Test
    fun `given legacy app login deep link without username or wpcomEmail, when parsed, then returns Invalid`() {
        val raw = "woocommerce://app-login?siteUrl=https%3A%2F%2Fstore.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given legacy app login deep link with blank username, when parsed, then returns Invalid`() {
        val raw = "woocommerce://app-login?siteUrl=https%3A%2F%2Fstore.example&username="

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given legacy app login deep link without siteUrl, when parsed, then returns Invalid`() {
        val raw = "woocommerce://app-login?username=admin"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given legacy app login deep link with userinfo in siteUrl, when parsed, then returns Invalid`() {
        val raw = "woocommerce://app-login?siteUrl=https%3A%2F%2Fuser%3Apass%40store.example&username=admin"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given legacy app login deep link with uppercase host, when parsed, then returns AppLogin Credentials`() {
        val raw = "WOOCOMMERCE://APP-LOGIN?siteUrl=https%3A%2F%2Fstore.example&username=admin"

        assertThat(parser.parse(raw)).isInstanceOf(QrLoginPayload.AppLogin.Credentials::class.java)
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
    fun `given missing token but valid siteUrl, when parsed, then returns SiteUrl`() {
        val raw = "woocommerce://qr-login?siteUrl=https%3A%2F%2Fstore.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.SiteUrl(siteUrl = "https://store.example"))
    }

    @Test
    fun `given blank token but valid siteUrl, when parsed, then returns SiteUrl`() {
        val raw = "woocommerce://qr-login?token=&siteUrl=https%3A%2F%2Fstore.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.SiteUrl(siteUrl = "https://store.example"))
    }

    @Test
    fun `given missing siteUrl, when parsed, then returns Invalid`() {
        val raw = "woocommerce://qr-login?token=$VALID_TOKEN"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given missing token and non-https siteUrl, when parsed, then returns Invalid`() {
        // siteUrl validation is identical for Ticket and SiteUrl branches
        val raw = "woocommerce://qr-login?siteUrl=http%3A%2F%2Fstore.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given missing token and siteUrl with userinfo, when parsed, then returns Invalid`() {
        val raw = "woocommerce://qr-login?siteUrl=https%3A%2F%2Fuser%3Apass%40store.example"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given missing token and siteUrl with non-default port, when parsed, then preserves port`() {
        val raw = "woocommerce://qr-login?siteUrl=https%3A%2F%2Fstore.example%3A8443"

        assertThat(parser.parse(raw)).isEqualTo(
            QrLoginPayload.SiteUrl(siteUrl = "https://store.example:8443")
        )
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
    fun `given install QR url with utm source, when parsed, then returns InstallQrCode`() {
        val raw = "https://woocommerce.com/mobile/?utm_source=wc_onboarding_mobile_task"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.InstallQrCode)
    }

    @Test
    fun `given install QR url without query, when parsed, then returns InstallQrCode`() {
        assertThat(parser.parse("https://woocommerce.com/mobile/")).isEqualTo(QrLoginPayload.InstallQrCode)
        assertThat(parser.parse("https://woocommerce.com/mobile")).isEqualTo(QrLoginPayload.InstallQrCode)
    }

    @Test
    fun `given install QR url with mixed case host, when parsed, then returns InstallQrCode`() {
        assertThat(parser.parse("https://WooCommerce.com/mobile/")).isEqualTo(QrLoginPayload.InstallQrCode)
    }

    @Test
    fun `given other woocommerce com path, when parsed, then returns Invalid`() {
        assertThat(parser.parse("https://woocommerce.com/")).isEqualTo(QrLoginPayload.Invalid)
        assertThat(parser.parse("https://woocommerce.com/blog")).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given http install QR url, when parsed, then returns InstallQrCode`() {
        assertThat(parser.parse("http://woocommerce.com/mobile/")).isEqualTo(QrLoginPayload.InstallQrCode)
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

    @Test
    fun `given wp dot com magic link url, when parsed, then returns WpComMagicLinkUrl with original url`() {
        val raw = "https://wordpress.com/wp-login.php?action=magic-login&scheme=woocommerce&token=abc123"

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(QrLoginPayload.WpComMagicLinkUrl(url = raw))
    }

    @Test
    fun `given wp dot com magic link url with token containing reserved chars, when parsed, then preserves verbatim`() {
        // Real wp.com tokens contain percent-encoded reserved characters; the parser must not
        // transform them — the URL is forwarded to the browser as-is.
        val raw = "https://wordpress.com/wp-login.php" +
            "?token=%2Fu5iR4Bv3B%2BGLC5zt1V89A%3D%3D%3AqkpOuOFo9IeVclfMciTlEw%3D%3D" +
            "&action=magic-login&scheme=woocommerce"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.WpComMagicLinkUrl(url = raw))
    }

    @Test
    fun `given wp dot com magic link url with flow, when parsed, then preserves entire url`() {
        val raw = "https://wordpress.com/wp-login.php" +
            "?action=magic-login&scheme=woocommerce&token=abc&flow=jetpack-connection"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.WpComMagicLinkUrl(url = raw))
    }

    @Test
    fun `given wp dot com magic link url with mixed case host, when parsed, then returns WpComMagicLinkUrl`() {
        val raw = "https://WordPress.com/wp-login.php?action=magic-login&scheme=woocommerce&token=abc"

        assertThat(parser.parse(raw)).isInstanceOf(QrLoginPayload.WpComMagicLinkUrl::class.java)
    }

    @Test
    fun `given wp dot com magic link url with reordered query params, when parsed, then returns WpComMagicLinkUrl`() {
        val raw = "https://wordpress.com/wp-login.php?token=abc&scheme=woocommerce&action=magic-login"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.WpComMagicLinkUrl(url = raw))
    }

    @Test
    fun `given wp dot com url with scheme wordpress, when parsed, then returns Invalid`() {
        // Intended for the WordPress app — must not be silently consumed by us.
        val raw = "https://wordpress.com/wp-login.php?action=magic-login&scheme=wordpress&token=abc"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given wp dot com url with non magic-login action, when parsed, then returns Invalid`() {
        val raw = "https://wordpress.com/wp-login.php?action=lostpassword&scheme=woocommerce&token=abc"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given wp dot com magic link url without token, when parsed, then returns Invalid`() {
        val raw = "https://wordpress.com/wp-login.php?action=magic-login&scheme=woocommerce"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given wp dot com magic link url with blank token, when parsed, then returns Invalid`() {
        val raw = "https://wordpress.com/wp-login.php?action=magic-login&scheme=woocommerce&token="

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given http wp dot com magic link url, when parsed, then returns Invalid`() {
        val raw = "http://wordpress.com/wp-login.php?action=magic-login&scheme=woocommerce&token=abc"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given wp dot com url with non wp-login path, when parsed, then returns Invalid`() {
        val raw = "https://wordpress.com/wp-admin/login.php?action=magic-login&scheme=woocommerce&token=abc"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given qr-login deep link with token and encrypted but no siteUrl, when parsed, then returns WpComToken`() {
        val token = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef:" +
            "fedcba9876543210fedcba9876543210"
        val encrypted = "AAECAwQFBgcICQoLDA0ODw"
        val raw = "woocommerce://qr-login?token=$token&encrypted=$encrypted"

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(QrLoginPayload.WpComToken(token = token, encrypted = encrypted))
    }

    @Test
    fun `given qr-login deep link with token, encrypted, and siteUrl, when parsed, then prefers self-hosted Ticket`() {
        val raw = "woocommerce://qr-login?token=$VALID_TOKEN&siteUrl=https%3A%2F%2Fexample.com" +
            "&encrypted=AAECAwQFBgcICQoLDA0ODw"

        val result = parser.parse(raw)

        assertThat(result).isEqualTo(
            QrLoginPayload.Ticket(token = VALID_TOKEN, siteUrl = "https://example.com")
        )
    }

    @Test
    fun `given qr-login deep link with token but no encrypted and no siteUrl, when parsed, then returns Invalid`() {
        val raw = "woocommerce://qr-login?token=$VALID_TOKEN"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    @Test
    fun `given qr-login deep link with encrypted but no token and no siteUrl, when parsed, then returns Invalid`() {
        val raw = "woocommerce://qr-login?encrypted=AAECAwQFBgcICQoLDA0ODw"

        assertThat(parser.parse(raw)).isEqualTo(QrLoginPayload.Invalid)
    }

    private companion object {
        const val VALID_TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCD"
    }
}
