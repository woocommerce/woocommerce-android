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
        const val PAYMENT_SECTION = R.id.orderDetail_paymentInfo
        const val ISSUE_REFUND_BUTTON = R.id.paymentInfo_issueRefundButton
        const val ISSUE_REFUND_PRODUCTS_LIST = R.id.issueRefund_products
        const val REFUND_ITEMS_QUANTITY = R.id.refundItem_quantity
        const val REFUND_ITEMS_NEXT_BUTTON = R.id.issueRefund_btnNextFromItems
        const val REFUND_REASON_FIELD = R.id.refundSummary_reason
        const val REFUND_SUMMARY_BUTTON = R.id.refundSummary_btnRefund
        const val REFUNDED_PRODUCTS_SECTION = R.id.orderDetail_refundsInfo
        const val REFUNDED_PRODUCTS_ITEMS = R.id.refundsInfo_count
    }

    // TASKS
    fun addOrderNote(): SingleOrderScreen {
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

    fun issueRefund(): SingleOrderScreen {
        clickOn(ISSUE_REFUND_BUTTON)
        waitForElementToBeDisplayed(ISSUE_REFUND_PRODUCTS_LIST)
        clickOn(REFUND_ITEMS_QUANTITY, 0)
        setValueInNumberPicker(1)
        clickOnInDialogViewWithText("OK")
        clickOn(REFUND_ITEMS_NEXT_BUTTON)
        typeTextInto(REFUND_REASON_FIELD, TestDataGenerator.getHumanStyleDate())
        clickOn(REFUND_SUMMARY_BUTTON)
        clickOnInDialogViewWithText("REFUND")
        scrollTo(REFUNDED_PRODUCTS_SECTION)
        clickOn(REFUNDED_PRODUCTS_ITEMS)
        pressBack()
        return SingleOrderScreen()
    }

    // NAVIGATION
    fun scrollToNotesDetails(): SingleOrderScreen {
        scrollTo(NOTES_LIST)
        scrollTo(ADD_NOTE_BTN)
        return SingleOrderScreen()
    }

    fun scrollToPaymentDetails(): SingleOrderScreen {
        scrollTo(PAYMENT_SECTION)
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
