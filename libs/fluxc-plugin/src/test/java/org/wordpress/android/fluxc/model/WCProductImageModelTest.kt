package org.wordpress.android.fluxc.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WCProductImageModelTest {
    @Test
    fun `when the alt text is known, then the json includes it`() {
        val sut = WCProductImageModel(1L).apply {
            alt = "alt text"
        }

        val result = sut.toJson()

        assertThat(result.get("alt").asString).isEqualTo("alt text")
    }

    @Test
    fun `when the alt text is unknown, then the json leaves it out`() {
        val sut = WCProductImageModel(1L)

        val result = sut.toJson()

        assertThat(result.has("alt")).isFalse
    }
}
