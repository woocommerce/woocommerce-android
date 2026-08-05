package com.woocommerce.android.ui.login.auto

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@Suppress("FunctionNaming")
class AutoLoginRequestParserTest {
    private val parser = AutoLoginRequestParser()

    @Test
    fun `given either typed route, when parsed, then credentials are retained but redacted`() {
        AutoLoginConnection.entries.forEach { connection ->
            val result = parser.parse(payload(connection.name))

            val request = (result as AutoLoginRequestParseResult.Success).request
            assertThat(request.connection).isEqualTo(connection)
            assertThat(request.credentials.username).isEqualTo(USERNAME)
            assertThat(request.credentials.password).isEqualTo(PASSWORD)
            assertThat(request.toString()).doesNotContain(SITE_URL, USERNAME, PASSWORD)
            assertThat(request.credentials.toString()).doesNotContain(USERNAME, PASSWORD)
        }
    }

    @Test
    fun `given insecure malformed duplicate or extra input, when parsed, then it is rejected`() {
        val invalidPayloads = listOf(
            payload("WP_API", siteUrl = "http://store.example"),
            "{}",
            payload("WP_API", extra = ""","password":"duplicate""""),
            payload("WP_API", extra = ""","unexpected":"value"""")
        )

        invalidPayloads.forEach {
            assertThat(parser.parse(it)).isEqualTo(AutoLoginRequestParseResult.Invalid)
        }
    }

    private fun payload(
        connection: String,
        siteUrl: String = SITE_URL,
        extra: String = ""
    ) = """
        {
          "connection":"$connection",
          "site_url":"$siteUrl",
          "username":"$USERNAME",
          "password":"$PASSWORD"
          $extra
        }
    """.trimIndent()

    private companion object {
        const val SITE_URL = "https://store.example/shop"
        const val USERNAME = "user-canary@example.test"
        const val PASSWORD = "password-canary-\$()-`-unicode-雪"
    }
}
