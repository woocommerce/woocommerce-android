package com.example.iap

const val IAP_LOG_TAG = "IAP"

interface LogWrapper {
    fun w(tag: String, message: String)
    fun d(tag: String, message: String)
    fun e(tag: String, message: String)
}
