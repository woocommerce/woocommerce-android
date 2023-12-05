package com.woocommerce.android.notifications

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.woocommerce.android.R

fun Context.getChaChingUri(): Uri {
    return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + R.raw.cha_ching)
}
