package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.aiassistant.AIAssistantEligibilityChecker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ObserveAIAssistantWidgetStatusTest {
    private val aiAssistantEligibilityChecker: AIAssistantEligibilityChecker = mock()

    @Test
    fun `given ai assistant is eligible, when observing status, then status is available`() = runTest {
        // GIVEN
        whenever(aiAssistantEligibilityChecker.observeEligibility()).thenReturn(flowOf(true))

        // WHEN
        val status = ObserveAIAssistantWidgetStatus(aiAssistantEligibilityChecker).invoke().first()

        // THEN
        assertThat(status).isEqualTo(DashboardWidget.Status.Available)
    }

    @Test
    fun `given ai assistant is not eligible, when observing status, then status is hidden`() = runTest {
        // GIVEN
        whenever(aiAssistantEligibilityChecker.observeEligibility()).thenReturn(flowOf(false))

        // WHEN
        val status = ObserveAIAssistantWidgetStatus(aiAssistantEligibilityChecker).invoke().first()

        // THEN
        assertThat(status).isEqualTo(DashboardWidget.Status.Hidden)
    }
}
