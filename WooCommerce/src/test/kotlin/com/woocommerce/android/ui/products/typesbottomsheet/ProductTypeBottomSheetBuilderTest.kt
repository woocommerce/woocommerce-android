package com.woocommerce.android.ui.products.typesbottomsheet

import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.subscriptions.IsEligibleForSubscriptions
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.given
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ProductTypeBottomSheetBuilderTest : BaseUnitTest() {
    private val isEligibleForSubscriptions: IsEligibleForSubscriptions = mock {
        on { invoke() } doReturn true
    }

    private val sut = ProductTypeBottomSheetBuilder(
        isEligibleForSubscriptions = isEligibleForSubscriptions
    )

    @Test
    fun `given eligible for subscriptions, when building bottom sheet list, then subscription products are visible`() =
        testBlocking {
            given(isEligibleForSubscriptions()).willReturn(true)

            val result = sut.buildBottomSheetList()

            val subscriptionProduct = result.find { it.type == ProductType.SUBSCRIPTION }
            val variableSubscriptionProduct = result.find { it.type == ProductType.VARIABLE_SUBSCRIPTION }
            assertThat(subscriptionProduct?.isVisible).isTrue
            assertThat(variableSubscriptionProduct?.isVisible).isTrue
        }

    @Test
    fun `given not eligible for subscriptions, when building bottom sheet list, then subscription products are not visible`() =
        testBlocking {
            given(isEligibleForSubscriptions()).willReturn(false)

            val result = sut.buildBottomSheetList()

            val subscriptionProduct = result.find { it.type == ProductType.SUBSCRIPTION }
            val variableSubscriptionProduct = result.find { it.type == ProductType.VARIABLE_SUBSCRIPTION }
            assertThat(subscriptionProduct?.isVisible).isFalse()
            assertThat(variableSubscriptionProduct?.isVisible).isFalse()
        }

    @Test
    fun `when building bottom sheet list, then variable products are visible`() =
        testBlocking {
            val result = sut.buildBottomSheetList()

            val variableProduct = result.find { it.type == ProductType.VARIABLE }
            assertThat(variableProduct?.isVisible).isTrue
        }

    @Test
    fun `when building bottom sheet list, then grouped products are visible`() =
        testBlocking {
            val result = sut.buildBottomSheetList()

            val groupedProduct = result.find { it.type == ProductType.GROUPED }
            assertThat(groupedProduct?.isVisible).isTrue
        }
}
