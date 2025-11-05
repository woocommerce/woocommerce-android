package com.woocommerce.android.ui.compose.modifier

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.isTwoPanesShouldBeUsed
import com.woocommerce.android.extensions.windowWidthSizeClass
import org.wordpress.android.util.DisplayUtils

/**
 * Adds horizontal padding to detail screens when shown in a split-pane layout (tablets).
 * The padding mirrors the margin calculations used in TabletLayoutSetupHelper.setDetailsMargins.
 */
@Composable
fun Modifier.detailsPanePadding(): Modifier {
    val context = LocalContext.current
    val density = LocalDensity.current

    val isTwoPanes = context.isTwoPanesShouldBeUsed

    return if (isTwoPanes) {
        val marginDp = when (context.windowWidthSizeClass) {
            WindowSizeClass.Medium -> dimensionResource(id = R.dimen.major_100)
            WindowSizeClass.ExpandedAndBigger -> {
                val windowWidth = DisplayUtils.getWindowPixelWidth(context)
                with(density) { (windowWidth * MARGINS_FOR_TABLET).toDp() }
            }

            WindowSizeClass.Compact -> 0.dp
        }
        this.padding(horizontal = marginDp)
    } else {
        this
    }
}

private const val MARGINS_FOR_TABLET: Float = 0.1F
