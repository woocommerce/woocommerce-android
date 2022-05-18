package com.woocommerce.android.push

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.store.NotificationStore

@ExperimentalCoroutinesApi
class NotificationRepositoryTest : BaseUnitTest() {
    lateinit var sut: NotificationRepository

    private val notificationStore: NotificationStore = mock()

    @Before
    fun setUp() {
        sut = NotificationRepository(notificationStore)
    }

    @Test
    fun `should send a WooCommerce tagged token when requested`() = testBlocking {
        // when
        sut.registerDevice("123")

        // then
        verify(notificationStore).registerDevice("123", NotificationStore.NotificationAppKey.WOOCOMMERCE)
    }
}
