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

    @Test
    fun `given artifact customer data, when text is redacted, then common pii is removed`() {
        val redactor = WooAiSmokeRedactor(
            siteUrl = "",
            username = "",
            appPassword = "",
        )

        val redacted = redactor.redact(
            """
            {
              "id": 12345,
              "product_id": 67890,
              "first_name": "Ada",
              "last_name": "Lovelace",
              "name": "Ada Lovelace",
              "email": "ada@example.com",
              "phone": "555-1234",
              "billing": {
                "address_1": "123 Main Street",
                "address_2": "Suite 4",
                "city": "Portland",
                "state": "OR",
                "postcode": 97201,
                "country": "US",
                "company": "Analytical Engines"
              },
              "shipping_phone": "+1 (503) 555-0188",
              "notes": "Alternate contact is contact@example.net or 415-555-0199."
            }
            """.trimIndent()
        )

        assertThat(redacted)
            .doesNotContain("Ada")
            .doesNotContain("Lovelace")
            .doesNotContain("ada@example.com")
            .doesNotContain("555-1234")
            .doesNotContain("123 Main Street")
            .doesNotContain("Suite 4")
            .doesNotContain("Portland")
            .doesNotContain("97201")
            .doesNotContain("Analytical Engines")
            .doesNotContain("+1 (503) 555-0188")
            .doesNotContain("contact@example.net")
            .doesNotContain("415-555-0199")
            .contains("\"id\": 12345")
            .contains("\"product_id\": 67890")
            .contains("[REDACTED]")
    }
}
