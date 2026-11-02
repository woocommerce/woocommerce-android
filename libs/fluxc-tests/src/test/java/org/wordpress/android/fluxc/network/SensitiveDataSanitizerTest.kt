package org.wordpress.android.fluxc.network

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SensitiveDataSanitizerTest {
    @Test
    fun `given secrets and API keys in key value pairs, when sanitizing, then their values are redacted`() {
        val text = "client_secret=abc123 api_key=def456 consumer_key=ck_1"

        val result = SensitiveDataSanitizer.sanitize(text)

        assertThat(result).isEqualTo("client_secret=[redacted] api_key=[redacted] consumer_key=[redacted]")
    }

    @Test
    fun `given secrets with spaces around the equals sign, after punctuation or with commas, when sanitizing, then their values are redacted`() {
        val text = "password = hunter2 Credentials(token=abc123) secret=abc,def"

        val result = SensitiveDataSanitizer.sanitize(text)

        assertThat(result).isEqualTo("password = [redacted] Credentials(token=[redacted] secret=[redacted]")
    }

    @Test
    fun `given secrets after a colon, when sanitizing, then their values are redacted`() {
        val text = "password: hunter2, API Key: \"abc def\", access_token:xyz"

        val result = SensitiveDataSanitizer.sanitize(text)

        assertThat(result).isEqualTo("password: [redacted] API Key: [redacted], access_token:[redacted]")
    }

    @Test
    fun `given secrets in JSON, when sanitizing, then only their values are redacted`() {
        val text = """{"client_secret":"abc","name":"Store"}"""

        val result = SensitiveDataSanitizer.sanitize(text)

        assertThat(result).isEqualTo("""{"client_secret":"[redacted]","name":"Store"}""")
    }

    @Test
    fun `given invisible characters, when sanitizing, then they are removed`() {
        val text = "Log​in fai‮led\u0000"

        val result = SensitiveDataSanitizer.sanitize(text)

        assertThat(result).isEqualTo("Login failed")
    }

    @Test
    fun `given an invisible character inside a secret key, when sanitizing, then the value is redacted`() {
        val text = "pass​word=hunter2"

        val result = SensitiveDataSanitizer.sanitize(text)

        assertThat(result).isEqualTo("password=[redacted]")
    }

    @Test
    fun `given text without secrets, when sanitizing, then it is kept`() {
        val text = "Checking WooCommerce API version failed for site 257941494"

        val result = SensitiveDataSanitizer.sanitize(text)

        assertThat(result).isEqualTo(text)
    }
}
