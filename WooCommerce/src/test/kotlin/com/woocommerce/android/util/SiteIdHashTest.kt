package com.woocommerce.android.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SiteIdHashTest {
    @Test
    fun `given same remote id, when hashed, then produces same result`() {
        // WHEN
        val hashA = siteIdHash(123L)
        val hashB = siteIdHash(123L)

        // THEN
        assertThat(hashA).isEqualTo(hashB)
    }

    @Test
    fun `given different remote ids, when hashed, then produces different results`() {
        // WHEN
        val hashA = siteIdHash(123L)
        val hashB = siteIdHash(456L)

        // THEN
        assertThat(hashA).isNotEqualTo(hashB)
    }

    @Test
    fun `when hashed, then result does not contain raw remote id`() {
        // WHEN
        val hash = siteIdHash(987654321L)

        // THEN
        assertThat(hash).doesNotContain("987654321")
    }
}
