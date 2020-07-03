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
        clickOps.clickOn(ADD_NOTE_BTN)
        waitOps.waitForElementToBeDisplayed(ADD_NOTE_EDITOR)
        typeOps.typeTextInto(
            ADD_NOTE_EDITOR,
            TestDataGenerator.getHumanStyleDate()
        )
        clickOps.clickOn(ADD_BTN)
        return SingleOrderScreen()
    }

    fun checkBillingInfo(): SingleOrderScreen {
        actionOps.scrollTo(NOTE_LIST_LABEL)
        actionOps.scrollTo(BILLING_INFO_DDM)
        clickOps.clickOn(BILLING_INFO_DDM)
        actionOps.scrollTo(NOTE_LIST_LABEL)
        clickOps.clickOn(BILLING_INFO_DDM)
        return SingleOrderScreen()
    }

    fun issueRefund(): SingleOrderScreen {
        clickOps.clickOn(ISSUE_REFUND_BUTTON)
        waitOps.waitForElementToBeDisplayed(ISSUE_REFUND_PRODUCTS_LIST)
        clickOps.clickOn(REFUND_ITEMS_QUANTITY, 0)
        actionOps.setValueInNumberPicker(1)
        clickOps.clickOnInDialogViewWithText("OK")
        clickOps.clickOn(REFUND_ITEMS_NEXT_BUTTON)
        typeOps.typeTextInto(REFUND_REASON_FIELD, TestDataGenerator.getHumanStyleDate())
        clickOps.clickOn(REFUND_SUMMARY_BUTTON)
        clickOps.clickOnInDialogViewWithText("REFUND")
        waitOps.waitForElementToBeDisplayedWithoutFailure(REFUNDED_PRODUCTS_SECTION)
        actionOps.scrollTo(REFUNDED_PRODUCTS_SECTION)
        clickOps.clickOn(REFUNDED_PRODUCTS_ITEMS)
        actionOps.pressBack()
        return SingleOrderScreen()
    }

    // NAVIGATION
    fun scrollToNotesDetails(): SingleOrderScreen {
        actionOps.scrollTo(NOTES_LIST)
        actionOps.scrollTo(ADD_NOTE_BTN)
        return SingleOrderScreen()
    }

    fun scrollToPaymentDetails(): SingleOrderScreen {
        actionOps.scrollTo(PAYMENT_SECTION)
        return SingleOrderScreen()
    }

    fun goBackToSearch(): OrderSearchScreen {
        actionOps.pressBack()
        return OrderSearchScreen()
    }

    fun goBackToOrderList(): OrderListScreen {
        actionOps.pressBack()
        return OrderListScreen()
    }
}
