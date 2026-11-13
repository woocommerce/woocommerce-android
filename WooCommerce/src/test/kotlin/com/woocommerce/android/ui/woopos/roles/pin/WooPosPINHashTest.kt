package com.woocommerce.android.ui.woopos.roles.pin

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class WooPosPINHashTest {
    @Test
    fun `when hashing the same PIN twice, then fresh salts produce distinct hashes`() {
        val first = WooPosPINHash.create("0123")
        val second = WooPosPINHash.create("0123")
        assertThat(first.salt).isNotEqualTo(second.salt)
        assertThat(first.hash).isNotEqualTo(second.hash)
        assertThat(first.matches("0123")).isTrue()
        assertThat(first.matches("9999")).isFalse()
    }

    @Test
    fun `given a known PBKDF2 SHA256 vector, when matching, then the derived hash agrees`() {
        val hash = WooPosPINHash(
            ByteArray(16) { it.toByte() },
            "869e6c8350c5beb0acc399fbaac3b60d220433896b26a647734d0d8f1586e1fa".chunked(2)
                .map { it.toInt(16).toByte() }.toByteArray()
        )
        assertThat(hash.matches("1234")).isTrue()
    }

    @Test
    fun `given invalid hash sizes, when constructing a hash, then they are rejected before derivation`() {
        assertThatThrownBy { WooPosPINHash(byteArrayOf(), ByteArray(32)) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { WooPosPINHash(ByteArray(16), byteArrayOf()) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
