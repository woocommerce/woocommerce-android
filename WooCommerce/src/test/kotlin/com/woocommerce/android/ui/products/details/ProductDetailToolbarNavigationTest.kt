package com.woocommerce.android.ui.products.details

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import com.woocommerce.android.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProductDetailToolbarNavigationTest {
    @Test
    fun `given back navigation, when applied, then toolbar has localized back label`() {
        assertNavigation(
            navigation = ProductDetailToolbarNavigation.BACK,
            expectedIcon = R.drawable.ic_back_24dp,
            expectedLabel = R.string.back,
            localizedLabel = "Back",
        )
    }

    @Test
    fun `given close navigation, when applied, then toolbar has localized close label`() {
        assertNavigation(
            navigation = ProductDetailToolbarNavigation.CLOSE,
            expectedIcon = R.drawable.ic_gridicons_cross_24dp,
            expectedLabel = R.string.close,
            localizedLabel = "Close",
        )
    }

    @Test
    fun `given navigation is removed, when applied, then stale accessibility label is cleared`() {
        val toolbar: Toolbar = mock()

        toolbar.setProductDetailNavigation(null)

        verify(toolbar).setNavigationIcon(null as Drawable?)
        verify(toolbar).setNavigationContentDescription(null as CharSequence?)
    }

    private fun assertNavigation(
        navigation: ProductDetailToolbarNavigation,
        expectedIcon: Int,
        expectedLabel: Int,
        localizedLabel: String,
    ) {
        val context: Context = mock()
        val toolbar: Toolbar = mock()
        val drawable: Drawable = mock()
        whenever(toolbar.context).thenReturn(context)
        whenever(context.getString(expectedLabel)).thenReturn(localizedLabel)

        mockStatic(AppCompatResources::class.java).use { resources ->
            resources.`when`<Drawable?> {
                AppCompatResources.getDrawable(context, expectedIcon)
            }.thenReturn(drawable)

            toolbar.setProductDetailNavigation(navigation)

            verify(toolbar).setNavigationIcon(drawable)
            verify(toolbar).setNavigationContentDescription(localizedLabel)
            assertThat(navigation.icon).isEqualTo(expectedIcon)
            assertThat(navigation.contentDescription).isEqualTo(expectedLabel)
        }
    }
}
