package com.woocommerce.android.ui.orders.shippinglabels.creation

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreateCustomPackageViewModel.*
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@ExperimentalCoroutinesApi
class ShippingLabelCreateCustomPackageViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ShippingLabelCreateCustomPackageViewModel
    private var resourceProvider: ResourceProvider = mock()
    private val shippingRepository: ShippingLabelRepository = mock()
    private val parameterRepository: ParameterRepository = mock()

    private val packageToCreate = ShippingPackage(
        id = "",
        title = "Test Package",
        isLetter = false,
        category = ShippingPackage.CUSTOM_PACKAGE_CATEGORY,
        PackageDimensions(1.0f, 1.0f, 1.0f),
        1f
    )

    fun setup() {
        val savedState = ShippingLabelCreatePackageFragmentArgs(0).initSavedStateHandle()
        whenever(parameterRepository.getParameters(any(), any<SavedStateHandle>())).thenReturn(
            SiteParameters(
                currencyCode = "USD",
                currencySymbol = "$",
                currencyFormattingParameters = null,
                weightUnit = "kg",
                dimensionUnit = "cm",
                gmtOffset = 0f
            )
        )

        viewModel = ShippingLabelCreateCustomPackageViewModel(
            savedState,
            resourceProvider,
            shippingRepository,
            parameterRepository
        )
    }

    private fun populateFields() {
        viewModel.onFieldTextChanged(packageToCreate.title, InputName.NAME)
        viewModel.onFieldTextChanged(packageToCreate.dimensions.length.toString(), InputName.LENGTH)
        viewModel.onFieldTextChanged(packageToCreate.dimensions.width.toString(), InputName.WIDTH)
        viewModel.onFieldTextChanged(packageToCreate.dimensions.height.toString(), InputName.HEIGHT)
        viewModel.onFieldTextChanged(packageToCreate.boxWeight.toString(), InputName.EMPTY_WEIGHT)
    }

    @Test
    fun `when an invalid package name is entered, then display error message`() {
        setup()
        var viewState: ShippingLabelCreateCustomPackageViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }
        viewModel.onFieldTextChanged("", InputName.NAME)

        assertThat(viewState!!.nameErrorMessage)
            .isEqualTo(R.string.shipping_label_create_custom_package_field_empty_hint)
    }

    @Test
    fun `given empty length, width, height, or weight value, when value is entered, then display error message`() {
        setup()
        var state: ShippingLabelCreateCustomPackageViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }
        viewModel.onFieldTextChanged("", InputName.LENGTH)
        viewModel.onFieldTextChanged("", InputName.WIDTH)
        viewModel.onFieldTextChanged("", InputName.HEIGHT)
        viewModel.onFieldTextChanged("", InputName.EMPTY_WEIGHT)

        assertThat(state!!.lengthErrorMessage).isEqualTo(R.string.shipping_label_create_custom_package_field_empty_hint)
        assertThat(state!!.widthErrorMessage).isEqualTo(R.string.shipping_label_create_custom_package_field_empty_hint)
        assertThat(state!!.heightErrorMessage).isEqualTo(R.string.shipping_label_create_custom_package_field_empty_hint)
        assertThat(state!!.weightErrorMessage).isEqualTo(R.string.shipping_label_create_custom_package_field_empty_hint)
    }

    @Test
    fun `given a dot length, width, height, or weight value, when value is entered, then display error message`() {
        setup()
        var state: ShippingLabelCreateCustomPackageViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }
        viewModel.onFieldTextChanged(".", InputName.LENGTH)
        viewModel.onFieldTextChanged(".", InputName.WIDTH)
        viewModel.onFieldTextChanged(".", InputName.HEIGHT)
        viewModel.onFieldTextChanged(".", InputName.EMPTY_WEIGHT)

        assertThat(state!!.lengthErrorMessage).isEqualTo(R.string.shipping_label_create_custom_package_field_empty_hint)
        assertThat(state!!.widthErrorMessage).isEqualTo(R.string.shipping_label_create_custom_package_field_empty_hint)
        assertThat(state!!.heightErrorMessage).isEqualTo(R.string.shipping_label_create_custom_package_field_empty_hint)
        assertThat(state!!.weightErrorMessage).isEqualTo(R.string.shipping_label_create_custom_package_field_empty_hint)
    }

    @Test
    fun `given a dot-something length,width,height,or weight value, when entered, then don't display error message`() {
        setup()
        var state: ShippingLabelCreateCustomPackageViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }
        viewModel.onFieldTextChanged(".5", InputName.LENGTH)
        viewModel.onFieldTextChanged(".5", InputName.WIDTH)
        viewModel.onFieldTextChanged(".5", InputName.HEIGHT)
        viewModel.onFieldTextChanged(".5", InputName.EMPTY_WEIGHT)

        assertThat(state!!.lengthErrorMessage).isEqualTo(null)
        assertThat(state!!.widthErrorMessage).isEqualTo(null)
        assertThat(state!!.heightErrorMessage).isEqualTo(null)
        assertThat(state!!.weightErrorMessage).isEqualTo(null)
    }

    @Test
    fun `given zero package length, width, or height value, when value is entered, then display error message`() {
        setup()
        var state: ShippingLabelCreateCustomPackageViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }
        viewModel.onFieldTextChanged("0", InputName.LENGTH)
        viewModel.onFieldTextChanged("0", InputName.WIDTH)
        viewModel.onFieldTextChanged("0", InputName.HEIGHT)

        assertThat(state!!.lengthErrorMessage)
            .isEqualTo(R.string.shipping_label_create_custom_package_field_invalid_hint)
        assertThat(state!!.widthErrorMessage)
            .isEqualTo(R.string.shipping_label_create_custom_package_field_invalid_hint)
        assertThat(state!!.heightErrorMessage)
            .isEqualTo(R.string.shipping_label_create_custom_package_field_invalid_hint)
    }

    @Test
    fun `given zero weight value, when value is entered, then do not display error message`() {
        setup()
        var state: ShippingLabelCreateCustomPackageViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }
        viewModel.onFieldTextChanged("0", InputName.EMPTY_WEIGHT)
        assertThat(state!!.weightErrorMessage).isEqualTo(null)
    }

    @Test
    fun `when a package is created successfully, then trigger success event`() =
        testBlocking {
            whenever(shippingRepository.createCustomPackage(any())).thenReturn(WooResult(true))
            setup()
            populateFields()

            viewModel.onCustomFormDoneMenuClicked()

            assertThat(viewModel.event.value).isEqualTo(PackageSuccessfullyMadeEvent(packageToCreate))
        }

    @Test
    fun `when a package creation is not saved properly, then show Snackbar`() =
        testBlocking {
            val error = WooError(API_ERROR, NETWORK_ERROR, "")
            whenever(shippingRepository.createCustomPackage(any())).thenReturn(WooResult(error = error))
            setup()
            populateFields()

            viewModel.onCustomFormDoneMenuClicked()

            assertThat(viewModel.event.value).isEqualTo(
                ShowSnackbar(
                    message = R.string.shipping_label_create_custom_package_api_failure,
                    args = arrayOf(error.message!!)
                )
            )
        }
}
