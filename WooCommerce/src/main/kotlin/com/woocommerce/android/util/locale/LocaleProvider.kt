package com.woocommerce.android.util.locale

import java.util.Locale

interface LocaleProvider {
    fun provideLocale(): Locale?
}
