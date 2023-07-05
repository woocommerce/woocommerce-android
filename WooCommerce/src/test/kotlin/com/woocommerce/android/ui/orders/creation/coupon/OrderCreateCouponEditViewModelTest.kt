package com.woocommerce.android.ui.orders.creation.coupon

import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.creation.coupon.edit.CouponValidator
import com.woocommerce.android.ui.orders.creation.coupon.edit.OrderCreateCouponEditFragmentArgs
import com.woocommerce.android.ui.orders.creation.coupon.edit.OrderCreateCouponEditViewModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class OrderCreateCouponEditViewModelTest : BaseUnitTest() {
    private val validator: CouponValidator = mock {
        onBlocking { isCouponValid(anyString()) } doReturn true
    }
    @Test
    fun `given non empty coupon, when passed to coupon edition screen, then should show remove button`() {
        val navArgs = OrderCreateCouponEditFragmentArgs(
            OrderCreateEditViewModel.Mode.Edit(1L),
            "coupon_code"
        ).initSavedStateHandle()

        val sut = OrderCreateCouponEditViewModel(validator, navArgs)

        sut.viewState.observeForever {
            assertTrue(it.isRemoveButtonVisible)
        }
    }

    @Test
    fun `given empty coupon, when passed to coupon edition screen, then should not show remove button`() {
        val navArgs = OrderCreateCouponEditFragmentArgs(
            OrderCreateEditViewModel.Mode.Edit(1L),
            "coupon_code"
        ).initSavedStateHandle()

        val sut = OrderCreateCouponEditViewModel(validator, navArgs)

        sut.viewState.observeForever {
            assertFalse(it.isRemoveButtonVisible)
        }
    }

    @Test
    fun `when non empty coupon code entered, then should show done button`() {
        val navArgs = OrderCreateCouponEditFragmentArgs(OrderCreateEditViewModel.Mode.Edit(1L)).initSavedStateHandle()

        val sut = OrderCreateCouponEditViewModel(validator, navArgs)
        sut.onCouponCodeChanged("new_code")

        sut.viewState.observeForever {
            assertTrue(it.isDoneButtonEnabled)
        }
    }

    @Test
    fun `when empty coupon code entered, then should not show done button`() {
        val navArgs =
            OrderCreateCouponEditFragmentArgs(OrderCreateEditViewModel.Mode.Edit(1L)).initSavedStateHandle()

        val sut = OrderCreateCouponEditViewModel(validator, navArgs)
        sut.onCouponCodeChanged("new_code")

        sut.viewState.observeForever {
            assertFalse(it.isDoneButtonEnabled)
        }
    }

    @Test
    fun `given non empty coupon, when coupon removed, then should clear coupon`() {
        val initialCouponCode = "coupon_code"
        val navArgs = OrderCreateCouponEditFragmentArgs(
            OrderCreateEditViewModel.Mode.Edit(1L),
            initialCouponCode
        ).initSavedStateHandle()

        val sut = OrderCreateCouponEditViewModel(validator, navArgs)

        sut.viewState.observeForever {
            assertTrue(it.couponCode.isNotNullOrEmpty())
            assertEquals(initialCouponCode, it.couponCode)
        }

        sut.onCouponRemoved()

        sut.event.observeForever {
            assertEquals(OrderCreateCouponEditViewModel.CouponEditResult.RemoveCoupon("coupon_code"), it)
        }
    }

    @Test
    fun `given non empty coupon, when done button is clicked, then should update order with coupon`() = testBlocking {
        val initialCouponCode = "coupon_code"
        val navArgs = OrderCreateCouponEditFragmentArgs(
            OrderCreateEditViewModel.Mode.Edit(1L),
            initialCouponCode
        ).initSavedStateHandle()

        val sut = OrderCreateCouponEditViewModel(validator, navArgs)

        sut.viewState.observeForever {
            assertTrue(it.couponCode.isNotNullOrEmpty())
            assertEquals(initialCouponCode, it.couponCode)
        }

        sut.onCouponCodeChanged("new_code")
        sut.onDoneClicked()

        sut.event.observeForever {
            assertEquals(
                OrderCreateCouponEditViewModel.CouponEditResult.UpdateCouponCode(
                    "coupon_code",
                    "new_code"
                ),
                it
            )
        }
    }

    @Test
    fun `given non empty coupon, when remove button is clicked, then should remove coupon from order`() {
        val initialCouponCode = "coupon_code"
        val navArgs = OrderCreateCouponEditFragmentArgs(
            OrderCreateEditViewModel.Mode.Edit(1L),
            initialCouponCode
        ).initSavedStateHandle()

        val sut = OrderCreateCouponEditViewModel(validator, navArgs)

        sut.viewState.observeForever {
            assertTrue(it.couponCode.isNotNullOrEmpty())
            assertEquals(initialCouponCode, it.couponCode)
        }

        sut.onCouponRemoved()

        sut.event.observeForever {
            assertEquals(OrderCreateCouponEditViewModel.CouponEditResult.RemoveCoupon("coupon_code"), it)
        }
    }

    @Test
    fun `given non empty coupon, when code is modified and remove button is clicked, then should remove correct coupon from order`() {
        val initialCouponCode = "coupon_code"
        val navArgs = OrderCreateCouponEditFragmentArgs(
            OrderCreateEditViewModel.Mode.Edit(1L),
            initialCouponCode
        ).initSavedStateHandle()

        val sut = OrderCreateCouponEditViewModel(validator, navArgs)

        sut.viewState.observeForever {
            assertTrue(it.couponCode.isNotNullOrEmpty())
            assertEquals(initialCouponCode, it.couponCode)
        }
        sut.onCouponCodeChanged("coupon_co")
        sut.onCouponRemoved()

        sut.event.observeForever {
            assertEquals(OrderCreateCouponEditViewModel.CouponEditResult.RemoveCoupon("coupon_code"), it)
        }
    }

    @Test
    fun `given invalid coupon, when done clicked, then should show error`() = testBlocking {
        val navArgs =
            OrderCreateCouponEditFragmentArgs(OrderCreateEditViewModel.Mode.Edit(1L)).initSavedStateHandle()
        val sut = OrderCreateCouponEditViewModel(validator, navArgs)

        var latestViewState: OrderCreateCouponEditViewModel.ViewState? = null
        sut.viewState.observeForever {
            latestViewState = it
        }

        sut.onDoneClicked()

        assertEquals(OrderCreateCouponEditViewModel.ValidationState.ERROR, latestViewState?.validationState)
    }

    @Test
    fun `given invalid coupon, when done clicked, then should hide done button`() = testBlocking {
        val navArgs =
            OrderCreateCouponEditFragmentArgs(OrderCreateEditViewModel.Mode.Edit(1L)).initSavedStateHandle()
        val sut = OrderCreateCouponEditViewModel(validator, navArgs)

        var latestViewState: OrderCreateCouponEditViewModel.ViewState? = null
        sut.viewState.observeForever {
            latestViewState = it
        }

        sut.onDoneClicked()

        assertFalse(latestViewState!!.isDoneButtonEnabled)
    }

    @Test
    fun `given invalid coupon, when coupon code modified, then should clear error`() = testBlocking {
        // given
        val navArgs =
            OrderCreateCouponEditFragmentArgs(OrderCreateEditViewModel.Mode.Edit(1L)).initSavedStateHandle()
        val sut = OrderCreateCouponEditViewModel(validator, navArgs)
        var latestViewState: OrderCreateCouponEditViewModel.ViewState? = null
        sut.viewState.observeForever {
            latestViewState = it
        }
        sut.onDoneClicked()
        assertEquals(OrderCreateCouponEditViewModel.ValidationState.ERROR, latestViewState?.validationState)

        // when
        sut.onCouponCodeChanged("new code")

        // then
        assertEquals(OrderCreateCouponEditViewModel.ValidationState.IDLE, latestViewState?.validationState)
    }

    @Test
    fun `given invalid coupon, when coupon code modified, then should show done button`() = testBlocking {
        // given
        whenever(validator.isCouponValid(anyString())).thenReturn(false)
        val navArgs =
            OrderCreateCouponEditFragmentArgs(OrderCreateEditViewModel.Mode.Edit(1L)).initSavedStateHandle()
        val sut = OrderCreateCouponEditViewModel(validator, navArgs)
        var latestViewState: OrderCreateCouponEditViewModel.ViewState? = null
        sut.viewState.observeForever {
            latestViewState = it
        }
        sut.onCouponCodeChanged("invalid code")
        sut.onDoneClicked()
        assertFalse(latestViewState!!.isDoneButtonEnabled)

        // when
        sut.onCouponCodeChanged("new code")

        // then
        assertTrue(latestViewState!!.isDoneButtonEnabled)
    }
}
