package com.woocommerce.android.e2e.screens.products

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Condition
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.ProductData
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.shared.FilterScreen
import com.woocommerce.android.ui.products.list.ProductListTestTags
import java.util.regex.Pattern

class ProductListScreen(
    private val composeTestRule: ComposeTestRule? = null,
) : Screen(R.id.products_compose_container) {
    private val device = UiDevice.getInstance(getInstrumentation())

    fun scrollToProduct(productTitle: String): ProductListScreen {
        UiScrollable(
            UiSelector().resourceId(ProductListTestTags.LIST)
        ).apply {
            setAsVerticalList()
            scrollTextIntoView(productTitle)
        }
        waitForText(productTitle)
        return this
    }

    fun selectProductByName(productName: String): SingleProductScreen {
        scrollToProduct(productName)
        requireNotNull(device.findObject(By.text(productName))) {
            "Product row '$productName' was not found"
        }.click()
        waitForElementToBeDisplayed(R.id.productDetail_root)
        return SingleProductScreen()
    }

    fun tapOnCreateProduct(): ProductListScreen {
        waitForTag(ProductListTestTags.ADD_ACTION).click()
        return this
    }

    fun tapOnAddManually(composeTestRule: ComposeTestRule): ProductListScreen {
        val buttonText = getTranslatedString(R.string.product_creation_ai_entry_sheet_manual_option_title)
        composeTestRule.onNodeWithText(buttonText).performClick()
        return this
    }

    fun goBackToProductList(): ProductListScreen {
        while (!isElementDisplayed(R.id.products_compose_container)) pressBack()
        return this
    }

    fun openSearchPane(): ProductListScreen {
        if (find(SEARCH_FIELD_SELECTOR) == null) {
            clickSearchTag(ProductListTestTags.SEARCH_ACTION)
        }
        waitFor(SEARCH_FIELD_SELECTOR, ProductListTestTags.SEARCH_FIELD)
        return this
    }

    fun tapSearchAllProducts(): ProductListScreen {
        clickSearchTag(ProductListTestTags.SEARCH_ALL)
        return this
    }

    fun tapSearchSKU(): ProductListScreen {
        clickSearchTag(ProductListTestTags.SEARCH_SKU)
        return this
    }

    fun enterSearchTerm(term: String): ProductListScreen {
        searchInput().text = term
        waitForProductRows(minimumCount = 1)
        return this
    }

    fun enterAbsentSearchTerm(term: String): ProductListScreen {
        searchInput().text = term
        waitForTag(ProductListTestTags.EMPTY)
        return this
    }

    fun tapFilters(): FilterScreen {
        waitForTag(ProductListTestTags.FILTERS).click()
        return FilterScreen()
    }

    fun tapSort(): ProductListScreen {
        waitForTag(ProductListTestTags.SORT).click()
        return this
    }

    fun selectSortOption(sortOption: String): ProductListScreen {
        clickByTextAndId(sortOption, R.id.sortingItem_name)
        return this
    }

    fun assertProductIsAtPosition(productName: String, position: Int): ProductListScreen {
        val rows = waitForProductRows(minimumCount = position + 1)
        check(productName in rows[position].allText()) {
            "Expected '$productName' at position $position, found ${rows[position].allText()}"
        }
        return this
    }

    fun leaveOrClearSearchMode(): ProductListScreen {
        leaveSearchMode()
        return this
    }

    fun leaveSearchMode(): ProductListScreen {
        if (find(SEARCH_FIELD_SELECTOR) != null) {
            val cancelText = getTranslatedString(R.string.cancel)
            if (composeTestRule != null) {
                composeTestRule.onNode(
                    hasText(cancelText)
                        .and(hasClickAction())
                        .and(hasAnyAncestor(hasTestTag(ProductListTestTags.SEARCH_FIELD)))
                ).performClick()
            } else {
                val cancelTextNode = waitFor(By.text(cancelText), "search Cancel action")
                requireNotNull(generateSequence(cancelTextNode) { it.parent }.firstOrNull { it.isClickable }) {
                    "Clickable search Cancel action was not found"
                }.click()
            }
            waitUntil(
                condition = { find(SEARCH_FIELD_SELECTOR) == null },
                failureMessage = {
                    "Compose node with tag '${ProductListTestTags.SEARCH_FIELD}' did not disappear"
                }
            )
        }
        waitFor(SEARCH_ACTION_SELECTOR, ProductListTestTags.SEARCH_ACTION)
        return this
    }

    fun assertProductCard(product: ProductData): ProductListScreen {
        scrollToProduct(product.name)
        val row = waitForProductRows(minimumCount = 1).firstOrNull { product.name in it.allText() }
        checkNotNull(row) { "Product row '${product.name}' was not found" }
        val rowText = row.allText().joinToString(" ")
        val expectedSku = product.sku.takeIf(String::isNotEmpty)?.let { "SKU: $it" }
        val stockStatus = product.stockStatus

        check(stockStatus == null || rowText.contains(stockStatus)) {
            "Product row did not contain stock status '$stockStatus': $rowText"
        }
        check(rowText.contains(product.priceDiscountedRaw)) {
            "Product row did not contain price '${product.priceDiscountedRaw}': $rowText"
        }
        check(expectedSku == null || rowText.contains(expectedSku)) {
            "Product row did not contain '$expectedSku': $rowText"
        }
        return this
    }

    fun assertProductsCount(count: Int): ProductListScreen {
        if (count == 0) {
            waitForTag(ProductListTestTags.EMPTY)
        }
        waitUntil(
            condition = { productRows().size == count },
            failureMessage = { "Expected $count product rows, found ${productRows().size}" }
        )
        return this
    }

    private fun waitForTag(tag: String): UiObject2 = waitFor(By.res(tag), tag)

    private fun waitFor(selector: BySelector, tag: String): UiObject2 {
        waitUntil(
            condition = { find(selector) != null },
            failureMessage = { "Compose node with tag '$tag' was not found" }
        )
        return requireNotNull(find(selector)) { "Compose node with tag '$tag' was not found" }
    }

    private fun find(selector: BySelector): UiObject2? = device.findObject(selector)

    private fun searchInput(): UiObject2 = requireNotNull(
        waitForTag(ProductListTestTags.SEARCH_FIELD).findObject(By.clazz("android.widget.EditText"))
    ) { "Editable search input was not found" }

    private fun waitForText(text: String): UiObject2 {
        val selector = By.text(text)
        waitUntil(
            condition = { find(selector) != null },
            failureMessage = { "Text '$text' was not found" }
        )
        return requireNotNull(find(selector)) { "Text '$text' was not found" }
    }

    private fun waitForProductRows(minimumCount: Int): List<UiObject2> {
        waitUntil(
            condition = { productRows().size >= minimumCount },
            failureMessage = {
                "Expected at least $minimumCount product rows, found ${productRows().size}"
            }
        )
        return productRows().also { rows ->
            check(rows.size >= minimumCount) {
                "Expected at least $minimumCount product rows, found ${rows.size}"
            }
        }
    }

    private fun productRows(): List<UiObject2> = device.findObjects(PRODUCT_ROW_SELECTOR)

    private fun clickSearchTag(tag: String) {
        if (composeTestRule != null) {
            composeTestRule.onNodeWithTag(tag).performClick()
        } else {
            waitForTag(tag).click()
        }
    }

    private fun waitUntil(condition: () -> Boolean, failureMessage: () -> String) {
        if (composeTestRule != null) {
            try {
                composeTestRule.waitUntil(timeoutMillis = NODE_TIMEOUT_MS, condition = condition)
            } catch (error: ComposeTimeoutException) {
                throw AssertionError(failureMessage(), error)
            }
        } else {
            check(device.wait(Condition<UiDevice, Boolean> { condition() }, NODE_TIMEOUT_MS)) {
                failureMessage()
            }
        }
    }

    private fun UiObject2.allText(): List<String> = buildList {
        text?.takeIf(String::isNotEmpty)?.let(::add)
        contentDescription?.takeIf(String::isNotEmpty)?.let(::add)
        children.forEach { addAll(it.allText()) }
    }

    companion object {
        private const val NODE_TIMEOUT_MS = 10_000L
        private val SEARCH_ACTION_SELECTOR = By.res(ProductListTestTags.SEARCH_ACTION)
        private val SEARCH_FIELD_SELECTOR = By.res(ProductListTestTags.SEARCH_FIELD)
        private val PRODUCT_ROW_SELECTOR = By.res(
            Pattern.compile("${ProductListTestTags.PRODUCT_ROW_PREFIX}[0-9]+")
        )
    }
}
