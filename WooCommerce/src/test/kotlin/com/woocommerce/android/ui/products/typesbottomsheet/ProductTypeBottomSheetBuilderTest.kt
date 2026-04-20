package com.woocommerce.android.ui.products.typesbottomsheet

import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.subscriptions.IsEligibleForSubscriptions
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.given
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ProductTypeBottomSheetBuilderTest : BaseUnitTest() {
    private val isEligibleForSubscriptions: IsEligibleForSubscriptions = mock {
        on { invoke() } doReturn true
    }
    private val ciabSiteGateKeeper: CIABSiteGateKeeper = mock {
        on { isFeatureSupported(any()) } doReturn true
    }

    private val sut = ProductTypeBottomSheetBuilder(
        isEligibleForSubscriptions = isEligibleForSubscriptions,
        ciabSiteGateKeeper = ciabSiteGateKeeper
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
    fun `given variable products are supported, when building bottom sheet list, then variable products are visible`() =
        testBlocking {
            given(ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.VariableProducts)).willReturn(true)

            val result = sut.buildBottomSheetList()

            val variableProduct = result.find { it.type == ProductType.VARIABLE }
            assertThat(variableProduct?.isVisible).isTrue
        }

    @Test
    fun `given variable products not supported, when building bottom sheet list, then variable and grouped products are not visible`() =
        testBlocking {
            given(ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.VariableProducts)).willReturn(false)

            val result = sut.buildBottomSheetList()

            val variableProduct = result.find { it.type == ProductType.VARIABLE }
            assertThat(variableProduct?.isVisible).isFalse()
        }

    @Test
    fun `given grouped products are supported, when building bottom sheet list, then grouped products are visible`() =
        testBlocking {
            given(ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.GroupedProducts)).willReturn(true)

            val result = sut.buildBottomSheetList()

            val groupedProduct = result.find { it.type == ProductType.GROUPED }
            assertThat(groupedProduct?.isVisible).isTrue
        }

    @Test
    fun `given grouped products not supported, when building bottom sheet list, then grouped products are not visible`() =
        testBlocking {
            given(ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.GroupedProducts)).willReturn(false)

            val result = sut.buildBottomSheetList()

            val groupedProduct = result.find { it.type == ProductType.GROUPED }
            assertThat(groupedProduct?.isVisible).isFalse()
        }

    @Test
    fun `given bookable service creation supported, when building bottom sheet list, then bookable service is visible`() =
        testBlocking {
            given(ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.BookableServiceCreation))
                .willReturn(true)

            val result = sut.buildBottomSheetList()

            val bookableService = result.find { it.type == ProductType.BOOKABLE_SERVICE }
            assertThat(bookableService?.isVisible).isTrue()
        }

    @Test
    fun `given bookable service creation not supported, when building bottom sheet list, then bookable service is not visible`() =
        testBlocking {
            given(ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.BookableServiceCreation))
                .willReturn(false)

            val result = sut.buildBottomSheetList()

            val bookableService = result.find { it.type == ProductType.BOOKABLE_SERVICE }
            assertThat(bookableService?.isVisible).isFalse()
        }
}
