package com.woocommerce.android.screenshots.util.screenhelpers

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.screenshots.util.FirstMatcher
import com.woocommerce.android.screenshots.util.Screen.Companion.isElementDisplayed
import com.woocommerce.android.screenshots.util.SupplierIdler
import org.hamcrest.CoreMatchers
import java.util.function.Supplier

open class Waits {
    fun waitForElementToBeDisplayed(elementID: Int) {
        waitForConditionToBeTrue(Supplier<Boolean> {
            isElementDisplayed(elementID)
        })
    }

    fun waitForElementToBeDisplayed(element: ViewInteraction) {
        waitForConditionToBeTrue(Supplier<Boolean> {
            isElementDisplayed(element)
        })
    }

    fun waitForElementToBeDisplayedWithoutFailure(elementId: Int): Boolean {
        try {
            waitForConditionToBeTrueWithoutFailure(Supplier<Boolean> {
                isElementDisplayed(elementId)
            })
        } catch (e: java.lang.Exception) { // ignore the failure
        }
        return isElementDisplayed(elementId)
    }

    private fun waitForConditionToBeTrue(supplier: Supplier<Boolean>) {
        if (supplier.get()) {
            return
        }
        SupplierIdler(supplier).idleUntilReady()
    }

    private fun waitForConditionToBeTrueWithoutFailure(supplier: Supplier<Boolean>) {
        if (supplier.get()) {
            return
        }
        SupplierIdler(supplier).idleUntilReady(false)
    }

    fun waitForAtLeastOneElementToBeDisplayed(elementId: Int) {
        waitForConditionToBeTrue(Supplier {
            atLeastOneElementIsDisplayed(elementId)
        })
    }

    // HELPERS
    private fun atLeastOneElementIsDisplayed(elementId: Int): Boolean {
        return try {
            Espresso.onView(
                CoreMatchers.allOf(
                    ViewMatchers.withId(elementId),
                    first()
                )
            ).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun idleFor(milliseconds: Int) {
        try {
            Thread.sleep(milliseconds.toLong())
        } catch (ex: Exception) {
            // do nothing
        }
    }

    // MATCHERS
    private fun first(): FirstMatcher {
        return FirstMatcher()
    }
}
