package com.woocommerce.android.ui.login.qrlogin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SecretTest {

    @Test
    fun `given secret, when toString called, then plaintext is not exposed`() {
        val secret = Secret("super-sensitive")

        assertThat(secret.toString()).doesNotContain("super-sensitive")
    }

    @Test
    fun `given secret, when reveal called, then returns original plaintext`() {
        val secret = Secret("ap-secret")

        assertThat(secret.reveal()).isEqualTo("ap-secret")
    }

    @Test
    fun `given two secrets with same value, when compared, then they are equal`() {
        val first = Secret("ap-secret")
        val second = Secret("ap-secret")

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `given two secrets with different values, when compared, then they are not equal`() {
        val first = Secret("ap-secret")
        val second = Secret("other-secret")

        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `given secret, when wiped, then reveal no longer returns the original plaintext`() {
        val secret = Secret("ap-secret")

        secret.wipe()

        assertThat(secret.reveal()).isNotEqualTo("ap-secret")
    }
}
