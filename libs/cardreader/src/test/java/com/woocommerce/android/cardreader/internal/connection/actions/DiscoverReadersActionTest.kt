package com.woocommerce.android.cardreader.internal.connection.actions

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.models.Reader
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Failure
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.FoundReaders
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Started
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DiscoverReadersActionTest : CardReaderBaseUnitTest() {
    private lateinit var action: DiscoverReadersAction
    private val terminal: TerminalWrapper = mock()
    private val logWrapper: LogWrapper = mock()

    @Before
    fun setUp() {
        action = DiscoverReadersAction(terminal, logWrapper)
    }

    @Test
    fun `when discovery started, then Started is emitted`() = testBlocking {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            mock<Cancelable>()
        }

        val result = action.discoverReaders(false).first()

        assertThat(result).isInstanceOf(Started::class.java)
    }

    @Test
    fun `when nearby readers found, then FoundReaders is emitted`() = testBlocking {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onUpdateDiscoveredReaders(args = it.arguments, readers = listOf(mock()))
            mock<Cancelable>()
        }

        val event = action.discoverReaders(false)
            .ignoreStartedEvent().first()

        assertThat(event).isInstanceOf(FoundReaders::class.java)
    }

    @Test
    fun `when new readers found, then FoundReaders is emitted`() = testBlocking {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onUpdateDiscoveredReaders(args = it.arguments, readers = listOf(mock()))
            onUpdateDiscoveredReaders(args = it.arguments, readers = listOf(mock(), mock()))
            onSuccess(args = it.arguments)
            mock<Cancelable>()
        }

        val events = action.discoverReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(events[0]).isInstanceOf(FoundReaders::class.java)
        assertThat(events[1]).isInstanceOf(FoundReaders::class.java)
    }

    @Test
    fun `when already found readers found, then FoundReaders is NOT emitted`() = testBlocking {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            val reader = mock<Reader>()
            onUpdateDiscoveredReaders(args = it.arguments, readers = listOf(reader))
            onUpdateDiscoveredReaders(args = it.arguments, readers = listOf(reader))
            onSuccess(args = it.arguments)
            mock<Cancelable>()
        }

        val events = action.discoverReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(events[0]).isInstanceOf(FoundReaders::class.java)
        assertThat(events[1]).isNotInstanceOf(FoundReaders::class.java)
    }

    @Test
    fun `when reader discover succeeds, then Success is emitted`() = testBlocking {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onSuccess(args = it.arguments)
            mock<Cancelable>()
        }

        val event = action.discoverReaders(false)
            .ignoreStartedEvent().first()

        assertThat(event).isInstanceOf(Success::class.java)
    }

    @Test
    fun `when reader discover fails, then Failure is emitted`() = testBlocking {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onFailure(it.arguments)
            mock<Cancelable>()
        }

        val event = action.discoverReaders(false)
            .ignoreStartedEvent().first()

        assertThat(event).isInstanceOf(Failure::class.java)
    }

    @Test
    fun `when reader discover succeeds, then flow is terminated`() = testBlocking {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onSuccess(args = it.arguments)
            mock<Cancelable>()
        }

        val event = action.discoverReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(event.size).isEqualTo(1)
    }

    @Test
    fun `when reader discover fails, then flow is terminated`() = testBlocking {
        whenever(terminal.discoverReaders(any(), any(), any())).thenAnswer {
            onFailure(it.arguments)
            mock<Cancelable>()
        }

        val event = action.discoverReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(event.size).isEqualTo(1)
    }

    @Test
    fun `given flow not terminated, when job canceled, then reader discovery gets canceled`() = testBlocking {
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
    fun `given flow already terminated, when job canceled, then reader discovery not canceled`() = testBlocking {
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
    fun `given last event is terminal, when multiple events emitted, then flow terminates`() = testBlocking {
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
