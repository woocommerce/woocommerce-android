package com.woocommerce.android.ui.compose.designsystem.preview

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.MenuItem
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.woocommerce.android.ui.compose.designsystem.R
import com.woocommerce.android.ui.compose.designsystem.component.WooDesignSystemToolbar
import com.google.android.material.R as MaterialR

@Composable
internal fun WooDesignSystemToolbarDemo(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ToolbarDemo(
            configuration = WooDesignSystemToolbar.Configuration.SMALL,
            titleAlignment = WooDesignSystemToolbar.TitleAlignment.START,
            supportingText = null,
            dividerMode = WooDesignSystemToolbar.DividerMode.NEVER,
        )
        Spacer(Modifier.height(16.dp))
        ToolbarDemo(
            configuration = WooDesignSystemToolbar.Configuration.MEDIUM,
            titleAlignment = WooDesignSystemToolbar.TitleAlignment.CENTER,
            supportingText = "All products",
            dividerMode = WooDesignSystemToolbar.DividerMode.ALWAYS,
        )
    }
}

@Composable
private fun ToolbarDemo(
    configuration: WooDesignSystemToolbar.Configuration,
    titleAlignment: WooDesignSystemToolbar.TitleAlignment,
    supportingText: String?,
    dividerMode: WooDesignSystemToolbar.DividerMode,
) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context -> WooDesignSystemToolbar(context.withToolbarPreviewTheme()) },
        update = { toolbar ->
            toolbar.configuration = configuration
            toolbar.titleAlignment = titleAlignment
            toolbar.supportingText = supportingText
            toolbar.dividerMode = dividerMode
            toolbar.title = "Products"
            toolbar.navigationIcon = AppCompatResources.getDrawable(
                toolbar.context,
                R.drawable.woo_ds_ic_regular_angle_left_24dp,
            )
            toolbar.navigationContentDescription = "Back"
            toolbar.menu.clear()
            toolbar.menu.add(0, TOOLBAR_DEMO_SEARCH_ACTION_ID, 0, "Search").apply {
                icon = AppCompatResources.getDrawable(
                    toolbar.context,
                    R.drawable.woo_ds_ic_regular_magnifying_glass_24dp,
                )
                setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
            toolbar.menu.add(0, TOOLBAR_DEMO_DONE_ACTION_ID, 1, "Done").apply {
                setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
        },
    )
}

private fun Context.withToolbarPreviewTheme(): Context =
    ContextThemeWrapper(this, MaterialR.style.Theme_MaterialComponents_DayNight_NoActionBar)

private const val TOOLBAR_DEMO_SEARCH_ACTION_ID = 1
private const val TOOLBAR_DEMO_DONE_ACTION_ID = 2
