package com.woocommerce.android.notifications

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.woocommerce.android.R

fun Context.getChaChingUri(): Uri {
    val resourceId = R.raw.cha_ching
    return Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(resources.getResourcePackageName(resourceId))
        .appendPath(resources.getResourceTypeName(resourceId))
        .appendPath(resources.getResourceEntryName(resourceId))
        .build()
}
