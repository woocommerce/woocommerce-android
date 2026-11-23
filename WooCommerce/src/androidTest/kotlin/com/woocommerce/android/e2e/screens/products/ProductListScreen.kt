package com.woocommerce.android.e2e.screens.products

import androidx.compose.ui.test.junit4.ComposeTestRule
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

    private fun waitForProductRows(minimumCount: Int): List<UiObject2> = composeUi.waitForAtLeast(
        selector = PRODUCT_ROW_SELECTOR,
        minimumCount = minimumCount,
        description = "product rows",
    )

    companion object {
        private val PRODUCT_ROW_SELECTOR = composeTestTagWithNumericSuffix(ProductListTestTags.PRODUCT_ROW_PREFIX)
    }
}
