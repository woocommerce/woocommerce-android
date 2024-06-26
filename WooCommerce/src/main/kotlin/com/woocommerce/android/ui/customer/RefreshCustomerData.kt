package com.woocommerce.android.ui.customer

import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class RefreshCustomerData @Inject constructor(
    private val customerRepository: CustomerListRepository
) {
    suspend operator fun invoke(remoteCustomerId: Long, analyticsCustomerId: Long?) = coroutineScope {
        val customerDeferred = if (remoteCustomerId != 0L) {
            async { customerRepository.fetchCustomerByRemoteId(remoteCustomerId) }
        } else {
            null
        }
        val customerAnalyticDeferred = when {
            analyticsCustomerId != null -> {
                async { customerRepository.fetchCustomerFromAnalyticsByAnalyticsCustomerId(analyticsCustomerId).model }
            }

            remoteCustomerId != 0L -> {
                async { customerRepository.fetchCustomerFromAnalyticsByUserId(remoteCustomerId).model }
            }

            else -> null
        }
        customerDeferred?.await()
        customerAnalyticDeferred?.await()
    }
}
