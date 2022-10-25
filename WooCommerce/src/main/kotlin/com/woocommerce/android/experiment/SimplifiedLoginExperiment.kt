package com.woocommerce.android.experiment

import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.config.RemoteConfigRepository
import javax.inject.Inject

class SimplifiedLoginExperiment @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    companion object {
        private const val VARIANT_CONTROL = "control"
        private const val VARIANT_SIMPLIFIED = "simplified_login_i1"
    }

    fun getCurrentVariant(): LoginVariant {
        return when (remoteConfigRepository.getSimplifiedLoginVariant()) {
            VARIANT_CONTROL -> LoginVariant.STANDARD
            VARIANT_SIMPLIFIED -> LoginVariant.SIMPLIFIED
            else ->  LoginVariant.STANDARD
        }
    }

    enum class LoginVariant {
        STANDARD,
        SIMPLIFIED
    }
}
