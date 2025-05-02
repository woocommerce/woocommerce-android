package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.wc.product.ProductTestUtils

@RunWith(RobolectricTestRunner::class)
class ProductTagsDaoTest {

    private val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }
    private lateinit var sut: ProductTagsDao
    private lateinit var database: WCAndroidDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sut = database.productTagsDao
    }

    @Test
    fun testInsertOrUpdateProductTag() = runTest {
        val tagModel = ProductTestUtils.generateProductTags(site.id)[0]
        assertNotNull(tagModel)

        // Test inserting a product tag
        sut.upsertProductTag(tagModel)

        var savedTagList = sut.getProductTags(site.id)
        assertThat(savedTagList).isEqualTo(listOf(tagModel))

        // Test updating the same product tag
        val updated = tagModel.copy(name = "Tag update")
        sut.upsertProductTag(updated)

        savedTagList = sut.getProductTags(site.id)
        assertThat(savedTagList).isEqualTo(listOf(updated))
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }
}
