package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.Share
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class WooTopAppBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given a small header, when rendered, then it is fixed at 64dp`() {
        // GIVEN
        val density = givenHeader(size = WooTopAppBarSize.Small)

        // WHEN
        val height = headerHeightInPixels()

        // THEN
        assertThat(height.roundToInt()).isEqualTo((SMALL_HEIGHT_DP * density).roundToInt())
        composeTestRule.onNodeWithText(TITLE).assertExists()
    }

    @Test
    fun `given an expanded medium header, when rendered, then it is 112dp`() {
        // GIVEN
        val density = givenHeader(
            size = WooTopAppBarSize.Medium,
        )

        // WHEN
        val height = headerHeightInPixels()

        // THEN
        assertThat(height.roundToInt()).isEqualTo((MEDIUM_EXPANDED_HEIGHT_DP * density).roundToInt())
        composeTestRule.onNodeWithText(TITLE).assertExists()
    }

    @Test
    fun `given a collapsed medium header, when rendered, then it uses the small height`() {
        // GIVEN
        val density = givenHeader(
            size = WooTopAppBarSize.Medium,
            collapsed = true,
        )

        // WHEN
        val height = headerHeightInPixels()

        // THEN
        assertThat(height.roundToInt()).isEqualTo((SMALL_HEIGHT_DP * density).roundToInt())
        assertThat(titleLayoutResult().layoutInput.style.fontSize).isEqualTo(20.sp)
    }

    @Test
    fun `given a medium header with supporting text, when rendered, then Material accommodates both lines`() {
        val density = givenHeader(size = WooTopAppBarSize.Medium, supportingText = SUPPORTING_TEXT)

        assertThat(headerHeightInPixels()).isGreaterThanOrEqualTo(MEDIUM_EXPANDED_HEIGHT_DP * density)
        composeTestRule.onNodeWithText(SUPPORTING_TEXT).assertExists()
        assertThat(titleLayoutResult().layoutInput.style.fontSize).isEqualTo(24.sp)
    }

    @Test
    fun `given a partially collapsed medium header, when rendered, then Material measures its interpolated height`() {
        val density = givenHeader(
            size = WooTopAppBarSize.Medium,
            collapseFraction = 0.5f,
        )

        assertThat(headerHeightInPixels().roundToInt())
            .isEqualTo(((MEDIUM_EXPANDED_HEIGHT_DP - MEDIUM_COLLAPSE_RANGE * 0.5f) * density).roundToInt())
    }

    @Test
    fun `given a collapsed medium header with an action, when rendered, then the action remains available`() {
        givenHeader(
            size = WooTopAppBarSize.Medium,
            supportingText = SUPPORTING_TEXT,
            collapsed = true,
            showAction = true,
        )

        composeTestRule.onNodeWithContentDescription(SHARE_DESCRIPTION).assertExists()
    }

    @Test
    fun `given scrolling is disabled, when body scrolls, then header state is unchanged`() {
        lateinit var behavior: WooTopAppBarScrollBehavior
        lateinit var state: TopAppBarState
        composeTestRule.setContent {
            WooDesignSystemTheme {
                state = remember {
                    TopAppBarState(
                        initialHeightOffsetLimit = -MEDIUM_COLLAPSE_RANGE,
                        initialHeightOffset = 0f,
                        initialContentOffset = 0f,
                    )
                }
                val materialBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                    state = state,
                    canScroll = { false },
                    snapAnimationSpec = null,
                    flingAnimationSpec = null,
                )
                behavior = remember(materialBehavior) { WooTopAppBarScrollBehavior(materialBehavior) }
            }
        }
        composeTestRule.waitForIdle()

        val consumed = behavior.nestedScrollConnection.onPreScroll(
            available = Offset(0f, -PARTIAL_SCROLL_DELTA),
            source = NestedScrollSource.UserInput,
        )

        assertThat(consumed).isEqualTo(Offset.Zero)
        assertThat(state.heightOffset).isZero()
    }

    @Test
    fun `given expanded header, when header is directly dragged, then Material updates its state`() {
        lateinit var state: TopAppBarState
        composeTestRule.setContent {
            WooDesignSystemTheme {
                state = remember {
                    TopAppBarState(
                        initialHeightOffsetLimit = -MEDIUM_COLLAPSE_RANGE,
                        initialHeightOffset = 0f,
                        initialContentOffset = 0f,
                    )
                }
                val materialBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                    state = state,
                    snapAnimationSpec = null,
                )
                val behavior = remember(materialBehavior) { WooTopAppBarScrollBehavior(materialBehavior) }
                WooTopAppBar(
                    title = TITLE,
                    modifier = Modifier
                        .width(HEADER_WIDTH)
                        .testTag(HEADER_TAG),
                    size = WooTopAppBarSize.Medium,
                    scrollBehavior = behavior,
                    windowInsets = WindowInsets(0),
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(HEADER_TAG).performTouchInput {
            down(center)
            advanceEventTime(16)
            moveBy(Offset(0f, -DIRECT_DRAG_DELTA))
            advanceEventTime(16)
            up()
        }
        composeTestRule.waitForIdle()

        assertThat(state.heightOffset).isLessThan(0f)
        assertThat(state.heightOffset).isGreaterThanOrEqualTo(state.heightOffsetLimit)
    }

    @Test
    fun `given long title at 2x font with action, when rendered, then title remains one line before action`() {
        givenHeader(
            title = LONG_TITLE,
            showAction = true,
            fontScale = 2f,
            collapsed = true,
        )

        val titleBounds = titleBounds(LONG_TITLE)
        val actionBounds = actionBounds()

        assertThat(titleBounds.right).isLessThanOrEqualTo(actionBounds.left)
        assertThat(titleLayoutResult(LONG_TITLE).lineCount).isEqualTo(1)
    }

    @Test
    fun `given fixed LTR header, when rendered, then title has a 16dp edge and action has a 48dp target`() {
        val density = givenHeader(showAction = true)
        val expectedEdge = LOGICAL_EDGE_DP * density
        val headerBounds = headerBounds()
        val titleBounds = titleBounds()
        val actionBounds = actionBounds()

        assertThat(titleBounds.left.roundToInt()).isEqualTo(expectedEdge.roundToInt())
        assertThat(actionBounds.width.roundToInt()).isEqualTo((TOUCH_TARGET_DP * density).roundToInt())
        // Material leaves 4dp outside the 48dp action target.
        assertThat((headerBounds.right - actionBounds.right).roundToInt())
            .isEqualTo((SEMANTIC_ACTION_EDGE_DP * density).roundToInt())
    }

    @Test
    fun `given fixed RTL header, when rendered, then title has a 16dp edge and action has a 48dp target`() {
        val density = givenHeader(showAction = true, layoutDirection = LayoutDirection.Rtl)
        val expectedEdge = LOGICAL_EDGE_DP * density
        val headerBounds = headerBounds()
        val titleBounds = titleBounds()
        val actionBounds = actionBounds()

        assertThat((headerBounds.right - titleBounds.right).roundToInt()).isEqualTo(expectedEdge.roundToInt())
        assertThat(actionBounds.width.roundToInt()).isEqualTo((TOUCH_TARGET_DP * density).roundToInt())
        // Material leaves 4dp outside the 48dp action target.
        assertThat(actionBounds.left.roundToInt()).isEqualTo((SEMANTIC_ACTION_EDGE_DP * density).roundToInt())
    }

    @Test
    fun `given content has not overlapped the header, when it is rendered, then divider is absent until overlap`() {
        // GIVEN
        lateinit var state: TopAppBarState
        composeTestRule.setContent {
            WooDesignSystemTheme {
                state = remember {
                    TopAppBarState(
                        initialHeightOffsetLimit = -MEDIUM_COLLAPSE_RANGE,
                        initialHeightOffset = 0f,
                        initialContentOffset = 0f,
                    )
                }
                val materialScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                    state = state,
                    snapAnimationSpec = null,
                    flingAnimationSpec = null,
                )
                val scrollBehavior = remember(materialScrollBehavior) {
                    WooTopAppBarScrollBehavior(materialScrollBehavior)
                }
                WooTopAppBar(
                    title = TITLE,
                    size = WooTopAppBarSize.Medium,
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets(0),
                )
            }
        }

        // THEN
        composeTestRule.onNodeWithTag(WOO_TOP_APP_BAR_DIVIDER_TEST_TAG).assertDoesNotExist()

        // WHEN
        composeTestRule.runOnIdle { state.contentOffset = -OVERLAP_OFFSET }

        // THEN
        composeTestRule.onNodeWithTag(WOO_TOP_APP_BAR_DIVIDER_TEST_TAG).assertExists()
    }

    @Test
    fun `given a fixed header, when showDivider toggles, then the divider follows it directly`() {
        // GIVEN
        var showDivider by mutableStateOf(false)
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooTopAppBar(
                    title = TITLE,
                    windowInsets = WindowInsets(0),
                    showDivider = showDivider,
                )
            }
        }

        // THEN
        composeTestRule.onNodeWithTag(WOO_TOP_APP_BAR_DIVIDER_TEST_TAG).assertDoesNotExist()

        // WHEN
        composeTestRule.runOnIdle { showDivider = true }

        // THEN
        composeTestRule.onNodeWithTag(WOO_TOP_APP_BAR_DIVIDER_TEST_TAG).assertExists()
    }

    @Composable
    private fun rememberFixedCollapseScrollBehavior(collapseFraction: Float): WooTopAppBarScrollBehavior {
        val state = remember {
            TopAppBarState(
                initialHeightOffsetLimit = -MEDIUM_COLLAPSE_RANGE,
                initialHeightOffset = -MEDIUM_COLLAPSE_RANGE * collapseFraction,
                initialContentOffset = 0f,
            )
        }
        val materialScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = state,
            snapAnimationSpec = null,
            flingAnimationSpec = null,
        )
        return remember(materialScrollBehavior) {
            WooTopAppBarScrollBehavior(materialScrollBehavior)
        }
    }

    private fun givenHeader(
        size: WooTopAppBarSize = WooTopAppBarSize.Small,
        supportingText: String? = null,
        collapsed: Boolean = false,
        collapseFraction: Float? = null,
        title: String = TITLE,
        showAction: Boolean = false,
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ): Float {
        var density = 0f
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * fontScale),
                LocalLayoutDirection provides layoutDirection,
            ) {
                WooDesignSystemTheme {
                    density = LocalDensity.current.density
                    val effectiveCollapseFraction = collapseFraction ?: if (collapsed) 1f else null
                    val scrollBehavior = effectiveCollapseFraction?.let { rememberFixedCollapseScrollBehavior(it) }
                    val topAppBarActions: @Composable WooTopAppBarActionsScope.() -> Unit = {
                        if (showAction) {
                            IconAction(
                                imageVector = WooIcons.Regular.Share,
                                contentDescription = SHARE_DESCRIPTION,
                                onClick = {},
                            )
                        }
                    }
                    val headerModifier = Modifier
                        .width(HEADER_WIDTH)
                        .testTag(HEADER_TAG)
                    if (scrollBehavior != null) {
                        WooTopAppBar(
                            title = title,
                            modifier = headerModifier,
                            size = size,
                            supportingText = supportingText,
                            scrollBehavior = scrollBehavior,
                            windowInsets = WindowInsets(0),
                            actions = topAppBarActions,
                        )
                    } else {
                        WooTopAppBar(
                            title = title,
                            modifier = headerModifier,
                            size = size,
                            supportingText = supportingText,
                            windowInsets = WindowInsets(0),
                            actions = topAppBarActions,
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        return density
    }

    private fun headerHeightInPixels() =
        composeTestRule.onNodeWithTag(HEADER_TAG).fetchSemanticsNode().boundsInRoot.height

    private fun headerBounds() = composeTestRule.onNodeWithTag(HEADER_TAG).fetchSemanticsNode().boundsInRoot

    private fun titleBounds(title: String = TITLE) =
        composeTestRule.onNodeWithText(title).fetchSemanticsNode().boundsInRoot

    private fun actionBounds() = composeTestRule
        .onNodeWithContentDescription(SHARE_DESCRIPTION)
        .fetchSemanticsNode()
        .boundsInRoot

    private fun titleLayoutResult(title: String = TITLE): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val action = composeTestRule
            .onNodeWithText(title)
            .fetchSemanticsNode()
            .config
            .getOrNull(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult)
            ?.action

        composeTestRule.runOnIdle {
            assertThat(action?.invoke(results)).isTrue()
        }
        return results.single()
    }

    private companion object {
        const val SMALL_HEIGHT_DP = 64f
        const val MEDIUM_EXPANDED_HEIGHT_DP = 112f
        const val MEDIUM_COLLAPSE_RANGE = 48f
        const val OVERLAP_OFFSET = 1f
        const val PARTIAL_SCROLL_DELTA = 14f
        const val DIRECT_DRAG_DELTA = 64f
        const val LOGICAL_EDGE_DP = 16f
        const val SEMANTIC_ACTION_EDGE_DP = 4f
        const val TOUCH_TARGET_DP = 48f
        const val HEADER_TAG = "top-app-header"
        const val TITLE = "Products"
        const val LONG_TITLE = "A very long top app header title that truncates before its action"
        const val SHARE_DESCRIPTION = "Share"
        const val SUPPORTING_TEXT = "12 products"
        val HEADER_WIDTH = 360.dp
    }
}
