package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCustomAmountCartHandlerTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val sut = WooPosCustomAmountCartHandler()

    @Test
    fun `when add submission is applied, then returns Added with new item using next item number`() = runTest {
        // WHEN
        val result = sut.applySubmittedToCart(
            event = ParentToChildrenEvent.CustomAmountSubmitted(
                name = "Tip",
                amount = BigDecimal("12.50"),
                isTaxable = false,
                editingItemNumber = null,
            ),
            formattedAmount = "$12.50",
            nextItemNumber = { 9 },
        )

        // THEN
        assertThat(result).isInstanceOf(WooPosCustomAmountCartHandler.SubmittedResult.Added::class.java)
        val added = result as WooPosCustomAmountCartHandler.SubmittedResult.Added
        assertThat(added.newItem.itemNumber).isEqualTo(9)
        assertThat(added.newItem.name).isEqualTo("Tip")
        assertThat(added.newItem.amount).isEqualByComparingTo(BigDecimal("12.50"))
        assertThat(added.newItem.formattedAmount).isEqualTo("$12.50")
        assertThat(added.newItem.isTaxable).isFalse()
    }

    @Test
    fun `when edit submission is applied, then returns Edited carrying editing item number and updated item`() =
        runTest {
            // WHEN
            val result = sut.applySubmittedToCart(
                event = ParentToChildrenEvent.CustomAmountSubmitted(
                    name = "Service updated",
                    amount = BigDecimal("30.00"),
                    isTaxable = true,
                    editingItemNumber = 3,
                ),
                formattedAmount = "$30.00",
                nextItemNumber = { error("Should not request a new item number on edit") },
            )

            // THEN
            assertThat(result).isInstanceOf(WooPosCustomAmountCartHandler.SubmittedResult.Edited::class.java)
            val edited = result as WooPosCustomAmountCartHandler.SubmittedResult.Edited
            assertThat(edited.editingItemNumber).isEqualTo(3)
            assertThat(edited.updatedItem.itemNumber).isEqualTo(3)
            assertThat(edited.updatedItem.name).isEqualTo("Service updated")
            assertThat(edited.updatedItem.amount).isEqualByComparingTo(BigDecimal("30.00"))
            assertThat(edited.updatedItem.formattedAmount).isEqualTo("$30.00")
            assertThat(edited.updatedItem.isTaxable).isTrue()
        }
}
