package com.woocommerce.android.extensions

import android.app.Activity
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.google.android.material.appbar.AppBarLayout
import com.woocommerce.android.R.dimen
import com.woocommerce.android.support.help.HelpActivity
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.main.AppBarStatus

/**
 * Used for starting the HelpActivity in a wrapped way whenever a troubleshooting URL click happens
 */
fun FragmentActivity.startHelpActivity(origin: HelpOrigin) =
    startActivity(
        HelpActivity.createIntent(
            this,
            origin,
            null
        )
    )

fun Activity.applyAppStatus(
    appBarStatus: AppBarStatus.Visible,
    toolbar: Toolbar,
    appBarLayout: AppBarLayout,
    appBarDivider: View? = null,
) {
    toolbar.navigationIcon = appBarStatus.navigationIcon?.let {
        ContextCompat.getDrawable(this, it)
    }
    appBarLayout.elevation = if (appBarStatus.hasShadow) {
        resources.getDimensionPixelSize(dimen.appbar_elevation).toFloat()
    } else 0f
    appBarDivider?.isVisible = appBarStatus.hasDivider
}
