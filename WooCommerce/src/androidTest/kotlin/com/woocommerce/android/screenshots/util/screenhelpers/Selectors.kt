package com.woocommerce.android.screenshots.util.screenhelpers

import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.google.android.material.tabs.TabLayout
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

class Selectors {
    fun selectItemWithTitleInTabLayout(stringID: Int, tabLayout: Int, elementParentId: Int) {
        val string = ActivityHelper.getTranslatedString(stringID)
        val tabLayout = Espresso.onView(
            CoreMatchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(elementParentId)),
                ViewMatchers.withId(tabLayout)
            )
        )

        tabLayout.perform(selectTabWithText(string))
    }

    private fun selectTabWithText(string: String): ViewAction {
        return object : ViewAction {
            override fun getDescription() = "with tab having title $string"

            override fun getConstraints(): Matcher<View>? {
                return CoreMatchers.allOf(
                    ViewMatchers.isDisplayed(),
                    ViewMatchers.isAssignableFrom(TabLayout::class.java)
                )
            }

            override fun perform(uiController: UiController, view: View) {
                val tabLayout = view as TabLayout
                tabLayout.tabCount

                for (i in 0 until tabLayout.tabCount) {
                    val tab = tabLayout.getTabAt(i)
                    if (tab?.text == string) {
                        tab.select()
                    }
                }
            }
        }
    }

    fun selectItemAtIndexInRecyclerView(index: Int, recyclerViewId: Int, elementId: Int = 0) {
        if (elementId == 0) {
            Screen.waitOps.waitForAtLeastOneElementToBeDisplayed(elementId)
        }

        Screen.waitOps.idleFor(1000)

        Espresso.onView(ViewMatchers.withId(recyclerViewId))
            .perform(RecyclerViewActions.actionOnItemAtPosition<ViewHolder>(index, ViewActions.click()))
    }

    fun selectItemAtIndexInRecyclerView1(index: Int, recyclerViewId: Int) {
        Screen.waitOps.waitForElementToBeDisplayedWithoutFailure(recyclerViewId)

        Screen.waitOps.idleFor(1000)
        Espresso.onView(ViewMatchers.withId(recyclerViewId))
            .perform(RecyclerViewActions.actionOnItemAtPosition<ViewHolder>(index, ViewActions.click()))
    }
}
