package com.woocommerce.android.ui.woopos.home.cart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.util.WooPosTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WooPosCartScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun productRowDoesNotExposeCouponTestTag() {
        setCartItem(
            WooPosCartItemViewState.Product.Simple(
                itemNumber = 1,
                id = 1,
                name = "Product",
                price = "$10.00",
                description = null,
                imageUrl = null,
            )
        )

        composeTestRule.onNodeWithTag(WooPosTestTags.CART_COUPON_ITEM).assertDoesNotExist()
    }

    @Test
    fun couponRowExposesCouponTestTag() {
        setCartItem(
            WooPosCartItemViewState.Coupon(
                itemNumber = 1,
                id = 1,
                name = "Coupon",
                summary = "10% off",
            )
        )

        composeTestRule.onNodeWithTag(WooPosTestTags.CART_COUPON_ITEM).assertIsDisplayed()
    }

    private fun setCartItem(item: WooPosCartItemViewState) {
        composeTestRule.setContent {
            WooPosTheme {
                WooPosCartScreen(
                    state = WooPosCartState(
                        body = WooPosCartState.Body.WithItems(listOf(item)),
                        checkoutButtonState = WooPosCartState.CheckoutButtonState.Invisible,
                    ),
                    onUIEvent = {},
                    checkoutSlot = WooPosCartCheckoutButtonSlot.External,
                )
            }
        }
    }
}
