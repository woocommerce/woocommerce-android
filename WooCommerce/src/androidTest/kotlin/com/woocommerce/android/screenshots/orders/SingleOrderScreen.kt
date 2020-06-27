package com.woocommerce.android.screenshots.orders

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen
import java.time.LocalDateTime

class SingleOrderScreen : Screen {
    companion object {
        const val ORDER_NUMBER_LABEL = R.id.orderDetail_container
        const val ADD_NOTE_BTN = R.id.noteList_addNoteContainer
        const val ADD_NOTE_EDITOR = R.id.addNote_editor
        const val EMAIL_NOTE_SWITCH = R.id.switchSetting_switch
        const val ADD_BTN = R.id.menu_add
    }

    constructor() : super(ORDER_NUMBER_LABEL)

    // TASKS
    fun emailOrderNoteToCustomer(): SingleOrderScreen {
        clickOn(ADD_NOTE_BTN)
        waitForElementToBeDisplayed(ADD_NOTE_EDITOR)
        typeTextInto(
            ADD_NOTE_EDITOR,
            "Posted this note on " + LocalDateTime.now().dayOfWeek + " in " + LocalDateTime.now().month
        )
        flipSwitchOn(EMAIL_NOTE_SWITCH)
        clickOn(ADD_BTN)
        return SingleOrderScreen()
    }

    fun checkBillingInfo(): SingleOrderScreen {
        waitForElementToBeDisplayedWithoutFailure(R.id.customerInfo_viewMoreButtonTitle)
        clickOn(R.id.customerInfo_viewMoreButtonTitle)
        waitForElementToBeDisplayedWithoutFailure(R.id.customerInfo_phone)
        clickOn(R.id.customerInfo_viewMoreButtonTitle)
        return SingleOrderScreen()
    }

    // NAVIGATION
    fun scrollToOrderDetails(): SingleOrderScreen {
        scrollTo(R.id.notesList_notes)
        scrollTo(R.id.noteList_addNoteContainer)
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
