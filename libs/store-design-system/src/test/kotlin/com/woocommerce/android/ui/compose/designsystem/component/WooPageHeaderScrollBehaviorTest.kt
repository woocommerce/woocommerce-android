package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
class WooPageHeaderScrollBehaviorTest {
    @Test
    fun `given collapsed header, when expand completes, then height and content offsets reset`() = runTest {
        // GIVEN
        val initialValues = mutableListOf<Float>()
        val fixture = givenBehavior(
            heightOffset = -COLLAPSE_RANGE,
            contentOffset = CONTENT_OFFSET,
            expansionAnimator = WooPageHeaderExpansionAnimator { initialValue, onFrame ->
                initialValues += initialValue
                onFrame(0f)
            },
        )

        // WHEN
        fixture.behavior.expand()

        // THEN
        assertThat(initialValues).containsExactly(-COLLAPSE_RANGE)
        assertThat(fixture.state.heightOffset).isZero()
        assertThat(fixture.state.contentOffset).isZero()
    }

    @Test
    fun `given partially collapsed header, when expand completes, then animation starts from current height`() =
        runTest {
            // GIVEN
            val frames = mutableListOf<Float>()
            val fixture = givenBehavior(
                heightOffset = -PARTIAL_OFFSET,
                contentOffset = CONTENT_OFFSET,
                expansionAnimator = WooPageHeaderExpansionAnimator { initialValue, onFrame ->
                    frames += initialValue
                    onFrame(-INTERMEDIATE_OFFSET)
                    frames += -INTERMEDIATE_OFFSET
                    onFrame(0f)
                },
            )

            // WHEN
            fixture.behavior.expand()

            // THEN
            assertThat(frames).containsExactly(-PARTIAL_OFFSET, -INTERMEDIATE_OFFSET)
            assertThat(fixture.state.heightOffset).isZero()
            assertThat(fixture.state.contentOffset).isZero()
        }

    @Test
    fun `given expansion is running, when newer expansion completes, then stale writes are rejected`() = runTest {
        // GIVEN
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val animationJobs = mutableListOf<Job>()
        var invocation = 0
        val fixture = givenBehavior(
            heightOffset = -COLLAPSE_RANGE,
            contentOffset = CONTENT_OFFSET,
            expansionAnimator = WooPageHeaderExpansionAnimator { _, onFrame ->
                animationJobs += currentCoroutineContext()[Job] ?: error("Expansion requires a child Job")
                invocation += 1
                if (invocation == 1) {
                    onFrame(-PARTIAL_OFFSET)
                    firstStarted.complete(Unit)
                    withContext(NonCancellable) {
                        releaseFirst.await()
                        onFrame(-STALE_OFFSET)
                    }
                } else {
                    onFrame(0f)
                }
            },
        )
        lateinit var firstCallerJob: Job
        val firstCaller = launch {
            firstCallerJob = currentCoroutineContext()[Job] ?: error("Caller requires a Job")
            fixture.behavior.expand()
        }
        firstStarted.await()

        // WHEN
        val newestCaller = launch { fixture.behavior.expand() }
        newestCaller.join()

        // THEN
        assertThat(fixture.state.heightOffset).isZero()
        assertThat(fixture.state.contentOffset).isZero()
        assertThat(animationJobs).hasSize(2)
        assertThat(animationJobs.first()).isNotSameAs(firstCallerJob)
        assertThat(firstCaller.isActive).isTrue()
        assertThat(newestCaller.isCancelled).isFalse()

        // WHEN a canceled animation attempts a late frame and final reset
        fixture.state.heightOffset = -LATEST_OFFSET
        fixture.state.contentOffset = LATEST_CONTENT_OFFSET
        releaseFirst.complete(Unit)
        firstCaller.join()

        // THEN
        assertThat(firstCaller.isCancelled).isFalse()
        assertThat(fixture.state.heightOffset).isEqualTo(-LATEST_OFFSET)
        assertThat(fixture.state.contentOffset).isEqualTo(LATEST_CONTENT_OFFSET)
    }

    @Test
    fun `given expansion is running, when meaningful pre-scroll input arrives, then cancellation precedes delegation`() =
        runTest {
            // GIVEN
            val animator = BlockingExpansionAnimator()
            var canceledBeforeDelegation = false
            val delegate = object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    canceledBeforeDelegation = animator.job.isCancelled
                    return PRE_SCROLL_RESULT
                }
            }
            val fixture = givenBehavior(
                heightOffset = -COLLAPSE_RANGE,
                contentOffset = CONTENT_OFFSET,
                delegate = delegate,
                expansionAnimator = animator,
            )
            val caller = launch { fixture.behavior.expand() }
            animator.started.await()

            // WHEN
            val result = fixture.behavior.nestedScrollConnection.onPreScroll(
                available = Offset(0f, -USER_DELTA),
                source = NestedScrollSource.UserInput,
            )
            runCurrent()

            // THEN
            assertThat(canceledBeforeDelegation).isTrue()
            assertThat(result).isEqualTo(PRE_SCROLL_RESULT)
            assertThat(caller.isCompleted).isTrue()
            assertThat(caller.isCancelled).isFalse()
            assertThat(fixture.state.heightOffset).isEqualTo(-COLLAPSE_RANGE)
            assertThat(fixture.state.contentOffset).isEqualTo(CONTENT_OFFSET)
        }

    @Test
    fun `given expansion is running, when meaningful post-scroll input arrives, then cancellation precedes delegation`() =
        runTest {
            // GIVEN
            val animator = BlockingExpansionAnimator()
            var canceledBeforeDelegation = false
            val delegate = object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    canceledBeforeDelegation = animator.job.isCancelled
                    return POST_SCROLL_RESULT
                }
            }
            val fixture = givenBehavior(
                heightOffset = -COLLAPSE_RANGE,
                delegate = delegate,
                expansionAnimator = animator,
            )
            val caller = launch { fixture.behavior.expand() }
            animator.started.await()

            // WHEN
            val result = fixture.behavior.nestedScrollConnection.onPostScroll(
                consumed = Offset(0f, -USER_DELTA),
                available = Offset.Zero,
                source = NestedScrollSource.UserInput,
            )
            runCurrent()

            // THEN
            assertThat(canceledBeforeDelegation).isTrue()
            assertThat(result).isEqualTo(POST_SCROLL_RESULT)
            assertThat(caller.isCompleted).isTrue()
            assertThat(caller.isCancelled).isFalse()
            assertThat(fixture.state.heightOffset).isEqualTo(-COLLAPSE_RANGE)
        }

    @Test
    fun `given expansion is running, when meaningful side-effect scroll arrives, then expansion remains active and finishes`() =
        runTest {
            // GIVEN
            val animator = CompletingExpansionAnimator()
            val fixture = givenBehavior(
                heightOffset = -COLLAPSE_RANGE,
                contentOffset = CONTENT_OFFSET,
                expansionAnimator = animator,
            )
            val caller = launch { fixture.behavior.expand() }
            animator.started.await()

            // WHEN
            fixture.behavior.nestedScrollConnection.onPreScroll(
                available = Offset(0f, -USER_DELTA),
                source = NestedScrollSource.SideEffect,
            )
            fixture.behavior.nestedScrollConnection.onPostScroll(
                consumed = Offset(0f, -USER_DELTA),
                available = Offset.Zero,
                source = NestedScrollSource.SideEffect,
            )

            // THEN
            assertThat(animator.job.isActive).isTrue()
            assertThat(caller.isActive).isTrue()

            // WHEN
            animator.finish.complete(Unit)
            caller.join()

            // THEN
            assertThat(caller.isCancelled).isFalse()
            assertThat(fixture.state.heightOffset).isZero()
            assertThat(fixture.state.contentOffset).isZero()
        }

    @Test
    fun `given expansion is running, when zero-delta user pre-scroll arrives, then expansion remains active and finishes`() =
        runTest {
            // GIVEN
            val animator = CompletingExpansionAnimator()
            val fixture = givenBehavior(
                heightOffset = -COLLAPSE_RANGE,
                contentOffset = CONTENT_OFFSET,
                expansionAnimator = animator,
            )
            val caller = launch { fixture.behavior.expand() }
            animator.started.await()

            // WHEN
            fixture.behavior.nestedScrollConnection.onPreScroll(
                available = Offset.Zero,
                source = NestedScrollSource.UserInput,
            )

            // THEN
            assertThat(animator.job.isActive).isTrue()
            assertThat(caller.isActive).isTrue()

            // WHEN
            animator.finish.complete(Unit)
            caller.join()

            // THEN
            assertThat(caller.isCancelled).isFalse()
            assertThat(fixture.state.heightOffset).isZero()
            assertThat(fixture.state.contentOffset).isZero()
        }

    @Test
    fun `given expansion is running, when fling boundaries arrive, then each cancellation precedes delegation`() =
        runTest {
            // GIVEN
            val preFlingAnimator = BlockingExpansionAnimator()
            var preFlingCanceledBeforeDelegation = false
            val preFlingDelegate = object : NestedScrollConnection {
                override suspend fun onPreFling(available: Velocity): Velocity {
                    preFlingCanceledBeforeDelegation = preFlingAnimator.job.isCancelled
                    return PRE_FLING_RESULT
                }
            }
            val preFlingFixture = givenBehavior(
                heightOffset = -COLLAPSE_RANGE,
                delegate = preFlingDelegate,
                expansionAnimator = preFlingAnimator,
            )
            val preFlingCaller = launch { preFlingFixture.behavior.expand() }
            preFlingAnimator.started.await()

            // WHEN
            val preFlingResult = preFlingFixture.behavior.nestedScrollConnection.onPreFling(FLING_VELOCITY)
            runCurrent()

            // THEN
            assertThat(preFlingCanceledBeforeDelegation).isTrue()
            assertThat(preFlingResult).isEqualTo(PRE_FLING_RESULT)
            assertThat(preFlingCaller.isCancelled).isFalse()

            // GIVEN
            val postFlingAnimator = BlockingExpansionAnimator()
            var postFlingCanceledBeforeDelegation = false
            val postFlingDelegate = object : NestedScrollConnection {
                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    postFlingCanceledBeforeDelegation = postFlingAnimator.job.isCancelled
                    return POST_FLING_RESULT
                }
            }
            val postFlingFixture = givenBehavior(
                heightOffset = -COLLAPSE_RANGE,
                delegate = postFlingDelegate,
                expansionAnimator = postFlingAnimator,
            )
            val postFlingCaller = launch { postFlingFixture.behavior.expand() }
            postFlingAnimator.started.await()

            // WHEN
            val postFlingResult = postFlingFixture.behavior.nestedScrollConnection.onPostFling(
                consumed = Velocity.Zero,
                available = FLING_VELOCITY,
            )
            runCurrent()

            // THEN
            assertThat(postFlingCanceledBeforeDelegation).isTrue()
            assertThat(postFlingResult).isEqualTo(POST_FLING_RESULT)
            assertThat(postFlingCaller.isCancelled).isFalse()
        }

    private fun givenBehavior(
        heightOffset: Float,
        contentOffset: Float = 0f,
        delegate: NestedScrollConnection = object : NestedScrollConnection {},
        expansionAnimator: WooPageHeaderExpansionAnimator,
    ): BehaviorFixture {
        val state = TopAppBarState(
            initialHeightOffsetLimit = -COLLAPSE_RANGE,
            initialHeightOffset = heightOffset,
            initialContentOffset = contentOffset,
        )
        val materialScrollBehavior = TestTopAppBarScrollBehavior(
            state = state,
            nestedScrollConnection = delegate,
        )
        return BehaviorFixture(
            behavior = WooPageHeaderScrollBehavior(
                materialScrollBehavior = materialScrollBehavior,
                expansionAnimator = expansionAnimator,
            ),
            state = state,
        )
    }

    private data class BehaviorFixture(
        val behavior: WooPageHeaderScrollBehavior,
        val state: TopAppBarState,
    )

    private class BlockingExpansionAnimator : WooPageHeaderExpansionAnimator {
        val started = CompletableDeferred<Unit>()
        lateinit var job: Job

        override suspend fun animate(initialValue: Float, onFrame: (Float) -> Unit) {
            job = currentCoroutineContext()[Job] ?: error("Expansion requires a child Job")
            started.complete(Unit)
            awaitCancellation()
        }
    }

    private class CompletingExpansionAnimator : WooPageHeaderExpansionAnimator {
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        lateinit var job: Job

        override suspend fun animate(initialValue: Float, onFrame: (Float) -> Unit) {
            job = currentCoroutineContext()[Job] ?: error("Expansion requires a child Job")
            started.complete(Unit)
            finish.await()
            onFrame(0f)
        }
    }

    private class TestTopAppBarScrollBehavior(
        override val state: TopAppBarState,
        override val nestedScrollConnection: NestedScrollConnection,
    ) : TopAppBarScrollBehavior {
        override val isPinned = false
        override val snapAnimationSpec: AnimationSpec<Float>? = null
        override val flingAnimationSpec: DecayAnimationSpec<Float>? = null
    }

    private companion object {
        const val COLLAPSE_RANGE = 48f
        const val PARTIAL_OFFSET = 24f
        const val INTERMEDIATE_OFFSET = 12f
        const val STALE_OFFSET = 40f
        const val LATEST_OFFSET = 8f
        const val USER_DELTA = 4f
        const val CONTENT_OFFSET = -80f
        const val LATEST_CONTENT_OFFSET = -30f
        val PRE_SCROLL_RESULT = Offset(0f, -2f)
        val POST_SCROLL_RESULT = Offset(0f, 2f)
        val FLING_VELOCITY = Velocity(0f, 1_000f)
        val PRE_FLING_RESULT = Velocity(0f, 100f)
        val POST_FLING_RESULT = Velocity(0f, 200f)
    }
}
