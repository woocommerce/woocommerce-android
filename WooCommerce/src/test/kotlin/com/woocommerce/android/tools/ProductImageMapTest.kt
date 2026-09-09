package com.woocommerce.android.tools

import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore

@OptIn(ExperimentalCoroutinesApi::class)
class ProductImageMapTest {
    private val site = SiteModel().apply { id = 1 }
    private val selectedSite: SelectedSite = mock {
        on { getIfExists() } doReturn site
    }
    private val product = WCProductModel(images = """[{"src":"$IMAGE_URL"}]""")
    private val productStore: WCProductStore = mock {
        on { getProductByRemoteId(site, PRODUCT_ID) } doReturn product
    }

    private val io = StandardTestDispatcher()
    private val caller = UnconfinedTestDispatcher(io.scheduler)
    private val scope = TestScope(io)
    private val imageMap = ProductImageMap(
        selectedSite,
        productStore,
        scope.backgroundScope,
        CoroutineDispatchers(caller, io, io)
    )

    @Test
    fun `when loading a product image, then the caller yields before reading the database`() = scope.runTest {
        // WHEN
        val image = async(caller) { imageMap.getAsync(PRODUCT_ID) }

        // THEN
        assertThat(image.isCompleted).isFalse()
        verifyNoInteractions(productStore)
        assertThat(image.await()).isEqualTo(IMAGE_URL)
    }

    @Test
    fun `when one caller cancels, then the shared lookup still completes and is cached`() = scope.runTest {
        // WHEN
        val first = async(caller) { imageMap.getAsync(PRODUCT_ID) }
        val second = async(caller) { imageMap.getAsync(PRODUCT_ID) }
        first.cancel()

        // THEN
        assertThat(first.isCancelled).isTrue()
        assertThat(second.await()).isEqualTo(IMAGE_URL)
        assertThat(imageMap.getAsync(PRODUCT_ID)).isEqualTo(IMAGE_URL)
        verify(productStore).getProductByRemoteId(site, PRODUCT_ID)
    }

    @Test
    fun `given an uncached product, when loading the image, then return the fetched image`() = scope.runTest {
        // GIVEN
        whenever(productStore.getProductByRemoteId(site, PRODUCT_ID)).thenReturn(null, product)
        whenever(productStore.fetchSingleProduct(any())).thenReturn(WCProductStore.OnProductChanged())

        // WHEN
        val image = imageMap.getAsync(PRODUCT_ID)

        // THEN
        assertThat(image).isEqualTo(IMAGE_URL)
        verify(productStore).fetchSingleProduct(any())
        verify(productStore, times(2)).getProductByRemoteId(site, PRODUCT_ID)
    }

    @Test
    fun `when resetting the cache during a lookup, then cancel the old request and load the new store image`() = scope.runTest {
        // GIVEN
        val oldImage = async(caller) { imageMap.getAsync(PRODUCT_ID) }
        val newSite = SiteModel().apply { id = 2 }
        whenever(selectedSite.getIfExists()).thenReturn(newSite)
        whenever(productStore.getProductByRemoteId(newSite, PRODUCT_ID)).thenReturn(product)

        // WHEN
        imageMap.reset()
        val newImage = imageMap.getAsync(PRODUCT_ID)

        // THEN
        assertThat(oldImage.isCancelled).isTrue()
        assertThat(newImage).isEqualTo(IMAGE_URL)
        verify(productStore).getProductByRemoteId(newSite, PRODUCT_ID)
        verify(productStore, never()).getProductByRemoteId(site, PRODUCT_ID)
    }

    @Test
    fun `given a failed fetch, when the product later reaches the database, then load its image`() = scope.runTest {
        // GIVEN
        whenever(productStore.getProductByRemoteId(site, PRODUCT_ID)).thenReturn(null, product)
        val failedFetch = WCProductStore.OnProductChanged().apply { error = WCProductStore.ProductError() }
        whenever(productStore.fetchSingleProduct(any())).thenReturn(failedFetch)
        assertThat(imageMap.getAsync(PRODUCT_ID)).isNull()

        // WHEN
        val image = imageMap.getAsync(PRODUCT_ID)

        // THEN
        assertThat(image).isEqualTo(IMAGE_URL)
        verify(productStore).fetchSingleProduct(any())
    }

    @Test
    fun `given a cached image, when loading it again, then return without dispatching`() = scope.runTest {
        // GIVEN
        imageMap.getAsync(PRODUCT_ID)

        // WHEN
        val image = async(caller) { imageMap.getAsync(PRODUCT_ID) }

        // THEN
        assertThat(image.isCompleted).isTrue()
        assertThat(image.await()).isEqualTo(IMAGE_URL)
    }

    @Test
    fun `given a product without an image, when rebinding, then do not repeat the remote fetch`() = scope.runTest {
        // GIVEN
        whenever(productStore.getProductByRemoteId(site, PRODUCT_ID)).thenReturn(null)
        whenever(productStore.fetchSingleProduct(any())).thenReturn(WCProductStore.OnProductChanged())

        // WHEN
        assertThat(imageMap.getAsync(PRODUCT_ID)).isNull()
        assertThat(imageMap.getAsync(PRODUCT_ID)).isNull()

        // THEN
        verify(productStore).fetchSingleProduct(any())
    }

    private companion object {
        const val PRODUCT_ID = 42L
        const val IMAGE_URL = "https://example.com/product.png"
    }
}
