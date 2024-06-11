package com.woocommerce.android.ui.products.ai

import com.woocommerce.android.R
import com.woocommerce.android.extensions.capitalize
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.ai.ProductPreviewSubViewModel.ProductPropertyCard
import com.woocommerce.android.ui.products.details.ProductDetailCardBuilder
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

/**
 * This is a simplified version of [ProductDetailCardBuilder], we decided to create it instead of reusing the other
 * class because our usage for now is quite simple, and [ProductDetailCardBuilder] is tightly coupled with the Product
 * Detail ViewModel.
 */
class BuildProductPreviewProperties @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val parameterRepository: ParameterRepository,
) {
    private val siteParameters by lazy { parameterRepository.getParameters() }

    operator fun invoke(product: Product): List<List<ProductPropertyCard>> = buildList {
        add(
            listOf(product.productType())
        )

        add(
            buildList {
                add(product.price())

                add(product.inventory())

                if (product.categories.isNotEmpty()) {
                    add(product.categories())
                }

                if (product.tags.isNotEmpty()) {
                    add(product.tags())
                }

                add(product.shipping())
            }
        )
    }

    private fun Product.productType(): ProductPropertyCard {
        fun Product.productTypeDisplayName(): String {
            return when (productType) {
                ProductType.SIMPLE -> {
                    when {
                        this.isVirtual -> resourceProvider.getString(R.string.product_type_virtual)
                        this.isDownloadable -> resourceProvider.getString(R.string.product_type_downloadable)
                        else -> resourceProvider.getString(R.string.product_type_physical)
                    }
                }

                ProductType.VARIABLE -> resourceProvider.getString(R.string.product_type_variable)
                ProductType.GROUPED -> resourceProvider.getString(R.string.product_type_grouped)
                ProductType.EXTERNAL -> resourceProvider.getString(R.string.product_type_external)
                ProductType.SUBSCRIPTION -> resourceProvider.getString(R.string.product_type_subscription)
                else -> this.type.capitalize() // show the actual product type R.string for unsupported products
            }
        }

        return ProductPropertyCard(
            icon = R.drawable.ic_gridicons_product,
            title = R.string.product_type,
            content = resourceProvider.getString(
                R.string.product_detail_product_type_hint,
                productTypeDisplayName()
            )
        )
    }

    private fun Product.price() = ProductPropertyCard(
        icon = R.drawable.ic_gridicons_money,
        title = R.string.product_price,
        content = PriceUtils.formatCurrency(
            regularPrice,
            siteParameters.currencyCode,
            currencyFormatter
        )
    )

    @Suppress("UnusedReceiverParameter")
    private fun Product.inventory() = ProductPropertyCard(
        icon = R.drawable.ic_gridicons_list_checkmark,
        title = R.string.product_inventory,
        content = resourceProvider.getString(R.string.product_stock_status_instock)
    )

    private fun Product.categories() = ProductPropertyCard(
        icon = R.drawable.ic_gridicons_folder,
        title = R.string.product_categories,
        content = categories.joinToString(transform = { it.name })
    )

    private fun Product.tags() = ProductPropertyCard(
        icon = R.drawable.ic_gridicons_tag,
        title = R.string.product_tags,
        content = tags.joinToString(transform = { it.name })
    )

    private fun Product.shipping(): ProductPropertyCard {
        val weightFormatted = getWeightWithUnits(siteParameters.weightUnit)
        val dimensionsFormatted = getSizeWithUnits(siteParameters.dimensionUnit)

        return ProductPropertyCard(
            icon = R.drawable.ic_gridicons_shipping,
            title = R.string.product_shipping,
            content = """
                ${resourceProvider.getString(R.string.product_weight)}: $weightFormatted
                ${resourceProvider.getString(R.string.product_dimensions)}: $dimensionsFormatted
                """
                .trimIndent()
        )
    }
}
