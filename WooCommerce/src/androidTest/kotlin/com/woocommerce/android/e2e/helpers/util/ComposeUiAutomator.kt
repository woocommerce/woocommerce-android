package com.woocommerce.android.e2e.helpers.util

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Condition
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import java.util.regex.Pattern

internal class ComposeUiAutomator(
    private val composeTestRule: ComposeTestRule? = null,
) {
    private val device = UiDevice.getInstance(getInstrumentation())

    fun find(selector: BySelector): UiObject2? = device.findObject(selector)

    fun waitFor(
        selector: BySelector,
        description: String,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
    ): UiObject2 {
        var result: UiObject2? = null
        waitUntil(
            timeoutMillis = timeoutMillis,
            condition = {
                result = find(selector)
                result != null
            },
            failureMessage = { "$description was not found" },
        )
        return checkNotNull(result) { "$description was not found" }
    }

    fun waitForTag(
        tag: String,
        description: String = "Compose node with tag '$tag'",
    ): UiObject2 = waitFor(By.res(tag), description)

    fun waitForAtLeast(
        selector: BySelector,
        minimumCount: Int,
        description: String,
    ): List<UiObject2> {
        var results = emptyList<UiObject2>()
        waitUntil(
            condition = {
                results = device.findObjects(selector)
                results.size >= minimumCount
            },
            failureMessage = {
                "Expected at least $minimumCount $description, found ${results.size}"
            },
        )
        return results
    }

    fun waitForCount(
        selector: BySelector,
        expectedCount: Int,
        description: String,
    ): List<UiObject2> {
        var results = emptyList<UiObject2>()
        waitUntil(
            condition = {
                results = device.findObjects(selector)
                results.size == expectedCount
            },
            failureMessage = {
                "Expected $expectedCount $description, found ${results.size}"
            },
        )
        return results
    }

    fun waitUntil(
        timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
        condition: () -> Boolean,
        failureMessage: () -> String,
    ) {
        if (composeTestRule != null) {
            try {
                composeTestRule.waitUntil(timeoutMillis = timeoutMillis, condition = condition)
            } catch (error: ComposeTimeoutException) {
                throw AssertionError(failureMessage(), error)
            }
        } else {
            check(
                device.wait(
                    Condition<UiDevice, Boolean> {
                        Espresso.onIdle()
                        condition()
                    },
                    timeoutMillis,
                )
            ) {
                failureMessage()
            }
        }
    }

    fun scrollTextIntoView(listTag: String, text: String) {
        waitForTag(listTag)
        UiScrollable(UiSelector().resourceId(listTag)).apply {
            setAsVerticalList()
            scrollTextIntoView(text)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000L
    }
}

internal fun composeTestTagWithNumericSuffix(prefix: String): BySelector = By.res(
    Pattern.compile("${Pattern.quote(prefix)}[0-9]+")
)

internal fun UiObject2.allText(): List<String> = buildList {
    text?.takeIf(String::isNotEmpty)?.let(::add)
    contentDescription?.takeIf(String::isNotEmpty)?.let(::add)
    children.forEach { addAll(it.allText()) }
}
