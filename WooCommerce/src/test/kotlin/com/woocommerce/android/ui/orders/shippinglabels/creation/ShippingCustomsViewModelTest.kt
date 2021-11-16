package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.ContentsType
import com.woocommerce.android.model.RestrictionType
import com.woocommerce.android.model.ShippingLabelPackage.Item
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.store.WCDataStore
import kotlin.math.ceil

class ShippingCustomsViewModelTest : BaseUnitTest() {
    private val countries = listOf(
        WCLocationModel().apply {
            name = "USA"
            code = "US"
        },
        WCLocationModel().apply {
            name = "Canada"
            code = "CA"
        },
        WCLocationModel().apply {
            name = "Syria"
            code = "SY"
        }
    )

    private lateinit var viewModel: ShippingCustomsViewModel

    private val selectedSite: SelectedSite = mock()
    private val parameterRepository: ParameterRepository = mock()
    private val dataStore: WCDataStore = mock {
        on { getCountries() } doReturn countries
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
        on { getString(any(), anyVararg()) } doAnswer { it.arguments[0].toString() }
    }

    private val order = OrderTestUtils.generateTestOrder()

    private val defaultNavArgs = ShippingCustomsFragmentArgs(
        shippingPackages = emptyArray(),
        originCountryCode = "US",
        destinationCountryCode = "CA",
        customsPackages = arrayOf(CreateShippingLabelTestUtils.generateCustomsPackage())
    )

    fun setup(navArgs: ShippingCustomsFragmentArgs = defaultNavArgs) {
        viewModel = ShippingCustomsViewModel(
            savedStateHandle = navArgs.initSavedStateHandle(),
            selectedSide = selectedSite,
            parameterRepository = parameterRepository,
            dataStore = dataStore,
            resourceProvider = resourceProvider
        )
    }

    @Test
    fun `create default customs package from shipping package`() = testBlocking {
        val shippingPackage = CreateShippingLabelTestUtils.generateShippingLabelPackage(
            items = order.items.map {
                Item(
                    productId = it.uniqueId,
                    name = it.name,
                    attributesDescription = it.attributesDescription,
                    weight = 0f,
                    value = it.price,
                    quantity = ceil(it.quantity).toInt()
                )
            }
        )
        val originCountryCode = "US"
        setup(
            ShippingCustomsFragmentArgs(
                shippingPackages = arrayOf(shippingPackage),
                originCountryCode = originCountryCode,
                destinationCountryCode = "CA",
                customsPackages = emptyArray()
            )
        )

        val viewStatePackages = viewModel.viewStateData.liveData.value!!.customsPackages
        assertThat(viewStatePackages).hasSize(1)
        assertThat(viewStatePackages[0].data.contentsType).isEqualTo(ContentsType.Merchandise)
        assertThat(viewStatePackages[0].data.restrictionType).isEqualTo(RestrictionType.None)
        assertThat(viewStatePackages[0].data.returnToSender).isEqualTo(true)
        assertThat(viewStatePackages[0].data.labelPackage).isEqualTo(shippingPackage)
        assertThat(viewStatePackages[0].data.lines).hasSize(shippingPackage.items.size)
        assertThat(viewStatePackages[0].data.lines[0].itemDescription).isEqualTo(shippingPackage.items[0].name)
        assertThat(viewStatePackages[0].data.lines[0].value).isEqualTo(shippingPackage.items[0].value)
        assertThat(viewStatePackages[0].data.lines[0].quantity).isEqualTo(shippingPackage.items[0].quantity)
    }

    @Test
    fun `show error when contents description is missing`() = testBlocking {
        val customsPackage = CreateShippingLabelTestUtils.generateCustomsPackage()
        setup(
            ShippingCustomsFragmentArgs(
                shippingPackages = emptyArray(),
                originCountryCode = "US",
                destinationCountryCode = "CA",
                customsPackages = arrayOf(customsPackage)
            )
        )

        viewModel.onContentsTypeChanged(0, ContentsType.Other)

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.contentsDescriptionErrorMessage)
            .isEqualTo(R.string.shipping_label_customs_contents_type_description_missing.toString())
    }

    @Test
    fun `hide error when a valid contents description is typed`() = testBlocking {
        val customsPackage = CreateShippingLabelTestUtils.generateCustomsPackage()
        setup(
            ShippingCustomsFragmentArgs(
                shippingPackages = emptyArray(),
                originCountryCode = "US",
                destinationCountryCode = "CA",
                customsPackages = arrayOf(customsPackage)
            )
        )

        viewModel.onContentsTypeChanged(0, ContentsType.Other)
        viewModel.onContentsDescriptionChanged(0, "description")

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.contentsDescriptionErrorMessage).isNull()
    }

    @Test
    fun `show error when restriction description is missing`() = testBlocking {
        setup()

        viewModel.onRestrictionTypeChanged(0, RestrictionType.Other)

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.restrictionDescriptionErrorMessage)
            .isEqualTo(R.string.shipping_label_customs_restriction_type_description_missing.toString())
    }

    @Test
    fun `hide error when a valid restriction description is typed`() = testBlocking {
        setup()

        viewModel.onRestrictionTypeChanged(0, RestrictionType.Other)
        viewModel.onRestrictionDescriptionChanged(0, "restriction")

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.restrictionDescriptionErrorMessage).isNull()
    }

    @Test
    fun `hide error when no ITN is enterd`() = testBlocking {
        setup()

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.itnErrorMessage).isNull()
    }

    @Test
    fun `show error when ITN is missing and country requires it`() = testBlocking {
        val destinationCountry = countries.first { it.code == "SY" }
        val navArgs = defaultNavArgs.copy(destinationCountryCode = destinationCountry.code)
        setup(navArgs)

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.itnErrorMessage)
            .isEqualTo(R.string.shipping_label_customs_itn_required_country.toString())
    }

    @Test
    fun `show error when ITN is missing and value per hs tariff is more than 2500`() = testBlocking {
        setup()

        viewModel.onHsTariffNumberChanged(0, 0, "123456")
        viewModel.onItemValueChanged(0, 0, "2501")

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.itnErrorMessage)
            .isEqualTo(R.string.shipping_label_customs_itn_required_items_over_2500.toString())
    }

    @Test
    fun `show error when ITN format is invalid`() {
        setup()

        viewModel.onItnChanged(0, "invalid ITN")

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.itnErrorMessage)
            .isEqualTo(R.string.shipping_label_customs_itn_invalid_format.toString())
    }

    @Test
    fun `hide error when ITN format is valid`() {
        setup()

        viewModel.onItnChanged(0, "AES X20160406131357")

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.itnErrorMessage).isNull()
    }

    @Test
    fun `hide error when hs tariff is missing`() {
        setup()

        viewModel.onHsTariffNumberChanged(0, 0, "")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.hsTariffErrorMessage).isNull()
    }

    @Test
    fun `show error when hs tariff format is invalid`() {
        setup()

        viewModel.onHsTariffNumberChanged(0, 0, "123")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.hsTariffErrorMessage)
            .isEqualTo(R.string.shipping_label_customs_hs_tariff_invalid_format.toString())
    }

    @Test
    fun `hide error when hs tariff has the right format`() {
        setup()

        viewModel.onHsTariffNumberChanged(0, 0, "123456")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.hsTariffErrorMessage).isNull()
    }

    @Test
    fun `show error when weight is empty`() {
        setup()

        viewModel.onWeightChanged(0, 0, "")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.weightErrorMessage)
            .isEqualTo(R.string.shipping_label_customs_required_field.toString())
    }

    @Test
    fun `show error when weight is invalid`() {
        setup()

        viewModel.onWeightChanged(0, 0, "0")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.weightErrorMessage)
            .isEqualTo(R.string.shipping_label_customs_weight_zero_error.toString())
    }

    @Test
    fun `show error when item's value is empty`() {
        setup()

        viewModel.onItemValueChanged(0, 0, "")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.valueErrorMessage)
            .isEqualTo(R.string.shipping_label_customs_required_field.toString())
    }

    @Test
    fun `show error when item's value is invalid`() {
        setup()

        viewModel.onItemValueChanged(0, 0, "0")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.valueErrorMessage)
            .isEqualTo(R.string.shipping_label_customs_value_zero_error.toString())
    }

    @Test
    fun `hide done button when form is invalid`() {
        setup()

        viewModel.onItnChanged(0, "invalid ITN")

        val viewState = viewModel.viewStateData.liveData.value!!
        assertThat(viewState.canSubmitForm).isFalse
    }

    @Test
    fun `show done button when form invalid`() {
        setup()

        viewModel.onItnChanged(0, "")

        val viewState = viewModel.viewStateData.liveData.value!!
        assertThat(viewState.canSubmitForm).isTrue
    }

    @Test
    fun `save customs packages when done is clicked`() {
        setup()

        viewModel.onDoneButtonClicked()

        val event = viewModel.event.value!!
        assertThat(event).isEqualTo(ExitWithResult(defaultNavArgs.customsPackages.toList()))
    }
}
