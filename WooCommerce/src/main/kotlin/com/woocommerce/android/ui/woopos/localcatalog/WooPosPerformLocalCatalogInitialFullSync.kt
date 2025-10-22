package com.woocommerce.android.ui.woopos.localcatalog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class WooPosPerformLocalCatalogInitialFullSync @Inject constructor(
    private val syncStatusChecker: WooPosFullSyncStatusChecker,
    private val performFullSync: WooPosPerformInstantCatalogFullSync,
) {
    operator fun invoke(): Flow<WooPosLocalCatalogInitialFullSyncState> = flow {
        val requirement = syncStatusChecker.checkSyncRequirement()

        when (requirement) {
            is WooPosFullSyncRequirement.NotRequired,
            is WooPosFullSyncRequirement.Overdue -> {
                emit(WooPosLocalCatalogInitialFullSyncState.NotRequired)
            }
            is WooPosFullSyncRequirement.BlockingRequired -> {
                performFullSync().collect { syncStatus ->
                    when (syncStatus) {
                        is WooPosFullSyncState.InProgress -> {
                            emit(WooPosLocalCatalogInitialFullSyncState.Syncing)
                        }
                        is WooPosFullSyncState.Success -> {
                            emit(WooPosLocalCatalogInitialFullSyncState.Completed)
                        }
                        is WooPosFullSyncState.Failed -> {
                            emit(WooPosLocalCatalogInitialFullSyncState.Failed(syncStatus.error))
                        }
                    }
                }
            }
            is WooPosFullSyncRequirement.Error -> {
                emit(WooPosLocalCatalogInitialFullSyncState.Failed(requirement.message))
            }

            is WooPosFullSyncRequirement.LocalCatalogDisabled -> {
                WooPosLocalCatalogInitialFullSyncState.LocalCatalogDisabled
            }
        }
    }
}

sealed class WooPosLocalCatalogInitialFullSyncState {
    data object NotRequired : WooPosLocalCatalogInitialFullSyncState()
    data object Syncing : WooPosLocalCatalogInitialFullSyncState()
    data object Completed : WooPosLocalCatalogInitialFullSyncState()
    data class Failed(val error: String) : WooPosLocalCatalogInitialFullSyncState()
    data object LocalCatalogDisabled : WooPosLocalCatalogInitialFullSyncState()
}
