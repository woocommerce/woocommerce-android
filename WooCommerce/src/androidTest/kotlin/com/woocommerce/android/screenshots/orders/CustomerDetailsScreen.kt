package com.woocommerce.android.screenshots.orders

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.Matchers.not
import org.hamcrest.core.AllOf.allOf

class CustomerDetailsScreen : Screen(TOOLBAR) {
    companion object {
        const val ADDRESS_SWITCH = R.id.addressSwitch
        const val FIRST_NAME_INPUT = R.id.first_name
        const val TOOLBAR = R.id.toolbar
        const val PROGRESS_BAR = R.id.progress_indicator
        const val FIRST_NAME_INPUT_HINT_TEXT = "First name"
    }

    fun addCustomerDetails(customerFirstName: String): UnifiedOrderScreen {
        waitForElementToDisappear(PROGRESS_BAR)

        // Enter First Name
        Espresso.onView(
            allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(FIRST_NAME_INPUT)),
                ViewMatchers.withHint(FIRST_NAME_INPUT_HINT_TEXT),
                ViewMatchers.isCompletelyDisplayed()
            )
        ).perform(
            scrollTo(),
            click(),
            replaceText(customerFirstName),
            closeSoftKeyboard()
        )

        // Scroll to the switch to enter shipping details
        Espresso.onView(allOf(ViewMatchers.withId(ADDRESS_SWITCH)))
            .perform(scrollTo())

        try {
            // Tap the switch if it's not already enabled
            Espresso.onView(
                allOf(
                    ViewMatchers.withId(ADDRESS_SWITCH),
                    ViewMatchers.isNotChecked()
                )
            ).perform(click())
        } catch (e: java.lang.Exception) { // ignore the failure
            println(e)
        }

        // Enter First Name used for Shipping
        Espresso.onView(
            allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(FIRST_NAME_INPUT)),
                ViewMatchers.withHint(FIRST_NAME_INPUT_HINT_TEXT),
                ViewMatchers.withText(not(customerFirstName))
            )
        ).perform(
            scrollTo(),
            click(),
            replaceText("$customerFirstName Shipping"),
            closeSoftKeyboard()
        )

        Espresso.onView(ViewMatchers.withText("DONE"))
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(click())

        return UnifiedOrderScreen()
    }
}
