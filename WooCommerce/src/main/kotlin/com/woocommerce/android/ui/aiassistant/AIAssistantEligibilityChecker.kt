package com.woocommerce.android.ui.aiassistant

import com.woocommerce.android.extensions.isEligibleForAI
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class AIAssistantEligibilityChecker @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val selectedSite: SelectedSite,
) {
    fun observeEligibility(): Flow<Boolean> {
        return selectedSite.observe()
            .combine(
                featureFlagRepository.observeIsEnabled(FeatureFlag.AI_ASSISTANT)
            ) { site, isAssistantEnabled -> isAssistantEnabled && site?.isEligibleForAI == true }
            .distinctUntilChanged()
    }
}
