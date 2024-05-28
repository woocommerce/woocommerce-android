package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.ProductTestUtils.generateProduct
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.generateVariation
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
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class GetProductVariationQuantityRulesTest : BaseUnitTest() {

    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val variationDetailRepository: VariationDetailRepository = mock()
    private val productDetailRepository: ProductDetailRepository = mock()

    private lateinit var sut: GetProductVariationQuantityRules

    @Before
    fun setUp() {
        sut = GetProductVariationQuantityRules(
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore,
            variationDetailRepository = variationDetailRepository,
            dispatchers = coroutinesTestRule.testDispatchers,
            productDetailRepository = productDetailRepository
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
    fun `when min max extension is NOT active then return null quantity rules`() = testBlocking {
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

    @Test
    fun `when min max extension is active and parent product combines variation quantities then return null`() =
        testBlocking {
            val productId = 1L
            val variationId = 1L
            val plugin = SitePluginModel().apply { setIsActive(true) }
            val product = ProductTestUtils.generateProduct(productCombinesVariationQuantities = true)
            whenever(
                wooCommerceStore.getSitePlugin(
                    any(),
                    eq(WooCommerceStore.WooPlugin.WOO_MIN_MAX_QUANTITIES)
                )
            )
                .doReturn(plugin)
            whenever(productDetailRepository.getProduct(productId)).doReturn(product)

            val result = sut.invoke(productId, variationId)

            Assertions.assertThat(result).isNull()
        }

    @Test
    fun `when min max extension is active and parent product combines variation quantities but variation does not override parent product rules then return quantity rules`() =
        testBlocking {
            val productId = 1L
            val variationId = 1L
            val plugin = SitePluginModel().apply { setIsActive(true) }
            val product = ProductTestUtils.generateProduct(productCombinesVariationQuantities = false)
            val variation = generateVariation().copy(overrideProductQuantities = false)
            whenever(
                wooCommerceStore.getSitePlugin(
                    any(),
                    eq(WooCommerceStore.WooPlugin.WOO_MIN_MAX_QUANTITIES)
                )
            )
                .doReturn(plugin)
            whenever(productDetailRepository.getProduct(productId)).doReturn(product)
            whenever(variationDetailRepository.getVariation(productId, variationId)).doReturn(variation)

            val result = sut.invoke(productId, variationId)

            Assertions.assertThat(result).isNull()
        }

    @Test
    fun `when min max extension is active and parent product combines variation quantities and variation overrides parent product rules then return quantity rules`() =
        testBlocking {
            val productId = 1L
            val variationId = 1L
            val plugin = SitePluginModel().apply { setIsActive(true) }
            val product = ProductTestUtils.generateProduct(productCombinesVariationQuantities = false)
            val variation = generateVariation().copy(overrideProductQuantities = true)
            val quantityRules = QuantityRules(2, 20, 2)
            whenever(
                wooCommerceStore.getSitePlugin(
                    any(),
                    eq(WooCommerceStore.WooPlugin.WOO_MIN_MAX_QUANTITIES)
                )
            )
                .doReturn(plugin)
            whenever(variationDetailRepository.getQuantityRules(productId, variationId)).doReturn(quantityRules)
            whenever(productDetailRepository.getProduct(productId)).doReturn(product)
            whenever(variationDetailRepository.getVariation(productId, variationId)).doReturn(variation)

            val result = sut.invoke(productId, variationId)

            Assertions.assertThat(result).isEqualTo(quantityRules)
        }
}
