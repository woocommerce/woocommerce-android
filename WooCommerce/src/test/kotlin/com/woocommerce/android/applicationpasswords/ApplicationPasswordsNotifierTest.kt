package com.woocommerce.android.applicationpasswords

import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordGenerationScenario

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationPasswordsNotifierTest : BaseUnitTest() {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val notifier = ApplicationPasswordsNotifier(analyticsTrackerWrapper)
    private val processLifecycle = ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry

    @Before
    fun setUp() {
        processLifecycle.currentState = STARTED
    }

    @After
    fun tearDown() {
        processLifecycle.currentState = CREATED
    }

    @Test
    fun `given a new password generation failure, when tracking, then include request context`() {
        val networkError = WPAPINetworkError(BaseNetworkError(GenericErrorType.SERVER_ERROR, "Server error"))

        notifier.onPasswordGenerationFailed(
            networkError,
            REQUEST_PATH,
            ApplicationPasswordGenerationScenario.GENERATION
        )

        val properties = argumentCaptor<Map<String, Any>>()
        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.APPLICATION_PASSWORDS_GENERATION_FAILED),
            properties.capture(),
            eq(networkError.javaClass.simpleName),
            eq(networkError.type.name),
            eq(networkError.message)
        )
        assertThat(properties.firstValue)
            .containsEntry(AnalyticsTracker.KEY_SCENARIO, "generation")
            .containsEntry(AnalyticsTracker.KEY_PATH, REQUEST_PATH)
            .containsEntry(AnalyticsTracker.KEY_IS_APP_IN_FOREGROUND, true)
            .containsEntry(AnalyticsTracker.KEY_CAUSE, "other")
    }

    @Test
    fun `given a regenerated password failure, when tracking, then use regeneration scenario`() {
        processLifecycle.currentState = CREATED
        val networkError = WPAPINetworkError(BaseNetworkError(GenericErrorType.SERVER_ERROR, "Server error"))

        notifier.onPasswordGenerationFailed(
            networkError,
            REQUEST_PATH,
            ApplicationPasswordGenerationScenario.REGENERATION
        )

        val properties = argumentCaptor<Map<String, Any>>()
        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.APPLICATION_PASSWORDS_GENERATION_FAILED),
            properties.capture(),
            eq(networkError.javaClass.simpleName),
            eq(networkError.type.name),
            eq(networkError.message)
        )
        assertThat(properties.firstValue)
            .containsEntry(AnalyticsTracker.KEY_SCENARIO, "regeneration")
            .containsEntry(AnalyticsTracker.KEY_PATH, REQUEST_PATH)
            .containsEntry(AnalyticsTracker.KEY_IS_APP_IN_FOREGROUND, false)
    }

    @Test
    fun `given a regenerated password is created, when tracking, then use regeneration scenario`() {
        notifier.onNewPasswordCreated(ApplicationPasswordGenerationScenario.REGENERATION)

        val properties = argumentCaptor<Map<String, Any>>()
        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.APPLICATION_PASSWORDS_NEW_PASSWORD_CREATED),
            properties.capture()
        )
        assertThat(properties.firstValue)
            .containsEntry(AnalyticsTracker.KEY_SCENARIO, "regeneration")
    }

    private companion object {
        const val REQUEST_PATH = "wp-json/wc/v3/products"
    }
}
