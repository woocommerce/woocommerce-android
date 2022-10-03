package com.woocommerce.android.screenshots.orders

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.NestedScrollViewExtension
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.core.AllOf.allOf

class UnifiedOrderScreen : Screen(ORDER_CREATION) {
    companion object {
        const val CREATE_BUTTON = R.id.menu_create
        const val CUSTOMER_NOTE_EDITOR = R.id.customerOrderNote_editor
        const val CUSTOMER_SECTION = R.id.customer_section
        const val DONE_BUTTON = R.id.menu_done
        const val EDIT_STATUS_BUTTON = R.id.orderStatus_editButton
        const val FEE_BUTTON = R.id.fee_button
        const val NOTES_SECTION = R.id.notes_section
        const val ORDER_CREATION = R.id.order_creation_root
        const val PAYMENT_SECTION = R.id.payment_section
        const val PRODUCTS_SECTION = R.id.products_section
        const val SHIPPING_BUTTON = R.id.shipping_button
        const val TOOLBAR = R.id.collapsing_toolbar
        const val UPDATE_STATUS_LIST_VIEW = R.id.select_dialog_listview
    }

    fun createOrder(): SingleOrderScreen {
        clickOn(CREATE_BUTTON)
        return SingleOrderScreen()
    }

    fun updateOrderStatus(newOrderStatus: String): UnifiedOrderScreen {
        clickOn(EDIT_STATUS_BUTTON)
        waitForElementToBeDisplayed(UPDATE_STATUS_LIST_VIEW)
        Espresso.onView(withText(newOrderStatus))
            .perform(click())
        Espresso.onView(withText("Apply"))
            .perform(click())
        return this
    }

    fun clickAddCustomerDetails(): CustomerDetailsScreen {
        waitForElementToBeDisplayed(PAYMENT_SECTION)
        Espresso.onView(withId(CUSTOMER_SECTION))
            .perform(NestedScrollViewExtension())
        Espresso.onView(withText("Add customer details"))
            .perform(click())

        return CustomerDetailsScreen()
    }

    fun addShipping(): UnifiedOrderScreen {
        waitForElementToBeDisplayed(PAYMENT_SECTION)
        clickOn(SHIPPING_BUTTON)
        Espresso.onView((withText("0")))
            .perform(ViewActions.replaceText("3.30"))
        clickOn(DONE_BUTTON)
        return this
    }

    fun addFee(): UnifiedOrderScreen {
        waitForElementToBeDisplayed(PAYMENT_SECTION)
        clickOn(FEE_BUTTON)

        // Clearing first before re-adding because of the mock file, this is prepopulated at this point
        Espresso.onView((allOf(withText("2.25"), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))))
            .perform(ViewActions.clearText())
        Espresso.onView((allOf(withText("0"), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))))
            .perform(ViewActions.replaceText("2.25"))

        clickOn(DONE_BUTTON)
        return this
    }

    fun addCustomerNotes(note: String): UnifiedOrderScreen {
        Espresso.onView(withId(NOTES_SECTION))
            .perform(NestedScrollViewExtension())
        Espresso.onView(withText("Add note"))
            .perform(click())

        Espresso.onView(withId(CUSTOMER_NOTE_EDITOR))
            .perform((ViewActions.replaceText(note)))

        clickOn(DONE_BUTTON)
        return this
    }

    fun addProductTap(): OrderSelectProductScreen {
        waitForElementToBeDisplayed(PRODUCTS_SECTION)
        Espresso.onView(withText(R.string.order_creation_add_products)).perform(click())
        return OrderSelectProductScreen()
    }

    fun assertNewOrderScreen(): UnifiedOrderScreen {
        Espresso.onView(withId(TOOLBAR))
            .check(matches(hasDescendant(withText(R.string.order_creation_fragment_title))))
            .check(matches(isDisplayed()))
        Espresso.onView(withId(ORDER_CREATION)).check(matches(isDisplayed()))
        Espresso.onView(withId(CREATE_BUTTON)).check(matches(isDisplayed()))
        return this
    }

    fun goBackToOrdersScreen(): OrderListScreen {
        pressBack()

        val discard = getTranslatedString(R.string.discard)
        if (isElementDisplayed(discard)) {
            clickButtonInDialogWithTitle(discard)
        }

        return OrderListScreen()
    }
}
