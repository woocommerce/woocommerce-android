package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ShippingLabelCreateServicePackageViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ShippingLabelCreateServicePackageViewModel
    private var resourceProvider: ResourceProvider = mock()
    private val shippingRepository: ShippingLabelRepository = mock()
    private val parameterRepository: ParameterRepository = mock()

    private val availablePackages = listOf(
        ShippingPackage(
            "id1", "title1", false, "DHL Express", PackageDimensions(1.0f, 1.0f, 1.0f), 1f, "dhl"
        ),
        ShippingPackage(
            "id2",
            "title2",
            false,
            "USPS Priority Mail Flat Rate Envelopes",
            PackageDimensions(1.0f, 1.0f, 1.0f),
            1f,
            "usps"
        )
    )

    suspend fun setup() {
        val savedState = ShippingLabelCreatePackageFragmentArgs(0).initSavedStateHandle()
        whenever(shippingRepository.getSelectableServicePackages()).thenReturn(WooResult(availablePackages))
        viewModel = ShippingLabelCreateServicePackageViewModel(
            savedState,
            resourceProvider,
            shippingRepository,
            parameterRepository
        )
    }

    @Test
    fun `Given no selected packages, when Done button is tapped, then display a Snackbar`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            setup()
            var event: MultiLiveEvent.Event? = null
            viewModel.event.observeForever { event = it }
            viewModel.onCustomFormDoneMenuClicked()

            assertThat(event).isEqualTo(ShowSnackbar(R.string.shipping_label_create_service_package_nothing_selected))
        }
}
