package com.woocommerce.android.tracker

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.tracker.TrackerStore
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date

@ExperimentalCoroutinesApi
class SendTelemetryTest {

    private lateinit var sut: SendTelemetry
    private val trackerStore = mock<TrackerStore>()
    private val trackerRepository = mock<TrackerRepository>()
    private val currentTimeProvider = mock<CurrentTimeProvider>()

    @Before
    fun setUp() {
        sut = SendTelemetry(
            trackerStore = trackerStore,
            trackerRepository = trackerRepository,
            currentTimeProvider = currentTimeProvider,
        )
    }

    @Test
    fun `should send telemetry if its after 1-day interval`(): Unit = runBlocking {
        // given
        currentTimeProvider.stub {
            on { currentDate() } doReturn Date(100L + SendTelemetry.UPDATE_INTERVAL)
        }
        trackerRepository.stub {
            on { observeLastSendingDate(site) } doReturn flowOf(10L)
        }

        // when
        sut.invoke("321", site)

        // then
        verify(trackerStore).sendTelemetry("321", site)
    }

    @Test
    fun `should not send telemetry if its before 1-day interval`(): Unit = runBlocking {
        // given
        currentTimeProvider.stub {
            on { currentDate() } doReturn Date(100L)
        }
        trackerRepository.stub {
            on { observeLastSendingDate(site) } doReturn flowOf(10L)
        }

        // when
        sut.invoke("321", site)

        // then
        verify(trackerStore, never()).sendTelemetry(any(), any())
    }

    companion object {
        val site = SiteModel().apply { id = 123 }
    }
}
