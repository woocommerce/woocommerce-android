package com.woocommerce.android.ui.login.jetpack

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackActivationRepositoryTest : BaseUnitTest() {
    private val dispatcher: Dispatcher = mock()
    private val siteStore: SiteStore = mock()
    private val jetpackStore: JetpackStore = mock {
        onBlocking { registerSite(any(), any()) }.thenReturn(JetpackStore.JetpackResult(123L))
        onBlocking { connectJetpackAccount(any(), any(), any()) }.thenReturn(JetpackStore.JetpackResult(Unit))
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private val repository = JetpackActivationRepository(
        dispatcher = dispatcher,
        siteStore = siteStore,
        jetpackStore = jetpackStore,
        wooCommerceStore = wooCommerceStore,
        selectedSite = selectedSite,
        analyticsTrackerWrapper = analyticsTracker
    )

    @Test
    fun `given site is already registered, when connecting Jetpack account, then skip registration`() = testBlocking {
        val jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
            siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
            blogId = 123L
        )

        repository.connectJetpackAccount(
            site = SiteModel(),
            jetpackConnectionStatus = jetpackConnectionStatus,
            useApplicationPasswords = false
        )

        verify(jetpackStore, never()).registerSite(any(), any())
    }

    @Test
    fun `given site is not registered, when connecting Jetpack account, then register site`() = testBlocking {
        val jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
            siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
            blogId = null
        )

        repository.connectJetpackAccount(
            site = SiteModel(),
            jetpackConnectionStatus = jetpackConnectionStatus,
            useApplicationPasswords = false
        )

        verify(jetpackStore).registerSite(any(), any())
    }

    @Test
    fun `when connecting Jetpack account succeeds, then return success`() = testBlocking {
        val jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
            siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
            blogId = null
        )

        val result = repository.connectJetpackAccount(
            site = SiteModel(),
            jetpackConnectionStatus = jetpackConnectionStatus,
            useApplicationPasswords = false
        )

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `when connecting Jetpack account fails, then return failure`() = testBlocking {
        whenever(jetpackStore.connectJetpackAccount(any(), any(), any()))
            .thenReturn(JetpackStore.JetpackResult(JetpackStore.JetpackError("error")))
        val jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
            siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
            blogId = null
        )

        val result = repository.connectJetpackAccount(
            site = SiteModel(),
            jetpackConnectionStatus = jetpackConnectionStatus,
            useApplicationPasswords = false
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `when site registration fails, then return failure`() = testBlocking {
        whenever(jetpackStore.registerSite(any(), any()))
            .thenReturn(JetpackStore.JetpackResult(JetpackStore.JetpackError("error")))
        val jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
            siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
            blogId = null
        )

        val result = repository.connectJetpackAccount(
            site = SiteModel(),
            jetpackConnectionStatus = jetpackConnectionStatus,
            useApplicationPasswords = false
        )

        assertThat(result.isFailure).isTrue()
    }
}
