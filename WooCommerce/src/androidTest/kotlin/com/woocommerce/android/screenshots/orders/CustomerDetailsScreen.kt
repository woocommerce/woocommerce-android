package com.woocommerce.android.screenshots.orders

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf.allOf

class CustomerDetailsScreen : Screen(TOOLBAR) {
    companion object {
        const val ADDRESS_SWITCH = R.id.addressSwitch
        const val EDIT_TEXT_FIELD = R.id.edit_text
        const val TOOLBAR = R.id.toolbar
        const val FIRST_NAME = "Mira"
        const val HINT_TEXT = "First name"
    }

    fun addCustomerDetails(): UnifiedOrderScreen {
        updateFirstName((allOf(withId(EDIT_TEXT_FIELD), withHint(HINT_TEXT), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))))
        Espresso.onView((allOf(withId(EDIT_TEXT_FIELD), withText(FIRST_NAME))))
            .perform(closeSoftKeyboard())
        Espresso.onView((allOf(withId(ADDRESS_SWITCH))))
            .perform(scrollTo(), click())
        updateFirstName((allOf(withId(EDIT_TEXT_FIELD), withHint(HINT_TEXT), withText(""))))
        Espresso.onView(withText("DONE"))
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(click())

        return UnifiedOrderScreen()
    }

    private fun updateFirstName (matchers: Matcher<View>) {
        Espresso.onView(matchers)
            .perform(scrollTo(), click())
            .perform(replaceText(FIRST_NAME))
    }
}
