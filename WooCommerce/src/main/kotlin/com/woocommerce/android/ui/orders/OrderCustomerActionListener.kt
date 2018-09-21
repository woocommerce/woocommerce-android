package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.WCOrderModel

interface OrderCustomerActionListener {
    enum class Action {
        EMAIL,
        CALL,
        SMS
    }

    /**
     * Dial the provided phone number via an external app.
     * [order] The active order
     * [phone] The phone number to dial
     */
    fun dialPhone(order: WCOrderModel, phone: String)

    /**
     * Create a new emailAddr using an external mail app.
     * [order] The active order
     * [emailAddr] The emailAddr address to populate in the to: field
     */
    fun createEmail(order: WCOrderModel, emailAddr: String)

    /**
     * Open hangouts app.
     * [order] The active order
     * [phone] The number to send to hangouts
     */
    fun sendSms(order: WCOrderModel, phone: String)
}
