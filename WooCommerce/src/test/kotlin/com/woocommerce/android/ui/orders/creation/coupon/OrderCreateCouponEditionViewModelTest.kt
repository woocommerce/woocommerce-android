package com.woocommerce.android.ui.orders.creation.coupon

import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
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
    fun `when non empty coupon code entered then should show done button`() {
        val navArgs = OrderCreateCouponEditionFragmentArgs().initSavedStateHandle()

        val sut = OrderCreateCouponEditionViewModel(navArgs)
        sut.onCouponCodeChanged("new_code")

        sut.viewState.observeForever {
            assertTrue(it.isDoneButtonEnabled)
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
    fun `given non empty coupon when coupon edited then should update coupon`() {
        val initialCouponCode = "coupon_code"
        val navArgs = OrderCreateCouponEditionFragmentArgs(initialCouponCode).initSavedStateHandle()

        val sut = OrderCreateCouponEditionViewModel(navArgs)

        sut.viewState.observeForever {
            assertTrue(it.couponCode.isNotNullOrEmpty())
            assertEquals(initialCouponCode, it.couponCode)
        }

        sut.onCouponCodeChanged("new_code")

        sut.event.observeForever {
            assertEquals(OrderCreateCouponEditionViewModel.UpdateCouponCode("new_code"), it)
        }
    }
}
