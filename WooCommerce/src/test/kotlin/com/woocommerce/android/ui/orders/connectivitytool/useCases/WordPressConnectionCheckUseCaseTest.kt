package com.woocommerce.android.ui.orders.connectivitytool.useCases

import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.Success
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WhatsNewStore
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewErrorType
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewFetchError

@OptIn(ExperimentalCoroutinesApi::class)
class WordPressConnectionCheckUseCaseTest : BaseUnitTest() {
    private lateinit var sut: WordPressConnectionCheckUseCase
    private lateinit var whatsNewStore: WhatsNewStore
    private lateinit var buildConfigWrapper: BuildConfigWrapper

    @Before
    fun setUp() {
        whatsNewStore = mock()
        buildConfigWrapper = mock()
        sut = WordPressConnectionCheckUseCase(whatsNewStore, buildConfigWrapper)
    }

    @Test
    fun `when fetchRemoteAnnouncements returns an error then emit Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityTestStatus>()
        val response = WhatsNewStore.OnWhatsNewFetched(
            fetchError = WhatsNewFetchError(WhatsNewErrorType.GENERIC_ERROR)
        )
        whenever(buildConfigWrapper.versionName).thenReturn("1.0.0")
        whenever(
            whatsNewStore.fetchRemoteAnnouncements(
                versionName = "1.0.0",
                appId = WhatsNewStore.WhatsNewAppId.WOO_ANDROID
            )
        ).thenReturn(response)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Failure))
    }

    @Test
    fun `when fetchRemoteAnnouncements returns no error then emit Success`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityTestStatus>()
        val response = WhatsNewStore.OnWhatsNewFetched()
        whenever(buildConfigWrapper.versionName).thenReturn("1.0.0")
        whenever(
            whatsNewStore.fetchRemoteAnnouncements(
                versionName = "1.0.0",
                appId = WhatsNewStore.WhatsNewAppId.WOO_ANDROID
            )
        ).thenReturn(response)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Success))
    }
}
