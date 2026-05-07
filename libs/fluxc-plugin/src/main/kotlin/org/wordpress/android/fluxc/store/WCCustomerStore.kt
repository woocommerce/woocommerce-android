package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
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
import org.wordpress.android.fluxc.persistence.dao.CustomerDao
import org.wordpress.android.fluxc.persistence.dao.CustomerFromAnalyticsDao
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCCustomerStore @Inject internal constructor(
    private val restClient: CustomerRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val mapper: WCCustomerMapper,
    private val customerDao: CustomerDao,
    private val customerFromAnalyticsDao: CustomerFromAnalyticsDao,
    private val customerFromAnalyticsMapper: WCCustomerFromAnalyticsMapper
) {
    /**
     * returns cached customers for the given site
     */
    suspend fun getCustomersForSite(site: SiteModel) = customerDao.getCustomersForSite(LocalId(site.id))

    /**
     * returns a cached customer with provided remote id or null if not in cache
     */
    suspend fun getCustomerByRemoteId(site: SiteModel, remoteCustomerId: Long) =
        customerDao.getCustomerByRemoteId(LocalId(site.id), RemoteId(remoteCustomerId))

    suspend fun saveCustomers(customers: List<WCCustomerModel>) {
        customerDao.upsertCustomers(customers)
    }

    suspend fun deleteCustomersForSite(site: SiteModel) {
        customerDao.deleteCustomersForSite(LocalId(site.id))
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
                    customerDao.upsertCustomer(customer)
                    WooResult(customer)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun fetchCustomers(
        site: SiteModel,
        search: String? = null,
        email: String? = null,
        include: List<Long>? = null,
        orderby: String = "registered_date",
        order: String = "desc",
        page: Int? = null,
        perPage: Int = 20,
    ): WooResult<List<WCCustomerModel>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchCustomers") {
            val response = restClient.fetchCustomers(
                site = site,
                search = search,
                email = email,
                include = include,
                orderby = orderby,
                order = order,
                page = page,
                perPage = perPage,
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val customers = response.result.map { mapper.mapToModel(site, it) }
                    customerDao.upsertCustomers(customers)
                    WooResult(customers)
                }

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
