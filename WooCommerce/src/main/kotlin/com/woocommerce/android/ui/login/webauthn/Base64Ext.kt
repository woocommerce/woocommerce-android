package com.woocommerce.android.ui.login.webauthn

import android.util.Base64

private const val BASE64_FLAG = Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE

fun ByteArray.toBase64(): String {
    return Base64.encodeToString(this, BASE64_FLAG)
}

fun String.decodeBase64(): ByteArray {
    return Base64.decode(this, BASE64_FLAG)
}
