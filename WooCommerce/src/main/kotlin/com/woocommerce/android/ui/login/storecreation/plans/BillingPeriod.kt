package com.woocommerce.android.ui.login.storecreation.plans

import androidx.annotation.StringRes
import com.woocommerce.android.R.string

enum class BillingPeriod(@StringRes val nameId: Int, val slug: String) {
    ECOMMERCE_MONTHLY(string.store_creation_ecommerce_plan_period_month, "ecommerce-bundle-monthly"),
    ECOMMERCE_YEARLY(string.store_creation_ecommerce_plan_period_year, "ecommerce-bundle"),
    ECOMMERCE_BIYEARLY(string.store_creation_ecommerce_plan_period_year, "ecommerce-bundle-2y");

    companion object {
        private const val YEARLY_BILLING_PERIOD = 365
        private const val BIYEARLY_BILLING_PERIOD = 730

        fun fromPeriodValue(periodValue: Int?): BillingPeriod {
            return when (periodValue) {
                YEARLY_BILLING_PERIOD -> ECOMMERCE_YEARLY
                BIYEARLY_BILLING_PERIOD -> ECOMMERCE_BIYEARLY
                else -> ECOMMERCE_MONTHLY
            }
        }
    }
}
