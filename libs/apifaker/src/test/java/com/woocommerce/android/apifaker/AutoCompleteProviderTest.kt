package com.woocommerce.android.apifaker

import com.woocommerce.android.apifaker.models.ApiType
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AutoCompleteProviderTest {
    @Test
    fun `when looking for a suggestion, then return correct results`() = runTest {
        val provider = AutoCompleteProvider()

        val result = provider.provideAutoCompleteSuggestions(ApiType.WPApi, "products")

        assert(result.isNotEmpty())
        assert(result.all { it.endpoint.contains("products") })
    }

    @Test
    fun `when getting WooCommerce suggestion, then mark namespace as not confirmed`() = runTest {
        val provider = AutoCompleteProvider()

        val result = provider.provideAutoCompleteSuggestions(ApiType.WPApi, "/wc/v3/")

        assert(result.isNotEmpty())
        assert(result.all { !it.isNameSpaceConfirmed })
    }

    @Test
    fun `when getting WordPress suggestion, then mark namespace as confirmed`() = runTest {
        val provider = AutoCompleteProvider()

        val result = provider.provideAutoCompleteSuggestions(ApiType.WPApi, "/wp/v2/")

        assert(result.isNotEmpty())
        assert(result.all { it.isNameSpaceConfirmed })
    }

    @Test
    fun `when getting Jetpack suggestion, then mark namespace as confirmed`() = runTest {
        val provider = AutoCompleteProvider()

        val result = provider.provideAutoCompleteSuggestions(ApiType.WPApi, "/jetpack/v4/")

        assert(result.isNotEmpty())
        assert(result.all { it.isNameSpaceConfirmed })
    }
}
