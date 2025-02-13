package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.google.WCGoogleAdsCampaign
import org.wordpress.android.fluxc.model.google.WCGoogleAdsCampaignMapper
import org.wordpress.android.fluxc.model.google.WCGoogleAdsCardStats
import org.wordpress.android.fluxc.model.google.WCGoogleAdsPrograms
import org.wordpress.android.fluxc.model.google.WCGoogleAdsProgramsMapper
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.GoogleAdsTotalsDTO
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.WCGoogleAdsProgramsDTO
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.WCGoogleRestClient
import org.wordpress.android.fluxc.store.WCGoogleStore.TotalsType.SALES
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This store is intended to be used to support the Google Listings and Ads plugin.
 * https://wordpress.org/plugins/google-listings-and-ads/
 */
@Singleton
class WCGoogleStore @Inject constructor(
    private val restClient: WCGoogleRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val campaignMapper: WCGoogleAdsCampaignMapper,
    private val programsMapper: WCGoogleAdsProgramsMapper
) {
    /**
     * Checks the connection status of the Google Ads account used in the plugin.
     *
     * @return WooResult<Boolean> true if the account is connected, false otherwise. Optionally,
     * passes error, too.
     */
    suspend fun isGoogleAdsAccountConnected(site: SiteModel): WooResult<Boolean> =
        coroutineEngine.withDefaultContext(API, this, "fetchGoogleAdsConnectionStatus") {
            val response = restClient.fetchGoogleAdsConnectionStatus(site)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> response.asWooResult()
                else -> WooResult(false)
            }
        }

    /**
     * Fetches the Google Ads campaigns.
     *
     * @param excludeRemovedCampaigns Exclude removed (deleted) campaigns from being fetched.
     * @return WooResult<List<WCGoogleAdsCampaign>> a list of Google Ads campaigns. Optionally,
     * passes error, too.
     */
    suspend fun fetchGoogleAdsCampaigns(
        site: SiteModel,
        excludeRemovedCampaigns: Boolean = true
    ): WooResult<List<WCGoogleAdsCampaign>> =
        coroutineEngine.withDefaultContext(API, this, "fetchGoogleAdsCampaigns") {
            val response = restClient.fetchGoogleAdsCampaigns(site, excludeRemovedCampaigns)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val campaigns = response.result.map { campaignMapper.mapToModel(it) }
                    WooResult(campaigns)
                }

                else -> WooResult(emptyList())
            }
        }

    suspend fun fetchAllPrograms(
        site: SiteModel,
        startDate: String,
        endDate: String,
        totals: List<TotalsType>,
        orderBy: TotalsType = SALES
    ): WooResult<WCGoogleAdsPrograms?> =
        coroutineEngine.withDefaultContext(API, this, "fetchAllPrograms") {
            val pages: MutableList<WCGoogleAdsProgramsDTO> = mutableListOf()
            var lastReceivedError: WooError? = null
            var nextPageToken: String? = null
            var hasMorePages = true

            while (hasMorePages) {
                val page = restClient.fetchAllPrograms(
                    site = site,
                    startDate = startDate,
                    endDate = endDate,
                    fields = totals.joinToString(",") { it.parameterName },
                    orderBy = orderBy.parameterName,
                    nextPageToken = nextPageToken
                )

                page.takeIf { it.isError.not() }?.result?.let {
                    pages.add(it)
                    nextPageToken = it.nextPageToken
                    hasMorePages = it.nextPageToken?.isNotEmpty() ?: false
                } ?: run { lastReceivedError = page.error }
            }

            val response = lastReceivedError?.takeIf { pages.isEmpty() }?.let {
                WooPayload(error = it)
            } ?: pages.mergeData()

            when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(programsMapper.mapToModel(response.result))
                else -> WooResult(
                    WooError(WooErrorType.INVALID_RESPONSE, GenericErrorType.INVALID_RESPONSE)
                )
            }
        }

    suspend fun fetchImpressionsAndClicks(
        site: SiteModel,
        startDate: String,
        endDate: String
    ): WooResult<WCGoogleAdsCardStats> =
        coroutineEngine.withDefaultContext(API, this, "fetchImpressionsAndClicks") {
            val response = restClient.fetchImpressionsAndClicks(site, startDate, endDate)

            if (response.isError) {
                return@withDefaultContext WooResult(response.error)
            }

            val totals = response.result?.totals
            val clicks = totals?.clicks
            val impressions = totals?.impressions

            if (clicks != null && impressions != null) {
                WooResult(
                    WCGoogleAdsCardStats(
                        clicks = clicks,
                        impressions = impressions
                    )
                )
            } else {
                WooResult(
                    WooError(WooErrorType.INVALID_RESPONSE, GenericErrorType.INVALID_RESPONSE)
                )
            }
        }

    private fun List<WCGoogleAdsProgramsDTO>.mergeData(): WooPayload<WCGoogleAdsProgramsDTO> {
        return WCGoogleAdsProgramsDTO(
            campaigns = flatMap { it.campaigns.orEmpty() },
            intervals = flatMap { it.intervals.orEmpty() },
            totals = GoogleAdsTotalsDTO(
                sales = sumOf { it.totals?.sales ?: 0.0 },
                spend = sumOf { it.totals?.spend ?: 0.0 },
                clicks = sumOf { it.totals?.clicks ?: 0.0 },
                impressions = sumOf { it.totals?.impressions ?: 0.0 },
                conversions = sumOf { it.totals?.conversions ?: 0.0 }
            ),
            nextPageToken = null
        ).let { WooPayload(it) }
    }

    enum class TotalsType(val parameterName: String) {
        SALES("sales"),
        SPEND("spend"),
        CLICKS("clicks"),
        IMPRESSIONS("impressions"),
        CONVERSIONS("conversions")
    }
}
