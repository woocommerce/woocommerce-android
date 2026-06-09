package org.wordpress.android.fluxc.model

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("UnitTestNamingRule")
class SiteModelTest {
    /* isCIABSite */
    @Test
    fun `given commerce garden site, when checking isCIABSite, then returns true`() {
        val site = SiteModel().apply {
            setIsGardenSite(true)
            gardenName = SiteModel.CIAB_GARDEN_NAME
        }

        assertTrue(site.isCIABSite)
    }

    @Test
    fun `given non-commerce garden site, when checking isCIABSite, then returns false`() {
        val site = SiteModel().apply {
            setIsGardenSite(true)
            gardenName = "other"
        }

        assertFalse(site.isCIABSite)
    }

    @Test
    fun `given non-garden site, when checking isCIABSite, then returns false`() {
        val site = SiteModel().apply {
            setIsGardenSite(false)
        }

        assertFalse(site.isCIABSite)
    }
}
