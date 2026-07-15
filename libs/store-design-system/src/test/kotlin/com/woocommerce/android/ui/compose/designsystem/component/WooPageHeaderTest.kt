package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
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
class WooPageHeaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given null scroll behavior, when rendered, then fixed header is 64dp with one 24sp bold heading`() {
        // GIVEN
        val density = givenHeader(collapsedFraction = null)

        // WHEN
        val bounds = headerBounds()

        // THEN
        assertThat(bounds.top).isZero()
        assertThat(bounds.height.roundToInt()).isEqualTo((FIXED_HEIGHT_DP * density).roundToInt())
        assertExactlyOneHeading()
        assertTitleTypography(fontSize = 24.sp)
    }

    @Test
    fun `given expanded medium behavior, when rendered, then header is 112dp with one 24sp bold heading`() {
        // GIVEN
        val density = givenHeader(collapsedFraction = 0f)

        // WHEN
        val bounds = headerBounds()

        // THEN
        assertThat(bounds.top).isZero()
        assertThat(bounds.height.roundToInt()).isEqualTo((EXPANDED_HEIGHT_DP * density).roundToInt())
        assertExactlyOneHeading()
        assertTitleTypography(fontSize = 24.sp)
    }

    @Test
    fun `given partially collapsed medium behavior, when rendered, then exactly one heading is exposed`() {
        // GIVEN
        givenHeader(collapsedFraction = 0.5f)

        // THEN
        assertExactlyOneHeading()
    }

    @Test
    fun `given collapsed medium behavior, when rendered, then header is 64dp with one 20sp bold heading`() {
        // GIVEN
        val density = givenHeader(collapsedFraction = 1f)

        // WHEN
        val bounds = headerBounds()

        // THEN
        assertThat(bounds.top).isZero()
        assertThat(bounds.height.roundToInt()).isEqualTo((FIXED_HEIGHT_DP * density).roundToInt())
        assertExactlyOneHeading()
        assertTitleTypography(fontSize = 20.sp)
    }

    @Test
    fun `when collapsible behavior is created, then real Material delegate owns scrolling without snap`() {
        // WHEN
        val fixture = whenScrollBehaviorIsCreated()

        // THEN
        assertThat(fixture.materialScrollBehavior.isPinned).isFalse()
        assertThat(fixture.materialScrollBehavior.snapAnimationSpec).isNull()
        assertThat(fixture.materialScrollBehavior.flingAnimationSpec).isNotNull()
    }

    @Test
    fun `given expanded header, when body partially scrolls up, then header remains partially collapsed`() {
        // GIVEN
        val fixture = whenScrollBehaviorIsCreated()
        val state = fixture.materialScrollBehavior.state
        state.heightOffsetLimit = -COLLAPSE_RANGE

        // WHEN
        val consumed = fixture.behavior.nestedScrollConnection.onPreScroll(
            available = Offset(x = 0f, y = -PARTIAL_SCROLL_DELTA),
            source = NestedScrollSource.UserInput,
        )

        // THEN
        assertThat(consumed).isEqualTo(Offset(x = 0f, y = -PARTIAL_SCROLL_DELTA))
        assertThat(state.heightOffset).isEqualTo(-PARTIAL_SCROLL_DELTA)
        assertThat(state.collapsedFraction).isEqualTo(0.5f)
    }

    @Test
    fun `given scrolling is disabled, when body scrolls, then header state is unchanged`() {
        // GIVEN
        val fixture = whenScrollBehaviorIsCreated(canScroll = { false })
        val state = fixture.materialScrollBehavior.state
        state.heightOffsetLimit = -COLLAPSE_RANGE

        // WHEN
        val consumed = fixture.behavior.nestedScrollConnection.onPreScroll(
            available = Offset(x = 0f, y = -PARTIAL_SCROLL_DELTA),
            source = NestedScrollSource.UserInput,
        )

        // THEN
        assertThat(consumed).isEqualTo(Offset.Zero)
        assertThat(state.heightOffset).isZero()
    }

    @Test
    fun `given collapsed header at top of body, when body scrolls down, then Material expands header`() {
        // GIVEN
        val fixture = whenScrollBehaviorIsCreated()
        fixture.materialScrollBehavior.state.heightOffsetLimit = -COLLAPSE_RANGE
        fixture.behavior.nestedScrollConnection.onPreScroll(
            available = Offset(x = 0f, y = -COLLAPSE_RANGE),
            source = NestedScrollSource.UserInput,
        )

        // WHEN
        val consumed = fixture.behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(x = 0f, y = PARTIAL_SCROLL_DELTA),
            source = NestedScrollSource.UserInput,
        )

        // THEN
        assertThat(consumed).isEqualTo(Offset(x = 0f, y = PARTIAL_SCROLL_DELTA))
        assertThat(fixture.materialScrollBehavior.state.heightOffset).isEqualTo(-PARTIAL_SCROLL_DELTA)
    }

    @Test
    fun `given expanded header, when header is directly dragged and flung up, then Material updates its state`() {
        // GIVEN
        val fixture = givenDirectlyDraggableHeader()
        val state = fixture.materialScrollBehavior.state

        // WHEN
        composeTestRule.onNodeWithTag(HEADER_TAG).performTouchInput {
            down(center)
            advanceEventTime(16)
            moveBy(Offset(x = 0f, y = -PARTIAL_SCROLL_DELTA))
            advanceEventTime(16)
            up()
        }
        composeTestRule.waitForIdle()

        // THEN
        assertThat(state.heightOffset).isLessThan(0f)
        assertThat(state.heightOffset).isGreaterThanOrEqualTo(state.heightOffsetLimit)
    }

    @Test
    fun `given long title at 2x font with action, when rendered, then title remains one line before action`() {
        // GIVEN
        givenHeader(
            collapsedFraction = 1f,
            title = LONG_TITLE,
            showAction = true,
            fontScale = 2f,
        )

        // WHEN
        val titleBounds = titleBounds(LONG_TITLE)
        val actionBounds = actionBounds()

        // THEN
        assertThat(titleBounds.right).isLessThanOrEqualTo(actionBounds.left)
        assertThat(titleLayoutResult(LONG_TITLE).lineCount).isEqualTo(1)
    }

    @Test
    fun `given fixed LTR header, when rendered, then title and action keep 24dp logical edges`() {
        // GIVEN
        val density = givenHeader(
            collapsedFraction = null,
            showAction = true,
        )
        val expectedEdge = LOGICAL_EDGE_DP * density

        // WHEN
        val headerBounds = headerBounds()
        val titleBounds = titleBounds()
        val actionBounds = actionBounds()

        // THEN
        assertThat(titleBounds.left.roundToInt()).isEqualTo(expectedEdge.roundToInt())
        assertThat((headerBounds.right - actionBounds.right).roundToInt()).isEqualTo(expectedEdge.roundToInt())
    }

    @Test
    fun `given fixed RTL header, when rendered, then title and action mirror 24dp logical edges`() {
        // GIVEN
        val density = givenHeader(
            collapsedFraction = null,
            showAction = true,
            layoutDirection = LayoutDirection.Rtl,
        )
        val expectedEdge = LOGICAL_EDGE_DP * density

        // WHEN
        val headerBounds = headerBounds()
        val titleBounds = titleBounds()
        val actionBounds = actionBounds()

        // THEN
        assertThat((headerBounds.right - titleBounds.right).roundToInt()).isEqualTo(expectedEdge.roundToInt())
        assertThat(actionBounds.left.roundToInt()).isEqualTo(expectedEdge.roundToInt())
    }

    private fun givenHeader(
        collapsedFraction: Float?,
        title: String = TITLE,
        showAction: Boolean = false,
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ): Float {
        var densityValue = 0f
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, fontScale),
                LocalLayoutDirection provides layoutDirection,
            ) {
                WooDesignSystemTheme {
                    densityValue = LocalDensity.current.density
                    val scrollBehavior = collapsedFraction?.let { fraction ->
                        val collapseRange = with(LocalDensity.current) {
                            (
                                TopAppBarDefaults.MediumAppBarCollapsedHeight -
                                    TopAppBarDefaults.MediumAppBarExpandedHeight
                                ).toPx()
                        }
                        val state = remember(fraction, collapseRange) {
                            TopAppBarState(
                                initialHeightOffsetLimit = collapseRange,
                                initialHeightOffset = collapseRange * fraction,
                                initialContentOffset = 0f,
                            )
                        }
                        val materialScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                            state = state,
                            snapAnimationSpec = null,
                            flingAnimationSpec = null,
                        )
                        remember(materialScrollBehavior) {
                            WooPageHeaderScrollBehavior(
                                materialScrollBehavior = materialScrollBehavior,
                            )
                        }
                    }
                    WooPageHeader(
                        title = title,
                        modifier = Modifier
                            .width(HEADER_WIDTH)
                            .testTag(HEADER_TAG),
                        showDivider = false,
                        scrollBehavior = scrollBehavior,
                        actions = {
                            if (showAction) {
                                WooOutlinedIconButton(
                                    imageVector = WooIcons.Regular.Share,
                                    contentDescription = SHARE_DESCRIPTION,
                                    onClick = {},
                                )
                            }
                        },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        return densityValue
    }

    private fun assertExactlyOneHeading() {
        val headings = composeTestRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
        ).fetchSemanticsNodes()

        assertThat(headings).hasSize(1)
    }

    private fun assertTitleTypography(fontSize: androidx.compose.ui.unit.TextUnit) {
        val style = titleLayoutResult().layoutInput.style

        assertThat(style.fontSize).isEqualTo(fontSize)
        assertThat(style.fontWeight).isEqualTo(FontWeight.Bold)
    }

    private fun titleLayoutResult(title: String = TITLE): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val action = composeTestRule
            .onNodeWithText(title)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action

        composeTestRule.runOnIdle {
            assertThat(action?.invoke(results)).isTrue()
        }
        return results.single()
    }

    private fun headerBounds() = composeTestRule.onNodeWithTag(HEADER_TAG).fetchSemanticsNode().boundsInRoot

    private fun titleBounds(title: String = TITLE) =
        composeTestRule.onNodeWithText(title).fetchSemanticsNode().boundsInRoot

    private fun actionBounds() = composeTestRule
        .onNodeWithContentDescription(SHARE_DESCRIPTION)
        .fetchSemanticsNode()
        .boundsInRoot

    private fun whenScrollBehaviorIsCreated(
        canScroll: () -> Boolean = { true },
    ): MaterialBehaviorFixture {
        lateinit var behavior: WooPageHeaderScrollBehavior
        lateinit var materialScrollBehavior: TopAppBarScrollBehavior
        composeTestRule.setContent {
            WooDesignSystemTheme {
                materialScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                    canScroll = canScroll,
                    snapAnimationSpec = null,
                )
                behavior = remember(materialScrollBehavior) {
                    WooPageHeaderScrollBehavior(materialScrollBehavior)
                }
            }
        }
        composeTestRule.waitForIdle()
        return MaterialBehaviorFixture(behavior, materialScrollBehavior)
    }

    private fun givenDirectlyDraggableHeader(): MaterialBehaviorFixture {
        lateinit var behavior: WooPageHeaderScrollBehavior
        lateinit var materialScrollBehavior: TopAppBarScrollBehavior
        composeTestRule.setContent {
            WooDesignSystemTheme {
                materialScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                    snapAnimationSpec = null,
                )
                behavior = remember(materialScrollBehavior) {
                    WooPageHeaderScrollBehavior(materialScrollBehavior)
                }
                WooPageHeader(
                    title = TITLE,
                    modifier = Modifier
                        .width(HEADER_WIDTH)
                        .testTag(HEADER_TAG),
                    showDivider = false,
                    scrollBehavior = behavior,
                )
            }
        }
        composeTestRule.waitForIdle()
        return MaterialBehaviorFixture(behavior, materialScrollBehavior)
    }

    private data class MaterialBehaviorFixture(
        val behavior: WooPageHeaderScrollBehavior,
        val materialScrollBehavior: TopAppBarScrollBehavior,
    )

    private companion object {
        const val FIXED_HEIGHT_DP = 64f
        const val EXPANDED_HEIGHT_DP = 112f
        const val LOGICAL_EDGE_DP = 24f
        const val COLLAPSE_RANGE = 48f
        const val PARTIAL_SCROLL_DELTA = 24f
        const val HEADER_TAG = "page-header"
        const val SHARE_DESCRIPTION = "Share"
        const val TITLE = "Products"
        const val LONG_TITLE = "A very long page header title that truncates before its action"
        val HEADER_WIDTH = 360.dp
    }
}
