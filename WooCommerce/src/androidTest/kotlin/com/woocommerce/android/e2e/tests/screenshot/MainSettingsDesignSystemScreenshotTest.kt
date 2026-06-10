package com.woocommerce.android.e2e.tests.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.e2e.helpers.InitializationRule
import com.woocommerce.android.e2e.helpers.TestBase
import com.woocommerce.android.e2e.screens.TabNavComponent
import com.woocommerce.android.e2e.screens.login.WelcomeScreen
import com.woocommerce.android.e2e.screens.mystore.settings.BetaFeaturesScreen
import com.woocommerce.android.e2e.screens.mystore.settings.SettingsScreen
import com.woocommerce.android.e2e.tests.FAKE_PASSWORD
import com.woocommerce.android.e2e.tests.FAKE_URL
import com.woocommerce.android.e2e.tests.FAKE_USERNAME
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.FeatureFlag
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule
import java.io.File

@HiltAndroidTest
class MainSettingsDesignSystemScreenshotTest : TestBase(failOnUnmatchedWireMockRequests = false) {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @get:Rule(order = 2)
    val composeTestRule = createComposeRule()

    @get:Rule(order = 3)
    val localeTestRule = LocaleTestRule()

    @get:Rule(order = 4)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        rule.inject()
    }

    @After
    fun tearDown() {
        AppPrefs.removeFeatureFlagOverride(FeatureFlag.NEW_DESIGN_SYSTEM)
    }

    @Test
    fun mainSettingsScreenshot() {
        val arguments = InstrumentationRegistry.getArguments()
        val theme = arguments.getString("theme") ?: "light"
        val designSystemEnabled = (arguments.getString("designSystemEnabled") ?: "false")
            .toBooleanStrictOrNull() ?: false
        val modeName = if (designSystemEnabled) "design-system" else "legacy"

        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        AppPrefs.setFeatureFlagOverride(FeatureFlag.NEW_DESIGN_SYSTEM, designSystemEnabled)

        WelcomeScreen
            .logoutIfNeeded(composeTestRule)
            .selectLogin()
            .proceedWith(FAKE_URL)
            .proceedWith(FAKE_USERNAME)
            .proceedWith(FAKE_PASSWORD)

        val settingsScreen = openSettingsScreen(theme)

        val screenshotName = "main-settings-$modeName-$theme"
        settingsScreen.thenTakeScreenshot<SettingsScreen>(screenshotName)
        saveScreenshot(screenshotName)

        val lowerScreenshotName = "main-settings-lower-$modeName-$theme"
        settingsScreen
            .scrollToLowerContent()
            .thenTakeScreenshot<SettingsScreen>(lowerScreenshotName)
        saveScreenshot(lowerScreenshotName)

        val betaFeaturesScreenshotName = "beta-features-settings-toolbar-$modeName-$theme"
        settingsScreen
            .openBetaFeatures()
            .thenTakeScreenshot<BetaFeaturesScreen>(betaFeaturesScreenshotName)
        saveScreenshot(betaFeaturesScreenshotName)
    }

    private fun openSettingsScreen(theme: String): SettingsScreen {
        val settingsScreen = TabNavComponent()
            .gotoMoreMenuScreen()
            .openSettings(composeTestRule)

        return if (theme == "light" || theme == "dark") {
            settingsScreen
                .setTheme(theme)
                .goBackToMoreMenuScreen()
                .openSettings(composeTestRule)
        } else {
            settingsScreen
        }
    }

    private fun saveScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshotsDirectory = instrumentation.targetContext
            .getExternalFilesDir("main-settings-screenshots")
            ?: error("Unable to resolve screenshot output directory")
        screenshotsDirectory.mkdirs()

        val screenshot = File(screenshotsDirectory, "$name.png")
        val uiDevice = UiDevice.getInstance(instrumentation)
        check(uiDevice.takeScreenshot(screenshot)) {
            "Unable to capture screenshot: ${screenshot.absolutePath}"
        }
        uiDevice.executeShellCommand("mkdir -p $DEVICE_SCREENSHOT_DIRECTORY")
        uiDevice.executeShellCommand("cp ${screenshot.absolutePath} $DEVICE_SCREENSHOT_DIRECTORY/$name.png")
    }

    private companion object {
        const val DEVICE_SCREENSHOT_DIRECTORY = "/sdcard/Download/pr7-main-settings-screenshots"
    }
}
