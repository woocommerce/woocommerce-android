package org.wordpress.android.fluxc.store

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerFromAnalytics
import org.wordpress.android.fluxc.model.customer.WCCustomerFromAnalyticsMapper
import org.wordpress.android.fluxc.model.customer.WCCustomerMapper
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.NAME_ASC
import org.wordpress.android.fluxc.persistence.CustomerSqlUtils
import org.wordpress.android.fluxc.persistence.dao.CustomerFromAnalyticsDao
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCCustomerStore @Inject constructor(
    private val restClient: CustomerRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val mapper: WCCustomerMapper,
    private val customerFromAnalyticsDao: CustomerFromAnalyticsDao,
    private val customerFromAnalyticsMapper: WCCustomerFromAnalyticsMapper
) {
    /**
     * returns cached customers for the given site
     */
    fun getCustomersForSite(site: SiteModel) = CustomerSqlUtils.getCustomersForSite(site)

    /**
     * returns a cached customer with provided remote id or null if not in cache
     */
    fun getCustomerByRemoteId(site: SiteModel, remoteCustomerId: Long) =
        CustomerSqlUtils.getCustomerByRemoteId(site, remoteCustomerId)

    /**
     * returns a cached customers with provided remote ids or empty list if not in cache
     */
    fun getCustomerByRemoteIds(site: SiteModel, remoteCustomerId: List<Long>) =
        CustomerSqlUtils.getCustomerByRemoteIds(site, remoteCustomerId)

    fun saveCustomers(customers: List<WCCustomerModel>) {
        CustomerSqlUtils.insertCustomers(customers)
    }

    fun deleteCustomersForSite(site: SiteModel) {
        CustomerSqlUtils.deleteCustomersForSite(site)
    }

    /**
     * returns a customer with provided remote id
     */
    suspend fun fetchSingleCustomer(
        site: SiteModel,
        remoteCustomerId: Long
    ): WooResult<WCCustomerModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchSingleCustomer") {
            val response = restClient.fetchSingleCustomer(site, remoteCustomerId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val customer = mapper.mapToModel(site, response.result)
                    CustomerSqlUtils.insertOrUpdateCustomer(customer)
                    WooResult(customer)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    /**
     * returns customers from the backend
     */
    suspend fun fetchCustomers(
        site: SiteModel,
        pageSize: Int = DEFAULT_CUSTOMER_PAGE_SIZE,
        offset: Long = 0,
        sortType: CustomerSorting = DEFAULT_CUSTOMER_SORTING,
        searchQuery: String? = null,
        email: String? = null,
        role: String? = null,
        context: String? = null,
        remoteCustomerIds: List<Long>? = null,
        excludedCustomerIds: List<Long>? = null
    ): WooResult<List<WCCustomerModel>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchCustomers") {
            val response = restClient.fetchCustomers(
                site = site,
                pageSize = pageSize,
                sortType = sortType,
                offset = offset,
                searchQuery = searchQuery,
                email = email,
                role = role,
                context = context,
                remoteCustomerIds = remoteCustomerIds,
                excludedCustomerIds = excludedCustomerIds
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val customers = response.result.map { mapper.mapToModel(site, it) }

                    // clear cache if it's the first page for the site
                    if (offset == 0L) CustomerSqlUtils.deleteCustomersForSite(site)
                    CustomerSqlUtils.insertOrUpdateCustomers(customers)

                    WooResult(customers)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    /**
     * returns customers from the backend by id and store them
     */
    suspend fun fetchCustomersByIdsAndCache(
        site: SiteModel,
        pageSize: Int = DEFAULT_CUSTOMER_PAGE_SIZE,
        remoteCustomerIds: List<Long>
    ): WooResult<Unit> {
        suspend fun doFetch(
            site: SiteModel,
            pageSize: Int,
            remoteCustomerIds: List<Long>
        ): WooResult<Unit> {
            val response = restClient.fetchCustomers(
                site = site,
                pageSize = pageSize,
                remoteCustomerIds = remoteCustomerIds
            )
            return when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val customers = response.result.map { mapper.mapToModel(site, it) }
                    CustomerSqlUtils.insertOrUpdateCustomers(customers)
                    WooResult(Unit)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }

        return coroutineEngine.withDefaultContext(
            AppLog.T.API,
            this,
            "fetchCustomersByIdsAndCache"
        ) {
            val jobs = mutableListOf<Deferred<WooResult<Unit>>>()
            remoteCustomerIds.chunked(pageSize).forEach { idsToFetch ->
                jobs.add(async { doFetch(site, pageSize, idsToFetch) })
            }

            val results = jobs.awaitAll()
            val firstError = results.firstOrNull { it.error != null }
            if (firstError == null) WooResult(Unit)
            else WooResult(WooError(GENERIC_ERROR, UNKNOWN, firstError.error?.message))
        }
    }

    /**
     * creates a customer on the backend
     */
    suspend fun createCustomer(
        site: SiteModel,
        customer: WCCustomerModel
    ): WooResult<WCCustomerModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "createCustomer") {
            val response = restClient.createCustomer(site, mapper.mapToDTO(customer))
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(mapper.mapToModel(site, response.result))
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    /**
     * returns customers from analytics
     */
    suspend fun fetchCustomersFromAnalytics(
        site: SiteModel,
        page: Int,
        pageSize: Int = DEFAULT_CUSTOMER_PAGE_SIZE,
        sortType: CustomerSorting = DEFAULT_CUSTOMER_SORTING,
        searchQuery: String? = null,
        searchBy: String? = null,
        filterEmpty: List<String>? = null,
    ): WooResult<List<WCCustomerModel>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchCustomersFromAnalytics") {
            val response = restClient.fetchCustomersFromAnalytics(
                site = site,
                pageSize = pageSize,
                page = page,
                sortType = sortType,
                searchQuery = searchQuery,
                searchBy = searchBy,
                filterEmpty = filterEmpty,
            )
            when {
                response.isError -> {
                    AppLog.e(AppLog.T.API, "Error fetching customers from analytics: ${response.error.message}")
                    WooResult(response.error)
                }

                response.result != null -> {
                    // Save customer locally
                    response.result.map {
                        customerFromAnalyticsMapper.mapDTOToEntity(site, it)
                    }.let { entities ->
                        customerFromAnalyticsDao.insertCustomersFromAnalytics(entities)
                    }

                    WooResult(response.result.map { mapper.mapToModel(site, it) })
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    /**
     * returns analytics customer by user id
     */
    suspend fun fetchSingleCustomerFromAnalyticsByUserId(
        site: SiteModel,
        remoteCustomerId: Long
    ): WooResult<WCCustomerFromAnalytics> {
        return coroutineEngine.withDefaultContext(
            AppLog.T.API,
            this,
            "fetchSingleCustomerFromAnalyticsByUserId"
        ) {
            val response = restClient.fetchSingleCustomerFromAnalyticsByUserId(site, remoteCustomerId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val entity = customerFromAnalyticsMapper.mapDTOToEntity(site, response.result)
                    customerFromAnalyticsDao.insertCustomerFromAnalytics(entity)
                    val customer = customerFromAnalyticsMapper.mapEntityToModel(entity)
                    WooResult(customer)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    /**
     * returns analytics customer by analytic id
     */
    suspend fun fetchSingleCustomerFromAnalyticsByAnalyticsCustomerId(
        site: SiteModel,
        analyticsCustomerId: Long
    ): WooResult<WCCustomerFromAnalytics> {
        return coroutineEngine.withDefaultContext(
            AppLog.T.API,
            this,
            "fetchSingleCustomerFromAnalyticsByAnalyticsCustomerId"
        ) {
            val response = restClient.fetchSingleCustomerFromAnalyticsByCustomerId(site, analyticsCustomerId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val entity = customerFromAnalyticsMapper.mapDTOToEntity(site, response.result)
                    customerFromAnalyticsDao.insertCustomerFromAnalytics(entity)
                    val customer = customerFromAnalyticsMapper.mapEntityToModel(entity)
                    WooResult(customer)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun getCustomerFromAnalyticsByAnalyticsId(
        site: SiteModel,
        analyticCustomerId: Long
    ): WCCustomerFromAnalytics? {
        return customerFromAnalyticsDao.getCustomerByAnalyticCustomerId(
            site.localId(),
            analyticCustomerId
        )?.let {
            customerFromAnalyticsMapper.mapEntityToModel(it)
        }
    }


    companion object {
        private const val DEFAULT_CUSTOMER_PAGE_SIZE = 25
        private val DEFAULT_CUSTOMER_SORTING = NAME_ASC
    }
}
