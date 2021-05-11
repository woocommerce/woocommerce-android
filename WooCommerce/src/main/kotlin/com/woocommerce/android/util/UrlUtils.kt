package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.AppUrls
import org.wordpress.android.util.LanguageUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlUtils @Inject constructor(
    private val context: Context
) {
    val tosUrlWithLocale by lazy {
        "${AppUrls.AUTOMATTIC_TOS}?locale=${LanguageUtils.getPatchedCurrentDeviceLanguage(context)}"
    }
}
