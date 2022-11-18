package com.woocommerce.android.iap.pub

const val IAP_LOG_TAG = "IAP"

interface IAPLogWrapper {
    fun w(tag: String, message: String)
    fun d(tag: String, message: String)
    fun e(tag: String, message: String)
}
