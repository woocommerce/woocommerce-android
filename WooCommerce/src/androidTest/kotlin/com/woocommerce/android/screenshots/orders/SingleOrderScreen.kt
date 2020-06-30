package com.woocommerce.android.screenshots.orders

import com.woocommerce.android.R.id
import com.woocommerce.android.screenshots.util.Screen
import com.woocommerce.android.screenshots.util.TestDataGenerator

class SingleOrderScreen : Screen {
    companion object {
        const val ORDER_NUMBER_LABEL = id.orderDetail_container
        const val ADD_NOTE_BTN = id.noteList_addNoteContainer
        const val ADD_NOTE_EDITOR = id.addNote_editor
        const val EMAIL_NOTE_SWITCH = id.switchSetting_switch
        const val ADD_BTN = id.menu_add
        const val BILLING_INFO_DDM = id.customerInfo_viewMore
    }

    constructor() : super(ORDER_NUMBER_LABEL)

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
        scrollTo(id.notesList_lblNotes)
        scrollTo(BILLING_INFO_DDM)
        clickOn(BILLING_INFO_DDM)
        scrollTo(id.notesList_lblNotes)
        clickOn(BILLING_INFO_DDM)
        return SingleOrderScreen()
    }

    // NAVIGATION
    fun scrollToNotesDetails(): SingleOrderScreen {
        scrollTo(id.notesList_notes)
        scrollTo(id.noteList_addNoteContainer)
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
