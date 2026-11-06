package com.woocommerce.android.ui.login.storesuggestion

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SuggestedHostMatcherTest {
    @Test
    fun `given submitted and final urls, when isSameHost, then only the same host ignoring www matches`() {
        val cases = listOf(
            Triple("https://store.com", "https://store.com", true),
            Triple("store.com", "https://store.com", true),
            Triple("store.com", "https://www.store.com", true),
            Triple("www.store.com", "https://store.com", true),
            Triple("store.com", "https://store.com/shop", true),
            Triple("store.com", "https://evil.com", false),
            Triple("store.com", "https://shop.store.com", false),
            Triple("store.com", "https://evilstore.com", false),
            Triple("store.com", "https://", false),
            Triple("https://", "https://store.com", false),
        )

        cases.forEach { (submitted, final, expected) ->
            assertThat(SuggestedHostMatcher.isSameHost(submittedUrl = submitted, finalUrl = final))
                .describedAs("$submitted -> $final")
                .isEqualTo(expected)
        }
    }
}
