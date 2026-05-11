package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCustomAmountCartHandlerTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val formatPrice: WooPosFormatPrice = mock()

    @Test
    fun `when add submission is applied, then returns Added with new item using next item number`() = runTest {
        // GIVEN
        whenever(formatPrice(BigDecimal("12.50"))).thenReturn("$12.50")
        val sut = WooPosCustomAmountCartHandler(formatPrice)

        // WHEN
        val result = sut.applySubmittedToCart(
            currentBody = WooPosCartState.Body.Empty,
            event = ParentToChildrenEvent.CustomAmountSubmitted(
                name = "Tip",
                amount = BigDecimal("12.50"),
                isTaxable = false,
                editingItemNumber = null,
            ),
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
    fun `when edit submission is applied, then returns Edited with updated item and others untouched`() = runTest {
        // GIVEN
        whenever(formatPrice(BigDecimal("30.00"))).thenReturn("$30.00")
        val existing = WooPosCartItemViewState.CustomAmount(
            itemNumber = 3,
            name = "Service",
            amount = BigDecimal("15.00"),
            formattedAmount = "$15.00",
            isTaxable = false,
        )
        val product = WooPosCartItemViewState.Product.Simple(
            itemNumber = 4,
            id = 100L,
            name = "Product",
            price = "$10",
            description = null,
            imageUrl = null,
        )
        val body = WooPosCartState.Body.WithItems(listOf(existing, product))
        val sut = WooPosCustomAmountCartHandler(formatPrice)

        // WHEN
        val result = sut.applySubmittedToCart(
            currentBody = body,
            event = ParentToChildrenEvent.CustomAmountSubmitted(
                name = "Service updated",
                amount = BigDecimal("30.00"),
                isTaxable = true,
                editingItemNumber = 3,
            ),
            nextItemNumber = { error("Should not request a new item number on edit") },
        )

        // THEN
        assertThat(result).isInstanceOf(WooPosCustomAmountCartHandler.SubmittedResult.Edited::class.java)
        val edited = result as WooPosCustomAmountCartHandler.SubmittedResult.Edited
        val updated = edited.updatedItems.first { it.itemNumber == 3 } as WooPosCartItemViewState.CustomAmount
        assertThat(updated.name).isEqualTo("Service updated")
        assertThat(updated.amount).isEqualByComparingTo(BigDecimal("30.00"))
        assertThat(updated.isTaxable).isTrue()
        assertThat(edited.updatedItems).contains(product)
    }

    @Test
    fun `when edit submission targets unknown item number, then list is unchanged`() = runTest {
        // GIVEN
        whenever(formatPrice(BigDecimal("5.00"))).thenReturn("$5.00")
        val existing = WooPosCartItemViewState.CustomAmount(
            itemNumber = 1,
            name = "A",
            amount = BigDecimal("1.00"),
            formattedAmount = "$1.00",
            isTaxable = false,
        )
        val body = WooPosCartState.Body.WithItems(listOf(existing))
        val sut = WooPosCustomAmountCartHandler(formatPrice)

        // WHEN
        val result = sut.applySubmittedToCart(
            currentBody = body,
            event = ParentToChildrenEvent.CustomAmountSubmitted(
                name = "X",
                amount = BigDecimal("5.00"),
                isTaxable = false,
                editingItemNumber = 999,
            ),
            nextItemNumber = { error("Should not request a new item number on edit") },
        )

        // THEN
        val edited = result as WooPosCustomAmountCartHandler.SubmittedResult.Edited
        assertThat(edited.updatedItems).containsExactly(existing)
    }
}
