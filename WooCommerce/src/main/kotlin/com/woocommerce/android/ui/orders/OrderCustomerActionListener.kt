package com.woocommerce.android.ui.orders

interface OrderCustomerActionListener {
    /**
     * Dial the provided phone number via an external app.
     * [phone] The phone number to dial
     */
    fun dialPhone(phone: String)

    /**
     * Create a new emailAddr using an external mail app.
     * [emailAddr] The emailAddr address to populate in the to: field
     */
    fun createEmail(emailAddr: String)

    /**
     * Open hangouts app.
     * [phone] The number to send to hangouts
     */
    fun sendSms(phone: String)
}
