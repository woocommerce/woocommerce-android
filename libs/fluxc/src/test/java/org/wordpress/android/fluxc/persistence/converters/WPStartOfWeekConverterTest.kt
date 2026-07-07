package org.wordpress.android.fluxc.persistence.converters

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.DayOfWeek

class WPStartOfWeekConverterTest {
    private val converter = WPStartOfWeekConverter()

    @Test
    fun `when reading wp start of week values, then explicit wordpress mapping is used`() {
        assertThat(converter.fromWPStartOfWeek(0)).isEqualTo(DayOfWeek.SUNDAY)
        assertThat(converter.fromWPStartOfWeek(1)).isEqualTo(DayOfWeek.MONDAY)
        assertThat(converter.fromWPStartOfWeek(2)).isEqualTo(DayOfWeek.TUESDAY)
        assertThat(converter.fromWPStartOfWeek(3)).isEqualTo(DayOfWeek.WEDNESDAY)
        assertThat(converter.fromWPStartOfWeek(4)).isEqualTo(DayOfWeek.THURSDAY)
        assertThat(converter.fromWPStartOfWeek(5)).isEqualTo(DayOfWeek.FRIDAY)
        assertThat(converter.fromWPStartOfWeek(6)).isEqualTo(DayOfWeek.SATURDAY)
    }

    @Test
    fun `when reading invalid wp start of week values, then null is returned`() {
        assertThat(converter.fromWPStartOfWeek(null)).isNull()
        assertThat(converter.fromWPStartOfWeek(-1)).isNull()
        assertThat(converter.fromWPStartOfWeek(7)).isNull()
    }

    @Test
    fun `when writing start of week values, then explicit wordpress mapping is used`() {
        assertThat(converter.toWPStartOfWeek(DayOfWeek.SUNDAY)).isEqualTo(0)
        assertThat(converter.toWPStartOfWeek(DayOfWeek.MONDAY)).isEqualTo(1)
        assertThat(converter.toWPStartOfWeek(DayOfWeek.TUESDAY)).isEqualTo(2)
        assertThat(converter.toWPStartOfWeek(DayOfWeek.WEDNESDAY)).isEqualTo(3)
        assertThat(converter.toWPStartOfWeek(DayOfWeek.THURSDAY)).isEqualTo(4)
        assertThat(converter.toWPStartOfWeek(DayOfWeek.FRIDAY)).isEqualTo(5)
        assertThat(converter.toWPStartOfWeek(DayOfWeek.SATURDAY)).isEqualTo(6)
        assertThat(converter.toWPStartOfWeek(null)).isNull()
    }
}
