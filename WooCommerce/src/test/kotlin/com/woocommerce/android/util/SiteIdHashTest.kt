package com.woocommerce.android.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel

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

    @Test
    fun `given same store with different login types, when matched, then identities match in both directions`() {
        // GIVEN
        val wpComLogin = identity(siteId = 123L, url = "https://store.example.com")
        val siteCredentialsLogin = identity(siteId = 0L, url = "http://www.Store.example.com/")

        // WHEN
        val tabletOnSiteCredentials = siteCredentialsLogin.matches(wpComLogin.siteHash, wpComLogin.siteUrlHash)
        val tabletOnWpCom = wpComLogin.matches(siteCredentialsLogin.siteHash, siteCredentialsLogin.siteUrlHash)

        // THEN
        assertThat(tabletOnSiteCredentials).isTrue()
        assertThat(tabletOnWpCom).isTrue()
    }

    @Test
    fun `given different stores without a blog id, when matched, then identities do not match`() {
        // GIVEN
        val storeA = identity(siteId = 0L, url = "https://a.example.com")
        val storeB = identity(siteId = 0L, url = "https://b.example.com")

        // WHEN
        val result = storeA.matches(storeB.siteHash, storeB.siteUrlHash)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given paths that differ only by case, when matched, then identities do not match`() {
        // GIVEN
        val upperCasePath = identity(siteId = 0L, url = "https://example.com/Shop")
        val lowerCasePath = identity(siteId = 0L, url = "https://example.com/shop")

        // WHEN
        val result = upperCasePath.matches(lowerCasePath.siteHash, lowerCasePath.siteUrlHash)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given no blog id and a blank url, when building identity, then there is none`() {
        // WHEN
        val blank = site(siteId = 0L, url = "").pairingIdentity()
        val missing = SiteModel().pairingIdentity()

        // THEN
        assertThat(blank).isNull()
        assertThat(missing).isNull()
    }

    @Test
    fun `given a blog id and a blank url, when matched against missing url hash, then only blog id matches`() {
        // GIVEN
        val identity = identity(siteId = 123L, url = "")

        // WHEN
        val sameBlog = identity.matches(siteIdHash(123L), null)
        val otherBlog = identity.matches(siteIdHash(456L), null)

        // THEN
        assertThat(sameBlog).isTrue()
        assertThat(otherBlog).isFalse()
    }

    private fun identity(siteId: Long, url: String) = requireNotNull(site(siteId, url).pairingIdentity())

    private fun site(siteId: Long, url: String) = SiteModel().apply {
        this.siteId = siteId
        this.url = url
    }
}
