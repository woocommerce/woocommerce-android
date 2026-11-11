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
}
