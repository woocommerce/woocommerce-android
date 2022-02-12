package com.woocommerce.android.ui.orders.creation.fees

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationAddFeeViewModel.*
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal

class OrderCreationAddFeeViewModelTest : BaseUnitTest() {
    private lateinit var sut: OrderCreationAddFeeViewModel

    @Before
    fun setUp() {
        val siteModelMock: SiteModel = mock()
        val siteMock: SelectedSite = mock {
            on { get() } doReturn siteModelMock
        }
        val settingsMock: WCSettingsModel = mock {
            on { currencyDecimalNumber } doReturn 123
        }
        val storeMock: WooCommerceStore = mock {
            on { getSiteSettings(siteModelMock) } doReturn settingsMock
        }

        sut = OrderCreationAddFeeViewModel(
            SavedStateHandle(),
            siteMock,
            storeMock
        )
    }

    @Test
    fun `when submitting fee as percentage, then trigger UpdateFee with expected data`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever { lastReceivedEvent = it }

        sut.onFeeAmountChanged(BigDecimal(123))
        sut.onFeePercentageChanged("25")
        sut.onPercentageSwitchChanged(isChecked = true)

        sut.onDoneSelected()

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? UpdateFee }
            ?.let { updateFeeEvent ->
                assertThat(updateFeeEvent.amount).isEqualTo(BigDecimal(25))
                assertThat(updateFeeEvent.feeType).isEqualTo(FeeType.PERCENTAGE)
            } ?: fail("Last event should be of UpdateFee type")
    }

    @Test
    fun `when submitting fee as amount, then trigger UpdateFee with expected data`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever { lastReceivedEvent = it }

        sut.onFeeAmountChanged(BigDecimal(123))
        sut.onFeePercentageChanged("25")
        sut.onPercentageSwitchChanged(isChecked = false)

        sut.onDoneSelected()

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? UpdateFee }
            ?.let { updateFeeEvent ->
                assertThat(updateFeeEvent.amount).isEqualTo(BigDecimal(123))
                assertThat(updateFeeEvent.feeType).isEqualTo(FeeType.AMOUNT)
            } ?: fail("Last event should be of UpdateFee type")
    }

    @Test
    fun `when submitting fee with initial state, then trigger UpdateFee as AMOUNT with zero`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever { lastReceivedEvent = it }

        sut.onDoneSelected()

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? UpdateFee }
            ?.let { updateFeeEvent ->
                assertThat(updateFeeEvent.amount).isEqualTo(BigDecimal.ZERO)
                assertThat(updateFeeEvent.feeType).isEqualTo(FeeType.AMOUNT)
            } ?: fail("Last event should be of UpdateFee type")
    }

    @Test
    fun `when submitting fee with invalid percentage value, then trigger UpdateFee with last valid value`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever { lastReceivedEvent = it }

        sut.onFeePercentageChanged("25")
        sut.onFeePercentageChanged("9@%@(&*%@@%*SSF-08a")
        sut.onPercentageSwitchChanged(isChecked = true)

        sut.onDoneSelected()

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? UpdateFee }
            ?.let { updateFeeEvent ->
                assertThat(updateFeeEvent.amount).isEqualTo(BigDecimal(25))
                assertThat(updateFeeEvent.feeType).isEqualTo(FeeType.PERCENTAGE)
            } ?: fail("Last event should be of UpdateFee type")
    }
}
