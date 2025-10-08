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
class WooPosSurveysNotificationSchedularTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private lateinit var localNotificationScheduler: LocalNotificationScheduler
    private lateinit var appPrefs: AppPrefsWrapper
    private lateinit var wooPosPreferencesRepository: WooPosPreferencesRepository
    private lateinit var selectedSite: SelectedSite
    private lateinit var wooCommerceStore: WooCommerceStore
    private lateinit var schedular: WooPosSurveysNotificationSchedular
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

        schedular = WooPosSurveysNotificationSchedular(
            localNotificationScheduler = localNotificationScheduler,
            appPrefs = appPrefs,
            wooPosPreferencesRepository = wooPosPreferencesRepository,
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore
        )
    }

    @Test
    fun `given all conditions met, when schedularPotentialUserSurveyNotification called, then notification scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "US")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            schedular.schedularPotentialUserSurveyNotification()

            verify(localNotificationScheduler).scheduleNotification(
                LocalNotification.WooPosSurveyPotentialUserNotification(siteId = 123L)
            )
        }

    @Test
    fun `given notification already shown, when schedularPotentialUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "US")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(true)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            schedular.schedularPotentialUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }

    @Test
    fun `given country not allowed, when schedularPotentialUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "FR")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            schedular.schedularPotentialUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }

    @Test
    fun `given WooPOS opened before, when schedularPotentialUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "US")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(true))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            schedular.schedularPotentialUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }

    @Test
    fun `given GB country code, when schedularPotentialUserSurveyNotification called, then notification scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "GB")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            schedular.schedularPotentialUserSurveyNotification()

            verify(localNotificationScheduler).scheduleNotification(
                LocalNotification.WooPosSurveyPotentialUserNotification(siteId = 123L)
            )
        }

    @Test
    fun `given lowercase country code, when schedularPotentialUserSurveyNotification called, then notification scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "us")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            schedular.schedularPotentialUserSurveyNotification()

            verify(localNotificationScheduler).scheduleNotification(
                LocalNotification.WooPosSurveyPotentialUserNotification(siteId = 123L)
            )
        }

    @Test
    fun `given null country code, when schedularPotentialUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            val siteSettings = WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = "")
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(siteSettings)

            schedular.schedularPotentialUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }

    @Test
    fun `given null site settings, when schedularPotentialUserSurveyNotification called, then notification not scheduled`() =
        runTest {
            whenever(appPrefs.isWooPosSurveyNotificationPotentialUserShown).thenReturn(false)
            whenever(wooPosPreferencesRepository.wasOpenedOnce).thenReturn(flowOf(false))
            whenever(wooCommerceStore.getSiteSettingsAsync(siteModel)).thenReturn(null)

            schedular.schedularPotentialUserSurveyNotification()

            verify(localNotificationScheduler, never()).scheduleNotification(any())
        }
}
