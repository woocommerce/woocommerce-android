package com.woocommerce.android.ui.orders.creation.configuration

import com.google.gson.Gson
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class GetProductConfigurationTest : BaseUnitTest() {

    private val variationDetailRepository: VariationDetailRepository = mock()

    private val gson: Gson = Gson()

    lateinit var sut: GetProductConfiguration

    @Before
    fun setUp() {
        sut = GetProductConfiguration(variationDetailRepository, gson)
    }

    @Test
    fun `optional rule configuration use default value`() = testBlocking {
        val rules = ProductRules.Builder().apply {
            productType = ProductType.BUNDLE
            setChildOptional(1L)
        }.build()

        val configuration = sut.invoke(rules)

        assertThat(configuration.childrenConfiguration).containsKey(1L)
        assertThat(configuration.childrenConfiguration?.getValue(1L)).isNotNull
        assertThat(configuration.childrenConfiguration?.getValue(1L)).containsKey(OptionalRule.KEY)
        assertThat(configuration.childrenConfiguration?.getValue(1L)?.getValue(OptionalRule.KEY)).isNull()
    }

    @Test
    fun `quantity rule configuration use default value`() = testBlocking {
        val rules = ProductRules.Builder().apply {
            productType = ProductType.BUNDLE
            setChildQuantityRules(itemId = 1L, quantityMin = 1f, quantityMax = 10f, quantityDefault = 5f)
        }.build()

        val configuration = sut.invoke(rules)

        assertThat(configuration.childrenConfiguration).containsKey(1L)
        assertThat(configuration.childrenConfiguration?.getValue(1L)).isNotNull
        assertThat(configuration.childrenConfiguration?.getValue(1L)).containsKey(QuantityRule.KEY)
        assertThat(
            configuration.childrenConfiguration?.getValue(1L)?.getValue(QuantityRule.KEY)
        ).isEqualTo(5f.toString())
    }

    @Test
    fun `variable rule configuration use null when variation ids size is empty`() = testBlocking {
        val rules = ProductRules.Builder().apply {
            productType = ProductType.BUNDLE
            setChildVariableRules(
                1L,
                listOf(VariantOption(5L, "Variation", "Option")),
                variationIds = listOf(23L, 45L)
            )
        }.build()

        val configuration = sut.invoke(rules)

        assertThat(configuration.childrenConfiguration).containsKey(1L)
        assertThat(configuration.childrenConfiguration?.getValue(1L)).isNotNull
        assertThat(configuration.childrenConfiguration?.getValue(1L)).containsKey(VariableProductRule.KEY)
        assertThat(configuration.childrenConfiguration?.getValue(1L)?.getValue(VariableProductRule.KEY)).isNull()
    }

    @Test
    fun `variable rule configuration use null when variation ids size is greater than 1`() = testBlocking {
        val rules = ProductRules.Builder().apply {
            productType = ProductType.BUNDLE
            setChildVariableRules(
                1L,
                listOf(VariantOption(5L, "Variation", "Option")),
                variationIds = listOf(23L, 45L)
            )
        }.build()

        val configuration = sut.invoke(rules)

        assertThat(configuration.childrenConfiguration).containsKey(1L)
        assertThat(configuration.childrenConfiguration?.getValue(1L)).isNotNull
        assertThat(configuration.childrenConfiguration?.getValue(1L)).containsKey(VariableProductRule.KEY)
        assertThat(configuration.childrenConfiguration?.getValue(1L)?.getValue(VariableProductRule.KEY)).isNull()
    }

    @Test
    fun `variable rule configuration use default when variation ids size is 1`() = testBlocking {
        val rules = ProductRules.Builder().apply {
            productType = ProductType.BUNDLE
            setChildVariableRules(
                1L,
                listOf(VariantOption(5L, "Variation", "Option")),
                variationIds = listOf(23L)
            )
        }.build()

        val configuration = sut.invoke(rules)

        assertThat(configuration.childrenConfiguration).containsKey(1L)
        assertThat(configuration.childrenConfiguration?.getValue(1L)).isNotNull
        assertThat(configuration.childrenConfiguration?.getValue(1L)).containsKey(VariableProductRule.KEY)
        assertThat(configuration.childrenConfiguration?.getValue(1L)?.getValue(VariableProductRule.KEY)).isNotBlank()
    }
}
