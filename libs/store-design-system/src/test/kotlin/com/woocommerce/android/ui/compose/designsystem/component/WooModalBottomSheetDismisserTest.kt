package com.woocommerce.android.ui.compose.designsystem.component

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooModalBottomSheetDismisserTest {
    @Test
    fun `when dismissal hides sheet, then completion callback is invoked`() = runTest {
        // GIVEN
        var isVisible = true
        var callbackCount = 0
        val dismisser = WooModalBottomSheetDismisser(
            coroutineScope = this,
            hide = { isVisible = false },
            isVisible = { isVisible },
            onDismissed = { callbackCount++ },
        )

        // WHEN
        dismisser.dismiss()

        // THEN
        assertThat(dismisser.isDismissing).isTrue()
        assertThat(callbackCount).isZero()
        runCurrent()
        assertThat(dismisser.isDismissing).isFalse()
        assertThat(callbackCount).isEqualTo(1)
    }

    @Test
    fun `given sheet remains visible, when dismissal completes, then callback is not invoked`() = runTest {
        // GIVEN
        var callbackCount = 0
        val dismisser = WooModalBottomSheetDismisser(
            coroutineScope = this,
            hide = {},
            isVisible = { true },
            onDismissed = { callbackCount++ },
        )

        // WHEN
        dismisser.dismiss()
        runCurrent()

        // THEN
        assertThat(dismisser.isDismissing).isFalse()
        assertThat(callbackCount).isZero()
    }

    @Test
    fun `given dismissal is running, when dismissed again, then only one dismissal runs`() = runTest {
        // GIVEN
        val hideStarted = CompletableDeferred<Unit>()
        val finishHiding = CompletableDeferred<Unit>()
        var hideCount = 0
        var callbackCount = 0
        var isVisible = true
        val dismisser = WooModalBottomSheetDismisser(
            coroutineScope = this,
            hide = {
                hideCount++
                hideStarted.complete(Unit)
                finishHiding.await()
                isVisible = false
            },
            isVisible = { isVisible },
            onDismissed = { callbackCount++ },
        )

        // WHEN
        dismisser.dismiss()
        dismisser.dismiss()
        hideStarted.await()

        // THEN
        assertThat(dismisser.isDismissing).isTrue()
        assertThat(hideCount).isEqualTo(1)

        // WHEN
        finishHiding.complete(Unit)
        runCurrent()

        // THEN
        assertThat(dismisser.isDismissing).isFalse()
        assertThat(callbackCount).isEqualTo(1)
    }

    @Test
    fun `given dismissal is running, when scope is cancelled, then dismissing state is reset`() = runTest {
        // GIVEN
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val hideStarted = CompletableDeferred<Unit>()
        val dismisser = WooModalBottomSheetDismisser(
            coroutineScope = scope,
            hide = {
                hideStarted.complete(Unit)
                awaitCancellation()
            },
            isVisible = { true },
            onDismissed = {},
        )
        dismisser.dismiss()
        hideStarted.await()

        // WHEN
        scope.cancel()
        runCurrent()

        // THEN
        assertThat(dismisser.isDismissing).isFalse()
    }
}
