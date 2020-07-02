package com.woocommerce.android.screenshots.orders

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen
import com.woocommerce.android.screenshots.util.TestDataGenerator

class SingleOrderScreen : Screen(ORDER_DETAIL_CONTAINER) {
    companion object {
        const val ORDER_DETAIL_CONTAINER = R.id.orderDetail_container
        const val ADD_NOTE_BTN = R.id.noteList_addNoteContainer
        const val ADD_NOTE_EDITOR = R.id.addNote_editor
        const val ADD_BTN = R.id.menu_add
        const val BILLING_INFO_DDM = R.id.customerInfo_viewMore
        const val NOTE_LIST_LABEL = R.id.notesList_lblNotes
        const val NOTES_LIST = R.id.notesList_notes
    }

    // TASKS
    fun emailOrderNoteToCustomer(): SingleOrderScreen {
        clickOn(ADD_NOTE_BTN)
        waitForElementToBeDisplayed(ADD_NOTE_EDITOR)
        typeTextInto(
            ADD_NOTE_EDITOR,
            TestDataGenerator.getHumanStyleDate()
        )
        clickOn(ADD_BTN)
        return SingleOrderScreen()
    }

    fun checkBillingInfo(): SingleOrderScreen {
        scrollTo(NOTE_LIST_LABEL)
        scrollTo(BILLING_INFO_DDM)
        clickOn(BILLING_INFO_DDM)
        scrollTo(NOTE_LIST_LABEL)
        clickOn(BILLING_INFO_DDM)
        return SingleOrderScreen()
    }

    // NAVIGATION
    fun scrollToNotesDetails(): SingleOrderScreen {
        scrollTo(NOTES_LIST)
        scrollTo(ADD_NOTE_BTN)
        return SingleOrderScreen()
    }

    fun goBackToSearch(): OrderSearchScreen {
        pressBack()
        return OrderSearchScreen()
    }

    fun goBackToOrderList(): OrderListScreen {
        pressBack()
        return OrderListScreen()
    }
}
