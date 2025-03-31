package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveShippingLabelNoticeTests : BaseUnitTest() {
    private val addressValidationHelper: AddressValidationHelper = mock()
    private val coroutineScope: CoroutineScope = TestScope(coroutinesTestRule.testDispatcher)

    private val sut = ObserveShippingLabelNotice(addressValidationHelper)

    private val defaultAddresses = WooShippingAddresses(
        shipFrom = OriginShippingAddress.EMPTY.copy(
            id = "1",
            firstName = "John",
            lastName = "Doe",
            address1 = "123 Main St",
            isVerified = true
        ),
        shipTo = DestinationShippingAddress(
            address = Address.EMPTY.copy(
                firstName = "John",
                lastName = "Doe",
                address1 = "123 Main St"
            ),
            isVerified = true
        ),
        originAddresses = emptyList()
    )
    private val defaultAddressesFlow = flowOf(defaultAddresses)
    private val customsFlow = flowOf(CustomsState.NotRequired)

    @Test
    fun `when no issues, then don't display any notification`() = runTest {
        val result = sut.invoke(defaultAddressesFlow, customsFlow, coroutineScope).first()
        assertThat(result).isNull()
    }

    @Test
    fun `when missing origin address was displayed but it is now verified, then display verified notice`() = runTest {
        val missingOriginAddress = defaultAddresses.copy(shipFrom = OriginShippingAddress.EMPTY)
        val result = sut.invoke(flowOf(missingOriginAddress, defaultAddresses), customsFlow, coroutineScope)
            .take(2)
            .last()
        assertThat(result?.type).isEqualTo(NoticeType.VERIFIED_ORIGIN_ADDRESS)
    }

    @Test
    fun `when missing destination address was displayed but it is now verified, then display verified notice`() =
        runTest {
            val missingDestinationAddress = defaultAddresses.copy(shipTo = DestinationShippingAddress.EMPTY)
            val result = sut.invoke(flowOf(missingDestinationAddress, defaultAddresses), customsFlow, coroutineScope)
                .take(2)
                .last()
            assertThat(result?.type).isEqualTo(NoticeType.VERIFIED_DESTINATION_ADDRESS)
        }

    @Test
    fun `when shipFrom is not verified, then display origin not verified`() = runTest {
        val addresses = defaultAddresses.copy(shipFrom = defaultAddresses.shipFrom.copy(isVerified = false))

        val result = sut.invoke(flowOf(addresses), customsFlow, coroutineScope).first()

        assertThat(result?.type).isEqualTo(NoticeType.UNVERIFIED_ORIGIN_ADDRESS)
    }

    @Test
    fun `when shipTo is not verified, then display destination not verified`() = runTest {
        val addresses = defaultAddresses.copy(shipTo = defaultAddresses.shipTo.copy(isVerified = false))

        val result = sut.invoke(flowOf(addresses), customsFlow, coroutineScope).first()

        assertThat(result?.type).isEqualTo(NoticeType.UNVERIFIED_DESTINATION_ADDRESS)
    }

    @Test
    fun `when shipTo is missing, then display destination missing`() = runTest {
        val missingDestinationAddress = defaultAddresses.copy(shipTo = DestinationShippingAddress.EMPTY)

        whenever(
            addressValidationHelper.isMissingDestinationAddress(missingDestinationAddress.shipTo.address)
        ) doReturn true

        val result = sut.invoke(flowOf(missingDestinationAddress), customsFlow, coroutineScope).first()

        assertThat(result?.type).isEqualTo(NoticeType.MISSING_DESTINATION_ADDRESS)
    }

    @Test
    fun `testing both addresses and customs with issues flow`() = runTest {
        var addresses = defaultAddresses.copy(
            shipTo = defaultAddresses.shipTo.copy(isVerified = false),
            shipFrom = defaultAddresses.shipFrom.copy(isVerified = false)
        )
        val addressesFlow = MutableStateFlow(addresses)
        val missingCustoms = MutableStateFlow<CustomsState>(CustomsState.ItnMissing)

        // When address have issues, then display origin warnings first
        var result = sut.invoke(addressesFlow, missingCustoms, coroutineScope)

        assertThat(result.first()?.type).isEqualTo(NoticeType.UNVERIFIED_ORIGIN_ADDRESS)

        // Fix origin issue
        addresses = addresses.copy(shipFrom = defaultAddresses.shipFrom.copy(isVerified = true))
        addressesFlow.value = addresses

        assertThat(result.first()?.type).isEqualTo(NoticeType.UNVERIFIED_DESTINATION_ADDRESS)

        // Fix destination issue
        addresses = defaultAddresses
        addressesFlow.value = addresses

        assertThat(result.first()?.type).isEqualTo(NoticeType.VERIFIED_DESTINATION_ADDRESS)

        // Wait 2 seconds for auto dismiss
        delay(AUTO_DISMISS_TIME)

        assertThat(result.first()?.type).isEqualTo(NoticeType.MISSING_ITN)

        // Fix ITN issue
        missingCustoms.value = CustomsState.NotRequired

        assertThat(result.first()?.type).isNull()
    }

    @Test
    fun `when missing itn, then display itn notice`() = runTest {
        val missingCustoms = flowOf(CustomsState.ItnMissing)

        val result = sut.invoke(defaultAddressesFlow, missingCustoms, coroutineScope).first()
        assertThat(result?.type).isEqualTo(NoticeType.MISSING_ITN)
    }

    @Test
    fun `when notice dismissed, then display no notice`() = runTest {
        val missingCustoms = flowOf(CustomsState.ItnMissing)

        val result = sut.invoke(defaultAddressesFlow, missingCustoms, coroutineScope)

        result.first()?.onDismissed?.invoke()

        assertThat(result.first()).isNull()
    }
}
