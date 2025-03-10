package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.wooshippinglabels.address.origin.GetAcceptedOriginCountries
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ContentType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.InputValue
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.RestrictionType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ShowContentTypeDialog
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ShowCountrySelector
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ShowRestrictionTypeDialog
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ViewState
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WooShippingCustomsFormViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: WooShippingCustomsFormViewModel
    private lateinit var getAcceptedOriginCountries: GetAcceptedOriginCountries

    @Before
    fun setup() {
        // Create mock locations for the tests
        val mockLocations = listOf(
            mock<Location> {
                on { code } doReturn "US"
                on { name } doReturn "United States"
            },
            mock<Location> {
                on { code } doReturn "CA"
                on { name } doReturn "Canada"
            }
        )

        // Configure the mock to return our test locations
        getAcceptedOriginCountries = mock {
            onBlocking { invoke() } doReturn Result.success(mockLocations)
        }

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

        viewModel = WooShippingCustomsFormViewModel(
            savedState = WooShippingCustomsFormFragmentArgs(
                shippableItems = arrayOf(testProduct, expensiveProduct),
                customsData = null
            ).toSavedStateHandle(),
            getAcceptedOriginCountries = getAcceptedOriginCountries
        )
    }

    @Test
    fun `onContentTypeClick should trigger ShowContentTypeDialog event`() = testBlocking {
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever {
            latestEvent = it
        }
        viewModel.onContentTypeClick()
        assertThat(latestEvent).isEqualTo(ShowContentTypeDialog(ContentType.MERCHANDISE))
    }

    @Test
    fun `onRestrictionTypeClick should trigger ShowRestrictionTypeDialog event`() = testBlocking {
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever {
            latestEvent = it
        }
        viewModel.onRestrictionTypeClick()
        assertThat(latestEvent).isEqualTo(ShowRestrictionTypeDialog(RestrictionType.NONE))
    }

    @Test
    fun `onITNChanged should update itnValue with valid input`() = testBlocking {
        val newItnValue = "AES X20201234567890"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onITNChanged(newItnValue)
        assertThat(capturedViewState?.itnValue).isEqualTo(InputValue.Data(newItnValue))
    }

    @Test
    fun `onITNChanged should update itnValue with invalid input`() = testBlocking {
        val newItnValue = "INVALID_ITN"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onITNChanged(newItnValue)
        assertThat(capturedViewState?.itnValue).isInstanceOf(InputValue.Error::class.java)
    }

    @Test
    fun `onReturnToSenderChanged should update returnToSenderChecked in viewState`() = testBlocking {
        val isChecked = true
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onReturnToSenderChanged(isChecked)
        assertThat(capturedViewState?.returnToSenderChecked).isEqualTo(isChecked)
    }

    @Test
    fun `onContentTypeSelected should update contentType in viewState`() = testBlocking {
        val contentType = ContentType.GIFT
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onContentTypeSelected(contentType)
        assertThat(capturedViewState?.contentType).isEqualTo(contentType)
    }

    @Test
    fun `onRestrictionTypeSelected should update restrictionType in viewState`() = testBlocking {
        val restrictionType = RestrictionType.QUARANTINE
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onRestrictionTypeSelected(restrictionType)
        assertThat(capturedViewState?.restrictionType).isEqualTo(restrictionType)
    }

    @Test
    fun `onOtherContentInputChanged should update otherContentInput with valid input`() = testBlocking {
        val newValue = "Important Stuff"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onOtherContentInputChanged(newValue)
        assertThat(capturedViewState?.otherContentInput).isEqualTo(InputValue.Data(newValue))
    }

    @Test
    fun `onOtherContentInputChanged should update otherContentInput with invalid input`() = testBlocking {
        val newValue = ""
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onOtherContentInputChanged(newValue)
        assertThat(
            capturedViewState?.otherContentInput
        ).isInstanceOf(InputValue.Error::class.java)
    }

    @Test
    fun `onRestrictionDetailsInputChanged should update otherRestrictionInput with valid input`() = testBlocking {
        val newValue = "Restricted Stuff"
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onRestrictionDetailsInputChanged(newValue)
        assertThat(capturedViewState?.otherRestrictionInput).isEqualTo(InputValue.Data(newValue))
    }

    @Test
    fun `onRestrictionDetailsInputChanged should update otherRestrictionInput with invalid input`() = testBlocking {
        val newValue = ""
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }
        viewModel.onRestrictionDetailsInputChanged(newValue)
        assertThat(
            capturedViewState?.otherRestrictionInput
        ).isInstanceOf(InputValue.Error::class.java)
    }

    @Test
    fun `onShippableProductExpanded should update isExpanded state for specific product`() = testBlocking {
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
    fun `onShippableProductDescriptionChanged should update description with valid input`() = testBlocking {
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
    fun `onShippableProductDescriptionChanged should update description with error for blank input`() =
        testBlocking {
            // Given
            val itemIndex = 0
            val blankDescription = ""
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
    fun `onShippableProductTariffNumberChanged should update tariffNumber with valid input`() = testBlocking {
        // Given
        val itemIndex = 0
        val newTariff = "HS 12345"
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
    fun `onShippableProductTariffNumberChanged should update tariffNumber with error for blank input`() =
        testBlocking {
            // Given
            val itemIndex = 0
            val blankTariff = ""
            var capturedViewState: ViewState? = null
            viewModel.viewState.observeForever {
                capturedViewState = it
            }

            // When
            viewModel.onShippableProductTariffNumberChanged(itemIndex, blankTariff)

            // Then
            assertThat(capturedViewState?.shippingProducts?.get(itemIndex)?.tariffNumber)
                .isInstanceOf(InputValue.Error::class.java)
        }

    @Test
    fun `onShippableProductValuePerUnitChanged should update valuePerUnit with valid input`() = testBlocking {
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
    fun `onShippableProductValuePerUnitChanged should update valuePerUnit with error for blank input`() =
        testBlocking {
            // Given
            val itemIndex = 0
            val blankValue = ""
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
    fun `onShippableProductWeightPerUnitChanged should update weightPerUnit with valid input`() = testBlocking {
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
    fun `onShippableProductWeightPerUnitChanged should update weightPerUnit with error for blank input`() =
        testBlocking {
            // Given
            val itemIndex = 0
            val blankWeight = ""
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
    fun `onCountrySelectorClick should trigger ShowCountrySelector event with possible locations`() = testBlocking {
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
    fun `onShippableProductOriginCountryChanged should update originCountry for selected product`() = testBlocking {
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
    fun `onITNChanged with blank value should set error when ITN is required`() = testBlocking {
        // Given
        val blankItn = ""

        // The second product in our setup is the expensive one,
        // so expanding the second product will make the ITN required
        var capturedViewState: ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }

        // When
        viewModel.onShippableProductValuePerUnitChanged(1, "2600")
        viewModel.onITNChanged(blankItn)

        // Then
        assertThat(capturedViewState?.itnValue).isInstanceOf(InputValue.Error::class.java)
    }
}
