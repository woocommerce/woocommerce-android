package com.woocommerce.android.ui.login

import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore

class LoginAnalyticsTrackerTest {
    private val unifiedLoginTracker: UnifiedLoginTracker = mock()

    private val sut = LoginAnalyticsTracker(
        accountStore = mock<AccountStore>(),
        siteStore = mock<SiteStore>(),
        unifiedLoginTracker = unifiedLoginTracker
    )

    @Test
    fun `given wpcom login, when username password form viewed, then wordpress com flow is tracked`() {
        sut.trackUsernamePasswordFormViewed(isWpcom = true)

        verify(unifiedLoginTracker).track(Flow.WORDPRESS_COM, Step.USERNAME_PASSWORD)
    }

    @Test
    fun `given site credentials login, when username password form viewed, then store creds flow is tracked`() {
        sut.trackUsernamePasswordFormViewed(isWpcom = false)

        verify(unifiedLoginTracker).track(Flow.LOGIN_STORE_CREDS, Step.USERNAME_PASSWORD)
    }

    @Test
    fun `given wpcom login, when username password screen resumed, then wordpress com flow and step are restored`() {
        sut.usernamePasswordScreenResumed(isWpcom = true)

        verify(unifiedLoginTracker).setFlowAndStep(Flow.WORDPRESS_COM, Step.USERNAME_PASSWORD)
    }

    @Test
    fun `given store creds login, when username password screen resumed, then store creds flow and step restored`() {
        sut.usernamePasswordScreenResumed(isWpcom = false)

        verify(unifiedLoginTracker).setFlowAndStep(Flow.LOGIN_STORE_CREDS, Step.USERNAME_PASSWORD)
    }

    @Test
    fun `given a null message, when track failure, then it is forwarded to the unified login tracker`() {
        sut.trackFailure(null)

        verify(unifiedLoginTracker).trackFailure(null)
    }
}
