package com.woocommerce.android.experiment

class SimplifiedLoginExperiment {
    fun run() = LoginVariant.STANDARD

    enum class LoginVariant {
        STANDARD,
        SIMPLIFIED
    }
}
