package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class InPersonPaymentsCanadaFeatureFlag @Inject constructor() {
    fun isEnabled() = FeatureFlag.IN_PERSON_PAYMENTS_CANADA.isEnabled(null)
}
