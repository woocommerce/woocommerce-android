package com.woocommerce.android.ui.products.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import kotlinx.coroutines.flow.emptyFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductSelectionHeaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenSelectionCountChangesThenSingularAndGeneralLocalizedTitlesAreShown() {
        var selectedCount by mutableIntStateOf(1)
        setHeader(selectedProductCount = { selectedCount })

        val singularTitle = context.getString(R.string.product_selection_count_single, 1)
        composeTestRule.onNodeWithTag(ProductListTestTags.SELECTION_TITLE)
            .assertTextEquals(singularTitle)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                )
            )
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        composeTestRule.runOnIdle { selectedCount = 3 }

        composeTestRule.onNodeWithTag(ProductListTestTags.SELECTION_TITLE)
            .assertTextEquals(context.getString(R.string.product_selection_count, 3))
        composeTestRule.onNodeWithText(singularTitle).assertDoesNotExist()
    }

    @Test
    fun whenHeaderIsShownThenCloseAndOverflowExposeLocalizedClickableSemantics() {
        setHeader()

        composeTestRule.onNodeWithTag(ProductListTestTags.SELECTION_CLOSE)
            .assertContentDescriptionEquals(context.getString(R.string.close))
            .assertHasClickAction()
        composeTestRule.onNodeWithTag(ProductListTestTags.SELECTION_OVERFLOW)
            .assertContentDescriptionEquals(context.getString(R.string.more_options))
            .assertHasClickAction()
    }

    @Test
    fun whenOverflowOpensThenActionsAndDividerAppearInExactVerticalOrder() {
        setHeader()

        composeTestRule.onNodeWithTag(ProductListTestTags.SELECTION_OVERFLOW).performClick()

        val orderedItems = listOf(
            ProductListTestTags.SELECTION_UPDATE_STATUS to R.string.product_selection_menu_update_status,
            ProductListTestTags.SELECTION_UPDATE_PRICE to R.string.product_selection_menu_update_price,
            ProductListTestTags.SELECTION_UPDATE_STOCK_STATUS to R.string.product_selection_menu_update_stock_status,
            ProductListTestTags.SELECTION_SELECT_ALL to R.string.product_selection_menu_select_all,
        )
        orderedItems.forEach { (tag, stringId) ->
            composeTestRule.onNodeWithTag(tag)
                .assertTextEquals(context.getString(stringId))
                .assertIsDisplayed()
        }

        val statusBounds = boundsForTag(ProductListTestTags.SELECTION_UPDATE_STATUS)
        val priceBounds = boundsForTag(ProductListTestTags.SELECTION_UPDATE_PRICE)
        val stockBounds = boundsForTag(ProductListTestTags.SELECTION_UPDATE_STOCK_STATUS)
        val dividerBounds = boundsForTag(ProductListTestTags.SELECTION_MENU_DIVIDER)
        val selectAllBounds = boundsForTag(ProductListTestTags.SELECTION_SELECT_ALL)

        assertThat(statusBounds.top).isLessThan(priceBounds.top)
        assertThat(priceBounds.top).isLessThan(stockBounds.top)
        assertThat(stockBounds.bottom).isLessThanOrEqualTo(dividerBounds.top)
        assertThat(dividerBounds.bottom).isLessThanOrEqualTo(selectAllBounds.top)
    }

    @Test
    fun whenEachOverflowActionIsClickedThenOnlyItsCallbackRunsOnceAndMenuDismisses() {
        val callbacks = CallbackCounts()
        setHeader(callbacks = callbacks)

        clickMenuAction(ProductListTestTags.SELECTION_UPDATE_STATUS)
        assertCallbackCounts(callbacks, CallbackCounts(updateStatus = 1))

        clickMenuAction(ProductListTestTags.SELECTION_UPDATE_PRICE)
        assertCallbackCounts(callbacks, CallbackCounts(updateStatus = 1, updatePrice = 1))

        clickMenuAction(ProductListTestTags.SELECTION_UPDATE_STOCK_STATUS)
        assertCallbackCounts(
            callbacks,
            CallbackCounts(updateStatus = 1, updatePrice = 1, updateStockStatus = 1),
        )

        clickMenuAction(ProductListTestTags.SELECTION_SELECT_ALL)
        assertCallbackCounts(
            callbacks,
            CallbackCounts(updateStatus = 1, updatePrice = 1, updateStockStatus = 1, selectAll = 1),
        )
    }

    @Test
    fun whenCloseIsClickedThenCloseCallbackRunsExactlyOnce() {
        val callbacks = CallbackCounts()
        setHeader(callbacks = callbacks)

        composeTestRule.onNodeWithTag(ProductListTestTags.SELECTION_CLOSE).performClick()

        assertCallbackCounts(callbacks, CallbackCounts(close = 1))
    }

    @Test
    fun givenAddProductAvailableWhenSelectionEntersAndExitsThenComposeOwnsFabVisibility() {
        var state by mutableStateOf(ProductListScreenState(isAddProductAvailable = true))
        setScreen { state }

        composeTestRule.onNodeWithTag(ProductListTestTags.ADD_FAB).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ProductListTestTags.SEARCH_ACTION).assertIsDisplayed()

        composeTestRule.runOnIdle { state = state.copy(selectedProductIds = setOf(1L)) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(ProductListTestTags.ADD_FAB).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ProductListTestTags.SELECTION_HEADER).assertIsDisplayed()

        composeTestRule.runOnIdle { state = state.copy(selectedProductIds = emptySet()) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(ProductListTestTags.SELECTION_HEADER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ProductListTestTags.ADD_FAB).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ProductListTestTags.SEARCH_ACTION).assertIsDisplayed()
    }

    @Test
    fun givenActiveSearchWhenSelectionEntersAndExitsThenExistingSearchQueryIsRestored() {
        val searchQuery = "Beanie"
        val zeroSelectedTitle = context.getString(R.string.product_selection_count, 0)
        var state by mutableStateOf(
            ProductListScreenState(
                isSearchActive = true,
                searchQuery = searchQuery,
                sortingTitle = "Newest",
                showBrowsingControls = true,
            )
        )
        composeTestRule.mainClock.autoAdvance = false
        setScreen { state }

        composeTestRule.onNodeWithTag(ProductListTestTags.SEARCH_FIELD).assertIsDisplayed()
        searchEditable().assertIsFocused()
        composeTestRule.onNodeWithText(searchQuery).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ProductListTestTags.CONTROL_RAIL).assertDoesNotExist()

        composeTestRule.runOnIdle { state = state.copy(selectedProductIds = setOf(1L)) }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        composeTestRule.onNodeWithTag(ProductListTestTags.SELECTION_HEADER).assertIsDisplayed()
        searchEditable().assertDoesNotExist()
        composeTestRule.onNodeWithTag(ProductListTestTags.SEARCH_ACTION).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ProductListTestTags.CONTROL_RAIL).assertDoesNotExist()

        composeTestRule.runOnIdle { state = state.copy(selectedProductIds = emptySet()) }
        composeTestRule.onNodeWithText(zeroSelectedTitle).assertDoesNotExist()
        repeat(EXIT_TRANSITION_TEST_FRAMES) {
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.onNodeWithText(zeroSelectedTitle).assertDoesNotExist()
        }

        composeTestRule.onNodeWithTag(ProductListTestTags.SELECTION_HEADER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ProductListTestTags.SEARCH_FIELD).assertIsDisplayed()
        searchEditable().assertIsFocused()
        composeTestRule.onNodeWithText(searchQuery).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ProductListTestTags.CONTROL_RAIL).assertDoesNotExist()
    }

    private fun setScreen(state: () -> ProductListScreenState) {
        composeTestRule.setContent {
            WooDesignSystemThemeWithBackground {
                ProductListScreen(
                    state = state(),
                    scrollToTopRequests = emptyFlow(),
                    onSearchClicked = {},
                    onSearchQueryChanged = {},
                    onSearchSubmitted = {},
                    onSearchClosed = {},
                    onSearchTypeChanged = {},
                    onBarcodeClicked = {},
                    onAddProductClicked = {},
                    onEmptyAddProductClicked = {},
                    onSortClicked = {},
                    onFiltersClicked = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onProductTapped = {},
                    onProductLongPressed = {},
                    onProductSelectionToggled = {},
                    onSelectionCloseClicked = {},
                    onSelectionUpdateStatusClicked = {},
                    onSelectionUpdatePriceClicked = {},
                    onSelectionUpdateStockStatusClicked = {},
                    onSelectionSelectAllClicked = {},
                    onListAtTopChanged = {},
                )
            }
        }
    }

    private fun setHeader(
        selectedProductCount: () -> Int = { 2 },
        callbacks: CallbackCounts = CallbackCounts(),
    ) {
        composeTestRule.setContent {
            WooDesignSystemThemeWithBackground {
                ProductSelectionHeader(
                    selectedProductCount = selectedProductCount(),
                    onCloseClicked = { callbacks.close++ },
                    onUpdateStatusClicked = { callbacks.updateStatus++ },
                    onUpdatePriceClicked = { callbacks.updatePrice++ },
                    onUpdateStockStatusClicked = { callbacks.updateStockStatus++ },
                    onSelectAllClicked = { callbacks.selectAll++ },
                )
            }
        }
    }

    private fun clickMenuAction(testTag: String) {
        composeTestRule.onNodeWithTag(ProductListTestTags.SELECTION_OVERFLOW).performClick()
        composeTestRule.onNodeWithTag(testTag).performClick()
        composeTestRule.onNodeWithTag(ProductListTestTags.SELECTION_MENU).assertDoesNotExist()
    }

    private fun assertCallbackCounts(actual: CallbackCounts, expected: CallbackCounts) {
        composeTestRule.runOnIdle {
            assertThat(actual).isEqualTo(expected)
        }
    }

    private fun boundsForTag(testTag: String): Rect =
        composeTestRule.onNodeWithTag(testTag).fetchSemanticsNode().boundsInRoot

    private fun searchEditable() = composeTestRule.onNode(
        matcher = hasSetTextAction().and(
            hasAnyAncestor(hasTestTag(ProductListTestTags.SEARCH_FIELD))
        ),
        useUnmergedTree = true,
    )

    private data class CallbackCounts(
        var close: Int = 0,
        var updateStatus: Int = 0,
        var updatePrice: Int = 0,
        var updateStockStatus: Int = 0,
        var selectAll: Int = 0,
    )

    private companion object {
        const val EXIT_TRANSITION_TEST_FRAMES = 30
    }
}
