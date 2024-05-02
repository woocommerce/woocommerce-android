package com.woocommerce.android.ui.products

import android.os.Parcelable
import com.woocommerce.android.model.Component
import com.woocommerce.android.model.QueryType
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class GetComponentOptions @Inject constructor(
    private val getProductsByIds: GetProductsByIds,
    private val getCategoriesByIds: GetCategoriesByIds,
    private val repository: ProductDetailRepository,
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(component: Component): ComponentOptions {
        return withContext(dispatchers.io) {
            val options = when (component.queryType) {
                QueryType.CATEGORY -> getCategoriesOptions(component.queryIds)
                QueryType.PRODUCT -> getProductOptions(component.queryIds)
            }
            val default = getDefaultValue(component.defaultOptionId)

            ComponentOptions(
                type = component.queryType,
                options = options,
                default = default
            )
        }
    }

    private suspend fun getDefaultValue(remoteProductId: Long?): String? {
        return remoteProductId?.let { repository.fetchProductOrLoadFromCache(it)?.name }
    }

    private suspend fun getCategoriesOptions(categoriesIds: List<Long>): List<ComponentOption> {
        return getCategoriesByIds(categoriesIds).map { category ->
            ComponentOption(
                id = category.remoteCategoryId,
                title = category.name,
                shouldDisplayImage = false
            )
        }
    }

    private suspend fun getProductOptions(productsIds: List<Long>): List<ComponentOption> {
        return getProductsByIds(productsIds).map { product ->
            ComponentOption(
                id = product.remoteId,
                title = product.name,
                shouldDisplayImage = true,
                imageUrl = product.firstImageUrl
            )
        }
    }
}

@Parcelize
data class ComponentOption(
    val id: Long,
    val title: String,
    val shouldDisplayImage: Boolean = false,
    val imageUrl: String? = null,
) : Parcelable

@Parcelize
data class ComponentOptions(
    val type: QueryType,
    val options: List<ComponentOption>,
    val default: String?
) : Parcelable
