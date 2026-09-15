package com.woocommerce.android.model

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ProductCategoryTest {
    private val resourceProvider: ResourceProvider = mock<ResourceProvider>().apply {
        whenever(getDimensionPixelSize(R.dimen.major_100)).thenReturn(STEP)
    }

    @Test
    fun `given a root category, when computing the margin, then it is a single step`() {
        // GIVEN
        val category = ProductCategory(remoteCategoryId = 1L, name = "Root", parentId = 0L)

        // WHEN
        val margin = category.computeCascadingMargin(resourceProvider, emptyMap())

        // THEN
        assertThat(margin).isEqualTo(STEP)
    }

    @Test
    fun `given a category nested below the cap, when computing the margin, then it grows per level`() {
        // GIVEN
        val category = ProductCategory(remoteCategoryId = 4L, name = "Level 3", parentId = 3L)

        // WHEN
        val margin = category.computeCascadingMargin(resourceProvider, chainOf(depth = 3))

        // THEN
        assertThat(margin).isEqualTo(STEP * 4)
    }

    @Test
    fun `given nested categories, when computing margins, then steps shrink after eight levels and stop after sixteen`() {
        // GIVEN
        val depths = listOf(7, 8, 9, 15, 16, 20)

        // WHEN
        val margins = depths.map { depth ->
            ProductCategory(remoteCategoryId = depth + 1L, name = "Level $depth", parentId = depth.toLong())
                .computeCascadingMargin(resourceProvider, chainOf(depth))
        }

        // THEN
        assertThat(margins).containsExactly(256, 288, 304, 400, 416, 416)
    }

    private fun chainOf(depth: Int) = (1..depth).associate { it.toLong() + 1 to it.toLong() }

    private companion object {
        const val STEP = 32
    }
}
