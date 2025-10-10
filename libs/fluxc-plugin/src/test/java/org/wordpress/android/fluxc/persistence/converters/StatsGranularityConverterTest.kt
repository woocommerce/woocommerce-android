package org.wordpress.android.fluxc.persistence.converters

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.wordpress.android.fluxc.store.WCStatsStore

class StatsGranularityConverterTest {
    @Test
    fun `when the db-stored value is invalid, then it returns default value`() {
        val converter = StatsGranularityConverter(logger = mock())
        val invalidValue = "invalid_stats_granularity"

        val result = converter.toStatsGranularity(invalidValue)

        assertThat(result).isEqualTo(WCStatsStore.StatsGranularity.HOURS)
    }
}
