package com.woocommerce.android.cardreader.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.Socket
import java.net.SocketException

internal class CardReaderRemoteConnection internal constructor(
    private val socket: Socket,
    private val protocol: CardReaderRemoteProtocol = CardReaderRemoteProtocol(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AutoCloseable {
    private val output = DataOutputStream(socket.getOutputStream())
    private val input = DataInputStream(socket.getInputStream())
    private val writeLock = Mutex()

    suspend fun send(msg: CardReaderRemoteMessage) {
        writeLock.withLock {
            withContext(ioDispatcher) {
                protocol.write(output, msg)
            }
        }
    }

    fun receive(): Flow<CardReaderRemoteMessage> = flow {
        while (!socket.isClosed) {
            val next = runCatching { protocol.read(input) }
                .getOrElse { err ->
                    if (err is EOFException || err is SocketException) return@flow
                    throw err
                }
            emit(next)
        }
    }.flowOn(ioDispatcher)

    override fun close() {
        runCatching { socket.close() }
    }
}
