package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.AccountMapper
import org.wordpress.android.fluxc.persistence.AccountStorePersistence
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.wc.product.ProductTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class ProductTagsDaoTest {

    @Rule
    @JvmField
    val wcDatabaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext<Application>())

    @Rule
    @JvmField
    val wpDatabaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    private val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }
    private lateinit var sut: ProductTagsDao
    private lateinit var siteSqlUtils: SiteSqlUtils

    @Before
    fun setUp() {
        sut = wcDatabaseRule.db.productTagsDao
        siteSqlUtils = SiteSqlUtils(AccountStorePersistence(wpDatabaseRule.db, AccountMapper()))
    }

    @Test
    fun testInsertOrUpdateProductTag() = runTest {
        val tagModel = ProductTestUtils.generateProductTags(site.id)[0]
        assertNotNull(tagModel)

        // Test inserting a product tag
        sut.upsertProductTag(tagModel)

        var savedTagList = sut.getProductTags(site.localId())
        assertThat(savedTagList).isEqualTo(listOf(tagModel))

        // Test updating the same product tag
        val updated = tagModel.copy(name = "Tag update")
        sut.upsertProductTag(updated)

        savedTagList = sut.getProductTags(site.localId())
        assertThat(savedTagList).isEqualTo(listOf(updated))
    }

    @Test
    fun testGetProductTagsForSite() = runTest {
        val tagList = ProductTestUtils.generateProductTags(site.id)
        assertTrue(tagList.isNotEmpty())

        // Insert product tag list
        sut.upsertProductTags(tagList)

        // Get tag list for site and verify
        val savedTagListExists = sut.getProductTags(site.localId())
        assertEquals(tagList.size, savedTagListExists.size)

        // Get tag list for a site that does not exist
        val nonExistingSiteId = LocalId(400)
        val savedTagList = sut.getProductTags(nonExistingSiteId)
        assertEquals(0, savedTagList.size)
    }

    @Test
    fun testGetProductTagByName() = runTest {
        val tagList = ProductTestUtils.generateProductTags(site.id)
        assertTrue(tagList.isNotEmpty())

        // Insert product tag list
        sut.upsertProductTags(tagList)

        // Get tag by name and verify
        val firstTag = tagList.first()
        val savedTagExists = sut.getProductTag(site.localId(), name = firstTag.name)
        assertThat(savedTagExists).isEqualTo(firstTag)

        // Get tag for a name that does not exist
        val nonExistingTagName = "test"
        val savedTag = sut.getProductTag(site.localId(), name = nonExistingTagName)
        assertThat(savedTag).isNull()
    }

    @Test
    fun testGetProductTagsByNames() = runTest {
        val tagList = ProductTestUtils.generateProductTags(site.id)
        assertTrue(tagList.isNotEmpty())

        // Insert product tag list
        sut.upsertProductTags(tagList)

        // Get tags by list of name and verify
        val tagNames = tagList.map { it.name }.toList()
        val savedTagListExists = sut.getProductTags(site.localId(), names = tagNames)
        assertThat(savedTagListExists).containsExactlyInAnyOrderElementsOf(tagList)

        // Get tags for a name that does not exist
        val monExistingTagList = sut.getProductTags(site.localId(), names = listOf("test", "test1", "test2"))
        assertEquals(0, monExistingTagList.size)

        val savedTagList = sut.getProductTags(site.localId(), names = listOf(tagNames[0], tagNames[1], "test"))
        assertEquals(2, savedTagList.size)
    }

    @Test
    fun testDeleteProductTagsForSite() = runTest {
        val tags = ProductTestUtils.generateProductTags(site.id)

        sut.upsertProductTags(tags)

        // Verify product tags inserted
        var savedTags = sut.getProductTags(site.localId())
        assertEquals(tags.size, savedTags.size)

        // Delete tags for site and verify
        sut.deleteProductTagsForSite(site.localId())
        savedTags = sut.getProductTags(site.localId())
        assertEquals(0, savedTags.size)
    }

    @Test
    @Ignore("This test is ignored until SiteModel is moved to Room and foreign key constraints are added")
    fun testDeleteSiteDeletesProductTags() = runTest {
        val tags = ProductTestUtils.generateProductTags(site.id)
        assertTrue(tags.isNotEmpty())

        sut.upsertProductTags(tags)

        // Verify tags inserted
        var savedTags = sut.getProductTags(site.localId())
        assertEquals(tags.size, savedTags.size)

        // Delete site and verify tags are deleted via foreign key constraint
        siteSqlUtils.deleteSite(site)
        savedTags = sut.getProductTags(site.localId())
        assertEquals(0, savedTags.size)
    }
}
