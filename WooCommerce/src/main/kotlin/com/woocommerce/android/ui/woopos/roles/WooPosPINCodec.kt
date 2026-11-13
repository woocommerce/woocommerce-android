package com.woocommerce.android.ui.woopos.roles

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class WooPosPINCodec {
    fun decode(bytes: ByteArray): Map<WooPosRole, WooPosPINHash> {
        require(bytes.size <= WooPosPINStorage.MAX_RECORD_BYTES)

        val record = Json.decodeFromString<PINRecord>(bytes.decodeToString(throwOnInvalidSequence = true))
        require(record.version == VERSION && record.pins.size in 1..WooPosRole.entries.size)
        val pins = buildMap {
            record.pins.forEach { pin ->
                require(!containsKey(pin.role))
                put(pin.role, WooPosPINHash(pin.iterations, pin.salt, pin.hash))
            }
        }
        require(WooPosRole.ADMIN in pins)
        return pins
    }

    fun encode(pins: Map<WooPosRole, WooPosPINHash>): ByteArray {
        require(WooPosRole.ADMIN in pins)
        val record = PINRecord(
            version = VERSION,
            pins = pins.map { (role, hash) -> StoredPIN(role, hash.iterations, hash.salt, hash.hash) },
        )
        return Json.encodeToString(record).encodeToByteArray().also {
            require(it.size <= WooPosPINStorage.MAX_RECORD_BYTES)
        }
    }

    @Serializable
    private data class PINRecord(val version: Int, val pins: List<StoredPIN>)

    @Serializable
    private data class StoredPIN(
        val role: WooPosRole,
        val iterations: Int,
        val salt: ByteArray,
        val hash: ByteArray,
    )

    companion object {
        private const val VERSION = 1
    }
}
