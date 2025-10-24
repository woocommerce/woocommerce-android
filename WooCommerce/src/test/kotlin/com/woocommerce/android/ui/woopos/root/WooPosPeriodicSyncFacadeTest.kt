package com.woocommerce.android.ui.woopos.root

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIncrementalSyncReason
import com.woocommerce.android.ui.woopos.localcatalog.WooPosPerformLocalCatalogIncrementalSync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.time.Duration.Companion.hours

@ExperimentalCoroutinesApi
class WooPosPeriodicSyncFacadeTest {

    private val incrementalSync: WooPosPerformLocalCatalogIncrementalSync = mock()
    private lateinit var facade: WooPosPeriodicSyncFacade
    private lateinit var lifecycleOwner: TestLifecycleOwner

    @Before
    fun setUp() {
        facade = WooPosPeriodicSyncFacade(incrementalSync)
    }

    @Test
    fun `given Activity resumed, when one hour passes, then periodic incremental sync is triggered`() = runTest {
        // GIVEN
        lifecycleOwner = TestLifecycleOwner(this)
        lifecycleOwner.lifecycle.addObserver(facade)
        lifecycleOwner.resume()

        // WHEN
        advanceTimeBy(1.hours)

        // THEN
        verify(incrementalSync).execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)
    }

    @Test
    fun `given Activity resumed, when multiple hours pass, then periodic sync runs multiple times`() = runTest {
        // GIVEN
        lifecycleOwner = TestLifecycleOwner(this)
        lifecycleOwner.lifecycle.addObserver(facade)
        lifecycleOwner.resume()

        // WHEN
        advanceTimeBy(3.hours)

        // THEN
        verify(incrementalSync, org.mockito.kotlin.times(3)).execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)
    }

    @Test
    fun `given Activity resumed then paused, when time passes, then periodic sync stops`() = runTest {
        // GIVEN
        lifecycleOwner = TestLifecycleOwner(this)
        lifecycleOwner.lifecycle.addObserver(facade)
        lifecycleOwner.resume()
        advanceTimeBy(1.hours)
        verify(incrementalSync).execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)

        // WHEN
        lifecycleOwner.pause()
        advanceTimeBy(2.hours)

        // THEN - no additional syncs after pause
        verify(incrementalSync, org.mockito.kotlin.times(1)).execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)
    }

    @Test
    fun `given Activity paused, when resumed again, then periodic sync restarts`() = runTest {
        // GIVEN
        lifecycleOwner = TestLifecycleOwner(this)
        lifecycleOwner.lifecycle.addObserver(facade)
        lifecycleOwner.resume()
        lifecycleOwner.pause()

        // WHEN
        lifecycleOwner.resume()
        advanceTimeBy(1.hours)

        // THEN
        verify(incrementalSync).execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)
    }

    @Test
    fun `given Activity not resumed, when time passes, then no sync occurs`() = runTest {
        // GIVEN
        lifecycleOwner = TestLifecycleOwner(this)
        lifecycleOwner.lifecycle.addObserver(facade)
        // Don't resume

        // WHEN
        advanceTimeBy(2.hours)

        // THEN
        verify(incrementalSync, never()).execute(WooPosIncrementalSyncReason.PERIODIC_HOURLY)
    }

    private class TestLifecycleOwner(@Suppress("unused") private val testScope: TestScope) : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        fun resume() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun pause() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
    }
}
