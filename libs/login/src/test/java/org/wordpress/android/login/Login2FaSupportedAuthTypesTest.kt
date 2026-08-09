package org.wordpress.android.login

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.login.Login2FaFragment.SupportedAuthTypes

class Login2FaSupportedAuthTypesTest {
    @Test
    fun `given SMS auth type, when parsing, then return SMS`() {
        assertThat(SupportedAuthTypes.fromString("sms")).isEqualTo(SupportedAuthTypes.SMS)
    }
}
