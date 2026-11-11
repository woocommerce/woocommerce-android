package com.woocommerce.android.ui.woopos.lock

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WooPosLockScreenLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun headingAndKeypadStayInPlaceAcrossStatusChanges() = verifyStatusLayout(fontScale = 1f, dark = false)

    @Test
    fun enlargedDarkStatusTextDoesNotClipAndControlsRemainReachable() = verifyStatusLayout(fontScale = 2f, dark = true)

    private fun verifyStatusLayout(fontScale: Float, dark: Boolean) {
        val state = mutableStateOf(WooPosLockScreenState(isLocked = true))
        val errorColor = showLock(state, fontScale, dark)
        val heading = composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_enter_pin))
        val key = composeTestRule.onNodeWithText("5")
        val headingBounds = heading.getUnclippedBoundsInRoot()
        val keyBounds = key.getUnclippedBoundsInRoot()
        val left = composeTestRule.onNodeWithText("1").getUnclippedBoundsInRoot().left
        val right = composeTestRule.onNodeWithText("3").getUnclippedBoundsInRoot().right
        val recovery = context.getString(R.string.woopos_lock_recovery_pending)
        val cases = statusCases()
        val messages = (cases.map { it.second } + recovery).toSet()
        assertOnlyVisibleMessages(messages, emptySet())
        cases.forEach { (next, message) ->
            composeTestRule.runOnIdle { state.value = next }
            composeTestRule.waitForIdle()
            assertThat(heading.getUnclippedBoundsInRoot()).isEqualTo(headingBounds)
            assertThat(key.getUnclippedBoundsInRoot()).isEqualTo(keyBounds)
            val visible = if (next.remainingSeconds > 0 && next.error == WooPosLockError.RECOVERY_UNAVAILABLE) {
                setOf(message, recovery)
            } else {
                setOf(message)
            }
            assertOnlyVisibleMessages(messages, visible)
            val status = composeTestRule.onNodeWithText(message)
            val bounds = status.getUnclippedBoundsInRoot()
            assertThat(bounds.left.value).isGreaterThanOrEqualTo(left.value)
            assertThat(bounds.right.value).isLessThanOrEqualTo(right.value)
            val layouts = mutableListOf<TextLayoutResult>()
            status.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertThat(layouts.single().hasVisualOverflow)
                .describedAs("Status must not clip: %s (%s)", message, layouts.single().size)
                .isFalse()
            if (next.error == WooPosLockError.INCORRECT || next.error == WooPosLockError.STORAGE) {
                assertThat(layouts.single().layoutInput.style.color).isEqualTo(errorColor)
            }
        }
        val digitLayouts = mutableListOf<TextLayoutResult>()
        key.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(digitLayouts) }
        assertThat(digitLayouts.single().hasVisualOverflow).isFalse()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_forgot_pin))
            .performScrollTo().assertIsDisplayed()
    }

    private fun showLock(
        state: MutableState<WooPosLockScreenState>,
        fontScale: Float,
        dark: Boolean,
    ): Color {
        var errorColor = Color.Unspecified
        composeTestRule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
                LocalConfiguration provides configuration,
            ) {
                WooPosTheme {
                    errorColor = MaterialTheme.colorScheme.error
                    WooPosLockScreen(state.value, onUIEvent = {})
                }
            }
        }
        composeTestRule.waitForIdle()
        return errorColor
    }

    private fun statusCases(): List<Pair<WooPosLockScreenState, String>> {
        val cases = mutableListOf(
            WooPosLockScreenState(isLocked = true, isChecking = true) to
                context.getString(R.string.woopos_lock_checking),
            WooPosLockScreenState(isLocked = true, error = WooPosLockError.INCORRECT) to
                context.getString(R.string.woopos_lock_incorrect),
            WooPosLockScreenState(isLocked = true, error = WooPosLockError.STORAGE) to
                context.getString(R.string.woopos_lock_storage_error),
        )
        listOf(30L, 59L, 60L, 61L, 3599L, 3600L).forEach { seconds ->
            cases += WooPosLockScreenState(isLocked = true, remainingSeconds = seconds) to
                context.getString(R.string.woopos_lock_wait, seconds / 60, seconds % 60)
        }
        cases += WooPosLockScreenState(
            isLocked = true, remainingSeconds = 3600, error = WooPosLockError.RECOVERY_UNAVAILABLE,
        ) to context.getString(R.string.woopos_lock_wait, 60, 0)
        return cases
    }

    private fun assertOnlyVisibleMessages(messages: Set<String>, visible: Set<String>) {
        messages.forEach { text ->
            listOf(false, true).forEach { unmerged ->
                composeTestRule.onAllNodesWithText(text, useUnmergedTree = unmerged)
                    .assertCountEquals(if (text in visible) 1 else 0)
            }
        }
    }
}
