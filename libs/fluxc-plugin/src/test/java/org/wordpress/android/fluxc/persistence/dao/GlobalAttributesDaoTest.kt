package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
@Suppress("UnitTestNamingRule")
class GlobalAttributesDaoTest {
    private lateinit var dao: GlobalAttributesDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        dao = databaseRule.db.globalAttributesDao
    }

    @Test
    fun `when replaceAllForSite called, then inserts attributes for site`() = runTest {
        val attributes = listOf(colorAttr1Site1, sizeAttr1Site1)

        dao.replaceAllForSite(siteId1, attributes)
        val retrieved = dao.getAttributesForSite(siteId1)

        assertThat(retrieved).containsExactlyInAnyOrderElementsOf(attributes)
    }

    @Test
    fun `given existing attributes, when replaceAllForSite called, then deletes attributes for site first`() = runTest {
        val existingAttributes = listOf(colorAttr1Site1, sizeAttr1Site1)
        dao.replaceAllForSite(siteId1, existingAttributes)
        val attributes = listOf(colorAttr2Site1, sizeAttr2Site1)

        dao.replaceAllForSite(siteId1, attributes)
        val retrieved = dao.getAttributesForSite(siteId1)

        assertThat(retrieved).containsExactlyInAnyOrderElementsOf(attributes)
    }

    @Test
    fun `given multiple sites, when getAttributesForSite called, then returns specific site attributes`() = runTest {
        val attrsSite1 = listOf(colorAttr1Site1, sizeAttr1Site1)
        val attrsSite2 = listOf(colorAttr1Site2)
        dao.replaceAllForSite(siteId1, attrsSite1)
        dao.replaceAllForSite(siteId2, attrsSite2)

        val retrievedSite1 = dao.getAttributesForSite(siteId1)
        val retrievedSite2 = dao.getAttributesForSite(siteId2)

        assertThat(retrievedSite1).containsExactlyElementsOf(attrsSite1)
        assertThat(retrievedSite2).containsExactlyElementsOf(attrsSite2)
    }

    companion object {
        private val siteId1 = LocalId(1)
        private val siteId2 = LocalId(2)
        private val colorAttrId1 = RemoteId(1)
        private val colorAttrId2 = RemoteId(2)
        private val sizeAttrId1 = RemoteId(3)
        private val sizeAttrId2 = RemoteId(4)

        private val colorAttr1Site1 = WCGlobalAttributeModel(
            siteId = siteId1,
            remoteId = colorAttrId1,
            name = "Color",
            slug = "pa_color_1",
            type = "select",
            orderBy = "menu_order",
            hasArchives = true
        )

        private val colorAttr2Site1 = WCGlobalAttributeModel(
            siteId = siteId1,
            remoteId = colorAttrId2,
            name = "Color",
            slug = "pa_color_2",
            type = "select",
            orderBy = "menu_order",
            hasArchives = true
        )

        private val sizeAttr1Site1 = WCGlobalAttributeModel(
            siteId = siteId1,
            remoteId = sizeAttrId1,
            name = "Size",
            slug = "pa_size_1",
            type = "select",
            orderBy = "menu_order",
            hasArchives = false
        )

        private val sizeAttr2Site1 = WCGlobalAttributeModel(
            siteId = siteId1,
            remoteId = sizeAttrId2,
            name = "Size",
            slug = "pa_size_2",
            type = "select",
            orderBy = "menu_order",
            hasArchives = false
        )

        private val colorAttr1Site2 = WCGlobalAttributeModel(
            siteId = siteId2,
            remoteId = colorAttrId1,
            name = "Color",
            slug = "pa_color_1",
            type = "select",
            orderBy = "menu_order",
            hasArchives = true
        )
    }
}
