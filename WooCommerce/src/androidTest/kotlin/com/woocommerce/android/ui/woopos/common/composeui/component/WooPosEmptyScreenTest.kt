package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WooPosEmptyScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomActionStaysVisibleWhileLargeTextScrollsInAShortWindow() {
        var clicks = 0
        composeTestRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 240.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    WooPosTheme {
                        WooPosEmptyScreen(
                            modifier = Modifier.fillMaxSize().testTag(WINDOW),
                            title = "Set up access control",
                            message = "PIN protection is off",
                            icon = ImageVector.vectorResource(R.drawable.woopos_access_control),
                            bottomActionLabel = ACTION,
                            onActionClicked = { clicks++ },
                        )
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("Set up access control")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val content = composeTestRule.onNode(hasScrollAction())
        val initialScroll = content.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertThat(initialScroll.maxValue()).isGreaterThan(0f)
        assertThat(initialScroll.value()).isZero()
        val action = composeTestRule.onNodeWithText(ACTION)
        action.assertIsDisplayed()
        val initialBounds = action.getUnclippedBoundsInRoot()
        val windowBounds = composeTestRule.onNodeWithTag(WINDOW).getUnclippedBoundsInRoot()
        assertThat(initialBounds.bottom).isEqualTo(windowBounds.bottom)

        // WHEN
        composeTestRule.onNodeWithText("PIN protection is off").performScrollTo().assertIsDisplayed()

        // THEN
        val scrolledContent = content.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertThat(scrolledContent.value()).isGreaterThan(0f)
        action.assertIsDisplayed()
        assertThat(action.getUnclippedBoundsInRoot()).isEqualTo(initialBounds)
        action.performClick()
        composeTestRule.runOnIdle { assertThat(clicks).isEqualTo(1) }
    }

    @Test
    fun existingVectorActionRemainsInTheCenteredContentGroup() {
        composeTestRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 600.dp))) {
                WooPosTheme {
                    WooPosEmptyScreen(
                        modifier = Modifier.fillMaxSize().testTag(WINDOW),
                        title = "Empty list",
                        message = "There are no items",
                        contentDescription = "Empty list illustration",
                        actionLabel = ACTION,
                        onActionClicked = {},
                    )
                }
            }
        }

        // THEN
        val action = composeTestRule.onNodeWithText(ACTION).assertIsDisplayed().getUnclippedBoundsInRoot()
        val window = composeTestRule.onNodeWithTag(WINDOW).getUnclippedBoundsInRoot()
        val image = composeTestRule.onNodeWithContentDescription("Empty list illustration").getUnclippedBoundsInRoot()
        val groupCenter = (image.top + action.bottom) / 2
        val windowCenter = (window.top + window.bottom) / 2
        assertThat(groupCenter.value).isCloseTo(windowCenter.value, offset(1f))
        composeTestRule.onNodeWithText("Empty list")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
    }

    private companion object {
        const val WINDOW = "Empty state window"
        const val ACTION = "Get started"
    }
}
