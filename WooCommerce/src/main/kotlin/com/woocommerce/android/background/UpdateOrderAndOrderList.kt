package com.woocommerce.android.background

import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class UpdateOrderAndOrderList @Inject constructor(
    private val updateOrdersListByStoreId: UpdateOrdersListByStoreId,
    private val orderStore: WCOrderStore,
    private val siteStore: SiteStore
) {
    suspend operator fun invoke(siteId: Long, remoteOrderId: Long): Result<Unit> {
        val orderFetchedSuccess = siteStore.getSiteBySiteId(siteId)?.let { site ->
            orderStore.fetchSingleOrderSync(site, remoteOrderId)
        } ?: WooResult(
            WooError(
                WooErrorType.GENERIC_ERROR,
                BaseRequest.GenericErrorType.NOT_FOUND,
                "Site not found"
            )
        )
        val listFetchedSuccess = updateOrdersListByStoreId(siteId)
        val classSimpleName = this::class.java.simpleName
        return when {
            orderFetchedSuccess.isError && listFetchedSuccess.isFailure -> {
                Result.failure(
                    MultipleErrorsException(
                        listOf(
                            Exception(
                                "$classSimpleName ${orderFetchedSuccess.error.message}"
                            ),
                            Exception(
                                "$classSimpleName ${listFetchedSuccess.exceptionOrNull()?.message}"
                            )
                        )
                    )
                )
            }

            orderFetchedSuccess.isError -> Result.failure(
                Exception("$classSimpleName ${orderFetchedSuccess.error.message}")
            )

            listFetchedSuccess.isFailure -> Result.failure(
                Exception("$classSimpleName ${listFetchedSuccess.exceptionOrNull()?.message}")
            )
            else -> Result.success(Unit)
        }
    }
}
