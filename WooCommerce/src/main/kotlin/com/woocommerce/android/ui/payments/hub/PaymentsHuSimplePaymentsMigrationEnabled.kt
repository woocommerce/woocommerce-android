package com.woocommerce.android.ui.payments.hub

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class PaymentsHuSimplePaymentsMigrationEnabled @Inject constructor() {
    operator fun invoke() = FeatureFlag.MIGRATION_SIMPLE_PAYMENTS.isEnabled()
}
