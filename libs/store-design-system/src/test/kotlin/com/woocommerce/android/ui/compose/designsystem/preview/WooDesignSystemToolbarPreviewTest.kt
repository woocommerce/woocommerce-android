package com.woocommerce.android.ui.compose.designsystem.preview

import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooDesignSystemToolbarPreviewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `when rendering toolbar demo from compose context, then preview toolbar is created`() {
        composeTestRule.setContent {
            WooDesignSystemToolbarDemo()
        }
    }
}
