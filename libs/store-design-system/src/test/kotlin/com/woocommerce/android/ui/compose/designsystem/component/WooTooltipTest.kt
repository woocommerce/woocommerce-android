package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class WooTooltipTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given normal Figma sizes, when geometry resolves, then the exact arrow tokens are preserved`() {
        EXPECTED_FIGMA_TIPS.forEach { expected ->
            val geometry = geometry(expected.edge, expected.size, expected.desiredCenter)

            assertThat(geometry.arrowTip).isEqualTo(expected.tip)
            assertThat(geometry.arrowDepth).isEqualTo(10f)
            assertThat(geometry.arrowHalfBase).isEqualTo(11f)
            assertThat(geometry.cornerRadius).isEqualTo(12f)
            assertThat(geometry.scale).isEqualTo(1f)
        }
    }

    @Test
    fun `given a 60dp title-only side tooltip, when geometry resolves, then it does not scale`() {
        listOf(WooTooltipPhysicalEdge.Left, WooTooltipPhysicalEdge.Right).forEach { edge ->
            val geometry = geometry(edge, Size(80f, 60f), desiredCenter = 30f)

            assertThat(geometry.scale).isEqualTo(1f)
            assertThat(geometry.arrowCenter).isEqualTo(30f)
            assertThat(geometry.arrowDepth).isEqualTo(10f)
            assertThat(geometry.arrowHalfBase).isEqualTo(11f)
            assertThat(geometry.cornerRadius).isEqualTo(12f)
        }
    }

    @Test
    fun `given constrained tooltips, when geometry resolves, then every outline remains valid and bounded`() {
        CONSTRAINED_SIZES.forEach { size ->
            WooTooltipPhysicalEdge.entries.forEach { edge ->
                val geometry = geometry(edge, size, desiredCenter = -100f)
                val edgeLength = if (edge == WooTooltipPhysicalEdge.Top || edge == WooTooltipPhysicalEdge.Bottom) {
                    size.width
                } else {
                    size.height
                }
                val base = if (edge == WooTooltipPhysicalEdge.Top || edge == WooTooltipPhysicalEdge.Bottom) {
                    geometry.arrowBaseStart.x..geometry.arrowBaseEnd.x
                } else {
                    geometry.arrowBaseStart.y..geometry.arrowBaseEnd.y
                }
                val outline = WooTooltipShape(edge, -100f, 12.dp).createOutline(size, LayoutDirection.Ltr, DENSITY)

                assertThat(geometry.scale).isGreaterThan(0f)
                assertThat(geometry.arrowHalfBase * 2f).isGreaterThan(0f)
                assertThat(base.start).isGreaterThanOrEqualTo(geometry.cornerRadius)
                assertThat(base.endInclusive).isLessThanOrEqualTo(edgeLength - geometry.cornerRadius)
                assertThat(geometry.cornerRadius).isLessThanOrEqualTo(geometry.bodyBounds.width / 2f)
                assertThat(geometry.cornerRadius).isLessThanOrEqualTo(geometry.bodyBounds.height / 2f)
                assertThat(outline.bounds.left).isGreaterThanOrEqualTo(0f)
                assertThat(outline.bounds.top).isGreaterThanOrEqualTo(0f)
                assertThat(outline.bounds.right).isLessThanOrEqualTo(size.width)
                assertThat(outline.bounds.bottom).isLessThanOrEqualTo(size.height)
            }
        }
    }

    @Test
    fun `given tiny finite sizes, when geometry resolves, then it scales continuously toward zero`() {
        listOf(1f, 0.5f, 0.1f).zipWithNext().forEach { (larger, smaller) ->
            val largerGeometry = geometry(WooTooltipPhysicalEdge.Top, Size(larger, larger), larger / 2f)
            val smallerGeometry = geometry(WooTooltipPhysicalEdge.Top, Size(smaller, smaller), smaller / 2f)

            assertThat(smallerGeometry.scale).isLessThan(largerGeometry.scale)
            assertThat(smallerGeometry.arrowDepth).isGreaterThan(0f)
            assertThat(smallerGeometry.arrowHalfBase).isGreaterThan(0f)
        }
    }

    @Test
    fun `given invalid sizes, when outlines are requested, then safe empty rectangles are returned`() {
        listOf(Size.Zero, Size.Unspecified, Size(Float.POSITIVE_INFINITY, 10f)).forEach { size ->
            val outline = WooTooltipShape(
                arrowEdge = WooTooltipPhysicalEdge.Top,
                desiredArrowCenter = 20f,
                cornerRadius = 12.dp,
            ).createOutline(size, LayoutDirection.Ltr, DENSITY)

            assertThat(outline).isInstanceOf(Outline.Rectangle::class.java)
            assertThat((outline as Outline.Rectangle).rect).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun `given equivalent parameters, when shapes are recreated, then they are value equal`() {
        assertThat(WooTooltipShape(WooTooltipPhysicalEdge.Top, 31f, 12.dp)).isEqualTo(
            WooTooltipShape(WooTooltipPhysicalEdge.Top, 31f, 12.dp)
        )
    }

    @Test
    fun `given a hidden tooltip, when composed, then its anchor remains and popup text is absent`() {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooTooltipBox(
                    state = rememberWooTooltipState(),
                    title = TITLE,
                ) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }

        composeTestRule.onNodeWithText(ANCHOR).assertExists()
        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
    }

    @Test
    fun `given visibility is observed in composition, when state changes, then the observation updates`() {
        lateinit var state: WooTooltipState
        lateinit var scope: CoroutineScope
        composeTestRule.setContent {
            WooDesignSystemTheme {
                state = rememberWooTooltipState()
                scope = rememberCoroutineScope()
                androidx.compose.material3.Text(if (state.isVisible) VISIBLE_STATUS else HIDDEN_STATUS)
                WooTooltipBox(state = state, title = TITLE) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }

        composeTestRule.onNodeWithText(HIDDEN_STATUS).assertExists()

        composeTestRule.runOnIdle { scope.launch { state.show() } }
        composeTestRule.onNodeWithText(VISIBLE_STATUS).assertExists()

        composeTestRule.runOnIdle { state.dismiss() }
        composeTestRule.onNodeWithText(HIDDEN_STATUS).assertExists()
    }

    @Test
    fun `given an anchor modifier, when composed, then standard long-click semantics are on its wrapper`() {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooTooltipBox(
                    state = rememberWooTooltipState(),
                    title = TITLE,
                    modifier = Modifier.testTag(ANCHOR_TAG),
                ) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }

        val longClick = composeTestRule.onNodeWithTag(ANCHOR_TAG).fetchSemanticsNode().config
            .getOrNull(SemanticsActions.OnLongClick)
        assertThat(longClick).isNotNull()
    }

    @Test
    fun `given visible rich text, when values change, then popup semantics update without duplication`() {
        var title by mutableStateOf(TITLE)
        var supportingText by mutableStateOf(SUPPORTING_TEXT)
        composeTestRule.setContent {
            WooDesignSystemTheme {
                val state = rememberWooTooltipState()
                LaunchedEffect(state) { state.show() }
                WooTooltipBox(
                    state = state,
                    title = title,
                    supportingText = supportingText,
                ) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(TITLE, useUnmergedTree = true).assertCountEquals(1)
        composeTestRule.onAllNodesWithText(SUPPORTING_TEXT, useUnmergedTree = true).assertCountEquals(1)

        composeTestRule.runOnIdle {
            title = UPDATED_TITLE
            supportingText = UPDATED_SUPPORTING_TEXT
        }

        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeTestRule.onNodeWithText(SUPPORTING_TEXT).assertDoesNotExist()
        composeTestRule.onAllNodesWithText(UPDATED_TITLE, useUnmergedTree = true).assertCountEquals(1)
        composeTestRule.onAllNodesWithText(UPDATED_SUPPORTING_TEXT, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `given a visible text-only tooltip, when semantics are inspected, then no click action is exposed`() {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                val state = rememberWooTooltipState()
                LaunchedEffect(state) { state.show() }
                WooTooltipBox(state = state, title = TITLE) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }

        composeTestRule.onNodeWithText(TITLE).assertExists()
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun `given a visible actionable tooltip, when semantics are inspected, then its label is a button`() {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                val state = rememberWooTooltipState()
                LaunchedEffect(state) { state.show() }
                WooTooltipBox(
                    state = state,
                    title = TITLE,
                    action = WooTooltipAction(label = ACTION_LABEL, onClick = {}),
                ) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }

        composeTestRule.onNodeWithText(ACTION_LABEL)
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun `given a visible actionable tooltip, when its action is semantically clicked, then dismiss and invoke one callback`() {
        var invocationCount = 0
        lateinit var state: WooTooltipState
        composeTestRule.setContent {
            WooDesignSystemTheme {
                state = rememberWooTooltipState()
                LaunchedEffect(state) { state.show() }
                WooTooltipBox(
                    state = state,
                    title = TITLE,
                    action = WooTooltipAction(
                        label = ACTION_LABEL,
                        onClick = { invocationCount++ },
                    ),
                ) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }

        composeTestRule.onNodeWithText(ACTION_LABEL).performClick()

        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeTestRule.runOnIdle {
            assertThat(invocationCount).isEqualTo(1)
        }
    }

    @Test
    fun `given a visible actionable tooltip, when its action is touched, then the surface does not steal the tap`() {
        var invocationCount = 0
        lateinit var state: WooTooltipState
        composeTestRule.setContent {
            WooDesignSystemTheme {
                state = rememberWooTooltipState()
                LaunchedEffect(state) { state.show() }
                WooTooltipBox(
                    state = state,
                    title = TITLE,
                    action = WooTooltipAction(
                        label = ACTION_LABEL,
                        onClick = { invocationCount++ },
                    ),
                ) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }

        composeTestRule.onNodeWithText(ACTION_LABEL).performTouchInput { click() }

        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeTestRule.runOnIdle {
            assertThat(invocationCount).isEqualTo(1)
            assertThat(state.isVisible).isFalse()
        }
    }

    @Test
    fun `given a visible actionable tooltip, when its background is touched, then dismiss without callback`() {
        var invocationCount = 0
        lateinit var state: WooTooltipState
        composeTestRule.setContent {
            WooDesignSystemTheme {
                state = rememberWooTooltipState()
                LaunchedEffect(state) { state.show() }
                WooTooltipBox(
                    state = state,
                    title = TITLE,
                    action = WooTooltipAction(
                        label = ACTION_LABEL,
                        onClick = { invocationCount++ },
                    ),
                ) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }

        composeTestRule.onNodeWithText(TITLE).performTouchInput { click() }

        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeTestRule.runOnIdle {
            assertThat(invocationCount).isZero()
            assertThat(state.isVisible).isFalse()
        }
    }

    @Test
    fun `given a visible actionable tooltip, when state dismisses it, then callback is not invoked`() {
        var invocationCount = 0
        lateinit var state: WooTooltipState
        composeTestRule.setContent {
            WooDesignSystemTheme {
                state = rememberWooTooltipState()
                LaunchedEffect(state) { state.show() }
                WooTooltipBox(
                    state = state,
                    title = TITLE,
                    action = WooTooltipAction(
                        label = ACTION_LABEL,
                        onClick = { invocationCount++ },
                    ),
                ) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }

        composeTestRule.onNodeWithText(TITLE).assertExists()
        composeTestRule.runOnIdle { state.dismiss() }

        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeTestRule.runOnIdle { assertThat(invocationCount).isZero() }
    }

    @Test
    fun `given a blank label, when an action is created, then throw`() {
        assertThatIllegalArgumentException().isThrownBy {
            WooTooltipAction(label = " ", onClick = {})
        }.withMessage("WooTooltipAction label must not be blank")
    }

    @Test
    fun `given caller-controlled state, when shown and bubble is tapped, then it presents and dismisses`() {
        lateinit var state: WooTooltipState
        lateinit var scope: CoroutineScope
        var dismissRequests = 0
        composeTestRule.setContent {
            WooDesignSystemTheme {
                state = rememberWooTooltipState()
                scope = rememberCoroutineScope()
                WooTooltipBox(
                    state = state,
                    title = TITLE,
                    onDismissRequest = { dismissRequests++ },
                ) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }

        composeTestRule.runOnIdle { scope.launch { state.show() } }
        composeTestRule.onNodeWithText(TITLE).assertExists()
        assertThat(state.isVisible).isTrue()

        composeTestRule.onNodeWithText(TITLE).performTouchInput { click() }

        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        assertThat(state.isVisible).isFalse()
        assertThat(dismissRequests).isZero()
        composeTestRule.onNodeWithText(ANCHOR).assertExists()
    }

    @Test
    fun `given a visible host, when its state is replaced, then the new state can show`() {
        var useSecondState by mutableStateOf(false)
        lateinit var firstState: WooTooltipState
        lateinit var secondState: WooTooltipState
        lateinit var scope: CoroutineScope
        lateinit var firstShowJob: Job
        lateinit var secondShowJob: Job
        composeTestRule.setContent {
            WooDesignSystemTheme {
                firstState = rememberWooTooltipState()
                secondState = rememberWooTooltipState()
                scope = rememberCoroutineScope()
                WooTooltipBox(
                    state = if (useSecondState) secondState else firstState,
                    title = TITLE,
                ) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }

        composeTestRule.runOnIdle { firstShowJob = scope.launch { firstState.show() } }
        composeTestRule.onNodeWithText(TITLE).assertExists()
        assertThat(firstState.isVisible).isTrue()

        composeTestRule.runOnIdle { useSecondState = true }
        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeTestRule.runOnIdle {
            assertThat(firstShowJob.isCompleted).isTrue()
            assertThat(secondState.isVisible).isFalse()
            secondShowJob = scope.launch { secondState.show() }
        }

        composeTestRule.onNodeWithText(TITLE).assertExists()
        composeTestRule.runOnIdle {
            assertThat(secondState.isVisible).isTrue()
            assertThat(secondShowJob.isActive).isTrue()
        }
    }

    @Test
    fun `given two tooltip states, when the second shows, then Material global coordination dismisses the first`() {
        lateinit var firstState: WooTooltipState
        lateinit var secondState: WooTooltipState
        lateinit var scope: CoroutineScope
        composeTestRule.setContent {
            WooDesignSystemTheme {
                firstState = rememberWooTooltipState()
                secondState = rememberWooTooltipState()
                scope = rememberCoroutineScope()
                Column {
                    WooTooltipBox(state = firstState, title = TITLE) {
                        androidx.compose.material3.Text("First anchor")
                    }
                    WooTooltipBox(state = secondState, title = UPDATED_TITLE) {
                        androidx.compose.material3.Text("Second anchor")
                    }
                }
            }
        }

        composeTestRule.runOnIdle { scope.launch { firstState.show() } }
        composeTestRule.onNodeWithText(TITLE).assertExists()

        composeTestRule.runOnIdle { scope.launch { secondState.show() } }

        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeTestRule.onNodeWithText(UPDATED_TITLE).assertExists()
        assertThat(firstState.isVisible).isFalse()
        assertThat(secondState.isVisible).isTrue()
    }

    @Test
    fun `given a visible tooltip, when its anchor leaves and reenters, then it stays dismissed`() {
        var anchorIsOffscreen by mutableStateOf(false)
        lateinit var state: WooTooltipState
        lateinit var scope: CoroutineScope
        lateinit var showJob: Job
        composeTestRule.setContent {
            WooDesignSystemTheme {
                state = rememberWooTooltipState()
                scope = rememberCoroutineScope()
                WooTooltipBox(
                    state = state,
                    title = TITLE,
                    modifier = Modifier.offsetOffscreenWhen { anchorIsOffscreen },
                ) {
                    androidx.compose.material3.Text(ANCHOR)
                }
            }
        }

        composeTestRule.runOnIdle { showJob = scope.launch { state.show() } }
        composeTestRule.onNodeWithText(TITLE).assertExists()

        composeTestRule.runOnIdle { anchorIsOffscreen = true }
        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeTestRule.runOnIdle {
            assertThat(showJob.isCompleted).isTrue()
            assertThat(state.isVisible).isFalse()
        }

        composeTestRule.runOnIdle { anchorIsOffscreen = false }
        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeTestRule.onNodeWithText(ANCHOR).assertExists()
        assertThat(state.isVisible).isFalse()
    }

    @Test
    fun `given an offscreen-dismissed tooltip, when a newer tooltip shows, then reentry does not displace it`() {
        var firstAnchorIsOffscreen by mutableStateOf(false)
        lateinit var firstState: WooTooltipState
        lateinit var secondState: WooTooltipState
        lateinit var scope: CoroutineScope
        lateinit var firstShowJob: Job
        composeTestRule.setContent {
            WooDesignSystemTheme {
                firstState = rememberWooTooltipState()
                secondState = rememberWooTooltipState()
                scope = rememberCoroutineScope()
                Column {
                    WooTooltipBox(
                        state = firstState,
                        title = TITLE,
                        modifier = Modifier.offsetOffscreenWhen { firstAnchorIsOffscreen },
                    ) {
                        androidx.compose.material3.Text("First anchor")
                    }
                    WooTooltipBox(state = secondState, title = UPDATED_TITLE) {
                        androidx.compose.material3.Text("Second anchor")
                    }
                }
            }
        }

        composeTestRule.runOnIdle { firstShowJob = scope.launch { firstState.show() } }
        composeTestRule.onNodeWithText(TITLE).assertExists()

        composeTestRule.runOnIdle { firstAnchorIsOffscreen = true }

        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        assertThat(firstState.isVisible).isFalse()
        composeTestRule.runOnIdle { scope.launch { secondState.show() } }
        composeTestRule.onNodeWithText(UPDATED_TITLE).assertExists()

        composeTestRule.runOnIdle { firstAnchorIsOffscreen = false }

        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeTestRule.onNodeWithText(UPDATED_TITLE).assertExists()
        composeTestRule.runOnIdle {
            assertThat(firstShowJob.isCompleted).isTrue()
            assertThat(firstState.isVisible).isFalse()
            assertThat(secondState.isVisible).isTrue()
        }

        composeTestRule.runOnIdle { secondState.dismiss() }
        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
    }

    @Test
    fun `given a visible preferred start tooltip, when direction changes, then host uses the current layout result`() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var latestResult: WooTooltipLayoutResult? = null
        lateinit var state: WooTooltipState
        lateinit var scope: CoroutineScope
        composeTestRule.setContent {
            WooDesignSystemTheme {
                state = rememberWooTooltipState()
                scope = rememberCoroutineScope()
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.absoluteOffset(x = 100.dp, y = 100.dp)) {
                        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                            WooTooltipBoxImpl(
                                state = state,
                                title = TITLE,
                                preferredPlacement = WooTooltipPlacement.Start,
                                onLayoutResult = { latestResult = it },
                            ) {
                                androidx.compose.material3.Text(ANCHOR)
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.runOnIdle { scope.launch { state.show() } }
        composeTestRule.onNodeWithText(TITLE).assertExists()
        composeTestRule.runOnIdle {
            assertThat(latestResult).isNotNull()
            assertThat(latestResult?.side).isEqualTo(WooTooltipPhysicalSide.Left)
        }
        val ltrResult = checkNotNull(latestResult)

        composeTestRule.runOnIdle { layoutDirection = LayoutDirection.Rtl }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TITLE).assertExists()
        assertThat(checkNotNull(latestResult).side).isEqualTo(WooTooltipPhysicalSide.Right)
        val rtlResult = checkNotNull(latestResult)

        assertThat(ltrResult.arrowEdge).isEqualTo(WooTooltipPhysicalEdge.Right)
        assertThat(rtlResult.arrowEdge).isEqualTo(WooTooltipPhysicalEdge.Left)
        assertThat(rtlResult.offset.x).isGreaterThan(ltrResult.offset.x)
        assertThat(rtlResult.arrowCenter).isFinite()
        assertThat(rtlResult.maxBubbleWidth).isGreaterThan(ltrResult.maxBubbleWidth)
        assertThat(rtlResult.maxBubbleWidth).isGreaterThan(0)
        composeTestRule.waitForIdle()
        assertThat(latestResult).isEqualTo(rtlResult)
    }

    @Test
    fun `given long supporting text, when rendered at constrained width, then text has no visual overflow`() {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                Column {
                    WooTooltipSurface(
                        title = TITLE,
                        supportingText = LONG_SUPPORTING_TEXT,
                        arrowEdge = WooTooltipPhysicalEdge.Left,
                        arrowCenter = 30f,
                        cornerRadius = 12.dp,
                        modifier = Modifier.width(120.dp),
                    )
                }
            }
        }

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        val action = composeTestRule
            .onNodeWithText(LONG_SUPPORTING_TEXT, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action

        composeTestRule.runOnIdle {
            assertThat(action?.invoke(textLayoutResults)).isTrue()
            assertThat(textLayoutResults.single().hasVisualOverflow).isFalse()
        }
    }

    @Test
    fun `given a long localized action at large font, when constrained, then text is soft-wrapped and bounded`() {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                    WooTooltipSurface(
                        title = TITLE,
                        supportingText = SUPPORTING_TEXT,
                        action = WooTooltipAction(label = LONG_LOCALIZED_ACTION_LABEL, onClick = {}),
                        arrowEdge = WooTooltipPhysicalEdge.Left,
                        arrowCenter = 30f,
                        cornerRadius = 12.dp,
                        modifier = Modifier
                            .width(120.dp)
                            .testTag(TOOLTIP_SURFACE_TAG),
                    )
                }
            }
        }

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        val textNode = composeTestRule
            .onNodeWithText(LONG_LOCALIZED_ACTION_LABEL, useUnmergedTree = true)
            .fetchSemanticsNode()
        val action = textNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action
        val actionBounds = composeTestRule.onNodeWithText(LONG_LOCALIZED_ACTION_LABEL)
            .fetchSemanticsNode()
            .boundsInRoot
        val surfaceBounds = composeTestRule.onNodeWithTag(TOOLTIP_SURFACE_TAG)
            .fetchSemanticsNode()
            .boundsInRoot

        composeTestRule.runOnIdle {
            assertThat(action?.invoke(textLayoutResults)).isTrue()
            val textLayoutResult = textLayoutResults.single()
            assertThat(textLayoutResult.layoutInput.softWrap).isTrue()
            assertThat(textLayoutResult.size.width.toFloat()).isLessThanOrEqualTo(actionBounds.width)
            assertThat(actionBounds.left).isGreaterThanOrEqualTo(surfaceBounds.left)
            assertThat(actionBounds.right).isLessThanOrEqualTo(surfaceBounds.right)
            assertThat(textLayoutResult.hasVisualOverflow).isFalse()
        }
    }

    private fun geometry(
        edge: WooTooltipPhysicalEdge,
        size: Size,
        desiredCenter: Float,
    ): WooTooltipGeometry = checkNotNull(
        wooTooltipGeometry(
            edge = edge,
            desiredArrowCenter = desiredCenter,
            size = size,
            tokens = TOKENS,
        )
    )

    private fun Modifier.offsetOffscreenWhen(isOffscreen: () -> Boolean): Modifier = offset {
        IntOffset(
            x = if (isOffscreen()) OFFSCREEN_OFFSET.roundToPx() else 0,
            y = 0,
        )
    }

    private companion object {
        val DENSITY = Density(1f)
        val TOKENS = WooTooltipGeometryTokens(12f, 11f, 10f)
        val OFFSCREEN_OFFSET = (-1000).dp
        val CONSTRAINED_SIZES = listOf(
            Size(16f, 100f),
            Size(100f, 16f),
            Size(16f, 16f),
            Size(0.1f, 0.1f),
        )
        val EXPECTED_FIGMA_TIPS = listOf(
            ExpectedTip(WooTooltipPhysicalEdge.Top, Size(200f, 122f), 31f, Offset(31f, 0f)),
            ExpectedTip(WooTooltipPhysicalEdge.Top, Size(200f, 122f), 100f, Offset(100f, 0f)),
            ExpectedTip(WooTooltipPhysicalEdge.Top, Size(200f, 122f), 169f, Offset(169f, 0f)),
            ExpectedTip(WooTooltipPhysicalEdge.Bottom, Size(200f, 122f), 31f, Offset(31f, 122f)),
            ExpectedTip(WooTooltipPhysicalEdge.Bottom, Size(200f, 122f), 100f, Offset(100f, 122f)),
            ExpectedTip(WooTooltipPhysicalEdge.Bottom, Size(200f, 122f), 169f, Offset(169f, 122f)),
            ExpectedTip(WooTooltipPhysicalEdge.Left, Size(200f, 112f), 31f, Offset(0f, 31f)),
            ExpectedTip(WooTooltipPhysicalEdge.Left, Size(200f, 112f), 56f, Offset(0f, 56f)),
            ExpectedTip(WooTooltipPhysicalEdge.Left, Size(200f, 112f), 81f, Offset(0f, 81f)),
            ExpectedTip(WooTooltipPhysicalEdge.Right, Size(200f, 112f), 31f, Offset(200f, 31f)),
            ExpectedTip(WooTooltipPhysicalEdge.Right, Size(200f, 112f), 56f, Offset(200f, 56f)),
            ExpectedTip(WooTooltipPhysicalEdge.Right, Size(200f, 112f), 81f, Offset(200f, 81f)),
        )
        const val TITLE = "Tooltip title"
        const val SUPPORTING_TEXT = "Supporting text"
        const val UPDATED_TITLE = "Updated title"
        const val UPDATED_SUPPORTING_TEXT = "Updated supporting text"
        const val ANCHOR = "Anchor"
        const val ANCHOR_TAG = "tooltip-anchor"
        const val VISIBLE_STATUS = "Visible"
        const val HIDDEN_STATUS = "Hidden"
        const val TOOLTIP_SURFACE_TAG = "tooltip-surface"
        const val ACTION_LABEL = "Got it"
        const val LONG_SUPPORTING_TEXT =
            "Supporting information wraps onto several lines and expands without clipping at large font scales."
        const val LONG_LOCALIZED_ACTION_LABEL =
            "Erweiterte Produkteinstellungen überprüfen und die Darstellung anpassen"
    }

    private data class ExpectedTip(
        val edge: WooTooltipPhysicalEdge,
        val size: Size,
        val desiredCenter: Float,
        val tip: Offset,
    )
}
