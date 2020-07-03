package com.woocommerce.android.screenshots.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.woocommerce.android.screenshots.products.ProductListScreen
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy

@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ProductTestSuite : TestBase() {
    @Test
    fun filterOutProductsSuccess() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        ProductListScreen
            .navigateToProducts()
            // Select filter and choose a random status
            .filterOutProductsBy("Stock status")
            .cancelFilters()

        ProductListScreen()
            .then<ProductListScreen> { it.isTitle("Products") }
            .logOut()
    }
}

