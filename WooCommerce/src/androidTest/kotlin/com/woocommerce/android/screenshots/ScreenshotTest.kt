package com.woocommerce.android.screenshots

import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import org.junit.Test

class ScreenshotTest {
    private val screenshotTestRule = ScreenshotTestRule()

    @Test
    fun screenshots() {
        screenshotTestRule.launch()

        WelcomeScreen.logoutIfNeeded()
                .selectLogin()
                .proceedWith("automatticwidgets.com")
                .proceedWith("nuttystephen.bem8kzmg@mailosaur.io")
                .proceedWithPassword()
                .proceedWith("*xBu(Z1Gr##Qo&SqaK0RBrqI")

                // Enable Products
                .openSettingsPane().openBetaFeatures()
                .enableProductEditing()
                .goBackToSettingsScreen().goBackToMyStoreScreen()

                // My Store
                .dismissTopBannerIfNeeded()
                .then<MyStoreScreen> { it.stats.switchToStatsDashboardYearsTab() }
                // Take dashboard screenshot

                // Orders
                .tabBar.gotoOrdersScreen()
                // Take order list screenshot
                .selectOrder(0)
                // Take order detail screenshot
                .goBackToOrdersScreen()

                .openSearchPane()
                // Take order search screenshot
                .cancel()

                // Reviews
                .tabBar.gotoReviewsScreen()
                // Take review list screenshot
                .selectReview(3)
                // Take review detail screenshot
                .goBackToReviewsScreen()

                // Products
                .tabBar.gotoProductsScreen()
                // Take product list screenshot
                .selectProduct(1)
                // Take product detail screenshot
    }
}
