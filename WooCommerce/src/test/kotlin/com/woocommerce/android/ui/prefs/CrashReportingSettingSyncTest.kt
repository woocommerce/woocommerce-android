package com.woocommerce.android.ui.prefs

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class CrashReportingSettingSyncTest : BaseUnitTest() {
    private val appPrefs: AppPrefsWrapper = mock()
    private val repository: PrivacySettingsRepository = mock()

    private val sut = CrashReportingSettingSync(appPrefs, repository)

    @Test
    fun `given account has crash reporting opted out, when syncing, then local crash reporting is disabled`() =
        testBlocking {
            // given
            repository.stub {
                on { accountCrashReportingOptOut() } doReturn true
            }

            // when
            sut()

            // then
            verify(appPrefs).setCrashReportingEnabled(false)
            verify(repository, never()).updateCrashReportingSetting(any())
        }

    @Test
    fun `given account has crash reporting opted in, when syncing, then local crash reporting is enabled`() =
        testBlocking {
            // given
            repository.stub {
                on { accountCrashReportingOptOut() } doReturn false
            }

            // when
            sut()

            // then
            verify(appPrefs).setCrashReportingEnabled(true)
            verify(repository, never()).updateCrashReportingSetting(any())
        }

    @Test
    fun `given account is null and user made a local choice, when syncing, then local value is backfilled to the account`() =
        testBlocking {
            // given
            repository.stub {
                on { accountCrashReportingOptOut() } doReturn null
                on { updateCrashReportingSetting(false) } doReturn Result.success(Unit)
            }
            appPrefs.stub {
                on { hasCrashReportingChoice() } doReturn true
                on { isCrashReportingEnabled() } doReturn false
            }

            // when
            sut()

            // then
            verify(repository).updateCrashReportingSetting(false)
            verify(appPrefs, never()).setCrashReportingEnabled(any())
        }

    @Test
    fun `given account is null and user never made a choice, when syncing, then nothing is pushed`() =
        testBlocking {
            // given
            repository.stub {
                on { accountCrashReportingOptOut() } doReturn null
            }
            appPrefs.stub {
                on { hasCrashReportingChoice() } doReturn false
            }

            // when
            sut()

            // then
            verify(repository, never()).updateCrashReportingSetting(any())
            verify(appPrefs, never()).setCrashReportingEnabled(any())
        }

    @Test
    fun `given account is null and user made a choice, when backfill fails, then local value stays untouched`() =
        testBlocking {
            // given
            repository.stub {
                on { accountCrashReportingOptOut() } doReturn null
                on { updateCrashReportingSetting(true) } doReturn Result.failure(Exception())
            }
            appPrefs.stub {
                on { hasCrashReportingChoice() } doReturn true
                on { isCrashReportingEnabled() } doReturn true
            }

            // when
            sut()

            // then
            verify(appPrefs, never()).setCrashReportingEnabled(any())
        }
}
