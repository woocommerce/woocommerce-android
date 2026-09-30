package org.wordpress.android.fluxc.network

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseKind.UNEXPECTED_CONTENT

class UnexpectedStoreResponseClassifierTest {
    @Test
    fun `given a JSON object with a 200 status, when classified, then it is not unexpected`() {
        val kind = classify(statusCode = 200, contentType = JSON, body = """{"name":"Store"}""")

        assertThat(kind).isNull()
    }

    @Test
    fun `given a JSON critical error with a 500 status, when classified, then it has an unacceptable status code`() {
        val kind = classify(
            statusCode = 500,
            contentType = JSON,
            body = """{"code":"internal_server_error","message":"<p>There has been a critical error.</p>"}"""
        )

        assertThat(kind).isEqualTo(UNACCEPTABLE_STATUS_CODE)
    }

    @Test
    fun `given a JSON scalar with a 400 status, when classified, then it is not unexpected`() {
        val kind = classify(statusCode = 400, contentType = HTML, body = "0")

        assertThat(kind).isNull()
    }

    @Test
    fun `given an empty body with a 404 status, when classified, then it is not unexpected`() {
        val kind = classify(statusCode = 404, contentType = null, body = " ")

        assertThat(kind).isNull()
    }

    @Test
    fun `given an empty body with a 502 status, when classified, then it has an unacceptable status code`() {
        val kind = classify(statusCode = 502, contentType = null, body = "")

        assertThat(kind).isEqualTo(UNACCEPTABLE_STATUS_CODE)
    }

    @Test
    fun `given an empty body with a 429 status, when classified, then it has an unacceptable status code`() {
        val kind = classify(statusCode = 429, contentType = null, body = "")

        assertThat(kind).isEqualTo(UNACCEPTABLE_STATUS_CODE)
    }

    @Test
    fun `given an HTML page with a 200 status, when classified, then it has unexpected content`() {
        val kind = classify(statusCode = 200, contentType = HTML, body = CHALLENGE_PAGE)

        assertThat(kind).isEqualTo(UNEXPECTED_CONTENT)
    }

    @Test
    fun `given a PHP warning before JSON with a 200 status, when classified, then it has unexpected content`() {
        val kind = classify(
            statusCode = 200,
            contentType = JSON,
            body = "<br />\n<b>Warning</b>: Undefined variable \$id<br />\n{\"name\":\"Store\"}"
        )

        assertThat(kind).isEqualTo(UNEXPECTED_CONTENT)
    }

    @Test
    fun `given a plain-text rate limit with a 429 status, when classified, then it has an unacceptable status code`() {
        val kind = classify(statusCode = 429, contentType = "text/plain", body = "429 Too Many Requests")

        assertThat(kind).isEqualTo(UNACCEPTABLE_STATUS_CODE)
    }

    @Test
    fun `given a firewall page with a 403 status, when classified, then it has an unacceptable status code`() {
        val kind = classify(statusCode = 403, contentType = "text/html; charset=UTF-8", body = FIREWALL_PAGE)

        assertThat(kind).isEqualTo(UNACCEPTABLE_STATUS_CODE)
    }

    @Test
    fun `given an HTML page without a content type, when classified, then it is recognized from the body`() {
        val kind = classify(statusCode = 403, contentType = null, body = "\n  $FIREWALL_PAGE")

        assertThat(kind).isEqualTo(UNACCEPTABLE_STATUS_CODE)
    }

    @Test
    fun `given plain text with a 401 status, when classified, then it is not unexpected`() {
        val kind = classify(statusCode = 401, contentType = "text/plain", body = "Unauthorized")

        assertThat(kind).isNull()
    }

    private fun classify(statusCode: Int, contentType: String?, body: String) =
        UnexpectedStoreResponseClassifier.classify(statusCode, contentType, body)

    private companion object {
        const val JSON = "application/json; charset=UTF-8"
        const val HTML = "text/html; charset=UTF-8"
        const val CHALLENGE_PAGE = "<!DOCTYPE html><html><head><title>Just a moment...</title></head></html>"
        const val FIREWALL_PAGE = "<html><head><title>Access Denied - Website Firewall</title></head></html>"
    }
}
