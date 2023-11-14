package com.woocommerce.android.ui.products

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
import org.wordpress.android.fluxc.store.WCProductStore

@OptIn(ExperimentalCoroutinesApi::class)
class GetCategoriesByIdsTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val productStore: WCProductStore = mock()

    lateinit var sut: GetCategoriesByIds

    @Before
    fun setUp() {
        sut = GetCategoriesByIds(
            selectedSite,
            productStore,
            coroutinesTestRule.testDispatchers,
        )
    }

    @Test
    fun `when API respond successfully, then return categories`() = testBlocking {
        whenever(productStore.fetchProductCategoryListSynced(any(), any())).doReturn(databaseCategories)
        val categoriesIds: List<Long> = mock()

        val result = sut.invoke(categoriesIds)

        assertThat(result).isEqualTo(productCategories)
    }

    @Test
    fun `when API fails, then return empty list`() = testBlocking {
        whenever(productStore.fetchProductCategoryListSynced(any(), any())).doReturn(null)
        val categoriesIds: List<Long> = mock()

        val result = sut.invoke(categoriesIds)

        assertThat(result.isEmpty()).isTrue()
    }

    private val databaseCategories = List(3) { n ->
        WCProductCategoryModel().apply {
            localSiteId = 3
            remoteCategoryId = n.toLong()
            name = "category $n"
        }
    }

    private var productCategories = databaseCategories.map { it.toProductCategory() }
}
