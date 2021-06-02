package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyVararg
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.store.WCDataStore

@RunWith(RobolectricTestRunner::class)
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
        on { getString(any()) } doReturn "text"
        on { getString(any(), anyVararg()) } doReturn "test"
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
                    attributesList = it.attributesList,
                    weight = 0f,
                    value = it.price,
                    quantity = it.quantity.toFloat()
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
        assertThat(viewStatePackages[0].data.box).isEqualTo(shippingPackage.selectedPackage)
        assertThat(viewStatePackages[0].data.lines).hasSize(shippingPackage.items.size)
        assertThat(viewStatePackages[0].data.lines[0].itemDescription).isEqualTo(shippingPackage.items[0].name)
        assertThat(viewStatePackages[0].data.lines[0].value).isEqualTo(shippingPackage.items[0].value)
        assertThat(viewStatePackages[0].data.lines[0].quantity).isEqualTo(shippingPackage.items[0].quantity)
    }

    @Test
    fun `contents description invalid`() = testBlocking {
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
        assertThat(validationState.contentsDescriptionErrorMessage).isNotEmpty()
    }

    @Test
    fun `contents description valid`() = testBlocking {
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
    fun `restriction description invalid`() = testBlocking {
        setup()

        viewModel.onRestrictionTypeChanged(0, RestrictionType.Other)

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.restrictionDescriptionErrorMessage).isNotEmpty()
    }

    @Test
    fun `restriction description valid`() = testBlocking {
        setup()

        viewModel.onRestrictionTypeChanged(0, RestrictionType.Other)
        viewModel.onRestrictionDescriptionChanged(0, "restriction")

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.restrictionDescriptionErrorMessage).isNull()
    }

    @Test
    fun `itn optional`() = testBlocking {
        setup()

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.itnErrorMessage).isNull()
    }

    @Test
    fun `itn required due to destination country`() = testBlocking {
        val destinationCountry = countries.first { it.code == "SY" }
        val navArgs = defaultNavArgs.copy(destinationCountryCode = destinationCountry.code)
        setup(navArgs)

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        verify(resourceProvider).getString(
            R.string.shipping_label_customs_itn_required_country,
            destinationCountry.name
        )
        assertThat(validationState.itnErrorMessage).isNotEmpty
    }

    @Test
    fun `itn required due to value per hs tariff number`() = testBlocking {
        setup()

        viewModel.onHsTariffNumberChanged(0, 0, "123456")
        viewModel.onItemValueChanged(0, 0, "2501")

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        verify(resourceProvider).getString(R.string.shipping_label_customs_itn_required_items_over_2500)
        assertThat(validationState.itnErrorMessage).isNotEmpty
    }

    @Test
    fun `itn invalid format`() {
        setup()

        viewModel.onItnChanged(0, "invalid ITN")

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        verify(resourceProvider).getString(R.string.shipping_label_customs_itn_invalid_format)
        assertThat(validationState.itnErrorMessage).isNotEmpty
    }

    @Test
    fun `itn valid`() {
        setup()

        viewModel.onItnChanged(0, "AES X20160406131357")

        val validationState = viewModel.viewStateData.liveData.value!!.customsPackages[0].validationState
        assertThat(validationState.itnErrorMessage).isNull()
    }

    @Test
    fun `hs tariff number optional`() {
        setup()

        viewModel.onHsTariffNumberChanged(0, 0, "")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.hsTariffErrorMessage).isNull()
    }

    @Test
    fun `hs tariff number invalid format`() {
        setup()

        viewModel.onHsTariffNumberChanged(0, 0, "123")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.hsTariffErrorMessage).isNotEmpty
    }

    @Test
    fun `hs tariff number valid`() {
        setup()

        viewModel.onHsTariffNumberChanged(0, 0, "123456")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.hsTariffErrorMessage).isNull()
    }

    @Test
    fun `weight required`() {
        setup()

        viewModel.onWeightChanged(0, 0, "")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.weightErrorMessage).isNotEmpty
        verify(resourceProvider).getString(R.string.shipping_label_customs_required_field)
    }

    @Test
    fun `weight invalid`() {
        setup()

        viewModel.onWeightChanged(0, 0, "0")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.weightErrorMessage).isNotEmpty
        verify(resourceProvider).getString(R.string.shipping_label_customs_weight_zero_error)
    }

    @Test
    fun `item value required`() {
        setup()

        viewModel.onItemValueChanged(0, 0, "")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.valueErrorMessage).isNotEmpty
        verify(resourceProvider).getString(R.string.shipping_label_customs_required_field)
    }

    @Test
    fun `item value invalid`() {
        setup()

        viewModel.onItemValueChanged(0, 0, "0")

        val validationState = viewModel.viewStateData.liveData.value!!
            .customsPackages[0]
            .validationState
            .linesValidationState[0]
        assertThat(validationState.valueErrorMessage).isNotEmpty
        verify(resourceProvider).getString(R.string.shipping_label_customs_value_zero_error)
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
