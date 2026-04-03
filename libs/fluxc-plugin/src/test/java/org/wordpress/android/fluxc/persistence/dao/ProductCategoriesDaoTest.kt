package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting.NAME_ASC
import org.wordpress.android.fluxc.wc.product.ProductTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ProductCategoriesDaoTest {
    private lateinit var sut: ProductCategoriesDao

    val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        sut = databaseRule.db.productCategoriesDao
    }

    @Test
    fun testInsertOrUpdateProductCategory() = runTest {
        val category = ProductTestUtils.getProductCategories(site.id)[0]
        assertNotNull(category)

        // Test inserting a product category
        sut.upsertProductCategory(category)
        var savedCategory = sut.getProductCategory(site.localId(), category.remoteCategoryId)

        assertNotNull(savedCategory)
        assertEquals(category.remoteCategoryId, savedCategory.remoteCategoryId)
        assertEquals(category.name, savedCategory.name)
        assertEquals(category.slug, savedCategory.slug)
        assertEquals(category.parent, savedCategory.parent)
        assertEquals(category.localSiteId, savedCategory.localSiteId)

        // Test updating the same product category
        val updated = category.copy(name = "foo")
        sut.upsertProductCategory(updated)
        savedCategory = sut.getProductCategory(site.localId(), category.remoteCategoryId)

        assertNotNull(savedCategory)
        assertEquals(updated.name, savedCategory.name)
    }

    @Test
    fun testInsertOrUpdateProductCategories() = runTest {
        val productCategories = ProductTestUtils.getProductCategories(site.id)
        assertTrue(productCategories.isNotEmpty())

        // Insert all product categories
        sut.upsertProductCategories(productCategories)

        assertEquals(
            productCategories,
            sut.getProductCategories(
                siteId = site.localId(),
                sortType = NAME_ASC
            )
        )
    }

    @Test
    fun testGetProductCategoriesForSite() = runTest {
        val categories = ProductTestUtils.getProductCategories(site.id)
        assertTrue(categories.isNotEmpty())

        // Insert all product categories
        sut.upsertProductCategories(categories)

        // Get all product categories for site and verify
        val savedCategoriesExist = sut.getProductCategories(site.localId(), NAME_ASC)
        assertEquals(categories.size, savedCategoriesExist.size)

        // Get all product categories for a site that do not exist
        val savedCategories = sut.getProductCategories(siteId = LocalId(400), NAME_ASC)
        assertEquals(0, savedCategories.size)
    }

    @Test
    fun testDeleteAllProductCategories() = runTest {
        val categories = ProductTestUtils.getProductCategories(site.id)
        assertTrue(categories.isNotEmpty())
        sut.upsertProductCategories(categories)

        // Verify categories inserted
        var savedCategories = sut.getProductCategories(site.localId(), NAME_ASC)
        assertEquals(categories.size, savedCategories.size)

        // Delete all categories and verify
        sut.deleteAllProductCategories()
        savedCategories = sut.getProductCategories(site.localId(), NAME_ASC)
        assertEquals(0, savedCategories.size)
    }

    @Test
    fun testDeleteProductCategoriesForSite() = runTest {
        val localSiteId = LocalId(site.id)
        val categories = ProductTestUtils.getProductCategories(localSiteId.value)

        sut.upsertProductCategories(categories)

        // Verify categories inserted
        var savedCategories = sut.getProductCategories(localSiteId, NAME_ASC)

        assertEquals(categories.size, savedCategories.size)

        // Delete categories for site and verify
        sut.deleteProductCategoriesForSite(localSiteId)
        savedCategories = sut.getProductCategories(localSiteId, NAME_ASC)
        assertEquals(0, savedCategories.size)
    }
}
