package com.woocommerce.android.aiassistant.tools.customers

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.store.WCCustomerStore
import javax.inject.Inject

internal class AICustomersDataSource @Inject constructor(
    private val selectedSite: SelectedSite,
    private val customerStore: WCCustomerStore,
) {
    suspend fun fetchCustomers(
        search: String? = null,
        email: String? = null,
        include: List<Long>? = null,
        orderby: String = DEFAULT_ORDERBY,
        order: String = DEFAULT_ORDER,
        page: Int? = null,
        perPage: Int = PAGE_SIZE,
    ): Result<List<WCCustomerModel>> {
        val site = selectedSite.getOrNull()
            ?: return Result.failure(NoSelectedSiteException)

        val result = customerStore.fetchCustomers(
            site = site,
            search = search,
            email = email,
            include = include,
            orderby = orderby,
            order = order,
            page = page,
            perPage = perPage,
        )

        return if (result.isError) {
            Result.failure(OnChangedException(requireNotNull(result.error)))
        } else {
            Result.success(result.model.orEmpty())
        }
    }

    object NoSelectedSiteException : IllegalStateException("No selected site")

    private companion object {
        const val DEFAULT_ORDERBY = "registered_date"
        const val DEFAULT_ORDER = "desc"
        const val PAGE_SIZE = 20
    }
}
