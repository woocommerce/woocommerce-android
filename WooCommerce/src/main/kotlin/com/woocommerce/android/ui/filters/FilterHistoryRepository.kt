package com.woocommerce.android.ui.filters

import android.os.Parcelable
import com.woocommerce.android.tools.SelectedSite
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.dao.FilterHistoryDao
import org.wordpress.android.fluxc.persistence.entity.FilterHistoryEntity
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import javax.inject.Inject

/**
 * Per-site store for "filter history" — the previously applied filter selections on the order and
 * product lists, gated by [com.woocommerce.android.util.FeatureFlag.FILTER_HISTORY].
 *
 * History is unlimited, deduped and newest-first (matching iOS): [save] upserts by [SavedFilter.payload],
 * so re-saving an identical selection bumps it to the top instead of creating a duplicate.
 *
 * Callers own the [SavedFilter.payload] contract: it must be a canonical, normalized serialization of
 * the active (non-empty) selection so that logically-identical filters dedup reliably across app
 * versions. The per-surface encoders/decoders live with the order and product filter screens.
 */
@Reusable
class FilterHistoryRepository @Inject constructor(
    private val filterHistoryDao: FilterHistoryDao,
    private val selectedSite: SelectedSite,
    private val currentTimeProvider: CurrentTimeProvider
) {
    fun observeHistory(type: FilterHistoryType): Flow<List<SavedFilter>> =
        filterHistoryDao.observeForSite(currentSiteId(), type.name)
            .map { entities -> entities.map { it.toSavedFilter() } }

    suspend fun save(
        type: FilterHistoryType,
        payload: String,
        readableString: String
    ) {
        filterHistoryDao.insertOrReplace(
            FilterHistoryEntity(
                localSiteId = currentSiteId(),
                filterType = type.name,
                payload = payload,
                readableString = readableString,
                dateModified = currentTimeProvider.currentDate().time
            )
        )
    }

    suspend fun remove(type: FilterHistoryType, filter: SavedFilter) {
        filterHistoryDao.delete(currentSiteId(), type.name, filter.payload)
    }

    suspend fun clear(type: FilterHistoryType) {
        filterHistoryDao.clear(currentSiteId(), type.name)
    }

    private fun currentSiteId() = LocalId(selectedSite.get().id)

    private fun FilterHistoryEntity.toSavedFilter() = SavedFilter(
        readableString = readableString,
        payload = payload
    )
}

/**
 * Which list a filter history entry belongs to. Persisted as the entity's `filterType` column.
 */
enum class FilterHistoryType {
    ORDERS,
    PRODUCTS
}

/**
 * A single persisted filter history entry.
 *
 * @param readableString human-readable summary of the filter, shown in the history list.
 * @param payload canonical serialization of the selection; also the identity used to
 * [FilterHistoryRepository.remove] a specific entry (unique per site and filter type).
 */
@Parcelize
data class SavedFilter(
    val readableString: String,
    val payload: String
) : Parcelable
