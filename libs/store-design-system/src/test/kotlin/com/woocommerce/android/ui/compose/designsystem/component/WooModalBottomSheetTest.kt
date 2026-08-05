package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooModalBottomSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val dismissActionMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)

    @Test
    fun `given sheet content, when rendered, then caller content and Material pane semantics are present`() {
        // GIVEN
        givenSheet()

        // THEN
        composeTestRule.onNodeWithText(SHEET_CONTENT).assertExists()
        val paneNodes = composeTestRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle))
            .fetchSemanticsNodes()
        assertThat(paneNodes).isNotEmpty
        composeTestRule.onAllNodes(dismissActionMatcher)
            .onFirst()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `given composed sheet, when Material dismiss action runs, then caller removes the sheet`() {
        // GIVEN
        givenCallerOwnedSheet()

        // WHEN
        composeTestRule.onAllNodes(dismissActionMatcher)
            .onFirst()
            .performSemanticsAction(SemanticsActions.Dismiss)
        composeTestRule.waitUntil(timeoutMillis = ANIMATION_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(SHEET_CONTENT).fetchSemanticsNodes().isEmpty()
        }

        // THEN
        composeTestRule.onNodeWithText(SHEET_CONTENT).assertDoesNotExist()
    }

    @Test
    fun `given wrapper state, when sheet is shown and hidden, then visibility delegates to Material state`() {
        // GIVEN
        lateinit var state: WooModalBottomSheetState
        lateinit var scope: CoroutineScope
        givenSheet(
            onState = { state = it },
            onScope = { scope = it },
        )

        composeTestRule.waitUntil(timeoutMillis = ANIMATION_TIMEOUT_MILLIS) { state.isVisible }

        // WHEN
        composeTestRule.runOnIdle { scope.launch { state.hide() } }
        composeTestRule.waitUntil(timeoutMillis = ANIMATION_TIMEOUT_MILLIS) { !state.isVisible }
        composeTestRule.runOnIdle { scope.launch { state.show() } }
        composeTestRule.waitUntil(timeoutMillis = ANIMATION_TIMEOUT_MILLIS) { state.isVisible }

        // THEN
        assertThat(state.isVisible).isTrue()
    }

    private fun givenCallerOwnedSheet() {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                var isComposed by remember { mutableStateOf(true) }
                val state = rememberWooModalBottomSheetState()

                if (isComposed) {
                    WooModalBottomSheet(
                        state = state,
                        onDismissRequest = { isComposed = false },
                    ) {
                        Text(SHEET_CONTENT)
                    }
                }
            }
        }
    }

    private fun givenSheet(
        onState: (WooModalBottomSheetState) -> Unit = {},
        onScope: (CoroutineScope) -> Unit = {},
    ) {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                val state = rememberWooModalBottomSheetState()
                val scope = rememberCoroutineScope()
                onState(state)
                onScope(scope)
                WooModalBottomSheet(state = state, onDismissRequest = {}) {
                    Column { Text(SHEET_CONTENT) }
                }
            }
        }
    }

    private companion object {
        const val SHEET_CONTENT = "Caller sheet content"
        const val ANIMATION_TIMEOUT_MILLIS = 5_000L
    }
}
