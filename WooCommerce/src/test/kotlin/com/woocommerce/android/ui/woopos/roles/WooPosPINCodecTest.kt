package com.woocommerce.android.ui.woopos.roles

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class WooPosPINCodecTest {
    private val codec = WooPosPINCodec()
    private val valid = codec.encode(mapOf(WooPosRole.ADMIN to WooPosPINHash.create("1234")))
    private val record = Json.parseToJsonElement(valid.decodeToString()).jsonObject
    private val pin = record.getValue("pins").jsonArray.single().jsonObject

    @Test
    fun `given invalid records, when decoding, then each is rejected`() {
        val malformed = listOf(
            byteArrayOf(),
            "{".encodeToByteArray(),
            valid.copyOf(10),
            valid + byteArrayOf(0),
            valid + "{}".encodeToByteArray(),
            byteArrayOf(0xc3.toByte(), 0x28),
            JsonObject(record + ("version" to JsonPrimitive(99))).toString().encodeToByteArray(),
            JsonObject(record - "version").toString().encodeToByteArray(),
            JsonObject(record + ("unknown" to JsonPrimitive(true))).toString().encodeToByteArray(),
            json(),
            json(JsonObject(pin + ("role" to JsonPrimitive("cashier")))),
            json(JsonObject(pin + ("role" to JsonPrimitive("unknown")))),
            json(JsonObject(pin + ("role" to JsonNull))),
            json(pin, pin),
            json(pin, pin, pin),
            json(JsonObject(pin - "hash")),
            json(JsonObject(pin + ("hash" to JsonNull))),
            json(JsonObject(pin + ("hash" to JsonArray(emptyList())))),
            json(JsonObject(pin + ("salt" to JsonArray(emptyList())))),
            json(JsonObject(pin + ("unknown" to JsonPrimitive(true)))),
        )
        malformed.forEach { bytes ->
            assertThatThrownBy { codec.decode(bytes) }.isInstanceOf(Exception::class.java)
        }
    }

    @Test
    fun `given valid JSON records, when decoded, then their hashes still verify`() {
        val decoded = codec.decode(valid)
        assertThat(decoded.getValue(WooPosRole.ADMIN).matches("1234")).isTrue()
        assertThat(decoded.getValue(WooPosRole.ADMIN).matches("5678")).isFalse()
        assertThat(record.getValue("pins").jsonArray.single().jsonObject.getValue("role"))
            .isEqualTo(JsonPrimitive("admin"))
    }

    @Test
    fun `given a stored JSON fixture, when decoded and encoded, then its schema and PIN are preserved`() {
        // GIVEN
        val stored = """
            {"version":1,"pins":[{"role":"admin",
            "salt":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15],
            "hash":[-122,-98,108,-125,80,-59,-66,-80,-84,-61,-103,-5,-86,-61,-74,13,
            34,4,51,-119,107,38,-90,71,115,77,13,-113,21,-122,-31,-6]}]}
        """.trimIndent()

        // WHEN
        val decoded = codec.decode(stored.encodeToByteArray())
        val encoded = codec.encode(decoded)

        // THEN
        assertThat(decoded.keys).containsExactly(WooPosRole.ADMIN)
        assertThat(decoded.getValue(WooPosRole.ADMIN).matches("1234")).isTrue()
        assertThat(Json.parseToJsonElement(encoded.decodeToString()))
            .isEqualTo(Json.parseToJsonElement(stored))
    }

    @Test
    fun `given all presets with largest byte values, when encoding, then JSON fits the storage limit`() {
        // GIVEN
        val hash = WooPosPINHash(
            ByteArray(WooPosPINHash.SALT_BYTES) { Byte.MIN_VALUE },
            ByteArray(WooPosPINHash.HASH_BYTES) { Byte.MIN_VALUE },
        )
        val pins = WooPosRole.entries.associateWith { hash }

        // WHEN
        val encoded = codec.encode(pins)

        // THEN
        assertThat(encoded.size).isLessThanOrEqualTo(WooPosPINStorage.MAX_RECORD_BYTES)
        assertThat(codec.decode(encoded).keys).containsExactlyElementsOf(WooPosRole.entries)
    }

    @Test
    fun `given valid JSON at the size limit, when adding one more byte, then decoding rejects it`() {
        // GIVEN
        val atLimit = valid + ByteArray(WooPosPINStorage.MAX_RECORD_BYTES - valid.size) { ' '.code.toByte() }

        // THEN
        assertThat(codec.decode(atLimit).getValue(WooPosRole.ADMIN).matches("1234")).isTrue()
        assertThatThrownBy { codec.decode(atLimit + byteArrayOf(' '.code.toByte())) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `given no Admin record, when encoding, then the record is rejected`() {
        assertThatThrownBy { codec.encode(emptyMap()) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { codec.encode(mapOf(WooPosRole.CASHIER to WooPosPINHash.create("5678"))) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun json(vararg pins: JsonElement): ByteArray = JsonObject(
        mapOf("version" to JsonPrimitive(1), "pins" to JsonArray(pins.toList()))
    ).toString().encodeToByteArray()
}
