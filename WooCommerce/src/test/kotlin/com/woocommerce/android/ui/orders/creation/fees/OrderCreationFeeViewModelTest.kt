package com.woocommerce.android.ui.orders.creation.fees

import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationFeeViewModel.RemoveFee
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationFeeViewModel.UpdateFee
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode.HALF_UP
import kotlin.test.assertTrue

class OrderCreationFeeViewModelTest : BaseUnitTest() {
    companion object {
        private val DEFAULT_ORDER_SUB_TOTAL = BigDecimal(2000)
        private val DEFAULT_FEE_VALUE = BigDecimal(250)
    }
    private lateinit var sut: OrderCreationFeeViewModel
    private var savedState = OrderCreationFeeFragmentArgs(DEFAULT_ORDER_SUB_TOTAL).initSavedStateHandle()

    @Before
    fun setUp() = initSut()

    private fun initSut() {
        sut = OrderCreationFeeViewModel(savedState)
    }

    @Test
    fun `when initializing the viewModel with existing navArgs currentFeeValue, then set fee amount based on it`() {
        var lastReceivedEvent: Event? = null

        savedState = OrderCreationFeeFragmentArgs(
            orderSubTotal = DEFAULT_ORDER_SUB_TOTAL,
            currentFeeValue = DEFAULT_FEE_VALUE
        ).initSavedStateHandle()
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

        savedState = OrderCreationFeeFragmentArgs(
            orderSubTotal = DEFAULT_ORDER_SUB_TOTAL,
            currentFeeValue = DEFAULT_FEE_VALUE
        ).initSavedStateHandle()
        initSut()
        sut.event.observeForever { lastReceivedEvent = it }

        sut.onPercentageSwitchChanged(isChecked = true)
        sut.onDoneSelected()

        assertThat(sut.viewStateData.liveData.value?.feePercentage).isEqualTo(BigDecimal(12.5))
        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? UpdateFee }
            ?.let { updateFeeEvent ->
                assertTrue(updateFeeEvent.amount.isEqualTo(DEFAULT_FEE_VALUE))
            } ?: fail("Last event should be of UpdateFee type")
    }

    @Test
    fun `when initializing the viewModel with order total with only the fee, then set fee percentage as zero`() {
        var lastReceivedEvent: Event? = null

        savedState = OrderCreationFeeFragmentArgs(
            orderSubTotal = BigDecimal.ZERO,
            currentFeeValue = DEFAULT_FEE_VALUE
        ).initSavedStateHandle()
        initSut()
        sut.event.observeForever { lastReceivedEvent = it }

        sut.onPercentageSwitchChanged(isChecked = true)
        sut.onDoneSelected()

        assertThat(sut.viewStateData.liveData.value?.feePercentage).isEqualTo(BigDecimal.ZERO)
        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? UpdateFee }
            ?.let { updateFeeEvent ->
                assertTrue(updateFeeEvent.amount.isEqualTo(BigDecimal.ZERO))
            } ?: fail("Last event should be of UpdateFee type")
    }

    @Test
    fun `when initializing the viewModel with non-terminating decimal percentage, then divide with rounding`() {
        var lastReceivedEvent: Event? = null
        val percentageBase = BigDecimal(100)
        val feeTotal = BigDecimal(500)
        val orderSubtotal = BigDecimal(515)

        val expectedPercentageValue =
            // obtaining percentage value from order total and fee total values
            (feeTotal.divide(orderSubtotal, 4, HALF_UP) * percentageBase).stripTrailingZeros()

        val expectedRecalculatedFeeTotal =
            ((orderSubtotal * expectedPercentageValue) / percentageBase)
                .round(MathContext(4))

        savedState = OrderCreationFeeFragmentArgs(orderSubtotal, feeTotal)
            .initSavedStateHandle()
        initSut()
        sut.event.observeForever { lastReceivedEvent = it }

        sut.onPercentageSwitchChanged(isChecked = true)
        sut.onDoneSelected()

        assertThat(sut.viewStateData.liveData.value?.feePercentage).isEqualTo(expectedPercentageValue)
        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? UpdateFee }
            ?.let { updateFeeEvent ->
                assertTrue(updateFeeEvent.amount.isEqualTo(expectedRecalculatedFeeTotal))
            } ?: fail("Last event should be of UpdateFee type")
    }

    @Test
    fun `when submitting fee as percentage, then trigger UpdateFee with expected data`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever { lastReceivedEvent = it }

        sut.onFeeAmountChanged(BigDecimal(123))
        sut.onPercentageSwitchChanged(isChecked = true)
        sut.onFeePercentageChanged("25")

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

        sut.onPercentageSwitchChanged(isChecked = false)
        sut.onFeeAmountChanged(BigDecimal(123))
        sut.onFeePercentageChanged("25")

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
    fun `when remove fee button is clicked, then trigger UpdateFee with zero as amount`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever { lastReceivedEvent = it }

        sut.onFeeAmountChanged(BigDecimal(123))
        sut.onFeePercentageChanged("25")
        sut.onFeePercentageChanged("9@%@(&*%@@%*SSF-08a")
        sut.onPercentageSwitchChanged(isChecked = true)

        sut.onRemoveFeeClicked()

        assertThat(lastReceivedEvent).isNotNull
        assertThat(lastReceivedEvent).isInstanceOf(RemoveFee::class.java)
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

    @Test
    fun `when current fee is existent, then set showDisplayRemoveFeeButton to true`() {
        var lastReceivedChange: Boolean? = null
        savedState = OrderCreationFeeFragmentArgs(DEFAULT_ORDER_SUB_TOTAL, DEFAULT_FEE_VALUE)
            .initSavedStateHandle()
        initSut()

        sut.viewStateData.observeForever { _, viewState ->
            lastReceivedChange = viewState.shouldDisplayRemoveFeeButton
        }

        assertThat(lastReceivedChange).isTrue
    }

    @Test
    fun `when current fee is null, then set showDisplayRemoveFeeButton to false`() {
        var lastReceivedChange: Boolean? = null
        savedState = OrderCreationFeeFragmentArgs(DEFAULT_ORDER_SUB_TOTAL)
            .initSavedStateHandle()
        initSut()

        sut.viewStateData.observeForever { _, viewState ->
            lastReceivedChange = viewState.shouldDisplayRemoveFeeButton
        }

        assertThat(lastReceivedChange).isFalse
    }

    @Test
    fun `when order total is zero, then set shouldDisplayPercentageSwitch to false`() {
        var lastReceivedChange: Boolean? = null
        savedState = OrderCreationFeeFragmentArgs(BigDecimal.ZERO)
            .initSavedStateHandle()
        initSut()

        sut.viewStateData.observeForever { _, viewState ->
            lastReceivedChange = viewState.shouldDisplayPercentageSwitch
        }

        assertThat(lastReceivedChange).isFalse
    }

    @Test
    fun `when order total contains only the fee value itself, then set shouldDisplayPercentageSwitch to false`() {
        var lastReceivedChange: Boolean? = null
        savedState = OrderCreationFeeFragmentArgs(BigDecimal.ZERO, DEFAULT_FEE_VALUE)
            .initSavedStateHandle()
        initSut()

        sut.viewStateData.observeForever { _, viewState ->
            lastReceivedChange = viewState.shouldDisplayPercentageSwitch
        }

        assertThat(lastReceivedChange).isFalse
    }

    @Test
    fun `when order total is not zero, then set shouldDisplayPercentageSwitch to true`() {
        var lastReceivedChange: Boolean? = null
        savedState = OrderCreationFeeFragmentArgs(DEFAULT_ORDER_SUB_TOTAL)
            .initSavedStateHandle()
        initSut()

        sut.viewStateData.observeForever { _, viewState ->
            lastReceivedChange = viewState.shouldDisplayPercentageSwitch
        }

        assertThat(lastReceivedChange).isTrue
    }

    @Test
    fun `when fee value start as zero, then set isDoneButtonEnabled to false`() {
        var lastReceivedChange: Boolean? = null
        savedState = OrderCreationFeeFragmentArgs(BigDecimal.ZERO, BigDecimal.ZERO)
            .initSavedStateHandle()
        initSut()

        sut.viewStateData.observeForever { _, viewState ->
            lastReceivedChange = viewState.isDoneButtonEnabled
        }

        assertThat(lastReceivedChange).isFalse
    }

    @Test
    fun `when fee value starts as bigger than zero, then set isDoneButtonEnabled to true`() {
        var lastReceivedChange: Boolean? = null
        savedState = OrderCreationFeeFragmentArgs(DEFAULT_ORDER_SUB_TOTAL, DEFAULT_FEE_VALUE)
            .initSavedStateHandle()
        initSut()

        sut.viewStateData.observeForever { _, viewState ->
            lastReceivedChange = viewState.isDoneButtonEnabled
        }

        assertThat(lastReceivedChange).isTrue
    }

    @Test
    fun `when fee value starts as negative amount, then set isDoneButtonEnabled to true`() {
        var lastReceivedChange: Boolean? = null
        savedState = OrderCreationFeeFragmentArgs(DEFAULT_ORDER_SUB_TOTAL, BigDecimal(-25))
            .initSavedStateHandle()
        initSut()

        sut.viewStateData.observeForever { _, viewState ->
            lastReceivedChange = viewState.isDoneButtonEnabled
        }

        assertThat(lastReceivedChange).isTrue
    }

    @Test
    fun `when fee amount is set to a negative value, then set isDoneButtonEnabled to true`() {
        var lastReceivedChange: Boolean? = null
        savedState = OrderCreationFeeFragmentArgs(DEFAULT_ORDER_SUB_TOTAL, DEFAULT_FEE_VALUE)
            .initSavedStateHandle()
        initSut()

        sut.viewStateData.observeForever { _, viewState ->
            lastReceivedChange = viewState.isDoneButtonEnabled
        }

        assertThat(lastReceivedChange).isTrue

        sut.onFeeAmountChanged(BigDecimal(-25))

        assertThat(lastReceivedChange).isTrue
    }

    @Test
    fun `when fee amount is set to zero, then set isDoneButtonEnabled to false`() {
        var lastReceivedChange: Boolean? = null
        savedState = OrderCreationFeeFragmentArgs(DEFAULT_ORDER_SUB_TOTAL, DEFAULT_FEE_VALUE)
            .initSavedStateHandle()
        initSut()

        sut.viewStateData.observeForever { _, viewState ->
            lastReceivedChange = viewState.isDoneButtonEnabled
        }

        assertThat(lastReceivedChange).isTrue

        sut.onFeeAmountChanged(BigDecimal.ZERO)

        assertThat(lastReceivedChange).isFalse
    }

    @Test
    fun `when fee percentage is set to zero, then set isDoneButtonEnabled to false`() {
        var lastReceivedChange: Boolean? = null
        savedState = OrderCreationFeeFragmentArgs(DEFAULT_ORDER_SUB_TOTAL, DEFAULT_FEE_VALUE)
            .initSavedStateHandle()
        initSut()

        sut.viewStateData.observeForever { _, viewState ->
            lastReceivedChange = viewState.isDoneButtonEnabled
        }

        assertThat(lastReceivedChange).isTrue

        sut.onPercentageSwitchChanged(true)

        sut.onFeePercentageChanged("0")

        assertThat(lastReceivedChange).isFalse
    }
}
