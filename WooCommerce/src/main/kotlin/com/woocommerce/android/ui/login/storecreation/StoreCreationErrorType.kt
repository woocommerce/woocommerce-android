package com.woocommerce.android.ui.login.storecreation

import androidx.annotation.StringRes
import com.woocommerce.android.R

enum class StoreCreationErrorType(@StringRes val title: Int, val isRetryPossible: Boolean) {
    SITE_ADDRESS_ALREADY_EXISTS(R.string.store_creation_ecommerce_plan_purchase_error, false),
    SITE_CREATION_FAILED(R.string.store_creation_ecommerce_plan_purchase_error, false),
    PLAN_PURCHASE_FAILED(R.string.store_creation_ecommerce_plan_purchase_error, false),
    STORE_NOT_READY(R.string.store_creation_ecommerce_store_loading_error, true),
    STORE_LOADING_FAILED(R.string.store_creation_ecommerce_store_loading_permanent_error, false),
    FREE_TRIAL_ASSIGNMENT_FAILED(R.string.store_creation_ecommerce_store_loading_permanent_error, false)
}
