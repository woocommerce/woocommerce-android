package com.woocommerce.android.ui.google

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.google.WCGoogleAdsCampaign
import javax.inject.Inject

class ObserveMostRecentGoogleAdsCampaign @Inject constructor(
    private val googleRepository: GoogleRepository
) {
    operator fun invoke(): Flow<Result<WCGoogleAdsCampaign?>> = flow {
        googleRepository.fetchGoogleAdsCampaigns()
            .onFailure { exception ->
                emit(Result.failure(exception))
            }
            .onSuccess { campaigns ->
                // Assume that the campaign with the largest ID is the most recent
                val campaignWithLargestId = campaigns.maxByOrNull { it.id ?: Long.MIN_VALUE }
                emit(Result.success(campaignWithLargestId))
            }
    }
}
