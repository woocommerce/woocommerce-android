package com.woocommerce.android.ui.reviews.domain

import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class SupportsReviewsReadStatusTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus = mock()

    private lateinit var sut: SupportsReviewsReadStatus

    @Before
    fun setUp() {
        sut = SupportsReviewsReadStatus(
            selectedSite = selectedSite,
            pushNotificationRegistrationStatus = pushNotificationRegistrationStatus
        )
    }

    @Test
    fun `given no site selected, when invoked, then returns false`() = testBlocking {
        whenever(selectedSite.getIfExists()).thenReturn(null)

        val result = sut()

        assertThat(result).isFalse()
    }

    @Test
    fun `given Jetpack site and REGISTERED_WPCOM_ONLY push status, when invoked, then returns true`() = testBlocking {
        val jetpackSite = jetpackSite()
        whenever(selectedSite.getIfExists()).thenReturn(jetpackSite)
        whenever(pushNotificationRegistrationStatus(TEST_SITE_ID)).thenReturn(Status.REGISTERED_WPCOM_ONLY)

        val result = sut()

        assertThat(result).isTrue()
    }

    @Test
    fun `given Jetpack site and UNREGISTERED push status, when invoked, then returns true`() = testBlocking {
        val jetpackSite = jetpackSite()
        whenever(selectedSite.getIfExists()).thenReturn(jetpackSite)
        whenever(pushNotificationRegistrationStatus(TEST_SITE_ID)).thenReturn(Status.UNREGISTERED)

        val result = sut()

        assertThat(result).isTrue()
    }

    @Test
    fun `given Jetpack site and REGISTERED_WOO_ONLY push status, when invoked, then returns false`() = testBlocking {
        val jetpackSite = jetpackSite()
        whenever(selectedSite.getIfExists()).thenReturn(jetpackSite)
        whenever(pushNotificationRegistrationStatus(TEST_SITE_ID)).thenReturn(Status.REGISTERED_WOO_ONLY)

        val result = sut()

        assertThat(result).isFalse()
    }

    @Test
    fun `given Jetpack site and REGISTERED_BOTH push status, when invoked, then returns false`() = testBlocking {
        val jetpackSite = jetpackSite()
        whenever(selectedSite.getIfExists()).thenReturn(jetpackSite)
        whenever(pushNotificationRegistrationStatus(TEST_SITE_ID)).thenReturn(Status.REGISTERED_BOTH)

        val result = sut()

        assertThat(result).isFalse()
    }

    @Test
    fun `given ApplicationPasswords site, when invoked, then returns false`() = testBlocking {
        val appPasswordsSite: SiteModel = mock {
            on { origin } doReturn SiteModel.ORIGIN_XMLRPC
        }
        whenever(selectedSite.getIfExists()).thenReturn(appPasswordsSite)

        val result = sut()

        assertThat(result).isFalse()
    }

    @Test
    fun `given JetpackConnectionPackage site, when invoked, then returns false`() = testBlocking {
        val jetpackCpSite: SiteModel = mock {
            on { origin } doReturn SiteModel.ORIGIN_WPCOM_REST
            on { isJetpackConnected } doReturn false
            on { isJetpackCPConnected } doReturn true
        }
        whenever(selectedSite.getIfExists()).thenReturn(jetpackCpSite)

        val result = sut()

        assertThat(result).isFalse()
    }

    private fun jetpackSite(): SiteModel = mock {
        on { siteId } doReturn TEST_SITE_ID
        on { origin } doReturn SiteModel.ORIGIN_WPCOM_REST
        on { isJetpackConnected } doReturn true
    }

    companion object {
        private const val TEST_SITE_ID = 123L
    }
}
