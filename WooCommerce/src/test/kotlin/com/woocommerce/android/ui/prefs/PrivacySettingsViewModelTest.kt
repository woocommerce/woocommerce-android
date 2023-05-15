package com.woocommerce.android.ui.prefs

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacySettingsViewModelTest : BaseUnitTest(StandardTestDispatcher()) {

    private val appPrefs: AppPrefsWrapper = mock()
    private val repository: PrivacySettingsRepository = mock {
        on { isUserWPCOM() } doReturn true
    }

    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }

    lateinit var sut: PrivacySettingsViewModel

    fun init() {
        sut = PrivacySettingsViewModel(
            mock(),
            appPrefs,
            resourceProvider,
            repository,
        )
    }

    @Test
    fun `given successful API response, when user turns on analytical events, turn on analytical events and update state`(): Unit =
        testBlocking {
            // given
            repository.stub {
                on { userOptOutFromTracks() } doReturn true
                onBlocking { updateTracksSetting(true) } doReturn Result.success(Unit)
            }
            init()

            // when
            sut.onSendStatsSettingChanged(true)
            runCurrent()

            // then
            assertThat(sut.state.value?.sendUsageStats).isTrue
            verify(appPrefs).setSendUsageStats(true)
        }

    @Test
    fun `given failed API response, when user turns on tracking analytical events, keep state unchanged and show snackbar`() =
        testBlocking {
            // given
            repository.stub {
                onBlocking { updateTracksSetting(true) } doReturn Result.failure(Exception())
                on { userOptOutFromTracks() } doReturn true
            }
            init()

            // when
            sut.onSendStatsSettingChanged(true)
            runCurrent()

            // then
            assertThat(sut.state.value?.sendUsageStats).isFalse
            assertThat(sut.event.value).isInstanceOf(MultiLiveEvent.Event.ShowActionSnackbar::class.java)
        }

    @Test
    fun `given the API responses with disabled tracking and tracking locally enabled, when user opens the screen, turn off tracking and update state`(): Unit =
        testBlocking {
            // given
            repository.stub {
                onBlocking { fetchAccountSettings() } doReturn Result.success(Unit)
                on { userOptOutFromTracks() } doReturn false doReturn true
            }

            // when
            init()
            runCurrent()

            // then
            assertThat(sut.state.value?.sendUsageStats).isFalse
            verify(appPrefs).setSendUsageStats(false)
        }

    @Test
    fun `given failed API response, when user opens the screen, keep state unchanged and show snackbar`() =
        testBlocking {
            // given
            repository.stub {
                on { userOptOutFromTracks() } doReturn true
                onBlocking { fetchAccountSettings() } doReturn Result.failure(Exception())
            }

            // when
            init()
            runCurrent()

            // then
            assertThat(sut.state.value?.sendUsageStats).isFalse
            assertThat(sut.event.value).isInstanceOf(MultiLiveEvent.Event.ShowActionSnackbar::class.java)
        }

    @Test
    fun `given user is not WPCOM, when user opens the screen, load settings from local preferences`() =
        testBlocking {
            // given
            repository.stub {
                on { isUserWPCOM() } doReturn false
            }
            appPrefs.stub {
                on { getSendUsageStats() } doReturn false
            }

            // when
            init()
            runCurrent()

            // then
            verify(appPrefs).getSendUsageStats()
            verify(repository, never()).fetchAccountSettings()
            assertThat(sut.state.value?.sendUsageStats).isFalse()
        }
}
