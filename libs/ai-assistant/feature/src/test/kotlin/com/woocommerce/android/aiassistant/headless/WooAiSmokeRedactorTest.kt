package com.woocommerce.android.aiassistant.headless

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooAiSmokeRedactorTest {
    @Test
    fun `given configured secrets, when text is redacted, then no secret remains`() {
        val redactor = WooAiSmokeRedactor(
            siteUrl = "https://store.example",
            username = "merchant@example.com",
            appPassword = "app password",
        )
        val basicValue = "Basic bWVyY2hhbnRAZXhhbXBsZS5jb206YXBwIHBhc3N3b3Jk"
        val jwtValue = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtZXJjaGFudCJ9.signature"

        val redacted = redactor.redact(
            "url=https://store.example user=merchant@example.com pass=app password " +
                "Authorization: $basicValue token=$jwtValue"
        )

        assertThat(redacted).doesNotContain("https://store.example")
        assertThat(redacted).doesNotContain("merchant@example.com")
        assertThat(redacted).doesNotContain("app password")
        assertThat(redacted).doesNotContain(basicValue)
        assertThat(redacted).doesNotContain(jwtValue)
        assertThat(redacted).contains("[REDACTED]")
    }
}
