package com.woocommerce.android.ui.prefs

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacySettingsViewModelTest : BaseUnitTest(StandardTestDispatcher()) {
    private val fakeSharedPreferencesEmitter = MutableStateFlow(false)
    private val crashReportingPreference = MutableStateFlow(false)

    private val appPrefs: AppPrefsWrapper = mock {
        on { observePrefs() } doReturn crashReportingPreference.map { }
        on { isCrashReportingEnabled() } doAnswer { crashReportingPreference.value }
    }
    private val repository: PrivacySettingsRepository = mock {
        on { isUserWPCOM() } doReturn true
    }

    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }

    val analyticsTrackerWrapper: AnalyticsTrackerWrapper = object : AnalyticsTrackerWrapper() {
        override fun observeSendUsageStats(): Flow<Boolean> = fakeSharedPreferencesEmitter

        override var sendUsageStats: Boolean = false
            set(value) {
                runBlocking {
                    fakeSharedPreferencesEmitter.emit(value)
                }
                field = value
            }
    }

    lateinit var sut: PrivacySettingsViewModel

    fun init() {
        sut = PrivacySettingsViewModel(
            mock(),
            appPrefs,
            analyticsTrackerWrapper,
            resourceProvider,
            repository,
        )
        sut.state.observeForever { }
    }

    private fun givenCrashReportingPreferenceIsWritable() {
        appPrefs.stub {
            on { setCrashReportingEnabled(any()) } doAnswer {
                crashReportingPreference.value = it.getArgument<Boolean>(0)
            }
        }
    }

    @Test
    fun `given successful API response, when user turns on analytical events, turn on analytical events and update state`(): Unit =
        testBlocking {
            // given
            analyticsTrackerWrapper.sendUsageStats = false
            repository.stub {
                on { updateTracksSetting(true) } doReturn Result.success(Unit)
            }
            init()

            // when
            sut.onSendStatsSettingChanged(true)
            runCurrent()

            // then
            assertThat(sut.state.value?.sendUsageStats).isTrue
            assertThat(analyticsTrackerWrapper.sendUsageStats).isTrue
        }

    @Test
    fun `given failed API response, when user turns on tracking analytical events, keep state unchanged and show snackbar`() =
        testBlocking {
            // given
            analyticsTrackerWrapper.sendUsageStats = false
            repository.stub {
                on { updateTracksSetting(true) } doReturn Result.failure(Exception())
            }
            init()

            // when
            sut.onSendStatsSettingChanged(true)
            runCurrent()

            // then
            assertThat(sut.state.value?.sendUsageStats).isFalse
            assertThat(sut.event.value).isInstanceOf(MultiLiveEvent.Event.ShowActionStringSnackbar::class.java)
        }

    @Test
    fun `given failed API response, when user opens the screen, keep state unchanged and show snackbar`() =
        testBlocking {
            // given
            analyticsTrackerWrapper.sendUsageStats = false
            repository.stub {
                on { updateAccountSettings() } doReturn Result.failure(Exception())
            }

            // when
            init()
            runCurrent()

            // then
            assertThat(sut.state.value?.sendUsageStats).isFalse
            assertThat(sut.event.value).isInstanceOf(MultiLiveEvent.Event.ShowActionStringSnackbar::class.java)
        }

    @Test
    fun `given user is not WPCOM, when user opens the screen, load settings from local preferences`() =
        testBlocking {
            // given
            repository.stub {
                on { isUserWPCOM() } doReturn false
            }
            analyticsTrackerWrapper.sendUsageStats = false

            // when
            init()
            runCurrent()

            // then
            assertThat(analyticsTrackerWrapper.sendUsageStats).isFalse
            verify(repository, never()).updateAccountSettings()
            assertThat(sut.state.value?.sendUsageStats).isFalse
        }

    @Test
    fun `given successful API response, when user turns on crash reporting, then local preference is updated and pushed to API`() =
        testBlocking {
            // given
            givenCrashReportingPreferenceIsWritable()
            repository.stub {
                on { updateAccountSettings() } doReturn Result.success(Unit)
                on { updateCrashReportingSetting(true) } doReturn Result.success(Unit)
            }
            init()
            advanceUntilIdle()

            // when
            sut.onCrashReportingSettingChanged(true)
            advanceUntilIdle()

            // then
            assertThat(sut.state.value?.crashReportingEnabled).isTrue
            verify(appPrefs).setCrashReportingEnabled(true)
            verify(repository).updateCrashReportingSetting(true)
        }

    @Test
    fun `given failed API response, when user turns on crash reporting, then local preference is reverted and snackbar is shown`() =
        testBlocking {
            // given
            givenCrashReportingPreferenceIsWritable()
            repository.stub {
                on { updateAccountSettings() } doReturn Result.success(Unit)
                on { updateCrashReportingSetting(true) } doReturn Result.failure(Exception())
            }
            init()
            advanceUntilIdle()

            // when
            sut.onCrashReportingSettingChanged(true)
            advanceUntilIdle()

            // then
            assertThat(sut.state.value?.crashReportingEnabled).isFalse
            verify(appPrefs).setCrashReportingEnabled(false)
            assertThat(sut.event.value).isInstanceOf(MultiLiveEvent.Event.ShowActionStringSnackbar::class.java)
        }

    @Test
    fun `given user is not WPCOM, when user changes crash reporting, then only local preference is updated`() =
        testBlocking {
            // given
            givenCrashReportingPreferenceIsWritable()
            repository.stub {
                on { isUserWPCOM() } doReturn false
            }
            init()
            advanceUntilIdle()

            // when
            sut.onCrashReportingSettingChanged(true)
            advanceUntilIdle()

            // then
            assertThat(sut.state.value?.crashReportingEnabled).isTrue
            verify(appPrefs).setCrashReportingEnabled(true)
            verify(repository, never()).updateCrashReportingSetting(any())
        }

    @Test
    fun `given crash reporting preference changes externally, when observed, then state reflects the new value`() =
        testBlocking {
            // given
            repository.stub {
                on { updateAccountSettings() } doReturn Result.success(Unit)
            }
            init()
            advanceUntilIdle()

            // when the preference is updated (e.g. by CrashReportingSettingSync after a fetch)
            crashReportingPreference.value = true
            advanceUntilIdle()

            // then
            assertThat(sut.state.value?.crashReportingEnabled).isTrue
        }

    @Test
    fun `given user toggled crash reporting during fetch, when fetch succeeds, then the choice is not overridden`() =
        testBlocking {
            // given
            givenCrashReportingPreferenceIsWritable()
            repository.stub {
                on { updateAccountSettings() } doReturn Result.success(Unit)
                on { updateCrashReportingSetting(true) } doReturn Result.success(Unit)
            }
            init()

            // when the user toggles while the fetch launched by init() is still in flight
            sut.onCrashReportingSettingChanged(true)
            advanceUntilIdle()

            // then
            assertThat(sut.state.value?.crashReportingEnabled).isTrue
        }

    @Test
    fun `given failed API response, when user tapps on retry button, retry updating account settings`() =
        testBlocking {
            // given
            analyticsTrackerWrapper.sendUsageStats = false
            repository.stub {
                on { updateTracksSetting(true) } doReturn Result.failure(Exception())
            }
            init()

            // when
            sut.onSendStatsSettingChanged(true)
            runCurrent()

            // then
            with((sut.event.value as MultiLiveEvent.Event.ShowActionStringSnackbar)) {
                action.onClick(null)
                runCurrent()
            }
            verify(repository, times(2)).updateTracksSetting(true)
        }
}
