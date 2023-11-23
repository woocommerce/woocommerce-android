package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.CustomAmountUIModel
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsDialogViewModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class MapFeeLineToCustomAmountUIModelTest : BaseUnitTest() {

    @Test
    fun `given fee line, when mapper called, then map to custom amount UI model`() {
        val feeLine = Order.FeeLine(
            id = 1,
            name = "Test Amount",
            total = BigDecimal.TEN,
            totalTax = BigDecimal.ZERO,
            taxStatus = Order.FeeLine.FeeLineTaxStatus.UNKNOWN
        )
        val expectedResult = CustomAmountUIModel(
            id = 1,
            amount = BigDecimal.TEN,
            name = "Test Amount",
            taxStatus = CustomAmountsDialogViewModel.TaxStatus(isTaxable = false)
        )

        val mapperResult = MapFeeLineToCustomAmountUiModel().invoke(feeLine)

        assertThat(mapperResult).isEqualTo(expectedResult)
    }

    @Test
    fun `given fee line with tax status as NONE, when mapper called, then map to custom amount UI model`() {
        val feeLine = Order.FeeLine(
            id = 1,
            name = "Test Amount",
            total = BigDecimal.TEN,
            totalTax = BigDecimal.ZERO,
            taxStatus = Order.FeeLine.FeeLineTaxStatus.NONE
        )
        val expectedResult = CustomAmountUIModel(
            id = 1,
            amount = BigDecimal.TEN,
            name = "Test Amount",
            taxStatus = CustomAmountsDialogViewModel.TaxStatus(isTaxable = false)
        )

        val mapperResult = MapFeeLineToCustomAmountUiModel().invoke(feeLine)

        assertThat(mapperResult).isEqualTo(expectedResult)
    }

    @Test
    fun `given fee line with tax status as TAXABLE, when mapper called, then map to custom amount UI model`() {
        val feeLine = Order.FeeLine(
            id = 1,
            name = "Test Amount",
            total = BigDecimal.TEN,
            totalTax = BigDecimal.ZERO,
            taxStatus = Order.FeeLine.FeeLineTaxStatus.TAXABLE
        )
        val expectedResult = CustomAmountUIModel(
            id = 1,
            amount = BigDecimal.TEN,
            name = "Test Amount",
            taxStatus = CustomAmountsDialogViewModel.TaxStatus(isTaxable = true)
        )

        val mapperResult = MapFeeLineToCustomAmountUiModel().invoke(feeLine)

        assertThat(mapperResult).isEqualTo(expectedResult)
    }
}
