package com.woocommerce.android.ui.woopos.common.util

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.local.LocalNotification
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.wc.settings.WCSettingsTestUtils

@ExperimentalCoroutinesApi
class WooPosSurveysNotificationSchedulerTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private lateinit var localNotificationScheduler: LocalNotificationScheduler
    private lateinit var appPrefs: AppPrefsWrapper
    private lateinit var wooPosPreferencesRepository: WooPosPreferencesRepository
    private lateinit var selectedSite: SelectedSite
    private lateinit var wooCommerceStore: WooCommerceStore
    private lateinit var scheduler: WooPosSurveysNotificationScheduler
    private lateinit var siteModel: SiteModel

    @Before
    fun setUp() {
        localNotificationScheduler = mock()
        appPrefs = mock()
        wooPosPreferencesRepository = mock()
        selectedSite = mock()
        wooCommerceStore = mock()
        siteModel = mock {
            on { siteId }.thenReturn(123L)
        }

        whenever(selectedSite.get()).thenReturn(siteModel)

        scheduler = WooPosSurveysNotificationScheduler(
            localNotificationScheduler = localNotificationScheduler,
            appPrefs = appPrefs,
            wooPosPreferencesRepository = wooPosPreferencesRepository,
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore
        )
    }

    @Test
    fun `given all conditions met, when schedulePotentialUserSurveyNotification called, then notification scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "US")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            scheduler.schedulePotentialUserSurveyNotification()

            verify(localNotificationScheduler).scheduleNotification(
                LocalNotification.WooPosSurveyPotentialUserNotification(siteId = 123L)
            )
        }

    @Test
    fun `given notification already shown, when schedulePotentialUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "US")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(true)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            scheduler.schedulePotentialUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }

    @Test
    fun `given country not allowed, when schedulePotentialUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "FR")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            scheduler.schedulePotentialUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }

    @Test
    fun `given WooPOS opened before, when schedulePotentialUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "US")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(true))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            scheduler.schedulePotentialUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }

    @Test
    fun `given GB country code, when schedulePotentialUserSurveyNotification called, then notification scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "GB")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            scheduler.schedulePotentialUserSurveyNotification()

            verify(localNotificationScheduler).scheduleNotification(
                LocalNotification.WooPosSurveyPotentialUserNotification(siteId = 123L)
            )
        }

    @Test
    fun `given lowercase country code, when schedulePotentialUserSurveyNotification called, then notification scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "us")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            scheduler.schedulePotentialUserSurveyNotification()

            verify(localNotificationScheduler).scheduleNotification(
                LocalNotification.WooPosSurveyPotentialUserNotification(siteId = 123L)
            )
        }

    @Test
    fun `given null country code, when schedulePotentialUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            scheduler.schedulePotentialUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }

    @Test
    fun `given null site settings, when schedulePotentialUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(null)

            scheduler.schedulePotentialUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }

    @Test
    fun `given all conditions met, when scheduleCurrentUserSurveyNotification called, then notification scheduled with delay`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "US")
            whenever(appPrefs.isWooPosSurveyNotificationCurrentUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(true))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            scheduler.scheduleCurrentUserSurveyNotification()

            verify(localNotificationScheduler).scheduleNotification(
                LocalNotification.WooPosSurveyCurrentUserNotification(
                    delay = 300000L,
                    siteId = 123L
                )
            )
        }

    @Test
    fun `given notification already shown, when scheduleCurrentUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "US")
            whenever(appPrefs.isWooPosSurveyNotificationCurrentUserShown).thenReturn(true)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(true))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            scheduler.scheduleCurrentUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }

    @Test
    fun `given WooPOS not opened before, when scheduleCurrentUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "US")
            whenever(appPrefs.isWooPosSurveyNotificationCurrentUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            scheduler.scheduleCurrentUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }

    @Test
    fun `given country not allowed, when scheduleCurrentUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "FR")
            whenever(appPrefs.isWooPosSurveyNotificationCurrentUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(true))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            scheduler.scheduleCurrentUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }
}
