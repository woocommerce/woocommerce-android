package com.woocommerce.android.ui.woopos.roles

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.nio.ByteBuffer

class WooPosLegacyPINDecoderTest {
    private val decoder = WooPosLegacyPINDecoder()

    @Test
    fun `given a legacy record, when decoded, then both stored PINs authenticate`() {
        // WHEN
        val decoded = decoder.decode(legacyPINRecord())

        // THEN
        assertThat(decoded.keys).containsExactly(WooPosRole.ADMIN, WooPosRole.CASHIER)
        assertThat(decoded.getValue(WooPosRole.ADMIN).matches("1234")).isTrue()
        assertThat(decoded.getValue(WooPosRole.CASHIER).matches("5678")).isTrue()
    }

    @Test
    fun `given invalid legacy records, when decoding, then each is rejected`() {
        // GIVEN
        val legacy = legacyPINRecord()
        val malformed = listOf(
            legacy.copyOf(10),
            legacy + byteArrayOf(0),
            legacy.copyOf().apply { ByteBuffer.wrap(this).putInt(0, 99) },
            legacy.copyOf().apply { ByteBuffer.wrap(this).putInt(4, 0) },
            legacy.copyOf().apply { ByteBuffer.wrap(this).putInt(4, Int.MAX_VALUE) },
            legacy.copyOf().apply { this[8] = 99 },
            legacy.copyOf().apply { this[8] = 2 },
            legacy.copyOf().apply { ByteBuffer.wrap(this).putInt(9, Int.MAX_VALUE) },
            legacy.copyOf().apply { ByteBuffer.wrap(this).putInt(9, 0) },
            legacy.copyOf().apply { ByteBuffer.wrap(this).putInt(9, -1) },
            legacy + ByteArray(128),
        )

        // THEN
        malformed.forEach { bytes ->
            assertThatThrownBy { decoder.decode(bytes) }.isInstanceOf(Exception::class.java)
        }
    }
}
