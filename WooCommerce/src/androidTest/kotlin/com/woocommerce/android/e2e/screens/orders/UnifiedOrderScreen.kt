package com.woocommerce.android.e2e.screens.orders

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.NestedScrollViewExtension
import com.woocommerce.android.e2e.helpers.util.Screen
import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf.allOf

class UnifiedOrderScreen : Screen(R.id.order_creation_root) {
    fun createOrder(): SingleOrderScreen {
        clickOn(R.id.menu_create)
        return SingleOrderScreen()
    }

    fun updateOrderStatus(newOrderStatus: String): UnifiedOrderScreen {
        clickOn(R.id.orderStatus_editButton)
        waitForElementToBeDisplayed(androidx.appcompat.R.id.select_dialog_listview)
        Espresso.onView(withText(newOrderStatus))
            .perform(click())
        Espresso.onView(withText("Apply"))
            .perform(click())
        return this
    }

    fun clickAddCustomerDetails(): CustomerDetailsScreen {
        waitForElementToBeDisplayed(R.id.payment_section)
        Espresso.onView(withId(R.id.customer_section))
            .perform(NestedScrollViewExtension())
        Espresso.onView(withText("Add customer details"))
            .perform(click())

        return CustomerDetailsScreen()
    }

    fun addShipping(): UnifiedOrderScreen {
        scrollTo(R.id.payment_section)
        waitForElementToBeDisplayed(R.id.payment_section)
        clickOn(R.id.shipping_button)
        waitForElementToBeDisplayed(R.id.amountEditText)

        Espresso.onView(
            allOf(
                isDescendantOfA(withId(R.id.amountEditText)),
                withClassName(endsWith("EditText"))
            )
        ).perform(ViewActions.replaceText("3.30"))

        clickOn(R.id.menu_done)
        return this
    }

    fun addFee(): UnifiedOrderScreen {
        waitForElementToBeDisplayed(R.id.payment_section)
        clickOn(R.id.fee_button)

        // Clearing first before re-adding because of the mock file, this is prepopulated at this point
        Espresso.onView((allOf(withText("2.25"), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))))
            .perform(ViewActions.clearText())
        Espresso.onView((allOf(withText("0"), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))))
            .perform(ViewActions.replaceText("2.25"))

        clickOn(R.id.menu_done)
        return this
    }

    fun editCustomerNote(note: String): UnifiedOrderScreen {
        waitForElementToBeDisplayedWithoutFailure(R.id.notes_section)
        scrollTo(R.id.notes_section)

        val editNoteButton = Espresso.onView(
            Matchers.allOf(
                isDescendantOfA(withId(R.id.notes_section)),
                withId(R.id.edit_button)
            )
        )

        clickOn(editNoteButton)
        typeTextInto(R.id.customerOrderNote_editor, note)
        clickOn(R.id.menu_done)
        return this
    }

    fun addProductTap(): ProductSelectorScreen {
        waitForElementToBeDisplayed(R.id.products_section)
        Espresso.onView(withText(R.string.order_creation_add_products)).perform(click())
        return ProductSelectorScreen()
    }

    fun assertNewOrderScreen(): UnifiedOrderScreen {
        Espresso.onView(withId(R.id.collapsing_toolbar))
            .check(matches(hasDescendant(withText(R.string.order_creation_fragment_title))))
            .check(matches(isDisplayed()))
        Espresso.onView(withId(R.id.order_creation_root)).check(matches(isDisplayed()))
        Espresso.onView(withId(R.id.menu_create)).check(matches(isDisplayed()))
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
