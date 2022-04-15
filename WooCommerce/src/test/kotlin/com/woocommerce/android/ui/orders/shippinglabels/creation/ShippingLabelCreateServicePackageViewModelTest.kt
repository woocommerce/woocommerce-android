package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreateServicePackageViewModel.PackageSuccessfullyMadeEvent
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreateServicePackageViewModel.ViewState
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@ExperimentalCoroutinesApi
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

    fun setup() {
        val savedState = ShippingLabelCreatePackageFragmentArgs(0).initSavedStateHandle()
        viewModel = ShippingLabelCreateServicePackageViewModel(
            savedState,
            resourceProvider,
            shippingRepository,
            parameterRepository
        )
    }

    @Test
    fun `when selectable packages fetching fails, then display a Snackbar`() =
        testBlocking {
            whenever(shippingRepository.getSelectableServicePackages()).thenReturn(
                WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            )
            setup()

            assertThat(viewModel.event.value).isEqualTo(ShowSnackbar(R.string.shipping_label_packages_loading_error))
        }

    @Test
    fun `given no selected packages, when Done button is tapped, then display a Snackbar`() =
        testBlocking {
            whenever(shippingRepository.getSelectableServicePackages()).thenReturn(WooResult(availablePackages))
            setup()
            var event: MultiLiveEvent.Event? = null
            viewModel.event.observeForever { event = it }
            viewModel.onCustomFormDoneMenuClicked()

            assertThat(event).isEqualTo(ShowSnackbar(R.string.shipping_label_create_service_package_nothing_selected))
        }

    @Test
    fun `when package is saved successfully, then trigger success event`() =
        testBlocking {
            whenever(shippingRepository.getSelectableServicePackages()).thenReturn(WooResult(availablePackages))
            whenever(shippingRepository.activateServicePackage(any())).thenReturn(WooResult(true))
            setup()
            viewModel.onPackageSelected(availablePackages[0].id)
            viewModel.onCustomFormDoneMenuClicked()

            assertThat(viewModel.event.value).isEqualTo(PackageSuccessfullyMadeEvent(availablePackages[0]))
        }

    @Test
    fun `when package is not saved successfully, then show Snackbar`() =
        testBlocking {
            val error = WooError(API_ERROR, NETWORK_ERROR, "")
            whenever(shippingRepository.getSelectableServicePackages()).thenReturn(WooResult(availablePackages))
            whenever(shippingRepository.activateServicePackage(any())).thenReturn(WooResult(error = error))
            setup()
            viewModel.onPackageSelected(availablePackages[0].id)
            viewModel.onCustomFormDoneMenuClicked()

            assertThat(viewModel.event.value).isEqualTo(
                ShowSnackbar(
                    message = R.string.shipping_label_create_custom_package_api_failure,
                    args = arrayOf(error.message!!)
                )
            )
        }

    @Test
    fun `given no available packages, when service package tab is opened, then show empty view and hide done button`() =
        testBlocking {
            whenever(shippingRepository.getSelectableServicePackages()).thenReturn(WooResult(emptyList()))
            setup()

            var state: ViewState? = null
            viewModel.viewStateData.observeForever { _, new -> state = new }

            assertThat(state!!.isEmpty).isEqualTo(true)
            assertThat(state!!.canSave).isEqualTo(false)
        }
}
