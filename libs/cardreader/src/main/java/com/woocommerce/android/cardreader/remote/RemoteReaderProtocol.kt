package com.woocommerce.android.cardreader.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.DataInputStream
import java.io.DataOutputStream

class RemoteReaderProtocol {
    private val moshi = Moshi.Builder()
        .add(
            PolymorphicJsonAdapterFactory.of(RemoteReaderMessage::class.java, "type")
                .withSubtype(RemoteReaderMessage.ConnectRequest::class.java, "connect_request")
                .withSubtype(RemoteReaderMessage.ConnectAck::class.java, "connect_ack")
                .withSubtype(RemoteReaderMessage.CollectPaymentRequest::class.java, "collect_payment")
                .withSubtype(RemoteReaderMessage.PaymentIntentResult::class.java, "payment_intent_result")
                .withSubtype(RemoteReaderMessage.ErrorMessage::class.java, "error")
        )
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(RemoteReaderMessage::class.java)

    fun write(out: DataOutputStream, msg: RemoteReaderMessage) {
        val bytes = adapter.toJson(msg).toByteArray(Charsets.UTF_8)
        require(bytes.size in 1..MAX_MESSAGE_BYTES) { "Invalid frame size ${bytes.size}" }
        out.writeInt(bytes.size)
        out.write(bytes)
        out.flush()
    }

    fun read(input: DataInputStream): RemoteReaderMessage {
        val size = input.readInt()
        require(size in 1..MAX_MESSAGE_BYTES) { "Invalid frame size $size" }
        val bytes = ByteArray(size).also { input.readFully(it) }
        return adapter.fromJson(String(bytes, Charsets.UTF_8))
            ?: error("Failed to decode RemoteReaderMessage")
    }

    companion object {
        private const val MAX_MESSAGE_BYTES = 64 * 1024
    }
}
