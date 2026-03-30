package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderattribution.ChannelSummaryApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderattribution.OrderAttributionRestClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCOrderAttributionStore @Inject constructor(
    private val restClient: OrderAttributionRestClient
) {
    suspend fun fetchChannelSummary(
        site: SiteModel,
        from: String,
        to: String,
        compareFrom: String,
        compareTo: String
    ): Result<List<ChannelSummaryApiResponse>> {
        val response = restClient.fetchChannelSummary(
            site = site,
            from = from,
            to = to,
            compareFrom = compareFrom,
            compareTo = compareTo
        )

        return if (response.isError) {
            Result.failure(
                Exception(response.error?.message ?: "Failed to fetch channel summary")
            )
        } else {
            val channels = response.result?.data.orEmpty().map {
                ChannelSummaryApiResponse.fromApiItem(it)
            }
            Result.success(channels)
        }
    }
}
