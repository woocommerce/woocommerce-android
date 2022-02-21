package com.woocommerce.android.ui.orders.creation.fees

import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationEditFeeViewModel.*
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.math.BigDecimal
import kotlin.test.assertTrue

class OrderCreationEditFeeViewModelTest : BaseUnitTest() {
    companion object {
        private val DEFAULT_ORDER_TOTAL = BigDecimal(2000)
        private val DEFAULT_FEE_VALUE = BigDecimal(250)
    }
    private lateinit var sut: OrderCreationEditFeeViewModel
    private var savedState = OrderCreationEditFeeFragmentArgs(DEFAULT_ORDER_TOTAL).initSavedStateHandle()

    @Before
    fun setUp() = initSut()

    private fun initSut() {
        sut = OrderCreationEditFeeViewModel(
                savedState,
                mock(),
                mock()
        )
    }

    @Test
    fun `when initializing the viewModel with existing navArgs currentFeeValue, then set fee amount based on it`() {
        var lastReceivedEvent: Event? = null

        savedState = OrderCreationEditFeeFragmentArgs(DEFAULT_ORDER_TOTAL, DEFAULT_FEE_VALUE)
            .initSavedStateHandle()

        initSut()

        sut.event.observeForever { lastReceivedEvent = it }

        sut.onDoneSelected()

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? UpdateFee }
            ?.let { updateFeeEvent ->
                assertThat(updateFeeEvent.amount).isEqualTo(DEFAULT_FEE_VALUE)
            } ?: fail("Last event should be of UpdateFee type")
    }

    @Test
    fun `when initializing the viewModel with existing navArgs currentFeeValue, then set fee percentage based on it`() {
        var lastReceivedEvent: Event? = null

        savedState = OrderCreationEditFeeFragmentArgs(DEFAULT_ORDER_TOTAL, DEFAULT_FEE_VALUE)
            .initSavedStateHandle()

        initSut()

        sut.event.observeForever { lastReceivedEvent = it }

        sut.onPercentageSwitchChanged(isChecked = true)
        sut.onDoneSelected()

        assertTrue(sut.viewStateData.liveData.value?.feePercentage.isEqualTo(BigDecimal(12.5)))
        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? UpdateFee }
            ?.let { updateFeeEvent ->
                assert(updateFeeEvent.amount.isEqualTo(DEFAULT_FEE_VALUE))
            } ?: fail("Last event should be of UpdateFee type")
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
                assertThat(updateFeeEvent.amount).isEqualTo(BigDecimal(500))
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
            } ?: fail("Last event should be of UpdateFee type")
    }

    @Test
    fun `when percentage switch is changed, then change viewState to the respective value`() {
        var lastReceivedChange: Boolean? = null
        sut.viewStateData.observeForever { _, viewState ->
            lastReceivedChange = viewState.isPercentageSelected
        }

        sut.onPercentageSwitchChanged(isChecked = true)
        assertThat(lastReceivedChange).isTrue

        sut.onPercentageSwitchChanged(isChecked = false)
        assertThat(lastReceivedChange).isFalse
    }
}
