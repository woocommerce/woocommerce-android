package com.woocommerce.android.experiment

import javax.inject.Inject

class SimplifiedLoginExperiment @Inject constructor() {
    fun getCurrentVariant() = LoginVariant.SIMPLIFIED

    enum class LoginVariant {
        STANDARD,
        SIMPLIFIED
    }
}
