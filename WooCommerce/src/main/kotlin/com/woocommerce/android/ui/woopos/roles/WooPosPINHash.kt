package com.woocommerce.android.ui.woopos.roles

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class WooPosPINHash internal constructor(
    internal val salt: ByteArray,
    internal val hash: ByteArray,
) {
    init {
        require(salt.size == SALT_BYTES && hash.size == HASH_BYTES)
    }

    fun matches(pin: String): Boolean = isValidPIN(pin) && MessageDigest.isEqual(hash, derive(pin, salt))

    companion object {
        private const val PIN_LENGTH = 4
        internal const val ITERATIONS = 100_000
        internal const val SALT_BYTES = 16
        internal const val HASH_BYTES = 32

        fun isValidPIN(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

        internal fun create(pin: String): WooPosPINHash {
            require(isValidPIN(pin))
            val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
            return WooPosPINHash(salt, derive(pin, salt))
        }

        private fun derive(pin: String, salt: ByteArray): ByteArray {
            val chars = pin.toCharArray()
            val spec = PBEKeySpec(chars, salt, ITERATIONS, HASH_BYTES * Byte.SIZE_BITS)
            return try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
                chars.fill('\u0000')
            }
        }
    }
}
