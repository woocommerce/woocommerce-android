package com.woocommerce.android.util

import java.security.MessageDigest

private const val SITE_HASH_PREFIX = "woopos-site-v1:"
private const val SITE_HASH_HEX_LENGTH = 32

fun siteIdHash(remoteId: Long): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest("$SITE_HASH_PREFIX$remoteId".toByteArray(Charsets.UTF_8))
    return bytes.joinToString(separator = "") { "%02x".format(it) }.take(SITE_HASH_HEX_LENGTH)
}
