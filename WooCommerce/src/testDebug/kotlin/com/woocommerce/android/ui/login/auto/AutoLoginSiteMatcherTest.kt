package com.woocommerce.android.ui.login.auto

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel

@Suppress("FunctionNaming")
class AutoLoginSiteMatcherTest {
    private val matcher = AutoLoginSiteMatcher()

    @Test
    fun `when only host case trailing slash and default port differ, then the site matches`() {
        assertThat(
            matcher.matches(
                site("https://store.example:443/shop/"),
                "https://STORE.example/shop",
                SiteModel.ORIGIN_WPCOM_REST
            )
        ).isTrue()
    }

    @Test
    fun `when path origin or scheme differs, then the site does not match`() {
        val target = "https://store.example/shop"
        val cases = listOf(
            site("https://store.example/shopper") to target,
            site(target).apply { origin = SiteModel.ORIGIN_WPAPI } to target,
            site(target) to "http://store.example/shop"
        )

        cases.forEach { (site, request) ->
            assertThat(matcher.matches(site, request, SiteModel.ORIGIN_WPCOM_REST)).isFalse()
        }
    }

    private fun site(url: String) = SiteModel().apply {
        this.url = url
        origin = SiteModel.ORIGIN_WPCOM_REST
    }
}
