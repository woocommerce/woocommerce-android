package com.woocommerce.android.ui.login.sitecredentials

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ApplicationPasswordSiteMatcherTest {
    @Test
    fun `given the same URL, when isWithinSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, SITE_URL)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a candidate with a different scheme, when isWithinSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "http://site.example")

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a candidate with a port, when isWithinSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "https://site.example:8443")

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a candidate with a path, when isWithinSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(
            SITE_URL,
            "https://site.example/wp/wp-admin/authorize-application.php?app_name=woo"
        )

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a candidate with a www prefix, when isWithinSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "https://www.site.example")

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a site address with a www prefix, when isWithinSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite("https://www.site.example", SITE_URL)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a candidate with an uppercase host, when isWithinSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "https://WWW.Site.EXAMPLE")

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a site address without a scheme, when isWithinSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite("site.example", SITE_URL)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a site address with surrounding whitespace, when isWithinSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite("  $SITE_URL  ", SITE_URL)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a candidate on another host, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "https://other.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a lookalike candidate host, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "https://site.example.evil.test")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate on a subdomain, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "https://shop.site.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate with a doubled www prefix, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "https://www.www.site.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a null candidate, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, null)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a blank candidate, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "   ")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a blank site address, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite("", SITE_URL)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate with a non-http scheme, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "ftp://site.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate with a username, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "https://merchant@site.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate with a username and password, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "https://merchant:secret@site.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate whose user info hides another host, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "https://site.example@other.example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given an unparseable candidate, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "https://")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a candidate with an invalid host, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(SITE_URL, "https://site example")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given an approval page at the site root, when approvalSiteUrl, then returns the site address`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.approvalSiteUrl(
            "https://wp.site.example/wp-admin/authorize-application.php?app_name=woo&success_url=woocommerce://login"
        )

        // THEN
        assertThat(result).isEqualTo("https://wp.site.example")
    }

    @Test
    fun `given an approval page in a subfolder install, when approvalSiteUrl, then keeps the subfolder`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.approvalSiteUrl(
            "https://site.example/wp/wp-admin/authorize-application.php"
        )

        // THEN
        assertThat(result).isEqualTo("https://site.example/wp")
    }

    @Test
    fun `given a renamed admin directory, when approvalSiteUrl, then drops it`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.approvalSiteUrl(
            "https://site.example/private-admin/authorize-application.php"
        )

        // THEN
        assertThat(result).isEqualTo(SITE_URL)
    }

    @Test
    fun `given a URL that isn't the approval page, when approvalSiteUrl, then returns null`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.approvalSiteUrl("https://site.example/ea-authorize/")

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given the approval page without an admin directory, when approvalSiteUrl, then returns null`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.approvalSiteUrl("https://site.example/authorize-application.php")

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given no URL, when approvalSiteUrl, then returns null`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.approvalSiteUrl(null)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given a sibling site on the same host, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(
            "https://network.example/shop",
            "https://network.example/evil"
        )

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given the parent of the site path, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(
            "https://network.example/shop",
            "https://network.example"
        )

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a page below the site path, when isWithinSite, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(
            "https://network.example/shop/",
            "https://network.example/Shop/wp-admin/authorize-application.php"
        )

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a path that only shares a prefix, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(
            "https://network.example/shop",
            "https://network.example/shopper"
        )

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given the same address with a trailing slash and www, when isSameSiteAddress, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSiteAddress(
            "https://site.example/shop",
            "http://www.site.example/shop/"
        )

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a deeper path, when isSameSiteAddress, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isSameSiteAddress(SITE_URL, "$SITE_URL/wp")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given extra path after the approval page, when isApprovalPage, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isApprovalPage(
            "https://site.example/wp-admin/authorize-application.php/x"
        )

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given an encoded approval page name, when isApprovalPage, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isApprovalPage(
            "https://site.example/wp-admin/authorize-application%2Ephp"
        )

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a login page, when isApprovalPage, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isApprovalPage("https://site.example/wp-login.php?redirect_to=x")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given extra path after the approval page, when approvalSiteUrl, then returns the site address`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.approvalSiteUrl(
            "https://site.example/wp-admin/authorize-application.php/x/y"
        )

        // THEN
        assertThat(result).isEqualTo(SITE_URL)
    }

    @Test
    fun `given an encoded slash before the approval page, when isApprovalPage, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isApprovalPage(
            "https://site.example/wp-admin%2Fauthorize-application.php"
        )

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given an encoded slash after the approval page, when isApprovalPage, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isApprovalPage(
            "https://site.example/wp-admin/authorize-application.php%2Fx"
        )

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given encoded dot segments that leave the site path, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(
            "https://network.example/evil",
            "https://network.example/evil%2F..%2Fshop/wp-admin/authorize-application.php"
        )

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given a backslash that leaves the site path, when isWithinSite, then returns false`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isWithinSite(
            "https://network.example/evil",
            "https://network.example/evil%5C..%5Cshop/wp-admin/authorize-application.php"
        )

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given an encoded slash before the approval page, when approvalSiteUrl, then returns the site address`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.approvalSiteUrl(
            "https://site.example/shop/wp-admin%2Fauthorize-application.php"
        )

        // THEN
        assertThat(result).isEqualTo("https://site.example/shop")
    }

    @Test
    fun `given dot segments after the approval page, when isApprovalPage, then returns true`() {
        // WHEN
        val result = ApplicationPasswordSiteMatcher.isApprovalPage(
            "https://site.example/wp-admin/authorize-application.php%2F..%2Fx"
        )

        // THEN
        assertThat(result).isTrue()
    }

    private companion object {
        const val SITE_URL = "https://site.example"
    }
}
