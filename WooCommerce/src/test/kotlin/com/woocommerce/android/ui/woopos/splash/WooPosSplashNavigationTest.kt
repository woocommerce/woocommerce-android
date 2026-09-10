package com.woocommerce.android.ui.woopos.splash

import android.os.Trace
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.NonLaunchabilityReason
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mockStatic

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSplashNavigationTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule(StandardTestDispatcher())

    @Test
    fun `given terminal result, when recomposed with a new callback, then navigates once`() = runTest {
        // GIVEN
        val reason = NonLaunchabilityReason.UnsupportedWooCommerceVersion
        val results = listOf(
            WooPosSplashState.Loaded to WooPosNavigationEvent.OpenHomeFromSplash,
            WooPosSplashState.NotEligible(reason) to WooPosNavigationEvent.OpenEligibilityScreenFromSplash(reason)
        )
        results.forEach { (state, expectedEvent) ->
            CompositionFixture(this).use { fixture ->
                val events = mutableListOf<WooPosNavigationEvent>()
                val callback = mutableStateOf<(WooPosNavigationEvent) -> Unit>({ events.add(it) })
                var compositions = 0
                fixture.composition.setContent {
                    WooPosSplashNavigation(state, callback.value)
                    SideEffect { compositions++ }
                }
                fixture.advance()
                val initialCompositions = compositions

                // WHEN
                callback.value = { events.add(it) }
                fixture.advance()

                // THEN
                assertThat(events).containsExactly(expectedEvent)
                assertThat(compositions).isGreaterThan(initialCompositions)
            }
        }
    }

    @Test
    fun `given ineligible result, when terminal result changes, then navigates to each destination`() = runTest {
        // GIVEN
        val initialReason = NonLaunchabilityReason.UnsupportedWooCommerceVersion
        val nextReason = NonLaunchabilityReason.UnsupportedCountry
        val state = mutableStateOf<WooPosSplashState>(WooPosSplashState.NotEligible(initialReason))
        val events = mutableListOf<WooPosNavigationEvent>()
        CompositionFixture(this).use { fixture ->
            fixture.composition.setContent {
                WooPosSplashNavigation(state.value) { events.add(it) }
            }
            fixture.advance()

            // WHEN
            state.value = WooPosSplashState.NotEligible(nextReason)
            fixture.advance()
            state.value = WooPosSplashState.Loaded
            fixture.advance()

            // THEN
            assertThat(events).containsExactly(
                WooPosNavigationEvent.OpenEligibilityScreenFromSplash(initialReason),
                WooPosNavigationEvent.OpenEligibilityScreenFromSplash(nextReason),
                WooPosNavigationEvent.OpenHomeFromSplash
            )
        }
    }

    private class CompositionFixture(private val scope: TestScope) : AutoCloseable {
        private val trace = mockStatic(Trace::class.java)
        private val frameClock = BroadcastFrameClock()
        private val recomposer = Recomposer(scope.coroutineContext + frameClock)
        val composition = Composition(
            object : AbstractApplier<Unit>(Unit) {
                override fun insertTopDown(index: Int, instance: Unit) = Unit
                override fun insertBottomUp(index: Int, instance: Unit) = Unit
                override fun remove(index: Int, count: Int) = Unit
                override fun move(from: Int, to: Int, count: Int) = Unit
                override fun onClear() = Unit
            },
            recomposer
        )

        init {
            scope.backgroundScope.launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }
        }

        fun advance() {
            Snapshot.sendApplyNotifications()
            scope.runCurrent()
            frameClock.sendFrame(scope.testScheduler.currentTime)
            scope.runCurrent()
        }

        override fun close() {
            composition.dispose()
            recomposer.cancel()
            trace.close()
        }
    }
}
