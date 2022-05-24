package com.woocommerce.android.ui.coupons.edit

import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.coupons.CouponTestUtils
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CouponRestrictionsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CouponRestrictionsViewModel
    private var storedRestrictions = CouponTestUtils.generateCouponRestrictions()

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()

        viewModel = CouponRestrictionsViewModel(
            savedStateHandle = CouponRestrictionsFragmentArgs(
                restrictions = storedRestrictions,
                currencyCode = "USD",
                showLimitUsageToXItems = true
            ).initSavedStateHandle(),
        )
    }

    @Test
    fun `when individual use toggle changes, then update restrictions draft`() = testBlocking {
        storedRestrictions = storedRestrictions.copy(isForIndividualUse = false)
        setup()

        viewModel.onIndividualUseChanged(true)

        val state = viewModel.viewState.captureValues().last()
        assertThat(state.restrictions.isForIndividualUse).isTrue()
    }

    @Test
    fun `when exclude sale items toggle changes, then update restrictions draft`() = testBlocking {
        storedRestrictions = storedRestrictions.copy(areSaleItemsExcluded = false)
        setup()

        viewModel.onExcludeSaleItemsChanged(true)

        val state = viewModel.viewState.captureValues().last()
        assertThat(state.restrictions.areSaleItemsExcluded).isTrue()
    }
}
