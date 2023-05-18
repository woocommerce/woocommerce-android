package com.woocommerce.android.ui.orders.creation.coupon

import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class OrderCreateCouponEditionViewModelTest : BaseUnitTest() {
    @Test
    fun `given non empty coupon when passed to coupon edition screen then should show remove button`() {
        val navArgs = OrderCreateCouponEditionFragmentArgs("coupon_code").initSavedStateHandle()

        val sut = OrderCreateCouponEditionViewModel(navArgs)

        sut.viewState.observeForever {
            assertTrue(it.isRemoveButtonVisible)
        }
    }

    @Test
    fun `given empty coupon when passed to coupon edition screen then should not show remove button`() {
        val navArgs = OrderCreateCouponEditionFragmentArgs("coupon_code").initSavedStateHandle()

        val sut = OrderCreateCouponEditionViewModel(navArgs)

        sut.viewState.observeForever {
            assertFalse(it.isRemoveButtonVisible)
        }
    }

    @Test
    fun `when non empty coupon code entered then should show done button`() {
        val navArgs = OrderCreateCouponEditionFragmentArgs().initSavedStateHandle()

        val sut = OrderCreateCouponEditionViewModel(navArgs)
        sut.onCouponCodeChanged("new_code")

        sut.viewState.observeForever {
            assertTrue(it.isDoneButtonEnabled)
        }
    }

    @Test
    fun `when empty coupon code entered then should not show done button`() {
        val navArgs = OrderCreateCouponEditionFragmentArgs().initSavedStateHandle()

        val sut = OrderCreateCouponEditionViewModel(navArgs)
        sut.onCouponCodeChanged("new_code")

        sut.viewState.observeForever {
            assertFalse(it.isDoneButtonEnabled)
        }
    }

    @Test
    fun `given non empty coupon when coupon removed then should clear coupon`() {
        val initialCouponCode = "coupon_code"
        val navArgs = OrderCreateCouponEditionFragmentArgs(initialCouponCode).initSavedStateHandle()

        val sut = OrderCreateCouponEditionViewModel(navArgs)

        sut.viewState.observeForever {
            assertTrue(it.couponCode.isNotNullOrEmpty())
            assertEquals(initialCouponCode, it.couponCode)
        }

        sut.onCouponRemoved()

        sut.event.observeForever {
            assertEquals(OrderCreateCouponEditionViewModel.UpdateCouponCode(""), it)
        }
    }

    @Test
    fun `given non empty coupon when done button is clicked then should update order with coupon`() {
        val initialCouponCode = "coupon_code"
        val navArgs = OrderCreateCouponEditionFragmentArgs(initialCouponCode).initSavedStateHandle()

        val sut = OrderCreateCouponEditionViewModel(navArgs)

        sut.viewState.observeForever {
            assertTrue(it.couponCode.isNotNullOrEmpty())
            assertEquals(initialCouponCode, it.couponCode)
        }

        sut.onCouponCodeChanged("new_code")
        sut.onDoneClicked()

        sut.event.observeForever {
            assertEquals(OrderCreateCouponEditionViewModel.UpdateCouponCode("new_code"), it)
        }
    }

    @Test
    fun `given non empty coupon when remove button is clicked then should remove coupon from order`() {
        val initialCouponCode = "coupon_code"
        val navArgs = OrderCreateCouponEditionFragmentArgs(initialCouponCode).initSavedStateHandle()

        val sut = OrderCreateCouponEditionViewModel(navArgs)

        sut.viewState.observeForever {
            assertTrue(it.couponCode.isNotNullOrEmpty())
            assertEquals(initialCouponCode, it.couponCode)
        }

        sut.onCouponRemoved()

        sut.event.observeForever {
            assertEquals(OrderCreateCouponEditionViewModel.UpdateCouponCode(""), it)
        }
    }
}
