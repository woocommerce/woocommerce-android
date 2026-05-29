package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import com.woocommerce.android.R
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.wooshippinglabels.address.GetAllCountries
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.InputValue
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ShowContentTypeDialog
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ShowCountrySelector
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ShowRestrictionTypeDialog
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ViewState
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.WooShippingCustomsValidator
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WooShippingCustomsFormViewModelTest : BaseUnitTest() {
    private val locations = listOf(
        Location(code = "US", name = "United States"),
        Location(code = "CA", name = "Canada")
    )
    private val getAllCountries: GetAllCountries = mock {
        on { invoke() } doReturn Result.success(locations)
    }

    private val customsValidator: WooShippingCustomsValidator = mock {
        on { validateITN(any(), any()) } doReturn WooShippingCustomsValidator.FieldValidationResult.Valid
        on { validateContentType(any(), any()) } doReturn WooShippingCustomsValidator.FieldValidationResult.Valid
        on { validateRestrictionType(any(), any()) } doReturn WooShippingCustomsValidator.FieldValidationResult.Valid
        on { validateProductDescription(any(), any(), any()) } doReturn
            WooShippingCustomsValidator.FieldValidationResult.Valid
        on { validateProductValue(any()) } doReturn WooShippingCustomsValidator.FieldValidationResult.Valid
        on { validateProductWeight(any()) } doReturn WooShippingCustomsValidator.FieldValidationResult.Valid
        on { validateHSTariffNumber(any(), any()) } doReturn WooShippingCustomsValidator.FieldValidationResult.Valid
    }

    private val currencyFormatter: CurrencyFormatter = mock()

    private lateinit var viewModel: WooShippingCustomsFormViewModel

    @Before
    fun setup() {
        createSut()
    }

    @Test
    fun `given content type click, when triggered, then should show content type dialog event`() = testBlocking {
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever {
            latestEvent = it
        }
        viewModel.onContentTypeClick()
        assertThat(latestEvent).isEqualTo(ShowContentTypeDialog(ContentType.MERCHANDISE))
    }

    @Test
    fun `given restriction type click, when triggered, then should show restriction type dialog event`() =
        testBlocking {
            var latestEvent: MultiLiveEvent.Event? = null
            viewModel.event.observeForever {
                latestEvent = it
            }
            viewModel.onRestrictionTypeClick()
            assertThat(latestEvent).isEqualTo(ShowRestrictionTypeDialog(RestrictionType.NONE))
        }

    @Test
    fun `given valid ITN input, when changed, then should update itn value`() = testBlocking {
        val newItnValue = "AES X20201234567890"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onITNChanged(newItnValue)
        assertThat(capturedViewState?.itnValue).isEqualTo(InputValue.Data(newItnValue))
    }

    @Test
    fun `given return to sender change, when updated, then should update return to sender checked state`() =
        testBlocking {
            val isChecked = true
            var capturedViewState: ViewState? = null
            viewModel.viewState.observeForever {
                capturedViewState = it
            }
            viewModel.onReturnToSenderChanged(isChecked)
            assertThat(capturedViewState?.returnToSenderChecked).isEqualTo(isChecked)
        }

    @Test
    fun `given content type selection, when updated, then should update content type in view state`() = testBlocking {
        val contentType = ContentType.GIFT
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onContentTypeSelected(contentType)
        assertThat(capturedViewState?.contentType).isEqualTo(contentType)
    }

    @Test
    fun `given restriction type selection, when updated, then should update restriction type in view state`() =
        testBlocking {
            val restrictionType = RestrictionType.QUARANTINE
            var capturedViewState: ViewState? = null
            viewModel.viewState.observeForever {
                capturedViewState = it
            }
            viewModel.onRestrictionTypeSelected(restrictionType)
            assertThat(capturedViewState?.restrictionType).isEqualTo(restrictionType)
        }

    @Test
    fun `given valid other content input, when changed, then should update other content input`() = testBlocking {
        val newValue = "Important Stuff"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onOtherContentInputChanged(newValue)
        assertThat(capturedViewState?.otherContentInput).isEqualTo(InputValue.Data(newValue))
    }

    @Test
    fun `given OTHER content type, when changing to invalid input, then should show validation error`() = testBlocking {
        // Given
        val newValue = ""
        whenever(customsValidator.validateContentType(ContentType.OTHER, newValue)).thenReturn(
            WooShippingCustomsValidator.FieldValidationResult.Invalid(
                errorMessage = UiString.UiStringRes(R.string.woo_shipping_labels_customs_other_error_message)
            )
        )

        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }

        // When
        viewModel.onContentTypeSelected(ContentType.OTHER)
        viewModel.onOtherContentInputChanged(newValue)

        // Then
        assertThat(capturedViewState?.otherContentInput).isInstanceOf(InputValue.Error::class.java)
    }

    @Test
    fun `given valid restriction details input, when changed, then should update other restriction input`() =
        testBlocking {
            val newValue = "Restricted Stuff"
            var capturedViewState: ViewState? = null
            viewModel.viewState.observeForever {
                capturedViewState = it
            }
            viewModel.onRestrictionDetailsInputChanged(newValue)
            assertThat(capturedViewState?.otherRestrictionInput).isEqualTo(InputValue.Data(newValue))
        }

    @Test
    fun `given OTHER restriction type, when changing to invalid input, then should show validation error`() =
        testBlocking {
            // Given
            val newValue = ""
            whenever(customsValidator.validateRestrictionType(RestrictionType.OTHER, newValue)).thenReturn(
                WooShippingCustomsValidator.FieldValidationResult.Invalid(
                    errorMessage = UiString.UiStringRes(R.string.woo_shipping_labels_customs_other_error_message)
                )
            )

            var capturedViewState: ViewState? = null
            viewModel.viewState.observeForever {
                capturedViewState = it
            }

            // When
            viewModel.onRestrictionTypeSelected(RestrictionType.OTHER)
            viewModel.onRestrictionDetailsInputChanged(newValue)

            // Then
            assertThat(capturedViewState?.otherRestrictionInput).isInstanceOf(InputValue.Error::class.java)
        }

    @Test
    fun `given shippable product expansion, when toggled, then should update expanded state for specific product`() =
        testBlocking {
            // Given
            val itemIndex = 0
            val isExpanded = true
            var capturedViewState: ViewState? = null
            viewModel.viewState.observeForever {
                capturedViewState = it
            }

            // When
            viewModel.onShippableProductExpanded(itemIndex, isExpanded)

            // Then
            assertThat(capturedViewState?.shippingProducts?.get(itemIndex)?.isExpanded).isEqualTo(isExpanded)
        }

    @Test
    fun `given valid product description input, when changed, then should update description`() = testBlocking {
        // Given
        val itemIndex = 0
        val newDescription = "Test description"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }

        // When
        viewModel.onShippableProductDescriptionChanged(itemIndex, newDescription)

        // Then
        assertThat(capturedViewState?.shippingProducts?.get(itemIndex)?.description)
            .isEqualTo(InputValue.Data(newDescription))
    }

    @Test
    fun `given blank product description input, when changed, then should show validation error`() =
        testBlocking {
            // Given
            val itemIndex = 0
            val blankDescription = ""
            whenever(customsValidator.validateProductDescription(eq(blankDescription), any(), any())).thenReturn(
                WooShippingCustomsValidator.FieldValidationResult.Invalid(
                    errorMessage = UiString.UiStringRes(
                        R.string.woo_shipping_labels_customs_product_details_description_missing
                    )
                )
            )

            var capturedViewState: ViewState? = null
            viewModel.viewState.observeForever {
                capturedViewState = it
            }

            // When
            viewModel.onShippableProductDescriptionChanged(itemIndex, blankDescription)

            // Then
            assertThat(capturedViewState?.shippingProducts?.get(itemIndex)?.description)
                .isInstanceOf(InputValue.Error::class.java)
        }

    @Test
    fun `given valid tariff number input, when changed, then should update tariff number`() = testBlocking {
        // Given
        val itemIndex = 0
        val newTariff = "123456"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }

        // When
        viewModel.onShippableProductTariffNumberChanged(itemIndex, newTariff)

        // Then
        assertThat(capturedViewState?.shippingProducts?.get(itemIndex)?.tariffNumber)
            .isEqualTo(InputValue.Data(newTariff))
    }

    @Test
    fun `given valid product value per unit input, when changed, then should update value per unit`() = testBlocking {
        // Given
        val itemIndex = 0
        val newValue = "10.00"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }

        // When
        viewModel.onShippableProductValuePerUnitChanged(itemIndex, newValue)

        // Then
        assertThat(capturedViewState?.shippingProducts?.get(itemIndex)?.valuePerUnit)
            .isEqualTo(InputValue.Data(newValue))
    }

    @Test
    fun `given blank product value per unit input, when changed, then should show validation error`() =
        testBlocking {
            // Given
            val itemIndex = 0
            val blankValue = ""
            whenever(customsValidator.validateProductValue(blankValue)).thenReturn(
                WooShippingCustomsValidator.FieldValidationResult.Invalid(
                    errorMessage = UiString.UiStringRes(
                        R.string.woo_shipping_labels_customs_product_details_value_required
                    )
                )
            )

            var capturedViewState: ViewState? = null
            viewModel.viewState.observeForever {
                capturedViewState = it
            }

            // When
            viewModel.onShippableProductValuePerUnitChanged(itemIndex, blankValue)

            // Then
            assertThat(capturedViewState?.shippingProducts?.get(itemIndex)?.valuePerUnit)
                .isInstanceOf(InputValue.Error::class.java)
        }

    @Test
    fun `given valid product weight per unit input, when changed, then should update weight per unit`() = testBlocking {
        // Given
        val itemIndex = 0
        val newWeight = "5.00"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }

        // When
        viewModel.onShippableProductWeightPerUnitChanged(itemIndex, newWeight)

        // Then
        assertThat(capturedViewState?.shippingProducts?.get(itemIndex)?.weightPerUnit)
            .isEqualTo(InputValue.Data(newWeight))
    }

    @Test
    fun `given blank product weight per unit input, when changed, then should show validation error`() =
        testBlocking {
            // Given
            val itemIndex = 0
            val blankWeight = ""
            whenever(customsValidator.validateProductWeight(blankWeight)).thenReturn(
                WooShippingCustomsValidator.FieldValidationResult.Invalid(
                    errorMessage = UiString.UiStringRes(
                        R.string.woo_shipping_labels_customs_product_details_value_required
                    )
                )
            )

            var capturedViewState: ViewState? = null
            viewModel.viewState.observeForever {
                capturedViewState = it
            }

            // When
            viewModel.onShippableProductWeightPerUnitChanged(itemIndex, blankWeight)

            // Then
            assertThat(capturedViewState?.shippingProducts?.get(itemIndex)?.weightPerUnit)
                .isInstanceOf(InputValue.Error::class.java)
        }

    @Test
    fun `given country selector click, when triggered, then should show country selector with possible locations`() =
        testBlocking {
            // Given
            val itemIndex = 0

            // Let the viewModel initialize to load countries
            advanceUntilIdle()

            var latestEvent: MultiLiveEvent.Event? = null
            viewModel.event.observeForever {
                latestEvent = it
            }

            // When
            viewModel.onCountrySelectorClick(itemIndex)

            // Then
            assertThat(latestEvent).isInstanceOf(ShowCountrySelector::class.java)
            val countries = (latestEvent as ShowCountrySelector).countries
            assertThat(countries.size).isEqualTo(2)
            assertThat(countries[0].code).isEqualTo("US")
            assertThat(countries[0].name).isEqualTo("United States")
            assertThat(countries[1].code).isEqualTo("CA")
            assertThat(countries[1].name).isEqualTo("Canada")
        }

    @Test
    fun `given origin country change, when selected, then should update origin country for selected product`() =
        testBlocking {
            // Given
            val itemIndex = 0
            val countryCode = "US"

            // Let the viewModel initialize to load countries
            advanceUntilIdle()

            // First set the item for country selection
            viewModel.onCountrySelectorClick(itemIndex)

            var capturedViewState: ViewState? = null
            viewModel.viewState.observeForever {
                capturedViewState = it
            }

            // When
            viewModel.onShippableProductOriginCountryChanged(countryCode)

            // Then
            assertThat(capturedViewState?.shippingProducts?.get(itemIndex)?.originCountry).isEqualTo("United States")
        }

    @Test
    fun `given ITN is required, when setting empty value, then should trigger validation error`() = testBlocking {
        // Given
        whenever(customsValidator.validateITN(any(), any())).thenReturn(
            WooShippingCustomsValidator.FieldValidationResult.Invalid(errorMessage = UiString.UiStringText(""))
        )
        createSut()

        // When
        val capturedViewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onITNChanged("")
        }.last()

        // Then
        assertThat(capturedViewState.itnValue).isInstanceOf(InputValue.Error::class.java)
    }

    @Suppress("LongMethod")
    private fun createSut() {
        // Create a test product with high value for ITN tests
        val testProduct = ShippableItemModel(
            itemId = 1,
            productId = 1,
            title = "Test Product",
            price = BigDecimal.ONE,
            quantity = 1f,
            currency = "USD",
            length = 1f,
            width = 1f,
            height = 1f,
            weight = 1f
        )

        // Create expensive product for ITN required tests
        val expensiveProduct = ShippableItemModel(
            itemId = 2,
            productId = 2,
            title = "Expensive Product",
            price = BigDecimal.valueOf(2600),
            quantity = 1f,
            currency = "USD",
            length = 1f,
            width = 1f,
            height = 1f,
            weight = 1f
        )

        val defaultCustomsData = CustomsData(
            contentType = ContentType.MERCHANDISE,
            contentDescription = "",
            restrictionType = RestrictionType.NONE,
            restrictionDescription = "",
            isReturnToSender = false,
            itn = "",
            items = listOf(testProduct, expensiveProduct).map {
                CustomsItem(
                    productID = it.productId,
                    description = it.title,
                    quantity = it.quantity,
                    value = it.price,
                    weight = it.weight,
                    hsTariffNumber = "",
                    originCountry = "",
                    originCountryCode = ""
                )
            }
        )

        val savedState = WooShippingCustomsFormFragmentArgs(
            originCountryCode = "US",
            destinationCountryCode = "CA",
            customsData = defaultCustomsData,
            storeOptions = StoreOptionsModel(
                currencySymbol = "$",
                weightUnit = "kg",
                dimensionUnit = "cm",
                originCountry = "US"
            )
        ).toSavedStateHandle()

        viewModel = WooShippingCustomsFormViewModel(
            getAllCountries = getAllCountries,
            customsValidator = customsValidator,
            dispatchers = coroutinesTestRule.testDispatchers,
            currencyFormatter = currencyFormatter,
            savedState = savedState
        )
    }
}
