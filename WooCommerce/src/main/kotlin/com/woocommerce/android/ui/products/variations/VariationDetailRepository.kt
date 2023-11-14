package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_LOADED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_UPDATE_ERROR
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_UPDATE_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.SubscriptionProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.ui.products.models.QuantityRulesMapper
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnVariationChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnVariationUpdated
import javax.inject.Inject

class VariationDetailRepository @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val quantityRulesMapper: QuantityRulesMapper
) {
    suspend fun fetchVariation(remoteProductId: Long, remoteVariationId: Long): OnVariationChanged {
        return productStore.fetchSingleVariation(
            selectedSite.get(),
            remoteProductId,
            remoteVariationId
        ).also {
            if (!it.isError) {
                AnalyticsTracker.track(PRODUCT_VARIATION_LOADED)
            }
        }
    }

    /**
     * Fires the request to update the variation
     *
     * @return the result of the action as [OnVariationUpdated]
     */
    suspend fun updateVariation(updatedVariation: ProductVariation): OnVariationUpdated {
        val variation = updatedVariation.toDataModel(
            getCachedWCVariation(
                updatedVariation.remoteProductId,
                updatedVariation.remoteVariationId
            )
        )
        val payload = WCProductStore.UpdateVariationPayload(selectedSite.get(), variation)

        return productStore.updateVariation(payload)
            .also {
                if (it.isError) {
                    AnalyticsTracker.track(
                        PRODUCT_VARIATION_UPDATE_ERROR,
                        mapOf(
                            AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                            AnalyticsTracker.KEY_ERROR_TYPE to it.error?.type?.toString(),
                            AnalyticsTracker.KEY_ERROR_DESC to it.error?.message
                        )
                    )
                } else {
                    AnalyticsTracker.track(PRODUCT_VARIATION_UPDATE_SUCCESS)
                }
            }
    }

    /**
     * Fires the request to delete a variation
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun deleteVariation(productID: Long, variationID: Long) =
        productStore
            .deleteVariation(selectedSite.get(), productID, variationID)
            .model?.let { true }
            ?: false

    private fun getCachedWCVariation(remoteProductId: Long, remoteVariationId: Long): WCProductVariationModel? =
        productStore.getVariationByRemoteId(selectedSite.get(), remoteProductId, remoteVariationId)

    suspend fun getVariation(remoteProductId: Long, remoteVariationId: Long): ProductVariation? =
        withContext(coroutineDispatchers.io) {
            getCachedWCVariation(remoteProductId, remoteVariationId)?.toAppModel()
        }

    private suspend fun getSubscriptionProductVariation(
        remoteProductId: Long,
        remoteVariationId: Long
    ): SubscriptionProductVariation? {
        return withContext(coroutineDispatchers.io) {
            productStore.getVariationByRemoteId(selectedSite.get(), remoteProductId, remoteVariationId)?.let { model ->
                SubscriptionProductVariation(model)
            }
        }
    }

    suspend fun getVariationByProductType(remoteProductId: Long, remoteVariationId: Long): ProductVariation? {
        return withContext(coroutineDispatchers.io) {
            val productType = productStore.getProductByRemoteId(selectedSite.get(), remoteProductId).let { model ->
                ProductType.fromString(model?.type ?: "")
            }
            when (productType) {
                ProductType.VARIABLE_SUBSCRIPTION -> getSubscriptionProductVariation(remoteProductId, remoteVariationId)
                else -> getVariation(remoteProductId, remoteVariationId)
            }
        }
    }

    suspend fun getQuantityRules(remoteProductId: Long, remoteVariationId: Long): QuantityRules? {
        return withContext(coroutineDispatchers.io) {
            getCachedWCVariation(remoteProductId, remoteVariationId)?.metadata
                ?.let { quantityRulesMapper.toAppModelFromVariationMetadata(it) }
        }
    }
}
