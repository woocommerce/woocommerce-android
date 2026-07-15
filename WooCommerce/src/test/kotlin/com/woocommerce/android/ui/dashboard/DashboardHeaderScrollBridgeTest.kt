package com.woocommerce.android.ui.dashboard

import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardHeaderScrollBridgeTest {
    private val firstIdentity = Any()
    private val replacementIdentity = Any()
    private val firstConnection = object : NestedScrollConnection {}
    private val replacementConnection = object : NestedScrollConnection {}

    @Test
    fun `when a behavior is attached, then the body can observe its connection`() {
        // GIVEN
        val bridge = DashboardHeaderScrollBridge()

        // WHEN
        bridge.attach(firstIdentity, firstConnection, expand = {})

        // THEN
        assertThat(bridge.nestedScrollConnection).isSameAs(firstConnection)
    }

    @Test
    fun `given an attached behavior, when it is detached, then the bridge is cleared`() {
        // GIVEN
        val bridge = givenBridgeWithFirstBehavior()

        // WHEN
        bridge.detach(firstIdentity)

        // THEN
        assertThat(bridge.nestedScrollConnection).isNull()
    }

    @Test
    fun `given a replacement behavior, when the previous behavior detaches, then the replacement remains`() {
        // GIVEN
        val bridge = givenBridgeWithFirstBehavior()
        bridge.attach(replacementIdentity, replacementConnection, expand = {})

        // WHEN
        bridge.detach(firstIdentity)

        // THEN
        assertThat(bridge.nestedScrollConnection).isSameAs(replacementConnection)
    }

    @Test
    fun `given an attached behavior, when scrolling to top, then body finishes before header expands`() = runTest {
        // GIVEN
        val operations = mutableListOf<String>()
        val finishBodyScroll = CompletableDeferred<Unit>()
        val bridge = DashboardHeaderScrollBridge().apply {
            attach(firstIdentity, firstConnection) { operations += HEADER_EXPANSION }
        }

        // WHEN
        val request = launch {
            bridge.scrollToTop {
                operations += BODY_SCROLL_STARTED
                finishBodyScroll.await()
                operations += BODY_SCROLL_FINISHED
            }
        }
        runCurrent()
        assertThat(operations).containsExactly(BODY_SCROLL_STARTED)
        finishBodyScroll.complete(Unit)
        request.join()

        // THEN
        assertThat(operations).containsExactly(BODY_SCROLL_STARTED, BODY_SCROLL_FINISHED, HEADER_EXPANSION)
    }

    @Test
    fun `given no attached behavior, when scrolling to top, then body still scrolls`() = runTest {
        // GIVEN
        var bodyScrolled = false

        // WHEN
        DashboardHeaderScrollBridge().scrollToTop { bodyScrolled = true }

        // THEN
        assertThat(bodyScrolled).isTrue()
    }

    private fun givenBridgeWithFirstBehavior() = DashboardHeaderScrollBridge().apply {
        attach(firstIdentity, firstConnection, expand = {})
    }

    private companion object {
        const val BODY_SCROLL_STARTED = "body started"
        const val BODY_SCROLL_FINISHED = "body finished"
        const val HEADER_EXPANSION = "header expanded"
    }
}
