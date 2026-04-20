package org.wordpress.android.fluxc.persistence

import android.app.Application
import android.database.sqlite.SQLiteBlobTooBigException
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
internal class LargeCursorWindowRoomIntegrationTest {
    private val appContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val defaultDb = DatabaseTestRule(appContext)

    @get:Rule
    val enlargedDb = DatabaseTestRule(appContext, LargeCursorWindowOpenHelperFactory(TEN_MEGABYTES))

    @Test
    fun `given default open helper factory, when product row exceeds default window, then query throws`() {
        runBlocking { defaultDb.db.productsDao.upsertProduct(hugeProduct()) }

        assertThatThrownBy {
            runBlocking { defaultDb.db.productsDao.fetchAllProducts() }
        }.isInstanceOf(SQLiteBlobTooBigException::class.java)
    }

    @Test
    fun `given enlarged cursor window factory, when product row exceeds default window, then query succeeds`() {
        runBlocking { enlargedDb.db.productsDao.upsertProduct(hugeProduct()) }

        val products = runBlocking { enlargedDb.db.productsDao.fetchAllProducts() }

        assertThat(products).hasSize(1)
        assertThat(products.first().description.length).isEqualTo(HUGE_DESCRIPTION_LENGTH)
    }

    private fun hugeProduct() = WCProductModel(
        localSiteId = LocalId(SITE_ID),
        remoteId = RemoteId(1L),
        name = "huge",
        description = "x".repeat(HUGE_DESCRIPTION_LENGTH)
    )

    private suspend fun ProductsDao.fetchAllProducts() = getProducts(
        localSiteId = SITE_ID,
        status = null,
        stockStatus = null,
        type = null,
        category = null,
        excludeSampleProducts = false,
        limit = null,
        excludedProductIds = emptyList(),
        sortType = ProductSorting.DATE_ASC
    )

    companion object {
        private const val SITE_ID = 1
        private const val HUGE_DESCRIPTION_LENGTH = 3 * 1024 * 1024 // 3 MB — over the default ~2 MB window
        private const val TEN_MEGABYTES = 10L * 1024L * 1024L
    }
}
