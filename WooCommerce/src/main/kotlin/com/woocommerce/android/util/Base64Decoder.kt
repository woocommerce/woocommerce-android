package com.woocommerce.android.util

import android.util.Base64
import javax.inject.Inject

/**
 * This is just a wrapper around the Android's class [android.util.Base64] to allow mocking the implementation in tests
 */
class Base64Decoder @Inject constructor() {
    fun decode(content: String, flags: Int): ByteArray = Base64.decode(content, flags)

    fun decode(input: ByteArray, flags: Int): ByteArray = Base64.decode(input, flags)
}
