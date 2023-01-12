package com.woocommerce.android.ui.login.storecreation.countrypicker

import java.util.Locale
import javax.inject.Inject

class LocalCountriesRepository @Inject constructor() {
    fun getLocalCountries(): Map<String, String> = Locale.getISOCountries().associateWith {
        Locale(Locale.getDefault().language, it).displayCountry
    }
}
