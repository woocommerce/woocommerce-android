package com.woocommerce.android.e2e.screens.products

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.ComposeUiAutomator
import com.woocommerce.android.e2e.helpers.util.ProductData
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.helpers.util.allText
import com.woocommerce.android.e2e.helpers.util.composeTestTagWithNumericSuffix
import com.woocommerce.android.e2e.screens.shared.FilterScreen
import com.woocommerce.android.ui.products.list.ProductListTestTags

class ProductListScreen(
    private val composeTestRule: ComposeTestRule? = null,
) : Screen(R.id.products_compose_container) {
    private val composeUi = ComposeUiAutomator(composeTestRule)

    fun scrollToProduct(productTitle: String): ProductListScreen {
        composeUi.scrollTextIntoView(ProductListTestTags.LIST, productTitle)
        composeUi.waitFor(By.text(productTitle), "Text '$productTitle'")
        return this
    }

    fun selectProductByName(productName: String): SingleProductScreen {
        scrollToProduct(productName)
        requireNotNull(composeUi.find(By.text(productName))) {
            "Product row '$productName' was not found"
        }.click()
        waitForElementToBeDisplayed(R.id.productDetail_root)
        return SingleProductScreen()
    }

    fun tapOnCreateProduct(): ProductListScreen {
        composeUi.waitForTag(ProductListTestTags.ADD_ACTION).click()
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
        if (composeUi.find(SEARCH_FIELD_SELECTOR) == null) {
            clickSearchTag(ProductListTestTags.SEARCH_ACTION)
        }
        composeUi.waitForTag(ProductListTestTags.SEARCH_FIELD)
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
        composeUi.waitForTag(ProductListTestTags.EMPTY)
        return this
    }

    fun tapFilters(): FilterScreen {
        composeUi.waitForTag(ProductListTestTags.FILTERS).click()
        return FilterScreen()
    }

    fun tapSort(): ProductListScreen {
        composeUi.waitForTag(ProductListTestTags.SORT).click()
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
        if (composeUi.find(SEARCH_FIELD_SELECTOR) != null) {
            val cancelText = getTranslatedString(R.string.cancel)
            if (composeTestRule != null) {
                composeTestRule.onNode(
                    hasText(cancelText)
                        .and(hasClickAction())
                        .and(hasAnyAncestor(hasTestTag(ProductListTestTags.SEARCH_FIELD)))
                ).performClick()
            } else {
                val cancelTextNode = composeUi.waitFor(By.text(cancelText), "search Cancel action")
                requireNotNull(generateSequence(cancelTextNode) { it.parent }.firstOrNull { it.isClickable }) {
                    "Clickable search Cancel action was not found"
                }.click()
            }
            composeUi.waitUntil(
                condition = { composeUi.find(SEARCH_FIELD_SELECTOR) == null },
                failureMessage = {
                    "Compose node with tag '${ProductListTestTags.SEARCH_FIELD}' did not disappear"
                }
            )
        }
        composeUi.waitForTag(ProductListTestTags.SEARCH_ACTION)
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
            composeUi.waitForTag(ProductListTestTags.EMPTY)
        }
        composeUi.waitForCount(
            selector = PRODUCT_ROW_SELECTOR,
            expectedCount = count,
            description = "product rows",
        )
        return this
    }

    private fun searchInput(): UiObject2 = requireNotNull(
        composeUi.waitForTag(ProductListTestTags.SEARCH_FIELD)
            .findObject(By.clazz("android.widget.EditText"))
    ) { "Editable search input was not found" }

    private fun waitForProductRows(minimumCount: Int): List<UiObject2> = composeUi.waitForAtLeast(
        selector = PRODUCT_ROW_SELECTOR,
        minimumCount = minimumCount,
        description = "product rows",
    )

    private fun clickSearchTag(tag: String) {
        if (composeTestRule != null) {
            composeTestRule.onNodeWithTag(tag).performClick()
        } else {
            composeUi.waitForTag(tag).click()
        }
    }

    companion object {
        private val SEARCH_FIELD_SELECTOR = By.res(ProductListTestTags.SEARCH_FIELD)
        private val PRODUCT_ROW_SELECTOR = composeTestTagWithNumericSuffix(ProductListTestTags.PRODUCT_ROW_PREFIX)
    }
}
