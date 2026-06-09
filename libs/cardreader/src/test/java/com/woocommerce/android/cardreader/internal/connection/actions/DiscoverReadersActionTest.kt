package com.woocommerce.android.cardreader.internal.connection.actions

import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Failure
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.FoundReaders
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Started
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
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
        val result = action.discoverExternalReaders(false).first()

        assertThat(result).isInstanceOf(Started::class.java)
    }

    @Test
    fun `when nearby readers found, then FoundReaders is emitted`() = testBlocking {
        whenever(terminal.discoverReaders(any())).thenReturn(
            flow { emit(listOf(mock<Reader>())) }
        )

        val event = action.discoverExternalReaders(false)
            .ignoreStartedEvent().first()

        assertThat(event).isInstanceOf(FoundReaders::class.java)
    }

    @Test
    fun `when new readers found, then FoundReaders is emitted`() = testBlocking {
        whenever(terminal.discoverReaders(any())).thenReturn(
            flow {
                emit(listOf(mock<Reader>()))
                emit(listOf(mock<Reader>(), mock()))
            }
        )

        val events = action.discoverExternalReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(events[0]).isInstanceOf(FoundReaders::class.java)
        assertThat(events[1]).isInstanceOf(FoundReaders::class.java)
    }

    @Test
    fun `when already found readers found, then FoundReaders is NOT emitted`() = testBlocking {
        val reader = mock<Reader>()
        whenever(terminal.discoverReaders(any())).thenReturn(
            flow {
                emit(listOf(reader))
                emit(listOf(reader))
            }
        )

        val events = action.discoverExternalReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(events[0]).isInstanceOf(FoundReaders::class.java)
        assertThat(events[1]).isNotInstanceOf(FoundReaders::class.java)
    }

    @Test
    fun `when reader discover succeeds, then Success is emitted`() = testBlocking {
        whenever(terminal.discoverReaders(any())).thenReturn(flow { })

        val events = action.discoverExternalReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(events.last()).isInstanceOf(Success::class.java)
    }

    @Test
    fun `when reader discover fails, then Failure is emitted`() = testBlocking {
        whenever(terminal.discoverReaders(any())).thenReturn(
            flow { throw mock<TerminalException>() }
        )

        val event = action.discoverExternalReaders(false)
            .ignoreStartedEvent().first()

        assertThat(event).isInstanceOf(Failure::class.java)
    }

    @Test
    fun `when reader discover succeeds, then flow is terminated`() = testBlocking {
        whenever(terminal.discoverReaders(any())).thenReturn(flow { })

        val events = action.discoverExternalReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(events.size).isEqualTo(1)
    }

    @Test
    fun `when reader discover fails, then flow is terminated`() = testBlocking {
        whenever(terminal.discoverReaders(any())).thenReturn(
            flow { throw mock<TerminalException>() }
        )

        val events = action.discoverExternalReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(events.size).isEqualTo(1)
    }

    @Test
    fun `given last event is terminal, when discovery external readers, then flow terminates`() = testBlocking {
        whenever(terminal.discoverReaders(any())).thenReturn(
            flow {
                emit(listOf(mock<Reader>()))
                emit(listOf(mock<Reader>(), mock()))
                throw mock<TerminalException>()
            }
        )

        val result = action.discoverExternalReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(result.size).isEqualTo(3)
    }

    @Test
    fun `when discovery external readers, then config keeps bluetooth scan`() = testBlocking {
        whenever(terminal.discoverReaders(any())).thenReturn(flow { })

        action.discoverExternalReaders(false).toList()

        val configCaptor = argumentCaptor<DiscoveryConfiguration>()
        verify(terminal).discoverReaders(configCaptor.capture())
        assertThat(configCaptor.firstValue).isEqualTo(
            DiscoveryConfiguration.BluetoothDiscoveryConfiguration(
                60,
                false
            )
        )
    }

    @Test
    fun `when discovery built in readers, then config keeps local mobile`() = testBlocking {
        whenever(terminal.discoverReaders(any())).thenReturn(flow { })

        action.discoverBuildInReaders(true).toList()

        val configCaptor = argumentCaptor<DiscoveryConfiguration>()
        verify(terminal).discoverReaders(configCaptor.capture())
        assertThat(configCaptor.firstValue).isEqualTo(
            DiscoveryConfiguration.TapToPayDiscoveryConfiguration(
                true
            )
        )
    }

    @Test
    fun `given last event is terminal, when discovery built in readers, then flow terminates`() = testBlocking {
        whenever(terminal.discoverReaders(any())).thenReturn(
            flow {
                emit(listOf(mock<Reader>()))
                emit(listOf(mock<Reader>(), mock()))
                throw mock<TerminalException>()
            }
        )

        val result = action.discoverBuildInReaders(false)
            .ignoreStartedEvent().toList()

        assertThat(result.size).isEqualTo(3)
    }

    private fun <T> Flow<T>.ignoreStartedEvent(): Flow<T> = filterNot { it is Started }
}
