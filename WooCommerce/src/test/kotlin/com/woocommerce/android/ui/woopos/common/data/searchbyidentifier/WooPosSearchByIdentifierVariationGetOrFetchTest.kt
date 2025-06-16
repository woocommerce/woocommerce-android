package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore

class WooPosSearchByIdentifierVariationGetOrFetchTest {

    private lateinit var sut: WooPosSearchByIdentifierVariationGetOrFetch
    private val selectedSite: SelectedSite = mock()
    private val productStore: WCProductStore = mock()
    private val variationsCache: WooPosVariationsLRUCache = mock()
    private val site: SiteModel = mock()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierVariationGetOrFetch(selectedSite, productStore, variationsCache)
        whenever(selectedSite.get()).thenReturn(site)
    }

    @Test
    fun `given variation exists in cache, when invoke called, then return cached variation`() = runTest {
        // GIVEN
        val variationId = 456L
        val parentId = 123L
        val cachedVariation: ProductVariation = mock {
            on { remoteVariationId }.thenReturn(variationId)
        }
        whenever(variationsCache.get(variationId)).thenReturn(listOf(cachedVariation))

        // WHEN
        val result = sut(variationId, parentId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierVariationGetOrFetch.VariationFetchResult.Success(cachedVariation),
            result
        )
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
            WooPosSearchByIdentifierVariationGetOrFetch.VariationFetchResult.Success(variation),
            result
        )
        verify(variationsCache).add(parentId, variation)
    }

    @Test
    fun `given variation not in cache and network error, when invoke called, then return network error`() = runTest {
        // GIVEN
        val variationId = 456L
        val parentId = 123L
        val fetchResult: WCProductStore.OnVariationChanged = mock {
            on { isError }.thenReturn(true)
        }
        
        whenever(variationsCache.get(variationId)).thenReturn(null)
        whenever(productStore.fetchSingleVariation(site, parentId, variationId)).thenReturn(fetchResult)

        // WHEN
        val result = sut(variationId, parentId)

        // THEN
        assertEquals(WooPosSearchByIdentifierVariationGetOrFetch.VariationFetchResult.NetworkError, result)
    }

    @Test
    fun `given variation not in cache and not found in store after fetch, when invoke called, then return not found`() = runTest {
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
        assertEquals(WooPosSearchByIdentifierVariationGetOrFetch.VariationFetchResult.NotFound, result)
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
            WooPosSearchByIdentifierVariationGetOrFetch.VariationFetchResult.Success(variation),
            result
        )
        verify(variationsCache).add(parentId, variation)
    }
}