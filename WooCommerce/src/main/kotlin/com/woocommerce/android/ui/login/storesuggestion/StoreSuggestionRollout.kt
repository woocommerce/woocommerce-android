package com.woocommerce.android.ui.login.storesuggestion

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.util.BuildConfigWrapper
import javax.inject.Inject
import kotlin.random.Random

class StoreSuggestionRollout @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    fun isInRollout(): Boolean {
        if (buildConfigWrapper.debug) return true
        return getOrAssignRolloutBucket() <= ROLLOUT_PERCENT
    }

    private fun getOrAssignRolloutBucket(): Int =
        appPrefsWrapper.storeSuggestionRolloutBucket
            ?: Random.nextInt(ROLLOUT_BUCKET_MIN, ROLLOUT_BUCKET_MAX + 1)
                .also { appPrefsWrapper.storeSuggestionRolloutBucket = it }

    companion object {
        const val ROLLOUT_BUCKET_MIN = 1
        const val ROLLOUT_BUCKET_MAX = 100
        const val ROLLOUT_PERCENT = 50
    }
}
