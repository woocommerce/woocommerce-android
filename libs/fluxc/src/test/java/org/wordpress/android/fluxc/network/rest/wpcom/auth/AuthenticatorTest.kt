package org.wordpress.android.fluxc.network.rest.wpcom.auth

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthenticatorTest {
    @Test
    fun `given needs 2fa body with mixed types, when parsing, then return string types in order`() {
        val error = VolleyError(
            NetworkResponse(
                400,
                """{"error":"needs_2fa","two_step_supported_auth_types":["backup",1,"sms"]}""".toByteArray(),
                emptyMap(),
                false
            )
        )

        assertThat(Authenticator.volleyErrorToSupportedAuthTypes(error)).containsExactly("backup", "sms")
    }

    @Test
    fun `given needs 2fa body without supported types, when parsing, then return empty list`() {
        val error = VolleyError(
            NetworkResponse(400, """{"error":"needs_2fa"}""".toByteArray(), emptyMap(), false)
        )

        assertThat(Authenticator.volleyErrorToSupportedAuthTypes(error)).isEmpty()
    }

    @Test
    fun `given non JSON body, when parsing supported types, then return empty list`() {
        val error = VolleyError(NetworkResponse(400, "not json".toByteArray(), emptyMap(), false))

        assertThat(Authenticator.volleyErrorToSupportedAuthTypes(error)).isEmpty()
    }
}
