package com.woocommerce.android.screenshots.util.screenhelpers

import android.view.View
import android.widget.NumberPicker
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R.id
import com.woocommerce.android.screenshots.util.NestedScrollViewExtension
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers

open class Actions {
    fun scrollTo(elementID: Int) {
        Screen.waitOps.waitForElementToBeDisplayedWithoutFailure(elementID)

        // Need to use the NestedScrollViewExtension because Espresso doesn't natively support it:
        // https://medium.com/@devasierra/espresso-nestedscrollview-scrolling-via-kotlin-delegation-5e7f0aa64c09
        Espresso.onView(ViewMatchers.withId(elementID)).perform(NestedScrollViewExtension())
    }

    fun setValueInNumberPicker(number: Int) {
        val numPicker = Espresso.onView(
            ViewMatchers.withClassName(
                Matchers.equalTo(
                    NumberPicker::class.java.name
                )
            )
        )
        numPicker.perform(setNumber(number))
    }

    fun openToolbarActionMenu() {
        Screen.waitOps.waitForElementToBeDisplayed(id.toolbar)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Espresso.openActionBarOverflowOrOptionsMenu(context)
    }

    fun flipSwitchOn(elementID: Int, elementParentId: Int = 0) {
        if (elementParentId == 0) {
            Espresso.onView(ViewMatchers.withId(elementID))
                .check(ViewAssertions.matches(ViewMatchers.isNotChecked()))
                .perform(ViewActions.click())
        } else {
            Espresso.onView(
                CoreMatchers.allOf(
                    ViewMatchers.isDescendantOfA(ViewMatchers.withId(elementParentId)),
                    ViewMatchers.withId(elementID)
                )
            )
                .check(ViewAssertions.matches(ViewMatchers.isNotChecked()))
                .perform(ViewActions.click())
        }
    }

    fun dismissSoftwareKeyboard() {
        Espresso.closeSoftKeyboard()
    }

    fun pressBack() {
        Espresso.pressBack()
    }

    // HELPERS

    open fun setNumber(num: Int): ViewAction? {
        return object : ViewAction {
            override fun perform(uiController: UiController, view: View) {
                val np = view as NumberPicker
                np.value = num
            }

            override fun getDescription(): String {
                return "Set the passed number into the NumberPicker"
            }

            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(NumberPicker::class.java)
            }
        }
    }
}
