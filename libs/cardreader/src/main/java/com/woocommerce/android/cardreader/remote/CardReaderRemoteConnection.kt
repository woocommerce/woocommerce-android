package com.woocommerce.android.cardreader.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.Socket
import java.net.SocketException
import javax.net.ssl.SSLException

internal class CardReaderRemoteConnection internal constructor(
    private val socket: Socket,
    private val protocol: CardReaderRemoteProtocol = CardReaderRemoteProtocol(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AutoCloseable {
    private val output = DataOutputStream(socket.getOutputStream())
    private val input = DataInputStream(socket.getInputStream())
    private val writeLock = Mutex()

    private val readerScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val messages = Channel<CardReaderRemoteMessage>(capacity = MESSAGE_BUFFER_CAPACITY)

    private val readerJob: Job = readerScope.launch {
        var fatalError: Throwable? = null
        try {
            while (!socket.isClosed) {
                val next = runCatching { protocol.read(input) }.getOrElse { err ->
                    if (err !is EOFException && err !is SocketException && err !is SSLException) {
                        fatalError = err
                    }
                    return@launch
                }
                messages.send(next)
            }
        } finally {
            messages.close(fatalError)
        }
    }

    suspend fun send(msg: CardReaderRemoteMessage) {
        writeLock.withLock {
            withContext(ioDispatcher) {
                protocol.write(output, msg)
            }
        }
    }

    fun receive(): Flow<CardReaderRemoteMessage> = messages.receiveAsFlow()

    override fun close() {
        runCatching { socket.close() }
        readerJob.cancel()
        readerScope.cancel()
        messages.close()
    }

    private companion object {
        const val MESSAGE_BUFFER_CAPACITY = 64
    }
}
