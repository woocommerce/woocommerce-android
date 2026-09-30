package com.woocommerce.android.ui.jitm

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JitmBannerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenBannerWhenRenderedThenMessageAndActionAreDisplayed() {
        setBanner()

        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(DESCRIPTION).assertIsDisplayed()
        composeTestRule.onNodeWithText(CTA).assertIsDisplayed()
        composeTestRule.onNodeWithTag(JITM_BADGE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun givenBannerWhenCtaIsClickedThenPrimaryActionRunsOnce() {
        var primaryActionClickCount = 0
        setBanner(onPrimaryActionClicked = { primaryActionClickCount++ })

        composeTestRule.onNodeWithText(CTA).performClick()

        composeTestRule.runOnIdle {
            assertThat(primaryActionClickCount).isOne()
        }
    }

    @Test
    fun givenBannerWhenHideIsClickedThenDismissRunsOnceAndMenuCloses() {
        var dismissClickCount = 0
        val hideLabel = context.getString(R.string.card_reader_upsell_card_reader_banner_hide_content)
        setBanner(onDismissClicked = { dismissClickCount++ })

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.more_menu))
            .performClick()
        composeTestRule.onNodeWithText(hideLabel).performClick()

        composeTestRule.runOnIdle {
            assertThat(dismissClickCount).isOne()
        }
        composeTestRule.onNodeWithText(hideLabel).assertDoesNotExist()
    }

    @Test
    fun givenBadgeIconWhenBannerIsRenderedThenBadgeIsDisplayed() {
        setBanner(badgeIcon = JitmState.Banner.RemoteIcon(BADGE_LIGHT_URL, BADGE_DARK_URL))

        composeTestRule.onNodeWithTag(JITM_BADGE_TEST_TAG).assertExists()
    }

    private fun setBanner(
        onPrimaryActionClicked: () -> Unit = {},
        onDismissClicked: () -> Unit = {},
        badgeIcon: JitmState.Banner.RemoteIcon? = null,
    ) {
        composeTestRule.setContent {
            WooDesignSystemThemeWithBackground {
                JitmBanner(
                    state = JitmState.Banner(
                        onPrimaryActionClicked = onPrimaryActionClicked,
                        onDismissClicked = onDismissClicked,
                        title = UiString.UiStringText(TITLE),
                        description = UiString.UiStringText(DESCRIPTION),
                        primaryActionLabel = UiString.UiStringText(CTA),
                        backgroundImage = JitmState.Banner.LocalOrRemoteImage.Local(
                            R.drawable.ic_banner_upsell_card_reader_illustration,
                        ),
                        badgeIcon = badgeIcon,
                    ),
                )
            }
        }
    }

    private companion object {
        const val TITLE = "Grow your business"
        const val DESCRIPTION = "Sell in person with WooCommerce"
        const val CTA = "Learn more"
        const val BADGE_LIGHT_URL = "https://example.test/badge-light.svg"
        const val BADGE_DARK_URL = "https://example.test/badge-dark.svg"
    }
}
