package com.woocommerce.android.cardreader.internal.connection.actions

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.stripeterminal.callable.Callback
import com.stripe.stripeterminal.callable.Cancelable
import com.stripe.stripeterminal.callable.DiscoveryListener
import com.stripe.stripeterminal.model.external.Reader
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Failure
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.FoundReaders
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Started
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class DiscoverReadersActionTest {
    private lateinit var action: DiscoverReadersAction
    private val terminal: TerminalWrapper = mock()

    @Before
    fun setUp() {
        action = DiscoverReadersAction(terminal)
    }

    @Test
    fun `when discovery started, then Started is emitted`() = runBlockingTest {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            mock<Cancelable>()
        }

        val result = action.discoverReaders(false).first()

        assertThat(result).isInstanceOf(Started::class.java)
    }

    @Test
    fun `when nearby readers found, then FoundReaders is emitted`() = runBlockingTest {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onUpdateDiscoveredReaders(args = it.arguments, readers = listOf(mock()))
            mock<Cancelable>()
        }

        val event = action.discoverReaders(false)
            .ignoreStartedEvent().first()

        assertThat(event).isInstanceOf(FoundReaders::class.java)
    }

    @Test
    fun `when reader discover succeeds, then Success is emitted`() = runBlockingTest {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onSuccess(args = it.arguments)
            mock<Cancelable>()
        }

        val event = action.discoverReaders(false)
            .ignoreStartedEvent().first()

        assertThat(event).isInstanceOf(Success::class.java)
    }

    @Test
    fun `when reader discover fails, then Failure is emitted`() = runBlockingTest {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onFailure(it.arguments)
            mock<Cancelable>()
        }

        val event = action.discoverReaders(false)
            .ignoreStartedEvent().first()

        assertThat(event).isInstanceOf(Failure::class.java)
    }

    @Test
    fun `when reader discover succeeds, then flow is terminated`() = runBlockingTest {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onSuccess(args = it.arguments)
            mock<Cancelable>()
        }

        val event = action.discoverReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(event.size).isEqualTo(1)
    }

    @Test
    fun `when reader discover fails, then flow is terminated`() = runBlockingTest {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onFailure(it.arguments)
            mock<Cancelable>()
        }

        val event = action.discoverReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(event.size).isEqualTo(1)
    }

    @Test
    fun `given flow not terminated, when job canceled, then reader discovery gets canceled`() = runBlockingTest {
        val cancelable = mock<Cancelable>()
        whenever(cancelable.isCompleted).thenReturn(false)
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer { cancelable }
        val job = launch {
            action.discoverReaders(false).collect { }
        }

        job.cancel()
        joinAll(job)

        verify(cancelable).cancel(any())
    }

    @Test
    fun `given flow already terminated, when job canceled, then reader discovery not canceled`() = runBlockingTest {
        val cancelable = mock<Cancelable>()
        whenever(cancelable.isCompleted).thenReturn(true)
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onSuccess(it.arguments)
            cancelable
        }
        val job = launch {
            action.discoverReaders(false).collect { }
        }

        job.cancel()
        joinAll(job)

        verify(cancelable, never()).cancel(any())
    }

    @Test
    fun `given last event is terminal, when multiple events emitted, then flow terminates`() = runBlockingTest {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onUpdateDiscoveredReaders(args = it.arguments, readers = listOf(mock()))
            onUpdateDiscoveredReaders(args = it.arguments, readers = listOf(mock()))
            onFailure(it.arguments)
            mock<Cancelable>()
        }

        val result = action.discoverReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(result.size).isEqualTo(3)
    }

    @Test(expected = ClosedSendChannelException::class)
    fun `given more events emitted, when terminal event already processed, then exception is thrown`() =
        runBlockingTest {
            whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
                onSuccess(it.arguments)
                onUpdateDiscoveredReaders(args = it.arguments, readers = listOf())
                mock<Cancelable>()
            }

            action.discoverReaders(false).toList()
        }

    private fun onUpdateDiscoveredReaders(args: Array<Any>, readers: List<Reader>) {
        args.filterIsInstance<DiscoveryListener>().first().onUpdateDiscoveredReaders(readers)
    }

    private fun onSuccess(args: Array<Any>) {
        args.filterIsInstance<Callback>().first().onSuccess()
    }

    private fun onFailure(args: Array<Any>) {
        args.filterIsInstance<Callback>().first().onFailure(mock())
    }

    private fun <T> Flow<T>.ignoreStartedEvent(): Flow<T> = filterNot { it is Started }
}
