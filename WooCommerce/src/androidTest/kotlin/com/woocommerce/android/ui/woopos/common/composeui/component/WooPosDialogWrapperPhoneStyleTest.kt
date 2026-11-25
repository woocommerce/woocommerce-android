package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.WindowInsets
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WooPosDialogWrapperPhoneStyleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private var behindClicks = 0
    private var backClicks = 0
    private var closeClicks = 0
    private var dismissRequests = 0

    @Test
    fun fullScreen_tapDuringSlideIn_doesNotReachContentBehind() {
        val isVisible = mutableStateOf(false)
        setContent(phoneStyle = WooPosPhoneModalStyle.FullScreen(title = TITLE), isVisible = isVisible)
        composeTestRule.mainClock.autoAdvance = false

        isVisible.value = true
        composeTestRule.mainClock.advanceTimeBy(MID_TRANSITION_MS)
        composeTestRule.onNodeWithTag(BEHIND_TAG).performClick()

        assertThat(behindClicks).isZero()
    }

    @Test
    fun fullScreen_afterExit_tapReachesContentBehind() {
        val isVisible = mutableStateOf(true)
        setContent(phoneStyle = WooPosPhoneModalStyle.FullScreen(title = TITLE), isVisible = isVisible)
        composeTestRule.mainClock.autoAdvance = false

        isVisible.value = false
        composeTestRule.mainClock.advanceTimeBy(MID_TRANSITION_MS)
        composeTestRule.onNodeWithTag(BEHIND_TAG).performClick()
        composeTestRule.mainClock.advanceTimeBy(AFTER_TRANSITION_MS)
        composeTestRule.onNodeWithTag(BEHIND_TAG).performClick()

        assertThat(behindClicks).isEqualTo(1)
        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
    }

    @Test
    fun fullScreen_headerBackAndClose_runSeparateActions() {
        setContent(phoneStyle = WooPosPhoneModalStyle.FullScreen(title = TITLE, onBackClick = { backClicks++ }))

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.close)).performClick()

        assertThat(backClicks).isEqualTo(1)
        assertThat(closeClicks).isEqualTo(1)
        assertThat(dismissRequests).isZero()
    }

    @Test
    fun fullScreen_systemBackWithStepBack_runsStepBack() {
        setContent(phoneStyle = WooPosPhoneModalStyle.FullScreen(title = TITLE, onBackClick = { backClicks++ }))

        Espresso.pressBack()
        composeTestRule.waitForIdle()

        assertThat(backClicks).isEqualTo(1)
        assertThat(dismissRequests).isZero()
    }

    @Test
    fun fullScreen_systemBackWithoutStepBack_requestsDismiss() {
        setContent(phoneStyle = WooPosPhoneModalStyle.FullScreen(title = TITLE))

        Espresso.pressBack()
        composeTestRule.waitForIdle()

        assertThat(dismissRequests).isEqualTo(1)
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).assertDoesNotExist()
    }

    @Test
    fun bottomSheet_hasNoCloseButton() {
        setContent(phoneStyle = WooPosPhoneModalStyle.BottomSheet, showClose = true)

        composeTestRule.onNodeWithText(CONTENT).assertExists()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.close)).assertDoesNotExist()
    }

    @Test
    fun bottomSheet_systemBack_requestsDismiss() {
        setContent(phoneStyle = WooPosPhoneModalStyle.BottomSheet)

        Espresso.pressBack()
        composeTestRule.waitForIdle()

        assertThat(dismissRequests).isEqualTo(1)
    }

    @Test
    fun tablet_fullScreenStyle_showsCenterDialogWithoutHeader() {
        setContent(
            phoneStyle = WooPosPhoneModalStyle.FullScreen(title = TITLE, onBackClick = { backClicks++ }),
            size = TABLET_SIZE,
        )

        composeTestRule.onNodeWithText(CONTENT).assertExists()
        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.close)).assertExists()
    }

    @Test
    fun tablet_bottomSheetStyle_showsCenterDialogWithClose() {
        setContent(phoneStyle = WooPosPhoneModalStyle.BottomSheet, size = TABLET_SIZE, showClose = true)

        composeTestRule.onNodeWithText(CONTENT).assertExists()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.close)).performClick()

        assertThat(closeClicks).isEqualTo(1)
    }

    private fun setContent(
        phoneStyle: WooPosPhoneModalStyle,
        isVisible: MutableState<Boolean> = mutableStateOf(true),
        size: DpSize = PHONE_SIZE,
        showClose: Boolean = phoneStyle !is WooPosPhoneModalStyle.BottomSheet,
    ) {
        composeTestRule.setContent {
            val configuration = DeviceConfigurationOverride.ForcedSize(size) then
                DeviceConfigurationOverride.WindowSize(size) then
                DeviceConfigurationOverride.WindowInsets(WindowInsetsCompat.CONSUMED)
            DeviceConfigurationOverride(configuration) {
                WooPosTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        WooPosButton(
                            modifier = Modifier.testTag(BEHIND_TAG),
                            text = "Behind",
                            onClick = { behindClicks++ },
                        )
                        WooPosDialogWrapper(
                            isVisible = isVisible.value,
                            dialogBackgroundContentDescription = SCRIM_DESCRIPTION,
                            phoneStyle = phoneStyle,
                            onCloseClick = if (showClose) {
                                { closeClicks++ }
                            } else {
                                null
                            },
                            onDismissRequest = { dismissRequests++ },
                        ) {
                            WooPosText(text = CONTENT, style = WooPosTypography.BodyLarge)
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val TITLE = "Modal title"
        const val CONTENT = "Modal content"
        const val BEHIND_TAG = "behind"
        const val SCRIM_DESCRIPTION = "Scrim"
        const val MID_TRANSITION_MS = 100L
        const val AFTER_TRANSITION_MS = 1_000L
        val PHONE_SIZE = DpSize(400.dp, 800.dp)
        val TABLET_SIZE = DpSize(1280.dp, 800.dp)
    }
}
