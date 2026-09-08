package com.woocommerce.android.ui.orders.creation.taxes.rates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRateSelectorViewModel.ViewState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The empty state renders a header plus an illustration/text/button inside the same
 * scrollable [androidx.compose.foundation.lazy.LazyColumn] as the populated list, so it can
 * overflow a short viewport and must still be scrollable - it is not exempt just because the
 * list of tax rates itself is empty.
 */
@RunWith(AndroidJUnit4::class)
class TaxRateSelectorScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenEmptyStateOverflowsViewportWhenScrolledThenToolbarDividerAppears() {
        val state = MutableStateFlow(ViewState())
        composeTestRule.setContent {
            WooThemeWithBackground {
                Box(modifier = Modifier.height(CONSTRAINED_VIEWPORT_HEIGHT)) {
                    TaxRateSelectorScreen(
                        viewState = state,
                        onEditTaxRatesInAdminClicked = {},
                        onInfoIconClicked = {},
                        onTaxRateClick = {},
                        onDismiss = {},
                        onLoadMore = {},
                        onEmptyScreenButtonClicked = {},
                        onAutoRateToggleStateToggled = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(TOP_APP_BAR_DIVIDER_TEST_TAG).assertDoesNotExist()

        val emptyStateButtonText = context.getString(R.string.tax_rate_selector_empty_list_button)
        composeTestRule.onNodeWithText(emptyStateButtonText).performScrollTo()

        composeTestRule.onNodeWithTag(TOP_APP_BAR_DIVIDER_TEST_TAG).assertIsDisplayed()
    }

    private companion object {
        // Mirrors the internal WOO_TOP_APP_BAR_DIVIDER_TEST_TAG constant in
        // libs/store-design-system's WooTopAppBar.kt, which isn't visible from this module.
        const val TOP_APP_BAR_DIVIDER_TEST_TAG = "woo_top_app_bar_divider"
        val CONSTRAINED_VIEWPORT_HEIGHT = 320.dp
    }
}
