package com.woocommerce.android.util.locale

import android.content.Context
import androidx.core.os.ConfigurationCompat
import java.util.Locale
import javax.inject.Inject

class ContextBasedLocaleProvider @Inject constructor(
    private val context: Context
) : LocaleProvider {
    override fun provideLocale(): Locale? {
        return ConfigurationCompat.getLocales(context.resources.configuration)[0]
    }
}
