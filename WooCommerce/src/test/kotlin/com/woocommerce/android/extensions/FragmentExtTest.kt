package com.woocommerce.android.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Unit tests for the nav-result staleness logic that guards against a consumed result being replayed after
 * process death (WOOMOB-3275). The full observer/restore behaviour needs an Android runtime; these cover the
 * pure envelope + token-comparison logic.
 */
class FragmentExtTest {
    @Test
    fun `given an envelope from the current process, when checked, then it is not stale`() {
        val result: Any = NavResultEnvelope(session = navResultSessionId, value = "payload")

        assertThat(result.isStaleNavResult()).isFalse()
    }

    @Test
    fun `given an envelope from a different process, when checked, then it is stale`() {
        val result: Any = NavResultEnvelope(session = "a-previous-process-token", value = "payload")

        assertThat(result.isStaleNavResult()).isTrue()
    }

    @Test
    fun `given a non-envelope legacy value, when checked, then it is not stale`() {
        val result: Any = "raw-legacy-value"

        assertThat(result.isStaleNavResult()).isFalse()
    }

    @Test
    fun `given an envelope, when unwrapped, then the inner value is returned`() {
        val payload = "payload"
        val result: Any = NavResultEnvelope(session = navResultSessionId, value = payload)

        assertThat(result.unwrapNavResult()).isEqualTo(payload)
    }

    @Test
    fun `given an envelope wrapping null, when unwrapped, then null is returned`() {
        val result: Any = NavResultEnvelope(session = navResultSessionId, value = null)

        assertThat(result.unwrapNavResult()).isNull()
    }

    @Test
    fun `given a non-envelope legacy value, when unwrapped, then it is returned as-is`() {
        val result: Any = "raw-legacy-value"

        assertThat(result.unwrapNavResult()).isEqualTo("raw-legacy-value")
    }
}
