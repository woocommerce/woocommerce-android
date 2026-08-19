package com.woocommerce.android.ui.woopos.home.totals

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
class WooPosCheckoutBottomBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `cardEnabled_otherMethodsPresent_rendersCashAndOther`() {
        composeTestRule.setContent {
            WooPosTheme {
                CheckoutBottomBar(
                    state = checkoutState(isCardPaymentEnabledForCountry = true),
                    onUIEvent = {},
                    hasOtherPaymentMethods = true,
                )
            }
        }

        composeTestRule.onNodeWithTag(WooPosTestTags.CASH_PAYMENT_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WooPosTestTags.OTHER_PAYMENT_METHODS_BUTTON).assertIsDisplayed()
    }

    @Test
    fun `cardEnabled_noOtherMethods_rendersOnlyCash`() {
        composeTestRule.setContent {
            WooPosTheme {
                CheckoutBottomBar(
                    state = checkoutState(isCardPaymentEnabledForCountry = true),
                    onUIEvent = {},
                    hasOtherPaymentMethods = false,
                )
            }
        }

        composeTestRule.onNodeWithTag(WooPosTestTags.CASH_PAYMENT_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WooPosTestTags.OTHER_PAYMENT_METHODS_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `cardDisabled_otherMethodsPresent_rendersCashAndOther`() {
        composeTestRule.setContent {
            WooPosTheme {
                CheckoutBottomBar(
                    state = checkoutState(isCardPaymentEnabledForCountry = false),
                    onUIEvent = {},
                    hasOtherPaymentMethods = true,
                )
            }
        }

        composeTestRule.onNodeWithTag(WooPosTestTags.CASH_PAYMENT_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WooPosTestTags.OTHER_PAYMENT_METHODS_BUTTON).assertIsDisplayed()
    }

    @Test
    fun `cardDisabled_noOtherMethods_rendersOnlyCash`() {
        composeTestRule.setContent {
            WooPosTheme {
                CheckoutBottomBar(
                    state = checkoutState(isCardPaymentEnabledForCountry = false),
                    onUIEvent = {},
                    hasOtherPaymentMethods = false,
                )
            }
        }

        composeTestRule.onNodeWithTag(WooPosTestTags.CASH_PAYMENT_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WooPosTestTags.OTHER_PAYMENT_METHODS_BUTTON).assertDoesNotExist()
    }

    private fun checkoutState(isCardPaymentEnabledForCountry: Boolean) = WooPosTotalsViewState.Checkout(
        totals = WooPosTotalsViewState.Totals.Visible(
            orderDiscountText = null,
            orderSubtotalText = "$10.00",
            orderTaxText = "$1.00",
            orderTotalText = "$11.00",
        ),
        readerStatus = WooPosTotalsViewState.ReaderStatus.Unavailable,
        isCardPaymentEnabledForCountry = isCardPaymentEnabledForCountry,
    )
}
