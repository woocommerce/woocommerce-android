package com.woocommerce.android.ui.woopos.roles

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.nio.ByteBuffer

class WooPosPINCodecTest {
    private val codec = WooPosPINCodec()
    private val valid = codec.encode(mapOf(WooPosRole.ADMIN to WooPosPINHash.create("1234")))

    @Test
    fun `given invalid records, when decoding, then each is rejected`() {
        val malformed = listOf(
            byteArrayOf(),
            valid.copyOf(10),
            valid + byteArrayOf(0),
            valid.copyOf().apply { ByteBuffer.wrap(this).putInt(0, 99) },
            valid.copyOf().apply { ByteBuffer.wrap(this).putInt(4, 0) },
            valid.copyOf().apply { ByteBuffer.wrap(this).putInt(4, Int.MAX_VALUE) },
            valid.copyOf().apply { this[8] = 99 },
            valid.copyOf().apply { this[8] = 2 },
            valid.copyOf().apply { ByteBuffer.wrap(this).putInt(9, Int.MAX_VALUE) },
            valid.copyOf().apply { ByteBuffer.wrap(this).putInt(9, 0) },
            valid.copyOf().apply { ByteBuffer.wrap(this).putInt(9, -1) },
            valid.copyOf().apply { ByteBuffer.wrap(this).putInt(4, 2) } + valid.copyOfRange(8, valid.size),
            ByteArray(1024),
        )
        malformed.forEach { record ->
            assertThatThrownBy { codec.decode(record) }.isInstanceOf(Exception::class.java)
        }
    }

    @Test
    fun `given valid stored records, when decoded, then their hashes still verify`() {
        val decoded = codec.decode(valid)
        assertThat(decoded.getValue(WooPosRole.ADMIN).matches("1234")).isTrue()
        assertThat(decoded.getValue(WooPosRole.ADMIN).matches("5678")).isFalse()
    }

    @Test
    fun `given a version one record, when decoded and encoded, then role identifiers and bytes are preserved`() {
        // GIVEN
        val legacyRecord = (
            "0000000100000002" +
                "01000186a0" +
                "000102030405060708090a0b0c0d0e0f" +
                "101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f" +
                "02000186a0" +
                "303132333435363738393a3b3c3d3e3f" +
                "404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f"
            ).chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        // WHEN
        val decoded = codec.decode(legacyRecord)

        // THEN
        assertThat(decoded.keys).containsExactly(WooPosRole.ADMIN, WooPosRole.CASHIER)
        assertThat(decoded.getValue(WooPosRole.ADMIN).hash).containsExactly(*ByteArray(32) { (it + 0x10).toByte() })
        assertThat(decoded.getValue(WooPosRole.CASHIER).hash).containsExactly(*ByteArray(32) { (it + 0x40).toByte() })
        assertThat(codec.encode(decoded)).containsExactly(*legacyRecord)
    }
}
