package com.woocommerce.android.ui.products

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.model.toProductCategory
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore

@OptIn(ExperimentalCoroutinesApi::class)
class GetProductsByIdsTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val productStore: WCProductStore = mock()

    lateinit var sut: GetProductsByIds

    @Before
    fun setUp() {
        sut = GetProductsByIds(
            selectedSite,
            productStore,
            coroutinesTestRule.testDispatchers,
        )
    }

    @Test
    fun `when API respond successfully, then return products`() = testBlocking {
        whenever(productStore.fetchProductListSynced(any(), any())).doReturn(databaseProducts)
        val productsIds: List<Long> = mock()

        val result = sut.invoke(productsIds)

        assertThat(result.size).isEqualTo(products.size)
    }

    @Test
    fun `when API fails, then return empty list`() = testBlocking {
        whenever(productStore.fetchProductListSynced(any(), any())).doReturn(null)
        val categoriesIds: List<Long> = mock()

        val result = sut.invoke(categoriesIds)

        assertThat(result.isEmpty()).isTrue()
    }

    private val databaseProducts = List(3) { n ->
        WCProductModel().apply {
            localSiteId = 3
            remoteProductId = n.toLong()
            name = "product $n"
            attributes = "[]"
        }
    }

    private var products = databaseProducts.map { it.toAppModel() }
}
