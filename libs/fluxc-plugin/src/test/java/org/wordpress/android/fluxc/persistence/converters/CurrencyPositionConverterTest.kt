package org.wordpress.android.fluxc.persistence.converters

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.wordpress.android.fluxc.model.settings.CurrencyPosition

class CurrencyPositionConverterTest {
    @Test
    fun `when the db-stored value is invalid, then it returns default value`() {
        val converter = CurrencyPositionConverter(logger = mock())
        val invalidValue = "invalid_currency_position"

        val result = converter.toCurrencyPosition(invalidValue)

        assertThat(result).isEqualTo(CurrencyPosition.LEFT)
    }
}
