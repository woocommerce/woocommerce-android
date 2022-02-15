package com.woocommerce.android.ui.orders.creation.fees

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationAddFeeViewModel.*
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.math.BigDecimal

class OrderCreationAddFeeViewModelTest : BaseUnitTest() {
    private lateinit var sut: OrderCreationAddFeeViewModel

    @Before
    fun setUp() {
        sut = OrderCreationAddFeeViewModel(
            SavedStateHandle(),
            mock(),
            mock()
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
    fun `when submitting fee with invalid percentage value, then trigger UpdateFee with zero as amount`() {
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
                assertThat(updateFeeEvent.amount).isEqualTo(BigDecimal.ZERO)
                assertThat(updateFeeEvent.feeType).isEqualTo(FeeType.PERCENTAGE)
            } ?: fail("Last event should be of UpdateFee type")
    }

    @Test
    fun `when percentage switch is deactivated, then trigger ChangePercentageEditTextVisibility event set as false`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever { lastReceivedEvent = it }

        sut.onPercentageSwitchChanged(isChecked = false)

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? ChangePercentageEditTextVisibility }
            ?.let { event -> assertThat(event.visible).isFalse }
            ?: fail("Last event should be of ChangePercentageEditTextVisibility type")
    }

    @Test
    fun `when percentage switch is activated, then trigger ChangePercentageEditTextVisibility event set as true`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever { lastReceivedEvent = it }

        sut.onPercentageSwitchChanged(isChecked = true)

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? ChangePercentageEditTextVisibility }
            ?.let { event -> assertThat(event.visible).isTrue }
            ?: fail("Last event should be of ChangePercentageEditTextVisibility type")
    }
}
