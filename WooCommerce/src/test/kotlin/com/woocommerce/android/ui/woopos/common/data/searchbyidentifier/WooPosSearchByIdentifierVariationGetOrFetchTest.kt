package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType

class WooPosSearchByIdentifierVariationGetOrFetchTest {

    private lateinit var sut: WooPosSearchByIdentifierVariationFetch
    private val selectedSite: SelectedSite = mock()
    private val productStore: WCProductStore = mock()
    private val variationsCache: WooPosVariationsLRUCache = mock()
    private val site: SiteModel = mock()
    private val errorMapper: WooPosSearchByIdentifierProductErrorMapper = WooPosSearchByIdentifierProductErrorMapper()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierVariationFetch(selectedSite, productStore, variationsCache, errorMapper)
        whenever(selectedSite.get()).thenReturn(site)
    }

    @Test
    fun `given variation not in cache and successful fetch, when invoke called, then return success and cache variation`() = runTest {
        // GIVEN
        val variationId = 456L
        val parentId = 123L
        val wcVariation = WCProductVariationModel(LocalId(1)).copy(
            remoteVariationId = RemoteId(variationId),
            remoteProductId = RemoteId(parentId)
        )
        val variation = wcVariation.toAppModel()
        val fetchResult: WCProductStore.OnVariationChanged = mock {
            on { isError }.thenReturn(false)
        }

        whenever(variationsCache.get(variationId)).thenReturn(null)
        whenever(productStore.fetchSingleVariation(site, parentId, variationId)).thenReturn(fetchResult)
        whenever(productStore.getVariationByRemoteId(site, parentId, variationId)).thenReturn(wcVariation)

        // WHEN
        val result = sut(variationId, parentId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierVariationFetch.VariationFetchResult.Success(variation),
            result
        )
        verify(variationsCache).add(parentId, variation)
    }

    @Test
    fun `given variation not in cache and generic error, when invoke called, then return failure with unknown error`() = runTest {
        // GIVEN
        val variationId = 456L
        val parentId = 123L
        val error = ProductError(ProductErrorType.GENERIC_ERROR, "Generic error occurred")
        val fetchResult = WCProductStore.OnVariationChanged().apply {
            this.error = error
        }

        whenever(variationsCache.get(variationId)).thenReturn(null)
        whenever(productStore.fetchSingleVariation(site, parentId, variationId)).thenReturn(fetchResult)

        // WHEN
        val result = sut(variationId, parentId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierVariationFetch.VariationFetchResult.Failure(
                WooPosSearchByIdentifierResult.Error.UnknownError("Generic error occurred")
            ),
            result
        )
    }

    @Test
    fun `given variation not in cache and not found in store after fetch, when invoke called, then return failure with unknown error`() = runTest {
        // GIVEN
        val variationId = 456L
        val parentId = 123L
        val fetchResult: WCProductStore.OnVariationChanged = mock {
            on { isError }.thenReturn(false)
        }

        whenever(variationsCache.get(variationId)).thenReturn(null)
        whenever(productStore.fetchSingleVariation(site, parentId, variationId)).thenReturn(fetchResult)
        whenever(productStore.getVariationByRemoteId(site, parentId, variationId)).thenReturn(null)

        // WHEN
        val result = sut(variationId, parentId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierVariationFetch.VariationFetchResult.Failure(
                WooPosSearchByIdentifierResult.Error.UnknownError("Variation not found for ID: $variationId")
            ),
            result
        )
    }

    @Test
    fun `given cached variations with different id, when invoke called, then fetch from network`() = runTest {
        // GIVEN
        val variationId = 456L
        val parentId = 123L
        val differentVariation: ProductVariation = mock {
            on { remoteVariationId }.thenReturn(999L)
        }
        val wcVariation = WCProductVariationModel(LocalId(1)).copy(
            remoteVariationId = RemoteId(variationId),
            remoteProductId = RemoteId(parentId)
        )
        val variation = wcVariation.toAppModel()
        val fetchResult: WCProductStore.OnVariationChanged = mock {
            on { isError }.thenReturn(false)
        }

        whenever(variationsCache.get(variationId)).thenReturn(listOf(differentVariation))
        whenever(productStore.fetchSingleVariation(site, parentId, variationId)).thenReturn(fetchResult)
        whenever(productStore.getVariationByRemoteId(site, parentId, variationId)).thenReturn(wcVariation)

        // WHEN
        val result = sut(variationId, parentId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierVariationFetch.VariationFetchResult.Success(variation),
            result
        )
        verify(variationsCache).add(parentId, variation)
    }

    @Test
    fun `given fetch error, when invoke called, then return failure with mapped error`() = runTest {
        // GIVEN
        val variationId = 456L
        val parentId = 123L
        val error = ProductError(ProductErrorType.INVALID_PRODUCT_ID, "Invalid product ID")
        val fetchResult = WCProductStore.OnVariationChanged().apply {
            this.error = error
        }

        whenever(variationsCache.get(variationId)).thenReturn(null)
        whenever(productStore.fetchSingleVariation(site, parentId, variationId)).thenReturn(fetchResult)

        // WHEN
        val result = sut(variationId, parentId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierVariationFetch.VariationFetchResult.Failure(
                WooPosSearchByIdentifierResult.Error.NotFound
            ),
            result
        )
    }
}
