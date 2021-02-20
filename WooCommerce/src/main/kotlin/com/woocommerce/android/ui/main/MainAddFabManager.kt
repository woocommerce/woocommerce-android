package com.woocommerce.android.ui.main

import androidx.annotation.StringRes
import androidx.core.view.isGone
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.woocommerce.android.R
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.base.FabManager
import javax.inject.Inject

@ActivityScope
class MainAddFabManager @Inject constructor(val activity: MainActivity) : FabManager {
    private val addButton by lazy { activity.findViewById<FloatingActionButton>(R.id.addButton) }

    override fun showFabAnimated(@StringRes contentDescription: Int, onClick: () -> Unit) {
        with(addButton) {
            this.contentDescription = activity.getString(contentDescription)
            show()
            setOnClickListener { onClick() }
        }
    }

    override fun hideFabAnimated() {
        with(addButton) {
            addButton.setOnClickListener(null)
            hide()
        }
    }

    override fun hideFabImmediately() {
        with(addButton) {
            addButton.setOnClickListener(null)
            isGone = true
        }
    }
}
