package com.woocommerce.android.ui.prefs.privacy.banner

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.prefs.PrivacySettingsRepository
import com.woocommerce.android.ui.prefs.RequestedAnalyticsValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyBannerViewModelTest : BaseUnitTest(StandardTestDispatcher()) {

    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val repository: PrivacySettingsRepository = mock()
    private val appPrefs: AppPrefsWrapper = mock()

    lateinit var sut: PrivacyBannerViewModel

    @Before
    fun setUp() {
        sut = PrivacyBannerViewModel(
            mock(),
            analyticsTracker,
            appPrefs,
            repository,
        )
    }

    @Test
    fun `when banner is launched, then it shows the current analytics preference`() = testBlocking {
        // given
        analyticsTracker.stub {
            on { sendUsageStats } doReturn true
        }

        // when
        setUp()

        // then
        assertThat(sut.analyticsState.value?.analyticsSwitchEnabled).isEqualTo(true)
    }

    @Test
    fun `when switch is changed, then the state is updated`() = testBlocking {
        // when
        sut.onSwitchChanged(true)

        // then
        assertThat(sut.analyticsState.value?.analyticsSwitchEnabled).isEqualTo(true)

        // when
        sut.onSwitchChanged(false)

        // then
        assertThat(sut.analyticsState.value?.analyticsSwitchEnabled).isEqualTo(false)
    }

    @Test
    fun `given WPCOM user and successful remote update, when save button is pressed, then update the setting locally and mark banner as shown`() =
        testBlocking {
            // given
            repository.stub {
                on { isUserWPCOM() } doReturn true
            }
            sut.analyticsState.observeForever { }

            // when
            sut.onSwitchChanged(true)
            sut.onSavePressed()
            runCurrent()

            // then
            verify(repository).updateTracksSetting(true)
            verify(analyticsTracker).sendUsageStats = true
            verify(appPrefs).savedPrivacyBannerSettings = true
            assertThat(sut.event.value).isEqualTo(PrivacyBannerViewModel.Dismiss)
        }

    @Test
    fun `given WPCOM user and failed remote update, when save button is pressed, then do not update the setting locally and do not mark banner as shown`() =
        testBlocking {
            // given
            repository.stub {
                on { isUserWPCOM() } doReturn true
                onBlocking { updateTracksSetting(true) } doReturn Result.failure(Exception())
            }
            sut.analyticsState.observeForever { }

            // when
            sut.onSwitchChanged(true)
            sut.onSavePressed()
            runCurrent()

            // then
            verify(repository).updateTracksSetting(true)
            verify(analyticsTracker, never()).sendUsageStats = true
            verify(appPrefs, never()).savedPrivacyBannerSettings = true
            assertThat(sut.event.value).isEqualTo(PrivacyBannerViewModel.ShowError(true))
        }

    @Test
    fun `given non-WPCOM user, when save button is pressed, then update the setting locally and mark banner as shown`() =
        testBlocking {
            // given
            repository.stub {
                onBlocking { isUserWPCOM() } doReturn false
            }
            analyticsTracker.stub {
                onBlocking { sendUsageStats } doReturn true
            }
            sut.analyticsState.observeForever { }

            // when
            setUp()
            sut.onSwitchChanged(false)
            sut.onSavePressed()
            runCurrent()

            // then
            verify(analyticsTracker).sendUsageStats = false
            verify(appPrefs).savedPrivacyBannerSettings = true
            assertThat(sut.event.value).isEqualTo(PrivacyBannerViewModel.Dismiss)
        }

    @Test
    fun `given WPCOM user and successful remote update, when settings button is pressed, then update the setting locally, mark banner as shown and open settings`() =
        testBlocking {
            // given
            repository.stub {
                on { isUserWPCOM() } doReturn true
            }
            sut.analyticsState.observeForever { }

            // when
            sut.onSwitchChanged(true)
            sut.onSettingsPressed()
            runCurrent()

            // then
            verify(repository).updateTracksSetting(true)
            verify(analyticsTracker).sendUsageStats = true
            verify(appPrefs).savedPrivacyBannerSettings = true
            assertThat(sut.event.value).isEqualTo(PrivacyBannerViewModel.ShowSettings)
        }

    @Test
    fun `given WPCOM user and failed remote update, when settings button is pressed, only open settings and show error`() =
        testBlocking {
            // given
            repository.stub {
                on { isUserWPCOM() } doReturn true
                onBlocking { updateTracksSetting(true) } doReturn Result.failure(Exception())
            }
            sut.analyticsState.observeForever { }

            // when
            sut.onSwitchChanged(true)
            sut.onSettingsPressed()
            runCurrent()

            // then
            verify(repository).updateTracksSetting(true)
            verify(analyticsTracker, never()).sendUsageStats = true
            verify(appPrefs, never()).savedPrivacyBannerSettings = true
            assertThat(sut.event.value).isEqualTo(
                PrivacyBannerViewModel.ShowErrorOnSettings(
                    RequestedAnalyticsValue.ENABLED
                )
            )
        }

    @Test
    fun `given non-WPCOM user, when setting button is pressed, then update the setting locally and mark banner as shown`() =
        testBlocking {
            // given
            repository.stub {
                onBlocking { isUserWPCOM() } doReturn false
            }
            analyticsTracker.stub {
                onBlocking { sendUsageStats } doReturn true
            }
            sut.analyticsState.observeForever { }

            // when
            setUp()
            sut.onSwitchChanged(false)
            sut.onSettingsPressed()
            runCurrent()

            // then
            verify(analyticsTracker).sendUsageStats = false
            verify(appPrefs).savedPrivacyBannerSettings = true
            assertThat(sut.event.value).isEqualTo(PrivacyBannerViewModel.ShowSettings)
        }

    @Test
    fun `given the actual analytics setting is true, when setting button is tapped, then do not update setting on remote`() =
        testBlocking {
            // given
            analyticsTracker.stub {
                onBlocking { sendUsageStats } doReturn true
            }
            sut.analyticsState.observeForever { }

            // when
            setUp()
            sut.onSwitchChanged(true)
            sut.onSettingsPressed()
            runCurrent()

            // then
            verify(repository, never()).updateTracksSetting(true)
            verify(analyticsTracker, never()).sendUsageStats = true
            verify(appPrefs).savedPrivacyBannerSettings = true
            assertThat(sut.event.value).isEqualTo(PrivacyBannerViewModel.ShowSettings)
        }
}
