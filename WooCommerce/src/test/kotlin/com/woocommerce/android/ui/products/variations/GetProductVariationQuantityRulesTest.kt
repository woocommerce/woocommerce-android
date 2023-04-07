package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class GetProductVariationQuantityRulesTest : BaseUnitTest() {

    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val variationDetailRepository: VariationDetailRepository = mock()

    private lateinit var sut: GetProductVariationQuantityRules

    @Before
    fun setUp() {
        sut = GetProductVariationQuantityRules(
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore,
            variationDetailRepository = variationDetailRepository,
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `when min max extension is not installed then return null quantity rules`() = testBlocking {
        val productId = 1L
        val variationId = 1L
        val plugin = null
        whenever(
            wooCommerceStore.getSitePlugin(
                any(),
                eq(WooCommerceStore.WooPlugin.WOO_MIN_MAX_QUANTITIES)
            )
        )
            .doReturn(plugin)

        val result = sut.invoke(productId, variationId)

        Assertions.assertThat(result).isNull()
    }

    @Test
    fun `when min max extension is installed and active then get quantity rules`() = testBlocking {
        val productId = 1L
        val variationId = 1L
        val plugin = SitePluginModel().apply { setIsActive(true) }
        val quantityRules = QuantityRules(2, 20, 2)
        whenever(
            wooCommerceStore.getSitePlugin(
                any(),
                eq(WooCommerceStore.WooPlugin.WOO_MIN_MAX_QUANTITIES)
            )
        )
            .doReturn(plugin)
        whenever(variationDetailRepository.getQuantityRules(productId, variationId)).doReturn(quantityRules)

        val result = sut.invoke(productId, variationId)

        Assertions.assertThat(result).isEqualTo(quantityRules)
    }

    @Test
    fun `when min max extension is installed and NOT active then return null quantity rules`() = testBlocking {
        val productId = 1L
        val variationId = 1L
        val plugin = SitePluginModel().apply { setIsActive(false) }
        whenever(
            wooCommerceStore.getSitePlugin(
                any(),
                eq(WooCommerceStore.WooPlugin.WOO_MIN_MAX_QUANTITIES)
            )
        )
            .doReturn(plugin)

        val result = sut.invoke(productId, variationId)

        Assertions.assertThat(result).isNull()
    }
}
