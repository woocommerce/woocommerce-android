package com.woocommerce.android.ui.orders

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.ui.orders.details.AddressValidator
import com.woocommerce.android.ui.orders.details.EMPTY_ADDRESS_ERROR
import com.woocommerce.android.ui.orders.details.EMPTY_CITY_ERROR
import com.woocommerce.android.ui.orders.details.EMPTY_COUNTRY_ERROR
import com.woocommerce.android.ui.orders.details.EMPTY_NAME_ERROR
import com.woocommerce.android.ui.orders.details.EMPTY_POSTCODE_ERROR
import com.woocommerce.android.ui.orders.details.EMPTY_STATE_ERROR
import com.woocommerce.android.ui.orders.details.INVALID_PHONE_ERROR
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType

@OptIn(ExperimentalCoroutinesApi::class)
class AddressValidatorTest : BaseUnitTest() {
    private val shippingLabelAddressValidator: ShippingLabelAddressValidator = mock()
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val trackerWrapper: AnalyticsTrackerWrapper = mock()
    private lateinit var addressValidator: AddressValidator
    private val defaultOrderId = 1L
    private val defaultAddress = Address(
        firstName = "Test",
        lastName = "User",
        company = "Test Company",
        phone = "12345678",
        country = Location(code = "US", name = "Test Country"),
        state = AmbiguousLocation.Raw(value = "Test State"),
        address1 = "Test address ST, Test Town",
        address2 = "#1234 5B",
        city = "Test city",
        postcode = "2233",
        email = "testuser@testmail.com"
    )

    @Before
    fun setup() {
        clearInvocations(
            shippingLabelAddressValidator,
            orderDetailRepository,
            trackerWrapper
        )
        addressValidator = spy(
            AddressValidator(
                coroutineDispatchers = coroutinesTestRule.testDispatchers,
                shippingLabelAddressValidator = shippingLabelAddressValidator,
                orderDetailRepository = orderDetailRepository,
                analyticsTrackerWrapper = trackerWrapper
            )
        )
    }

    @Test
    fun `When the address has no name, EMPTY_NAME_ERROR is registered`() = testBlocking {
        // Given name and company name are empty
        val address = defaultAddress.copy(
            company = "",
            firstName = "",
            lastName = ""
        )
        // When the address is validated
        addressValidator.validate(defaultOrderId, address)

        // Then the EMPTY_NAME_ERROR is tracked
        verify(trackerWrapper).track(
            AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_MESSAGE to EMPTY_NAME_ERROR,
                AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_LOCAL,
                AnalyticsTracker.KEY_ORDER_ID to defaultOrderId
            )
        )
        // Then the address remote validation never happens
        verify(shippingLabelAddressValidator, never()).validateAddress(
            address = address,
            type = ShippingLabelAddressValidator.AddressType.DESTINATION,
            requiresPhoneNumber = false
        )
    }

    @Test
    fun `When the address has no address1, EMPTY_ADDRESS_ERROR is registered`() = testBlocking {
        // Given address1 is empty
        val address = defaultAddress.copy(
            address1 = ""
        )

        // When the address is validated
        addressValidator.validate(defaultOrderId, address)

        // Then the EMPTY_ADDRESS_ERROR is tracked
        verify(trackerWrapper).track(
            AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_MESSAGE to EMPTY_ADDRESS_ERROR,
                AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_LOCAL,
                AnalyticsTracker.KEY_ORDER_ID to defaultOrderId
            )
        )
        // Then the address remote validation never happens
        verify(shippingLabelAddressValidator, never()).validateAddress(
            address = address,
            type = ShippingLabelAddressValidator.AddressType.DESTINATION,
            requiresPhoneNumber = false
        )
    }

    @Test
    fun `When the address has no city, EMPTY_CITY_ERROR is registered`() = testBlocking {
        // Given city is empty
        val address = defaultAddress.copy(
            city = ""
        )

        // When the address is validated
        addressValidator.validate(defaultOrderId, address)

        // Then the EMPTY_CITY_ERROR is tracked
        verify(trackerWrapper).track(
            AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_MESSAGE to EMPTY_CITY_ERROR,
                AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_LOCAL,
                AnalyticsTracker.KEY_ORDER_ID to defaultOrderId
            )
        )
        // Then the address remote validation never happens
        verify(shippingLabelAddressValidator, never()).validateAddress(
            address = address,
            type = ShippingLabelAddressValidator.AddressType.DESTINATION,
            requiresPhoneNumber = false
        )
    }

    @Test
    fun `When the address has no postcode, EMPTY_POSTCODE_ERROR is registered`() = testBlocking {
        // Given postcode is empty
        val address = defaultAddress.copy(
            postcode = ""
        )

        // When the address is validated
        addressValidator.validate(defaultOrderId, address)

        // Then the EMPTY_POSTCODE_ERROR is tracked
        verify(trackerWrapper).track(
            AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_MESSAGE to EMPTY_POSTCODE_ERROR,
                AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_LOCAL,
                AnalyticsTracker.KEY_ORDER_ID to defaultOrderId
            )
        )
        // Then the address remote validation never happens
        verify(shippingLabelAddressValidator, never()).validateAddress(
            address = address,
            type = ShippingLabelAddressValidator.AddressType.DESTINATION,
            requiresPhoneNumber = false
        )
    }

    @Test
    fun `When the address has no state, EMPTY_STATE_ERROR is registered`() = testBlocking {
        // Given state is empty
        val address = defaultAddress.copy(
            state = AmbiguousLocation.Raw("")
        )

        // When the address is validated
        addressValidator.validate(defaultOrderId, address)

        // Then the EMPTY_STATE_ERROR is tracked
        verify(trackerWrapper).track(
            AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_MESSAGE to EMPTY_STATE_ERROR,
                AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_LOCAL,
                AnalyticsTracker.KEY_ORDER_ID to defaultOrderId
            )
        )
        // Then the address remote validation never happens
        verify(shippingLabelAddressValidator, never()).validateAddress(
            address = address,
            type = ShippingLabelAddressValidator.AddressType.DESTINATION,
            requiresPhoneNumber = false
        )
    }

    @Test
    fun `When the address has no country, EMPTY_COUNTRY_ERROR is registered`() = testBlocking {
        // Given country is empty
        val address = defaultAddress.copy(
            country = Location.EMPTY
        )

        // When the address is validated
        addressValidator.validate(defaultOrderId, address)

        // Then the EMPTY_COUNTRY_ERROR is tracked
        verify(trackerWrapper).track(
            AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_MESSAGE to EMPTY_COUNTRY_ERROR,
                AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_LOCAL,
                AnalyticsTracker.KEY_ORDER_ID to defaultOrderId
            )
        )
        // Then the address remote validation never happens
        verify(shippingLabelAddressValidator, never()).validateAddress(
            address = address,
            type = ShippingLabelAddressValidator.AddressType.DESTINATION,
            requiresPhoneNumber = false
        )
    }

    @Test
    fun `When the address has multiple errors, multiple errors are registered`() = testBlocking {
        // Given city is empty
        val address = defaultAddress.copy(
            postcode = "",
            city = ""
        )

        // When the address is validated
        addressValidator.validate(defaultOrderId, address)

        // Then the EMPTY_CITY_ERROR is tracked
        val message = listOf(EMPTY_CITY_ERROR, EMPTY_POSTCODE_ERROR).joinToString(", ")
        verify(trackerWrapper).track(
            AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_MESSAGE to message,
                AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_LOCAL,
                AnalyticsTracker.KEY_ORDER_ID to defaultOrderId
            )
        )
        // Then the address remote validation never happens
        verify(shippingLabelAddressValidator, never()).validateAddress(
            address = address,
            type = ShippingLabelAddressValidator.AddressType.DESTINATION,
            requiresPhoneNumber = false
        )
    }

    @Test
    fun `When the services plugin is not installed, don't call the remote validation`() = testBlocking {
        // Given a valid address & that the services plugin is not installed
        val pluginInfo = WooPlugin(isActive = false, isInstalled = false, version = null)
        whenever(orderDetailRepository.getWooServicesPluginInfo()).thenReturn(pluginInfo)

        // When the address is validated
        addressValidator.validate(defaultOrderId, defaultAddress)

        // Then no local error is tracked
        verify(trackerWrapper, never()).track(any(), any())

        // Then the address remote validation never happens
        verify(shippingLabelAddressValidator, never()).validateAddress(
            address = defaultAddress,
            type = ShippingLabelAddressValidator.AddressType.DESTINATION,
            requiresPhoneNumber = false
        )
    }

    @Test
    fun `When the services plugin is not active, don't call the remote validation`() = testBlocking {
        // Given a valid address & that the services plugin is not active
        val pluginInfo = WooPlugin(isActive = false, isInstalled = true, version = null)
        whenever(orderDetailRepository.getWooServicesPluginInfo()).thenReturn(pluginInfo)

        // When the address is validated
        addressValidator.validate(defaultOrderId, defaultAddress)

        // Then no local error is tracked
        verify(trackerWrapper, never()).track(any(), any())

        // Then the address remote validation never happens
        verify(shippingLabelAddressValidator, never()).validateAddress(
            address = defaultAddress,
            type = ShippingLabelAddressValidator.AddressType.DESTINATION,
            requiresPhoneNumber = false
        )
    }

    @Test
    fun `When the remote validation returns NameMissing, EMPTY_NAME_ERROR is tracked`() = testBlocking {
        // Given the remote validation returns NameMissing
        val pluginInfo = WooPlugin(isActive = true, isInstalled = true, version = null)
        whenever(orderDetailRepository.getWooServicesPluginInfo()).thenReturn(pluginInfo)

        whenever(
            shippingLabelAddressValidator.validateAddress(
                address = defaultAddress,
                type = ShippingLabelAddressValidator.AddressType.DESTINATION,
                requiresPhoneNumber = false
            )
        ).thenReturn(ShippingLabelAddressValidator.ValidationResult.NameMissing)

        // When the address is validated
        addressValidator.validate(defaultOrderId, defaultAddress)

        // Then the EMPTY_NAME_ERROR is tracked
        verify(trackerWrapper).track(
            AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_MESSAGE to EMPTY_NAME_ERROR,
                AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_REMOTE,
                AnalyticsTracker.KEY_ORDER_ID to defaultOrderId
            )
        )
    }

    @Test
    fun `When the remote validation returns PhoneInvalid, INVALID_PHONE_ERROR is tracked`() = testBlocking {
        // Given the remote validation returns PhoneInvalid
        val pluginInfo = WooPlugin(isActive = true, isInstalled = true, version = null)
        whenever(orderDetailRepository.getWooServicesPluginInfo()).thenReturn(pluginInfo)

        whenever(
            shippingLabelAddressValidator.validateAddress(
                address = defaultAddress,
                type = ShippingLabelAddressValidator.AddressType.DESTINATION,
                requiresPhoneNumber = false
            )
        ).thenReturn(ShippingLabelAddressValidator.ValidationResult.PhoneInvalid)

        // When the address is validated
        addressValidator.validate(defaultOrderId, defaultAddress)

        // Then the EMPTY_NAME_ERROR is tracked
        verify(trackerWrapper).track(
            AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_MESSAGE to INVALID_PHONE_ERROR,
                AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_REMOTE,
                AnalyticsTracker.KEY_ORDER_ID to defaultOrderId
            )
        )
    }

    @Test
    fun `When the remote validation returns Invalid, invalid message is tracked`() = testBlocking {
        // Given the remote validation returns Invalid
        val pluginInfo = WooPlugin(isActive = true, isInstalled = true, version = null)
        val message = "Invalid message"
        whenever(orderDetailRepository.getWooServicesPluginInfo()).thenReturn(pluginInfo)

        whenever(
            shippingLabelAddressValidator.validateAddress(
                address = defaultAddress,
                type = ShippingLabelAddressValidator.AddressType.DESTINATION,
                requiresPhoneNumber = false
            )
        ).thenReturn(ShippingLabelAddressValidator.ValidationResult.Invalid(message))

        // When the address is validated
        addressValidator.validate(defaultOrderId, defaultAddress)

        // Then the invalid message is tracked
        verify(trackerWrapper).track(
            AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_MESSAGE to message,
                AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_REMOTE,
                AnalyticsTracker.KEY_ORDER_ID to defaultOrderId
            )
        )
    }

    @Test
    fun `When the remote validation returns NotFound, not found message is tracked`() = testBlocking {
        // Given the remote validation returns NotFound
        val pluginInfo = WooPlugin(isActive = true, isInstalled = true, version = null)
        val message = "Not found message"
        whenever(orderDetailRepository.getWooServicesPluginInfo()).thenReturn(pluginInfo)

        whenever(
            shippingLabelAddressValidator.validateAddress(
                address = defaultAddress,
                type = ShippingLabelAddressValidator.AddressType.DESTINATION,
                requiresPhoneNumber = false
            )
        ).thenReturn(ShippingLabelAddressValidator.ValidationResult.NotFound(message))

        // When the address is validated
        addressValidator.validate(defaultOrderId, defaultAddress)

        // Then the not found message is tracked
        verify(trackerWrapper).track(
            AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_MESSAGE to message,
                AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_REMOTE,
                AnalyticsTracker.KEY_ORDER_ID to defaultOrderId
            )
        )
    }

    @Test
    fun `When the remote validation returns Error, error type name is tracked`() = testBlocking {
        // Given the remote validation returns Error
        val pluginInfo = WooPlugin(isActive = true, isInstalled = true, version = null)
        whenever(orderDetailRepository.getWooServicesPluginInfo()).thenReturn(pluginInfo)

        whenever(
            shippingLabelAddressValidator.validateAddress(
                address = defaultAddress,
                type = ShippingLabelAddressValidator.AddressType.DESTINATION,
                requiresPhoneNumber = false
            )
        ).thenReturn(ShippingLabelAddressValidator.ValidationResult.Error(WooErrorType.GENERIC_ERROR))

        // When the address is validated
        addressValidator.validate(defaultOrderId, defaultAddress)

        // Then the error type name is tracked
        verify(trackerWrapper).track(
            AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_MESSAGE to WooErrorType.GENERIC_ERROR.name,
                AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_REMOTE,
                AnalyticsTracker.KEY_ORDER_ID to defaultOrderId
            )
        )
    }

    @Test
    fun `When the remote validation returns SuggestedChanges, no event is tracked`() = testBlocking {
        // Given the remote validation returns Error
        val pluginInfo = WooPlugin(isActive = true, isInstalled = true, version = null)
        whenever(orderDetailRepository.getWooServicesPluginInfo()).thenReturn(pluginInfo)

        whenever(
            shippingLabelAddressValidator.validateAddress(
                address = defaultAddress,
                type = ShippingLabelAddressValidator.AddressType.DESTINATION,
                requiresPhoneNumber = false
            )
        ).thenReturn(
            ShippingLabelAddressValidator.ValidationResult.SuggestedChanges(
                suggested = defaultAddress.copy(address1 = "Suggested address"),
                isTrivial = true
            )
        )

        // When the address is validated
        addressValidator.validate(defaultOrderId, defaultAddress)

        // Then no error is tracked
        verify(trackerWrapper, never()).track(any(), any())
    }

    @Test
    fun `When the remote validation returns Valid, no event is tracked`() = testBlocking {
        // Given the remote validation returns Error
        val pluginInfo = WooPlugin(isActive = true, isInstalled = true, version = null)
        whenever(orderDetailRepository.getWooServicesPluginInfo()).thenReturn(pluginInfo)

        whenever(
            shippingLabelAddressValidator.validateAddress(
                address = defaultAddress,
                type = ShippingLabelAddressValidator.AddressType.DESTINATION,
                requiresPhoneNumber = false
            )
        ).thenReturn(ShippingLabelAddressValidator.ValidationResult.Valid)

        // When the address is validated
        addressValidator.validate(defaultOrderId, defaultAddress)

        // Then no error is tracked
        verify(trackerWrapper, never()).track(any(), any())
    }

    @Test
    fun `When the address has no company name but has user name, no error is registered`() = testBlocking {
        // Given company name is empty
        val address = defaultAddress.copy(
            company = ""
        )
        val pluginInfo = WooPlugin(isActive = true, isInstalled = true, version = null)
        whenever(orderDetailRepository.getWooServicesPluginInfo()).thenReturn(pluginInfo)

        whenever(
            shippingLabelAddressValidator.validateAddress(
                address = address,
                type = ShippingLabelAddressValidator.AddressType.DESTINATION,
                requiresPhoneNumber = false
            )
        ).thenReturn(ShippingLabelAddressValidator.ValidationResult.Valid)

        // When the address is validated
        addressValidator.validate(defaultOrderId, address)

        // Then no error is tracked
        verify(trackerWrapper, never()).track(any(), any())
    }

    @Test
    fun `When the address has no user name but has company name, no error is registered`() = testBlocking {
        // Given name is empty
        val address = defaultAddress.copy(
            firstName = "",
            lastName = ""
        )
        val pluginInfo = WooPlugin(isActive = true, isInstalled = true, version = null)
        whenever(orderDetailRepository.getWooServicesPluginInfo()).thenReturn(pluginInfo)

        whenever(
            shippingLabelAddressValidator.validateAddress(
                address = address,
                type = ShippingLabelAddressValidator.AddressType.DESTINATION,
                requiresPhoneNumber = false
            )
        ).thenReturn(ShippingLabelAddressValidator.ValidationResult.Valid)

        // When the address is validated
        addressValidator.validate(defaultOrderId, address)

        // Then no error is tracked
        verify(trackerWrapper, never()).track(any(), any())
    }

    @Test
    fun `When the address has no phone, no error is registered`() = testBlocking {
        // Given the phone is empty
        val address = defaultAddress.copy(
            phone = ""
        )
        val pluginInfo = WooPlugin(isActive = true, isInstalled = true, version = null)
        whenever(orderDetailRepository.getWooServicesPluginInfo()).thenReturn(pluginInfo)

        whenever(
            shippingLabelAddressValidator.validateAddress(
                address = address,
                type = ShippingLabelAddressValidator.AddressType.DESTINATION,
                requiresPhoneNumber = false
            )
        ).thenReturn(ShippingLabelAddressValidator.ValidationResult.Valid)

        // When the address is validated
        addressValidator.validate(defaultOrderId, address)

        // Then no error is tracked
        verify(trackerWrapper, never()).track(any(), any())
    }
}
