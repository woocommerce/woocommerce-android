package com.woocommerce.android.ui.woopos.roles

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import javax.inject.Inject

internal class WooPosPINCodec @Inject constructor() {
    fun decode(bytes: ByteArray): Map<WooPosRole, WooPosPINHash> =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(bytes.size <= MAX_BYTES && input.readInt() == VERSION)
            val count = input.readInt()
            require(count in 1..WooPosRole.entries.size)
            val pins = buildMap {
                repeat(count) {
                    val role = when (input.readByte().toInt()) {
                        ADMIN -> WooPosRole.ADMIN
                        CASHIER -> WooPosRole.CASHIER
                        else -> error("Unsupported PIN role")
                    }
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

    fun encode(pins: Map<WooPosRole, WooPosPINHash>): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(VERSION)
            output.writeInt(pins.size)
            pins.forEach { (role, hash) ->
                output.writeByte(if (role == WooPosRole.ADMIN) ADMIN else CASHIER)
                output.writeInt(hash.iterations)
                output.write(hash.salt)
                output.write(hash.hash)
            }
        }
        bytes.toByteArray()
    }

    companion object {
        private const val VERSION = 1
        private const val ADMIN = 1
        private const val CASHIER = 2
        private const val MAX_BYTES = 128
    }
}
