package com.woocommerce.android.tracker

import app.cash.turbine.test
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tracker.SendTelemetry.Result.NOT_SENT
import com.woocommerce.android.tracker.SendTelemetry.Result.SENT
import com.woocommerce.android.util.advanceTimeAndRun
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
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
class SendTelemetryTest : BaseUnitTest() {
    private lateinit var sut: SendTelemetry

    private val trackerStore = mock<TrackerStore>()
    private val trackerRepository = FakeTrackerRepository()
    private val currentTimeProvider = mock<CurrentTimeProvider> {
        on { currentDate() } doAnswer { Date(coroutinesTestRule.testDispatcher.scheduler.currentTime) }
    }
    private val selectedSite = mock<SelectedSite> {
        on { observe() } doReturn flowOf(site)
    }

    @Before
    fun setUp() {
        sut = SendTelemetry(
            trackerStore = trackerStore,
            trackerRepository = trackerRepository,
            currentTimeProvider = currentTimeProvider,
            selectedSite = selectedSite,
        )
    }

    @Test
    fun `should send telemetry on initial run`(): Unit = testBlocking {
        // when
        sut.invoke("321")
            // then
            .test {
                assertThat(awaitItem()).isEqualTo(SENT)
                verify(trackerStore).sendTelemetry("321", site)
            }
    }

    @Test
    fun `should not send telemetry if its before 1-day interval`(): Unit = testBlocking {
        // given
        val randomPointInTimeBeforeInterval = 222L
        trackerRepository.inMemoryState.value += (site to randomPointInTimeBeforeInterval)

        // when
        sut.invoke("321")
            // then
            .test {
                assertThat(awaitItem()).isEqualTo(NOT_SENT)
                verify(trackerStore, never()).sendTelemetry(any(), any())
            }
    }

    @Test
    fun `should send telemetry if site changed`(): Unit = runBlocking {
        // given
        val fakeSite = MutableSharedFlow<SiteModel?>()
        selectedSite.stub {
            on { observe() } doReturn fakeSite
        }

        // when
        sut.invoke("123")
            // then
            .test {
                fakeSite.emit(null)
                assertThat(awaitItem()).isEqualTo(NOT_SENT)

                fakeSite.emit(site)
                assertThat(awaitItem()).isEqualTo(SENT)
                verify(trackerStore).sendTelemetry("123", site)

                fakeSite.emit(siteB)
                assertThat(awaitItem()).isEqualTo(SENT)
                verify(trackerStore).sendTelemetry("123", siteB)
            }
    }

    @Test
    fun `should send after 1-day for the same site`() = testBlocking {
        // given some moment in time
        advanceTimeBy(1637795672903)
        val results = mutableListOf<SendTelemetry.Result>()

        // when
        val job = launch {
            sut.invoke("123").collect {
                results.add(it)
            }
        }
        advanceTimeAndRun(SendTelemetry.UPDATE_INTERVAL.toLong() * 3)

        // then
        assertThat(results).containsExactly(SENT, NOT_SENT, SENT, NOT_SENT, SENT, NOT_SENT, SENT)

        job.cancel()
    }

    @Test
    fun `should not send before 1-day for the same site`() = testBlocking {
        // given some moment in time
        advanceTimeBy(1637795672903)

        val results = mutableListOf<SendTelemetry.Result>()

        // when
        val initialRun = launch {
            sut.invoke("123").collect {
                results.add(it)
            }
        }
        initialRun.cancel()

        advanceTimeBy(SendTelemetry.UPDATE_INTERVAL.toLong() - 1000)
        val runAfterLessThanRequiredInterval = launch {
            sut.invoke("123").collect {
                results.add(it)
            }
        }
        runAfterLessThanRequiredInterval.cancel()

        advanceTimeBy(1000)
        val runOnExactlyInterval = launch {
            sut.invoke("123").collect {
                results.add(it)
            }
        }
        runOnExactlyInterval.cancel()

        // then
        assertThat(results).containsExactly(SENT, NOT_SENT, SENT)
    }

    companion object {
        val site = SiteModel().apply { id = 123 }
        val siteB = SiteModel().apply { id = 321 }
    }
}
