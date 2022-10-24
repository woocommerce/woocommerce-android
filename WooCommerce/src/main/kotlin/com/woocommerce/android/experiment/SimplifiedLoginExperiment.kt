package com.woocommerce.android.experiment

import javax.inject.Inject

class SimplifiedLoginExperiment @Inject constructor() {
    suspend fun getCurrentVariant() = LoginVariant.SIMPLIFIED

    enum class LoginVariant {
        STANDARD,
        SIMPLIFIED
    }
}
