package org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreOrderPreferences
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.PushNotificationsRestClient.PushTokenIdResponse
import org.wordpress.android.fluxc.persistence.dao.WooPushNotificationPreferencesDao
import org.wordpress.android.fluxc.persistence.entity.toEntity
import org.wordpress.android.fluxc.utils.initCoroutineEngine
import java.math.BigDecimal

class PushNotificationsStoreTest {
    private val restClient: PushNotificationsRestClient = mock()
    private val preferencesDao: WooPushNotificationPreferencesDao = mock()

    private lateinit var sut: WooPushNotificationsStore

    @Before
    fun setUp() {
        sut = WooPushNotificationsStore(
            pushNotificationsRestClient = restClient,
            coroutineEngine = initCoroutineEngine(),
            preferencesDao = preferencesDao
        )
    }

    @Test
    fun `when registerPushToken succeeds, then returns token id`() {
        runBlocking {
            val site = SiteModel().apply { id = 123 }
            val tokenId = "101"
            whenever(restClient.registerPushToken(any(), any()))
                .thenReturn(WooPayload(PushTokenIdResponse(tokenId)))

            val result = sut.registerPushToken(site, "token", "uuid", "en_US")

            assertThat(result.isError).isFalse()
            assertThat(result.model).isEqualTo(tokenId)
        }
    }

    @Test
    fun `when deletePushToken succeeds, then returns success`() {
        runBlocking {
            val site = SiteModel().apply { id = 123 }
            val pushTokenId = "100"
            whenever(restClient.deletePushToken(any(), any())).thenReturn(WooPayload(Unit))

            val result = sut.deletePushToken(site, pushTokenId)

            assertThat(result.isError).isFalse()
            verify(restClient).deletePushToken(site, pushTokenId)
        }
    }

    @Test
    fun `given cached notification preferences, when observeNotificationPreferences, then emit cached preferences`() {
        runBlocking {
            val site = SiteModel().apply { id = 123 }
            val preferences = WooPushNotificationPreferences(
                storeOrder = StoreOrderPreferences(enabled = true, minAmount = BigDecimal(100))
            )
            whenever(preferencesDao.observePreferences(site.localId()))
                .thenReturn(flowOf(preferences.toEntity(site.localId())))

            val result = sut.observeNotificationPreferences(site).first()

            assertThat(result).isEqualTo(preferences)
            verify(restClient, never()).fetchNotificationPreferences(site)
        }
    }

    @Test
    fun `given cached notification preferences are unchanged, when observing, then emit once`() {
        runBlocking {
            val site = SiteModel().apply { id = 123 }
            val preferences = WooPushNotificationPreferences(
                storeOrder = StoreOrderPreferences(enabled = true, minAmount = BigDecimal(100))
            )
            val entity = preferences.toEntity(site.localId())
            whenever(preferencesDao.observePreferences(site.localId())).thenReturn(flowOf(entity, entity))

            val result = sut.observeNotificationPreferences(site).toList()

            assertThat(result).containsExactly(preferences)
        }
    }

    @Test
    fun `when fetchNotificationPreferences succeeds, then cache refreshed preferences`() {
        runBlocking {
            val site = SiteModel().apply { id = 123 }
            val refreshedPreferences = WooPushNotificationPreferences(
                storeOrder = StoreOrderPreferences(enabled = false)
            )
            whenever(restClient.fetchNotificationPreferences(site)).thenReturn(WooPayload(refreshedPreferences))

            val refreshedResult = sut.fetchNotificationPreferences(site)

            assertThat(refreshedResult.model).isEqualTo(refreshedPreferences)
            verify(preferencesDao, never()).observePreferences(site.localId())
            verify(restClient).fetchNotificationPreferences(site)
            verify(preferencesDao).upsertPreferences(refreshedPreferences.toEntity(site.localId()))
        }
    }

    @Test
    fun `when notification preferences are updated, then cache updated preferences`() {
        runBlocking {
            val site = SiteModel().apply { id = 123 }
            val preferences = WooPushNotificationPreferences(
                storeOrder = StoreOrderPreferences(enabled = false)
            )
            whenever(restClient.updateNotificationPreferences(site, preferences)).thenReturn(WooPayload(preferences))

            val result = sut.updateNotificationPreferences(site, preferences)

            assertThat(result.model).isEqualTo(preferences)
            verify(preferencesDao).upsertPreferences(preferences.toEntity(site.localId()))
        }
    }
}
