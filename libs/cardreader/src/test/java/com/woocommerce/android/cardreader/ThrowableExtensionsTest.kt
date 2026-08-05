package com.woocommerce.android.cardreader

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ThrowableExtensionsTest {
    @Test
    fun `given a throwable without a cause, when described, then only the throwable is rendered`() {
        // GIVEN
        val throwable = IllegalStateException("boom")

        // WHEN
        val description = throwable.describeWithCauses()

        // THEN
        assertThat(description).isEqualTo("java.lang.IllegalStateException: boom")
    }

    @Test
    fun `given a nested cause chain, when described, then every cause is rendered`() {
        // GIVEN
        val root = java.security.cert.CertificateException("untrusted root")
        val throwable = IllegalStateException("handshake failed", RuntimeException("tls", root))

        // WHEN
        val description = throwable.describeWithCauses()

        // THEN
        assertThat(description).isEqualTo(
            "java.lang.IllegalStateException: handshake failed" +
                " <- caused by: java.lang.RuntimeException: tls" +
                " <- caused by: java.security.cert.CertificateException: untrusted root"
        )
    }

    @Test
    fun `given a chain deeper than the max depth, when described, then the chain is truncated`() {
        // GIVEN
        var throwable = RuntimeException("level-0")
        repeat(5) { level -> throwable = RuntimeException("level-${level + 1}", throwable) }

        // WHEN
        val description = throwable.describeWithCauses(maxDepth = 2)

        // THEN
        assertThat(description).isEqualTo(
            "java.lang.RuntimeException: level-5 <- caused by: java.lang.RuntimeException: level-4"
        )
    }

    @Test
    fun `given a self referencing cause, when described, then the chain terminates`() {
        // GIVEN
        val throwable = object : RuntimeException("loop") {
            override val cause: Throwable get() = this
        }

        // WHEN
        val description = throwable.describeWithCauses()

        // THEN
        assertThat(description).isEqualTo(throwable.toString())
    }
}
