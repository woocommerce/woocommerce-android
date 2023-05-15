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
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacySettingsViewModelTest : BaseUnitTest(StandardTestDispatcher()) {

    private val accountStore: AccountStore = mock {
        on { hasAccessToken() } doReturn true
    }

    private val appPrefs: AppPrefsWrapper = mock()
    private val repository: PrivacySettingsRepository = mock()

    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }

    lateinit var sut: PrivacySettingsViewModel

    fun init() {
        sut = PrivacySettingsViewModel(
            mock(),
            accountStore,
            appPrefs,
            resourceProvider,
            repository,
        )
    }

    @Test
    fun `given successful API response, when user turns on analytical events, turn on analytical events and update state`(): Unit =
        testBlocking {
            // given
            accountStore.stub {
                on { account } doReturn AccountModel().apply { tracksOptOut = true }
            }
            repository.stub {
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
            }
            accountStore.stub {
                on { account } doReturn AccountModel().apply { tracksOptOut = true }
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
            accountStore.stub {
                on { account } doReturn AccountModel().apply {
                    tracksOptOut = false
                } doReturn AccountModel().apply { tracksOptOut = true }
            }
            repository.stub {
                onBlocking { fetchAccountSettings() } doReturn Result.success(Unit)
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
            accountStore.stub {
                on { account } doReturn AccountModel().apply {
                    tracksOptOut = true
                }
            }
            repository.stub {
                onBlocking { fetchAccountSettings() } doReturn Result.failure(Exception())
            }

            // when
            init()
            runCurrent()

            // then
            assertThat(sut.state.value?.sendUsageStats).isFalse
            assertThat(sut.event.value).isInstanceOf(MultiLiveEvent.Event.ShowActionSnackbar::class.java)
        }
}
