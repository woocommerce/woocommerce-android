package org.wordpress.android.fluxc.model

data class WCGiftCardStats(
    val usedValue: Long,
    val netValue: Double,
    val intervals: List<WCGiftCardStatsInterval>
)

data class WCGiftCardStatsInterval(
    val usedValue: Long,
    val netValue: Double,
)
