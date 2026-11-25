package com.woocommerce.android.ui.woopos.common.pin

import android.graphics.Bitmap
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.WindowInsets
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.then
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.approval.WooPosApprovalAction
import com.woocommerce.android.ui.woopos.roles.approval.WooPosApprovalPhoneContent
import com.woocommerce.android.ui.woopos.roles.approval.WooPosApprovalState
import com.woocommerce.android.ui.woopos.roles.approval.WooPosApprovalTabletContent
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreen
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupDialog
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class WooPosPINViewportTest {
    @get:Rule
    val composeTestRule = createComposeRule(effectContext = NoMotion)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun narrowPhoneKeypadsFitAcrossTextSizes() = assertViewport(SIZES[0])

    @Test
    fun phoneKeypadsFitAcrossTextSizes() = assertViewport(SIZES[1])

    @Test
    fun compactLandscapeKeypadsFitAcrossTextSizes() = assertViewport(SIZES[2])

    @Test
    fun smallTabletKeypadsFitAcrossTextSizes() = assertViewport(SIZES[3])

    @Test
    fun tabletLandscapeKeypadsFitAcrossTextSizes() = assertViewport(SIZES[4])

    @Test
    fun tabletPortraitKeypadsFitAcrossTextSizes() = assertViewport(SIZES[5])

    private fun assertViewport(size: DpSize) {
        val scenario = mutableStateOf(Scenario(size, 1f, Flow.LOCK))
        setScenarioContent(scenario)
        for (scale in FONT_SCALES) {
            for (flow in Flow.entries) {
                val current = Scenario(size, scale, flow)
                assertScenarioPhases(current, scenario)
            }
        }
    }

    @Test
    fun representativeKeypadsFitInLightAndDark() {
        val scenario = mutableStateOf(Scenario(SIZES.first(), 1f, Flow.LOCK))
        setScenarioContent(scenario)
        val directory = File(context.getExternalFilesDir(null), "pin-viewports").apply { mkdirs() }
        val scenes = listOf(
            Scenario(DpSize(412.dp, 915.dp), 1f, Flow.APPROVAL_OFF),
            Scenario(DpSize(1024.dp, 674.dp), 1.15f, Flow.APPROVAL_OFF),
            Scenario(DpSize(320.dp, 568.dp), 2f, Flow.APPROVAL_ADMIN),
            Scenario(DpSize(800.dp, 320.dp), 2f, Flow.SETUP),
            Scenario(DpSize(1024.dp, 674.dp), 2f, Flow.APPROVAL_OFF),
            Scenario(DpSize(1024.dp, 674.dp), 2f, Flow.LOCK),
        )
        for (dark in listOf(false, true)) {
            for ((index, scene) in scenes.withIndex()) {
                val current = scene.copy(dark = dark)
                composeTestRule.runOnIdle { scenario.value = current }
                assertCompleteKeypad(current)
                assertApprovalHeadingIsReadable(current)
                assertSingleColumn(current)
                File(directory, "scene-$index-${if (dark) "dark" else "light"}.png").outputStream().use {
                    composeTestRule.onRoot().captureToImage().asAndroidBitmap().compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        it,
                    )
                }
            }
        }
    }

    @Test
    fun pendingPaymentKeepsKeysVisibleAndStatusCheckAccessible() {
        val scenario = mutableStateOf(Scenario(SIZES.first(), 1f, Flow.LOCK, phase = 3))
        setScenarioContent(scenario)
        val label = context.getString(R.string.woopos_payment_restore_check)
        for (size in SIZES) {
            for (scale in FONT_SCALES) {
                val current = Scenario(size, scale, Flow.LOCK, phase = 3)
                composeTestRule.runOnIdle { scenario.value = current }
                assertCompleteKeypad(current)
                val check = composeTestRule.onNodeWithText(label)
                if (composeTestRule.onAllNodes(hasText(label) and hasAnyAncestor(hasScrollAction()))
                        .fetchSemanticsNodes().isNotEmpty()
                ) {
                    check.performScrollTo()
                }
                check.assertIsDisplayed().assertIsEnabled().assertHasClickAction()
                assertCompleteKeypad(current)
            }
        }
    }

    private fun assertScenarioPhases(current: Scenario, scenario: MutableState<Scenario>) {
        composeTestRule.runOnIdle { scenario.value = current }
        assertCompleteKeypad(current)
        assertApprovalHeadingIsReadable(current)
        assertSingleColumn(current)
        val originalBounds = composeTestRule.onNodeWithText("0").getUnclippedBoundsInRoot()
        for (phase in 1..2) {
            composeTestRule.runOnIdle { scenario.value = current.copy(phase = phase) }
            assertCompleteKeypad(current.copy(phase = phase))
            assertThat(composeTestRule.onNodeWithText("0").getUnclippedBoundsInRoot())
                .describedAs("Keypad must not move when feedback or setup page changes: %s", current)
                .isEqualTo(originalBounds)
        }
    }

    private fun setScenarioContent(scenario: State<Scenario>) {
        composeTestRule.setContent {
            val current = scenario.value
            var configuration = DeviceConfigurationOverride.WindowSize(current.size) then
                DeviceConfigurationOverride.WindowInsets(WindowInsetsCompat.CONSUMED) then
                DeviceConfigurationOverride.FontScale(current.fontScale)
            current.dark?.let { configuration = configuration then DeviceConfigurationOverride.DarkMode(it) }
            configuration = configuration then DeviceConfigurationOverride.ForcedSize(current.size)
            DeviceConfigurationOverride(configuration) { ScenarioContent(current) }
        }
    }

    @Composable
    private fun ScenarioContent(current: Scenario) {
        key(current.size, current.fontScale, current.flow) {
            val density = LocalDensity.current
            val hostWindow = LocalWindowInfo.current
            val pixels = with(density) {
                IntSize(current.size.width.roundToPx(), current.size.height.roundToPx())
            }
            val window = remember(hostWindow, pixels, current.size) {
                object : WindowInfo by hostWindow {
                    override val containerSize = pixels
                    override val containerDpSize = current.size
                }
            }
            CompositionLocalProvider(LocalWindowInfo provides window) {
                WooPosTheme {
                    BoxWithConstraints(Modifier.requiredSize(current.size.width, current.size.height)) {
                        check(abs(maxWidth.value - current.size.width.value) < 1f) {
                            "Requested ${current.size}, actual $maxWidth x $maxHeight, density=$density"
                        }
                        check(abs(maxHeight.value - current.size.height.value) < 1f) {
                            "Requested ${current.size}, actual $maxWidth x $maxHeight, density=$density"
                        }
                        ScenarioFlow(current)
                    }
                }
            }
        }
    }

    @Composable
    private fun ScenarioFlow(current: Scenario) {
        when (current.flow) {
            Flow.LOCK -> WooPosLockScreen(
                WooPosLockScreenState(isLocked = true, status = current.status),
                onUIEvent = {},
            )
            Flow.SETUP -> {
                val role = if (current.fontScale < 1.5f) WooPosLocalRole.ADMIN else WooPosLocalRole.CASHIER
                val entry = when (current.phase) {
                    1 -> WooPosSettingsSecurityPINSetupState.Confirm(role)
                    2 -> WooPosSettingsSecurityPINSetupState.Choose(
                        role,
                        error = WooPosSettingsSecurityPINSetupState.Error.MISMATCH,
                    )
                    else -> WooPosSettingsSecurityPINSetupState.Choose(role)
                }
                WooPosSettingsSecurityPINSetupDialog(
                    entry,
                    isVisible = true,
                    onUIEvent = {},
                    onClose = {},
                )
            }
            else -> {
                val state = WooPosApprovalState(current.approvalAction, current.status)
                if (currentWooPosBreakpoint() == WooPosBreakpoint.Phone) {
                    WooPosApprovalPhoneContent(state, onUIEvent = {}, onDismiss = {})
                } else {
                    WooPosApprovalTabletContent(state, onUIEvent = {}, onDismiss = {})
                }
            }
        }
    }

    private fun assertCompleteKeypad(scenario: Scenario) {
        composeTestRule.waitForIdle()
        val keyMatchers = (0..9).map { hasText(it.toString()) } +
            hasContentDescription(context.getString(R.string.woopos_lock_delete_digit))
        val keys = composeTestRule.onAllNodes(keyMatchers.reduce { first, next -> first or next }).fetchSemanticsNodes()
        for (matcher in keyMatchers) {
            val matches = keys.filter { matcher.matches(it) }
            assertThat(matches).describedAs("Every keypad key must be present once: %s", scenario).hasSize(1)
            val node = matches.single()
            assertThat(node.boundsInRoot.height)
                .describedAs("Key must not be clipped: %s", scenario)
                .isCloseTo(node.layoutInfo.height.toFloat(), offset(2f))
            assertThat(node.boundsInRoot.width)
                .describedAs("Key must not be clipped horizontally: %s", scenario)
                .isCloseTo(node.layoutInfo.width.toFloat(), offset(2f))
            assertThat(node.boundsInRoot.height)
                .describedAs("Key must keep its minimum touch target: %s", scenario)
                .isGreaterThanOrEqualTo(with(node.layoutInfo.density) { 48.dp.toPx() } - 2f)
        }
        composeTestRule.onAllNodes(hasText("0") and hasAnyAncestor(hasScrollAction())).assertCountEquals(0)
        val visibleText = SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and
            !hasAnyAncestor(hasScrollAction())
        for (node in composeTestRule.onAllNodes(visibleText).fetchSemanticsNodes()) {
            val layouts = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            assertThat(layouts.any { it.didOverflowHeight })
                .describedAs("PIN feedback and key labels must not be clipped: %s", scenario)
                .isFalse()
        }
    }

    private fun assertApprovalHeadingIsReadable(scenario: Scenario) {
        if (scenario.flow == Flow.LOCK || scenario.flow == Flow.SETUP) return
        val layouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_approval_required))
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertThat(layouts).isNotEmpty
        assertThat(layouts.maxOf { it.lineCount })
            .describedAs("The approval heading must not wrap into a narrow column: %s", scenario)
            .isLessThanOrEqualTo(3)
    }

    private fun assertSingleColumn(scenario: Scenario) {
        val dots = composeTestRule.onNodeWithContentDescription(context.getString(R.string.woopos_lock_pin_entry))
            .fetchSemanticsNode()
        assertThat(dots.boundsInRoot.height)
            .describedAs("PIN dots must be fully visible: %s", scenario)
            .isCloseTo(dots.layoutInfo.height.toFloat(), offset(2f))
        val heading = composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .fetchSemanticsNode().boundsInRoot
        val keypad = composeTestRule.onNodeWithText("2").fetchSemanticsNode().boundsInRoot
        assertThat(heading.center.x)
            .describedAs("The heading and keypad must share one centered column: %s", scenario)
            .isCloseTo(keypad.center.x, offset(2f))
        assertThat(heading.bottom)
            .describedAs("The heading must be above the keypad: %s", scenario)
            .isLessThanOrEqualTo(keypad.top)
    }

    private enum class Flow {
        LOCK, SETUP, APPROVAL_OFF, APPROVAL_ADMIN, APPROVAL_COUPON, APPROVAL_REFUND, APPROVAL_EXIT
    }

    private data class Scenario(
        val size: DpSize,
        val fontScale: Float,
        val flow: Flow,
        val phase: Int = 0,
        val dark: Boolean? = null,
    ) {
        val approvalAction: WooPosApprovalAction
            get() = when (flow) {
                Flow.APPROVAL_OFF -> WooPosApprovalAction.DisablePINProtection
                Flow.APPROVAL_ADMIN -> WooPosApprovalAction.ChangeAdminPIN
                Flow.APPROVAL_COUPON -> WooPosApprovalAction.CreateCoupon
                Flow.APPROVAL_REFUND -> WooPosApprovalAction.IssueRefund("a test order reference")
                Flow.APPROVAL_EXIT -> WooPosApprovalAction.Exit
                else -> error("Expected an approval flow")
            }

        val status: WooPosPINEntryStatus
            get() = when (phase) {
                1 -> WooPosPINEntryStatus.Incorrect
                2 -> WooPosPINEntryStatus.Wait(90)
                3 -> WooPosPINEntryStatus.PaymentPending
                else -> WooPosPINEntryStatus.Idle
            }
    }

    private object NoMotion : MotionDurationScale {
        override val scaleFactor: Float = 0f
    }

    private companion object {
        val SIZES = listOf(
            DpSize(320.dp, 568.dp),
            DpSize(412.dp, 915.dp),
            DpSize(800.dp, 320.dp),
            DpSize(1024.dp, 674.dp),
            DpSize(1280.dp, 800.dp),
            DpSize(800.dp, 1280.dp),
        )
        val FONT_SCALES = listOf(1f, 1.15f, 1.5f, 2f)
    }
}
