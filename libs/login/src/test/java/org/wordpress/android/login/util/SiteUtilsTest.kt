package org.wordpress.android.login.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel

class SiteUtilsTest {
    private val site = SiteModel().apply { url = "https://nami.lt" }

    @Test
    fun `given site stored without www, when matching the www address, then the site is found`() {
        val match = SiteUtils.getSiteByMatchingUrl(listOf(site), "www.nami.lt")

        assertThat(match).isSameAs(site)
    }

    @Test
    fun `given site stored with www, when matching the bare address, then the site is found`() {
        site.url = "https://www.nami.lt/"

        val match = SiteUtils.getSiteByMatchingUrl(listOf(site), "nami.lt")

        assertThat(match).isSameAs(site)
    }

    @Test
    fun `given site stored without www, when matching an upper case www address, then the site is found`() {
        val match = SiteUtils.getSiteByMatchingUrl(listOf(site), "WWW.nami.lt")

        assertThat(match).isSameAs(site)
    }

    @Test
    fun `given both www and bare variants stored, when matching, then the exact host wins`() {
        val wwwSite = SiteModel().apply { url = "https://www.nami.lt" }

        val match = SiteUtils.getSiteByMatchingUrl(listOf(wwwSite, site), "nami.lt")

        assertThat(match).isSameAs(site)
    }

    @Test
    fun `given a different host, when matching, then nothing is found`() {
        val match = SiteUtils.getSiteByMatchingUrl(listOf(site), "www.other.lt")

        assertThat(match).isNull()
    }
}
