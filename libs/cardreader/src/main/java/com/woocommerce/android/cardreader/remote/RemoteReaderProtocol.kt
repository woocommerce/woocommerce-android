package com.woocommerce.android.cardreader.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.io.DataInputStream
import java.io.DataOutputStream
import java.lang.reflect.Type

class RemoteReaderProtocol {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(RemoteReaderMessage::class.java, MessageSerializer())
        .registerTypeAdapter(RemoteReaderMessage::class.java, MessageDeserializer())
        .create()

    fun write(out: DataOutputStream, msg: RemoteReaderMessage) {
        val bytes = gson.toJson(msg, RemoteReaderMessage::class.java).toByteArray(Charsets.UTF_8)
        require(bytes.size in 1..MAX_MESSAGE_BYTES) { "Invalid frame size ${bytes.size}" }
        out.writeInt(bytes.size)
        out.write(bytes)
        out.flush()
    }

    fun read(input: DataInputStream): RemoteReaderMessage {
        val size = input.readInt()
        require(size in 1..MAX_MESSAGE_BYTES) { "Invalid frame size $size" }
        val bytes = ByteArray(size).also { input.readFully(it) }
        return gson.fromJson(String(bytes, Charsets.UTF_8), RemoteReaderMessage::class.java)
    }

    private class MessageSerializer : JsonSerializer<RemoteReaderMessage> {
        override fun serialize(
            src: RemoteReaderMessage,
            typeOfSrc: Type,
            context: JsonSerializationContext,
        ): JsonElement {
            val concrete = context.serialize(src, src::class.java).asJsonObject
            concrete.addProperty(FIELD_TYPE, typeLabelFor(src::class.java))
            return concrete
        }
    }

    private class MessageDeserializer : JsonDeserializer<RemoteReaderMessage> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext,
        ): RemoteReaderMessage {
            val obj = json.asJsonObject
            val typeLabel = obj.get(FIELD_TYPE)?.asString
                ?: error("Missing '$FIELD_TYPE' discriminator in message")
            val targetClass = classForTypeLabel(typeLabel)
            return context.deserialize(obj, targetClass)
        }
    }

    companion object {
        private const val MAX_MESSAGE_BYTES = 64 * 1024
        private const val FIELD_TYPE = "type"

        private const val TYPE_CONNECT_REQUEST = "connect_request"
        private const val TYPE_CONNECT_ACK = "connect_ack"
        private const val TYPE_COLLECT_PAYMENT = "collect_payment"
        private const val TYPE_PAYMENT_INTENT_RESULT = "payment_intent_result"
        private const val TYPE_ERROR = "error"

        private fun typeLabelFor(clazz: Class<out RemoteReaderMessage>): String = when (clazz) {
            RemoteReaderMessage.ConnectRequest::class.java -> TYPE_CONNECT_REQUEST
            RemoteReaderMessage.ConnectAck::class.java -> TYPE_CONNECT_ACK
            RemoteReaderMessage.CollectPaymentRequest::class.java -> TYPE_COLLECT_PAYMENT
            RemoteReaderMessage.PaymentIntentResult::class.java -> TYPE_PAYMENT_INTENT_RESULT
            RemoteReaderMessage.ErrorMessage::class.java -> TYPE_ERROR
            else -> error("Unknown RemoteReaderMessage subtype: $clazz")
        }

        private fun classForTypeLabel(label: String): Class<out RemoteReaderMessage> = when (label) {
            TYPE_CONNECT_REQUEST -> RemoteReaderMessage.ConnectRequest::class.java
            TYPE_CONNECT_ACK -> RemoteReaderMessage.ConnectAck::class.java
            TYPE_COLLECT_PAYMENT -> RemoteReaderMessage.CollectPaymentRequest::class.java
            TYPE_PAYMENT_INTENT_RESULT -> RemoteReaderMessage.PaymentIntentResult::class.java
            TYPE_ERROR -> RemoteReaderMessage.ErrorMessage::class.java
            else -> error("Unknown message type label: $label")
        }
    }
}
