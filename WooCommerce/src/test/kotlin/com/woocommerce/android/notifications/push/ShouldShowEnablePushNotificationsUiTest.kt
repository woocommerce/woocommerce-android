package com.woocommerce.android.notifications.push

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class ShouldShowEnablePushNotificationsUiTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()

    private lateinit var sut: ShouldShowEnablePushNotificationsUi

    @Before
    fun setUp() {
        sut = ShouldShowEnablePushNotificationsUi(
            selectedSite = selectedSite,
            pushNotificationRegistrationStatus = pushNotificationRegistrationStatus,
            featureFlagRepository = featureFlagRepository
        )
    }

    @Test
    fun `given M1 flag is disabled, when invoked, then returns false without observing site`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
            .thenReturn(false)

        val result = sut().first()

        assertThat(result).isFalse()
        verify(selectedSite, never()).observe()
    }

    @Test
    fun `given M1 flag is enabled and no site selected, when invoked, then returns false`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
            .thenReturn(true)
        whenever(selectedSite.observe()).thenReturn(flowOf(null))

        val result = sut().first()

        assertThat(result).isFalse()
    }

    @Test
    fun `given M1 flag is enabled and Jetpack site selected, when invoked, then returns false`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
            .thenReturn(true)
        val jetpackSite: SiteModel = mock {
            on { origin } doReturn SiteModel.ORIGIN_WPCOM_REST
            on { isJetpackConnected } doReturn true
        }
        whenever(selectedSite.observe()).thenReturn(flowOf(jetpackSite))

        val result = sut().first()

        assertThat(result).isFalse()
    }

    @Test
    fun `given M1 flag is enabled and non-Jetpack site is already Woo-registered, when invoked, then returns false`() =
        testBlocking {
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
                .thenReturn(true)
            val appPasswordsSite: SiteModel = mock {
                on { siteId } doReturn TEST_SITE_ID
                on { origin } doReturn 0 // != ORIGIN_WPCOM_REST(1) → ApplicationPasswords
            }
            whenever(selectedSite.observe()).thenReturn(flowOf(appPasswordsSite))
            whenever(pushNotificationRegistrationStatus.observe(TEST_SITE_ID))
                .thenReturn(flowOf(PushNotificationRegistrationStatus.Status.REGISTERED_WOO_ONLY))

            val result = sut().first()

            assertThat(result).isFalse()
        }

    @Test
    fun `given M1 flag is enabled and non-Jetpack site is not Woo-registered, when invoked, then returns true`() =
        testBlocking {
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
                .thenReturn(true)
            val appPasswordsSite: SiteModel = mock {
                on { siteId } doReturn TEST_SITE_ID
                on { origin } doReturn 0 // != ORIGIN_WPCOM_REST(1) → ApplicationPasswords
            }
            whenever(selectedSite.observe()).thenReturn(flowOf(appPasswordsSite))
            whenever(pushNotificationRegistrationStatus.observe(TEST_SITE_ID))
                .thenReturn(flowOf(PushNotificationRegistrationStatus.Status.UNREGISTERED))

            val result = sut().first()

            assertThat(result).isTrue()
        }

    companion object {
        private const val TEST_SITE_ID = 123L
    }
}
