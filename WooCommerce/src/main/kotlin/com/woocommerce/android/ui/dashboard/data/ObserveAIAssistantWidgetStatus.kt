package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.aiassistant.AIAssistantEligibilityChecker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveAIAssistantWidgetStatus @Inject constructor(
    private val aiAssistantEligibilityChecker: AIAssistantEligibilityChecker,
) {
    operator fun invoke(): Flow<DashboardWidget.Status> =
        aiAssistantEligibilityChecker.observeEligibility().map { isEligible ->
            if (isEligible) DashboardWidget.Status.Available else DashboardWidget.Status.Hidden
        }
}
