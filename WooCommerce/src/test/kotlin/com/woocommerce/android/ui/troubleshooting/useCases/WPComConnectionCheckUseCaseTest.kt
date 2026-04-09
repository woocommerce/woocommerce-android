package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
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
class WPComConnectionCheckUseCaseTest : BaseUnitTest() {
    private lateinit var sut: WPComConnectionCheckUseCase
    private lateinit var whatsNewStore: WhatsNewStore
    private lateinit var buildConfigWrapper: BuildConfigWrapper

    @Before
    fun setUp() {
        whatsNewStore = mock()
        buildConfigWrapper = mock()
        sut = WPComConnectionCheckUseCase(whatsNewStore, buildConfigWrapper)
    }

    @Test
    fun `when fetchRemoteAnnouncements returns an error, then emit Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WhatsNewStore.OnWhatsNewFetched(
            fetchError = WhatsNewFetchError(WhatsNewErrorType.GENERIC_ERROR)
        )
        whenever(buildConfigWrapper.versionName).thenReturn("1.0.0")
        whenever(
            whatsNewStore.fetchRemoteAnnouncements(
                versionName = "1.0.0"
            )
        ).thenReturn(response)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
    }

    @Test
    fun `when fetchRemoteAnnouncements returns no error, then emit Success`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WhatsNewStore.OnWhatsNewFetched()
        whenever(buildConfigWrapper.versionName).thenReturn("1.0.0")
        whenever(
            whatsNewStore.fetchRemoteAnnouncements(
                versionName = "1.0.0"
            )
        ).thenReturn(response)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Success::class.java)
    }
}
