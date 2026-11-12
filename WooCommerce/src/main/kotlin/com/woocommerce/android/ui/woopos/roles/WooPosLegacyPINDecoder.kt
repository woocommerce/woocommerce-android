package com.woocommerce.android.ui.woopos.roles

import java.io.ByteArrayInputStream
import java.io.DataInputStream

/** Preserves existing PINs while all new records use JSON. */
internal class WooPosLegacyPINDecoder {
    fun decode(bytes: ByteArray): Map<WooPosRole, WooPosPINHash> =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(bytes.size <= MAX_BYTES && input.readInt() == VERSION)
            val count = input.readInt()
            require(count in 1..ROLES.size)
            val pins = buildMap {
                repeat(count) {
                    val role = ROLES[input.readByte().toInt()] ?: error("Unsupported legacy PIN role")
                    require(!containsKey(role))
                    put(
                        role,
                        WooPosPINHash(
                            iterations = input.readInt(),
                            salt = ByteArray(WooPosPINHash.SALT_BYTES).also { input.readFully(it) },
                            hash = ByteArray(WooPosPINHash.HASH_BYTES).also { input.readFully(it) },
                        )
                    )
                }
            }
            require(input.available() == 0 && WooPosRole.ADMIN in pins)
            pins
        }

    companion object {
        private const val VERSION = 1
        private const val MAX_BYTES = 128
        private val ROLES = mapOf(1 to WooPosRole.ADMIN, 2 to WooPosRole.CASHIER)
    }
}
