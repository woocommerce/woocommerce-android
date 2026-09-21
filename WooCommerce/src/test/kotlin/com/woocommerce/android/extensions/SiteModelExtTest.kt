package com.woocommerce.android.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel

class SiteModelExtTest {
    @Test
    fun `given HTTP admin and login URLs, when resolving URLs, then upgrade them to HTTPS`() {
        val site = SiteModel().apply {
            adminUrl = "http://example.com/wp-admin"
            loginUrl = "http://example.com/wp-login.php"
        }

        assertThat(site.adminUrlOrDefault).isEqualTo("https://example.com/wp-admin")
        assertThat(site.loginUrlOrDefault).isEqualTo("https://example.com/wp-login.php")
    }

    @Test
    fun `given scheme-less admin and login URLs, when resolving URLs, then preserve their values`() {
        val site = SiteModel().apply {
            adminUrl = "example.com/wp-admin"
            loginUrl = "example.com/wp-login.php"
        }

        assertThat(site.adminUrlOrDefault).isEqualTo("example.com/wp-admin")
        assertThat(site.loginUrlOrDefault).isEqualTo("example.com/wp-login.php")
    }

    @Test
    fun `given invalid admin and login URLs, when resolving URLs, then preserve their values`() {
        val site = SiteModel().apply {
            adminUrl = "not a url"
            loginUrl = "ftp://example.com/wp-login.php"
        }

        assertThat(site.adminUrlOrDefault).isEqualTo("not a url")
        assertThat(site.loginUrlOrDefault).isEqualTo("ftp://example.com/wp-login.php")
    }

    @Test
    fun `given empty site URL and no explicit URLs, when resolving defaults, then preserve fallback paths`() {
        val site = SiteModel().apply {
            url = ""
        }

        assertThat(site.adminUrlOrDefault).isEqualTo("/wp-admin")
        assertThat(site.loginUrlOrDefault).isEqualTo("/wp-login.php")
    }
}
