package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductTaxStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
@Suppress("MagicNumber", "TooGenericExceptionCaught")
class WooPosSearchProductsMockedDataSource @Inject constructor() {
    private var productCache: List<Product> = generateSampleProducts()
    private val filteredProductCache: MutableMap<String, List<Product>> = mutableMapOf()
    private val canLoadMore = AtomicBoolean(true)

    val hasMorePages: Boolean
        get() = canLoadMore.get()

    fun searchProducts(query: String, forceRefresh: Boolean = false): Flow<ProductsResult> = flow {
        if (forceRefresh) {
            updateFilteredProductCache(query, emptyList())
        }

        val cachedResults = getCachedSearchResults(query)
        emit(ProductsResult.Cached(cachedResults))

        delay(1500)

        if (Random.nextInt(3) == 0) {
            emit(ProductsResult.Remote(Result.failure(Exception("Failed to search products"))))
        } else {
            val remoteResults = performSearch(query, true)
            updateFilteredProductCache(query, remoteResults)

            canLoadMore.set(remoteResults.size >= PAGE_SIZE)

            emit(ProductsResult.Remote(Result.success(remoteResults)))
        }
    }.flowOn(Dispatchers.IO).take(2)

    suspend fun loadMore(query: String): Result<List<Product>> = withContext(Dispatchers.IO) {
        delay(1500)

        if (Random.nextInt(100) <= 30) {
            return@withContext Result.failure(Exception("Failed to load more products"))
        }

        val currentResults = filteredProductCache[query.lowercase()] ?: emptyList()
        val moreResults = performSearch(query, true, currentResults.size)
        val combinedResults = currentResults + moreResults
        updateFilteredProductCache(query, combinedResults)

        canLoadMore.set(moreResults.size >= PAGE_SIZE)

        Result.success(combinedResults)
    }

    fun getProductById(productId: Long): Product? {
        return productCache.find { it.remoteId == productId }
    }

    private fun getCachedSearchResults(query: String): List<Product> {
        return filteredProductCache[query.lowercase()] ?: run {
            val results = performSearch(query, false)
            updateFilteredProductCache(query, results)
            results
        }
    }

    private fun performSearch(query: String, isRemote: Boolean = false, offset: Int = 0): List<Product> {
        val searchTerm = query.trim().lowercase()
        if (searchTerm.isEmpty()) return productCache

        val sourceList = productCache

        val filteredResults = sourceList.filter { product ->
            product.name.lowercase().contains(searchTerm) ||
                product.sku.lowercase().contains(searchTerm) ||
                product.description.lowercase().contains(searchTerm)
        }

        return if (isRemote) {
            if (offset == 0) {
                filteredResults.take(PAGE_SIZE)
            } else {
                val startIndex = offset
                val endIndex = minOf(startIndex + PAGE_SIZE, filteredResults.size)

                if (startIndex < filteredResults.size) {
                    filteredResults.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }
            }
        } else {
            filteredResults
        }
    }

    private fun updateFilteredProductCache(query: String, results: List<Product>) {
        filteredProductCache[query.lowercase()] = results
    }

    private fun generateSampleProducts(): List<Product> {
        return List(1000) { index -> createSampleProduct(index.toLong() + 1) }
    }

    @Suppress("LongMethod")
    private fun createSampleProduct(id: Long): Product {
        val productTypes = listOf(
            ProductType.SIMPLE.value,
            ProductType.VARIABLE.value,
            ProductType.GROUPED.value
        )
        val categories = listOf(
            ProductCategory(1, "Electronics", "electronics"),
            ProductCategory(2, "Clothing", "clothing"),
            ProductCategory(3, "Food", "food"),
            ProductCategory(4, "Books", "books"),
            ProductCategory(5, "Toys", "toys")
        )
        val tags = listOf(
            ProductTag(1, "Featured", "featured"),
            ProductTag(2, "Sale", "sale"),
            ProductTag(3, "New", "new"),
            ProductTag(4, "Popular", "popular")
        )

        val productType = productTypes.random()
        val productName = getRandomProductName(id)
        val price = BigDecimal(Random.nextDouble(5.0, 500.0)).setScale(2, RoundingMode.HALF_DOWN)

        val stockStatuses = listOf(
            ProductStockStatus.InStock,
            ProductStockStatus.OutOfStock,
            ProductStockStatus.OnBackorder
        )

        val backorderStatuses = listOf(
            ProductBackorderStatus.No,
            ProductBackorderStatus.Yes,
            ProductBackorderStatus.Notify
        )

        return Product(
            remoteId = id,
            parentId = 0,
            name = productName,
            description = "This is a detailed description for $productName. Perfect for all your needs.",
            shortDescription = "Short description for $productName",
            slug = productName.lowercase().replace(" ", "-"),
            type = productType,
            status = ProductStatus.PUBLISH,
            catalogVisibility = ProductCatalogVisibility.VISIBLE,
            isFeatured = Random.nextBoolean(),
            stockStatus = stockStatuses.random(),
            backorderStatus = backorderStatuses.random(),
            dateCreated = Date(),
            firstImageUrl = getProductImageUrl(id, productName),
            totalSales = Random.nextLong(0, 1000),
            reviewsAllowed = true,
            isVirtual = Random.nextBoolean(),
            ratingCount = Random.nextInt(0, 100),
            averageRating = Random.nextFloat() * 5,
            permalink = "https://example.com/product/$id",
            externalUrl = "",
            buttonText = "",
            price = price,
            salePrice = if (Random.nextBoolean()) price.multiply(BigDecimal("0.8")) else null,
            regularPrice = price,
            taxClass = "standard",
            isStockManaged = Random.nextBoolean(),
            stockQuantity = Random.nextDouble(1.0, 100.0),
            sku = "SKU-${id.toString().padStart(4, '0')}",
            globalUniqueId = "",
            shippingClass = "",
            shippingClassId = 0,
            isDownloadable = false,
            downloads = emptyList(),
            downloadLimit = 0,
            downloadExpiry = 0,
            purchaseNote = "",
            numVariations = if (productType == ProductType.VARIABLE.value) Random.nextInt(1, 5) else 0,
            images = listOf(
                Product.Image(
                    id = id,
                    name = "Product image",
                    source = getProductImageUrl(id, productName),
                    dateCreated = Date(),
                    isCoverImage = true
                )
            ),
            attributes = emptyList(),
            saleEndDateGmt = null,
            saleStartDateGmt = null,
            isSoldIndividually = false,
            taxStatus = ProductTaxStatus.Taxable,
            isSaleScheduled = false,
            isPurchasable = true,
            menuOrder = 0,
            categories = listOf(categories.random()),
            tags = if (Random.nextBoolean()) {
                listOf(tags.random())
            } else {
                emptyList()
            },
            groupedProductIds = emptyList(),
            crossSellProductIds = emptyList(),
            upsellProductIds = emptyList(),
            variationIds = if (productType == ProductType.VARIABLE.value) {
                List(Random.nextInt(1, 5)) { Random.nextLong(1000, 2000) }
            } else {
                emptyList()
            },
            length = Random.nextFloat() * 10,
            width = Random.nextFloat() * 10,
            height = Random.nextFloat() * 10,
            weight = Random.nextFloat() * 5,
            isSampleProduct = false,
            specialStockStatus = null,
            isConfigurable = false,
            minAllowedQuantity = if (Random.nextBoolean()) Random.nextInt(1, 3) else null,
            maxAllowedQuantity = if (Random.nextBoolean()) Random.nextInt(5, 10) else null,
            bundleMinSize = null,
            bundleMaxSize = null,
            groupOfQuantity = null,
            combineVariationQuantities = null,
            password = null
        )
    }

    private fun getRandomProductName(id: Long): String {
        val adjectives = listOf(
            "Premium", "Luxury", "Essential", "Organic", "Handcrafted",
            "Vintage", "Modern", "Classic", "Eco-friendly", "Professional"
        )
        val products = listOf(
            "T-Shirt", "Laptop", "Coffee Maker", "Headphones", "Smartphone",
            "Watch", "Camera", "Backpack", "Sneakers", "Blender",
            "Desk Chair", "Sunglasses", "Water Bottle", "Yoga Mat", "Bluetooth Speaker"
        )

        val adjective = adjectives[id.toInt() % adjectives.size]
        val product = products[id.toInt() % products.size]

        return "$adjective $product"
    }

    private fun getProductImageUrl(id: Long, productName: String): String {
        return if (id % 2 == 0L) {
            "https://example.com/images/$productName.jpg"
        } else {
            "https://i0.wp.com/paymentwithoutaddress.wpcomstaging.com/wp-content/" +
                "uploads/2024/09/303c15ed-36ba-4cbb-b093-4b7b20c988ff-1128-0000008a1d4aec00_file." +
                "jpeg?fit=3000%2C2250&ssl=1"
        }
    }

    sealed class ProductsResult {
        data class Cached(val products: List<Product>) : ProductsResult()
        data class Remote(val productsResult: Result<List<Product>>) : ProductsResult()
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
