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

internal class CardReaderRemoteProtocol {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(CardReaderRemoteMessage::class.java, MessageSerializer())
        .registerTypeAdapter(CardReaderRemoteMessage::class.java, MessageDeserializer())
        .create()

    fun write(out: DataOutputStream, msg: CardReaderRemoteMessage) {
        val bytes = gson.toJson(msg, CardReaderRemoteMessage::class.java).toByteArray(Charsets.UTF_8)
        require(bytes.size in 1..MAX_MESSAGE_BYTES) { "Invalid frame size ${bytes.size}" }
        out.writeInt(bytes.size)
        out.write(bytes)
        out.flush()
    }

    fun read(input: DataInputStream): CardReaderRemoteMessage {
        val size = input.readInt()
        require(size in 1..MAX_MESSAGE_BYTES) { "Invalid frame size $size" }
        val bytes = ByteArray(size).also { input.readFully(it) }
        return gson.fromJson(String(bytes, Charsets.UTF_8), CardReaderRemoteMessage::class.java)
    }

    private class MessageSerializer : JsonSerializer<CardReaderRemoteMessage> {
        override fun serialize(
            src: CardReaderRemoteMessage,
            typeOfSrc: Type,
            context: JsonSerializationContext,
        ): JsonElement {
            val concrete = context.serialize(src, src::class.java).asJsonObject
            concrete.addProperty(FIELD_TYPE, typeLabelFor(src::class.java))
            return concrete
        }
    }

    private class MessageDeserializer : JsonDeserializer<CardReaderRemoteMessage> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext,
        ): CardReaderRemoteMessage {
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
        private const val TYPE_PING = "ping"

        private fun typeLabelFor(clazz: Class<out CardReaderRemoteMessage>): String = when (clazz) {
            CardReaderRemoteMessage.ConnectRequest::class.java -> TYPE_CONNECT_REQUEST
            CardReaderRemoteMessage.ConnectAck::class.java -> TYPE_CONNECT_ACK
            CardReaderRemoteMessage.CollectPaymentRequest::class.java -> TYPE_COLLECT_PAYMENT
            CardReaderRemoteMessage.PaymentIntentResult::class.java -> TYPE_PAYMENT_INTENT_RESULT
            CardReaderRemoteMessage.ErrorMessage::class.java -> TYPE_ERROR
            CardReaderRemoteMessage.Ping::class.java -> TYPE_PING
            else -> error("Unknown CardReaderRemoteMessage subtype: $clazz")
        }

        private fun classForTypeLabel(label: String): Class<out CardReaderRemoteMessage> = when (label) {
            TYPE_CONNECT_REQUEST -> CardReaderRemoteMessage.ConnectRequest::class.java
            TYPE_CONNECT_ACK -> CardReaderRemoteMessage.ConnectAck::class.java
            TYPE_COLLECT_PAYMENT -> CardReaderRemoteMessage.CollectPaymentRequest::class.java
            TYPE_PAYMENT_INTENT_RESULT -> CardReaderRemoteMessage.PaymentIntentResult::class.java
            TYPE_ERROR -> CardReaderRemoteMessage.ErrorMessage::class.java
            TYPE_PING -> CardReaderRemoteMessage.Ping::class.java
            else -> error("Unknown message type label: $label")
        }
    }
}
