package com.woocommerce.android.util

import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R

fun Fragment.setupToolbar(
    title: Int,
    onMenuItemSelected: (menuItem: MenuItem) -> Boolean,
    onCreateMenu: (menu: Toolbar) -> Unit
) {
    val toolbar = requireView().findViewById<Toolbar>(R.id.toolbar)
    toolbar.title = getString(title)
    toolbar.setOnMenuItemClickListener { menuItem ->
        onMenuItemSelected(menuItem)
    }
    toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

    onCreateMenu(toolbar)
}
