package org.wordpress.android.fluxc.store.pos.localcatalog

data class WooPosGenerateCatalogResult(
    val scheduledAt: String? = null,
    val completedAt: String? = null,
    val state: WooPosGenerateCatalogState,
    val progress: Int? = null,
    val processed: Int? = null,
    val total: Int? = null,
    val url: String? = null,
)

enum class WooPosGenerateCatalogState(val rawValue: String) {
    SCHEDULED("scheduled"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    UNKNOWN("");

    companion object {
        fun from(value: String?): WooPosGenerateCatalogState = when (value) {
            "scheduled" -> SCHEDULED
            "in_progress" -> IN_PROGRESS
            "completed" -> COMPLETED
            else -> UNKNOWN
        }
    }
}
