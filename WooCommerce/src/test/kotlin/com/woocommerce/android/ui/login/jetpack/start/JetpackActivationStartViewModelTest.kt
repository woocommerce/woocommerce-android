package com.woocommerce.android.ui.login.jetpack.start

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackActivationStartViewModelTest : BaseUnitTest() {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val unifiedLoginTracker: UnifiedLoginTracker = mock()

    private fun whenViewModelIsCreated(
        isJetpackInstalled: Boolean,
        openedFromLogin: Boolean = true
    ) = JetpackActivationStartViewModel(
        savedStateHandle = JetpackActivationStartFragmentArgs(
            openedFromLogin = openedFromLogin,
            siteUrl = SITE_URL,
            jetpackStatus = JetpackStatus(
                isJetpackInstalled = isJetpackInstalled,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                    siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
                    blogId = null
                )
            )
        ).toSavedStateHandle(),
        analyticsTrackerWrapper = analyticsTrackerWrapper,
        unifiedLoginTracker = unifiedLoginTracker
    )

    @Test
    fun `given jetpack is installed, when the screen is shown, then jetpack_not_connected is reported`() {
        // WHEN
        whenViewModelIsCreated(isJetpackInstalled = true)

        // THEN
        verify(unifiedLoginTracker).track(
            flow = anyOrNull(),
            step = eq(UnifiedLoginTracker.Step.JETPACK_NOT_CONNECTED),
            properties = any()
        )
    }

    @Test
    fun `given jetpack is not installed, when the screen is shown, then jetpack_not_installed is reported`() {
        // WHEN
        whenViewModelIsCreated(isJetpackInstalled = false)

        // THEN
        verify(unifiedLoginTracker).track(
            flow = anyOrNull(),
            step = eq(UnifiedLoginTracker.Step.JETPACK_NOT_INSTALLED),
            properties = any()
        )
    }

    @Test
    fun `given a site is being connected, when the screen is shown, then its address is reported`() {
        // WHEN
        whenViewModelIsCreated(isJetpackInstalled = false)

        // THEN the address names the site being connected, not the selected store
        verify(unifiedLoginTracker).track(
            flow = anyOrNull(),
            step = any(),
            properties = argThat { get(AnalyticsTracker.KEY_URL) == "example.com" }
        )
    }

    @Test
    fun `given no flow was set, when the screen is shown, then the epilogue flow is reported`() {
        // GIVEN the screen is opened outside a login session
        whenever(unifiedLoginTracker.getFlow()).thenReturn(null)

        // WHEN
        whenViewModelIsCreated(isJetpackInstalled = true)

        // THEN
        verify(unifiedLoginTracker).track(
            flow = eq(UnifiedLoginTracker.Flow.EPILOGUE),
            step = any(),
            properties = any()
        )
    }

    @Test
    fun `given site discovery set the flow, when the screen is shown, then that flow is kept`() {
        // GIVEN
        whenever(unifiedLoginTracker.getFlow()).thenReturn(UnifiedLoginTracker.Flow.SITE_DISCOVERY)

        // WHEN
        whenViewModelIsCreated(isJetpackInstalled = false)

        // THEN
        verify(unifiedLoginTracker).track(
            flow = eq(UnifiedLoginTracker.Flow.SITE_DISCOVERY),
            step = any(),
            properties = any()
        )
    }

    @Test
    fun `given the screen was not opened from login, when it is shown, then the legacy event still fires`() {
        // GIVEN the merchant reached it from the dashboard, long after signing in

        // WHEN
        whenViewModelIsCreated(isJetpackInstalled = false, openedFromLogin = false)

        // THEN the screen is still counted; only the login step is suppressed
        verify(analyticsTrackerWrapper).track(
            stat = eq(AnalyticsEvent.LOGIN_JETPACK_REQUIRED_SCREEN_VIEWED),
            properties = any()
        )
    }

    @Test
    fun `given the screen was not opened from login, when it is shown, then no step is reported`() {
        // GIVEN the merchant reached it from the dashboard, long after signing in

        // WHEN
        whenViewModelIsCreated(isJetpackInstalled = true, openedFromLogin = false)

        // THEN a store connection outside a login journey is not a login step
        verify(unifiedLoginTracker, never()).track(
            flow = anyOrNull(),
            step = any(),
            properties = any()
        )
    }

    companion object {
        private const val SITE_URL = "https://example.com"
    }
}
