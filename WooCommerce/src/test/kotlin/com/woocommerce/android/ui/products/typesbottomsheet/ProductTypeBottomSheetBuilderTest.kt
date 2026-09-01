package com.woocommerce.android.ui.products.typesbottomsheet

import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.subscriptions.GetSubscriptionProductCreationStatus
import com.woocommerce.android.ui.subscriptions.GetSubscriptionProductCreationStatus.SubscriptionProductCreationStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ProductTypeBottomSheetBuilderTest : BaseUnitTest() {
    private val getSubscriptionProductCreationStatus: GetSubscriptionProductCreationStatus = mock()

    private val sut = ProductTypeBottomSheetBuilder(
        getSubscriptionProductCreationStatus = getSubscriptionProductCreationStatus
    )

    @Test
    fun `given both subscription types creatable, when building bottom sheet list, then subscription products are visible`() =
        testBlocking {
            stubStatus(simple = true, variable = true)

            val result = sut.buildBottomSheetList()

            val subscriptionProduct = result.find { it.type == ProductType.SUBSCRIPTION }
            val variableSubscriptionProduct = result.find { it.type == ProductType.VARIABLE_SUBSCRIPTION }
            assertThat(subscriptionProduct?.isVisible).isTrue
            assertThat(variableSubscriptionProduct?.isVisible).isTrue
        }

    @Test
    fun `given no subscription type creatable, when building bottom sheet list, then subscription products are not visible`() =
        testBlocking {
            stubStatus(simple = false, variable = false)

            val result = sut.buildBottomSheetList()

            val subscriptionProduct = result.find { it.type == ProductType.SUBSCRIPTION }
            val variableSubscriptionProduct = result.find { it.type == ProductType.VARIABLE_SUBSCRIPTION }
            assertThat(subscriptionProduct?.isVisible).isFalse()
            assertThat(variableSubscriptionProduct?.isVisible).isFalse()
        }

    @Test
    fun `given only simple subscription creatable, when building bottom sheet list, then only simple subscription is visible`() =
        testBlocking {
            stubStatus(simple = true, variable = false)

            val result = sut.buildBottomSheetList()

            val subscriptionProduct = result.find { it.type == ProductType.SUBSCRIPTION }
            val variableSubscriptionProduct = result.find { it.type == ProductType.VARIABLE_SUBSCRIPTION }
            assertThat(subscriptionProduct?.isVisible).isTrue
            assertThat(variableSubscriptionProduct?.isVisible).isFalse()
        }

    @Test
    fun `given only variable subscription creatable, when building bottom sheet list, then only variable subscription is visible`() =
        testBlocking {
            stubStatus(simple = false, variable = true)

            val result = sut.buildBottomSheetList()

            val subscriptionProduct = result.find { it.type == ProductType.SUBSCRIPTION }
            val variableSubscriptionProduct = result.find { it.type == ProductType.VARIABLE_SUBSCRIPTION }
            assertThat(subscriptionProduct?.isVisible).isFalse()
            assertThat(variableSubscriptionProduct?.isVisible).isTrue
        }

    @Test
    fun `when building bottom sheet list, then variable products are visible`() =
        testBlocking {
            stubStatus(simple = false, variable = false)

            val result = sut.buildBottomSheetList()

            val variableProduct = result.find { it.type == ProductType.VARIABLE }
            assertThat(variableProduct?.isVisible).isTrue
        }

    @Test
    fun `when building bottom sheet list, then grouped products are visible`() =
        testBlocking {
            stubStatus(simple = false, variable = false)

            val result = sut.buildBottomSheetList()

            val groupedProduct = result.find { it.type == ProductType.GROUPED }
            assertThat(groupedProduct?.isVisible).isTrue
        }

    private suspend fun stubStatus(simple: Boolean, variable: Boolean) {
        given(getSubscriptionProductCreationStatus()).willReturn(
            SubscriptionProductCreationStatus(
                isSimpleSubscriptionCreatable = simple,
                isVariableSubscriptionCreatable = variable
            )
        )
    }
}
