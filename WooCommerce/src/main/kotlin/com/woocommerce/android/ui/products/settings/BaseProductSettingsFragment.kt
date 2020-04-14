package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener

/**
 * All fragments shown from the product settings fragment should extend this class
 */
abstract class BaseProductSettingsFragment : BaseFragment(), BackPressListener {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_done)  {
            navigateBackWithResult()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        super.onStop()
        CustomDiscardDialog.onCleared()
    }

    /**
     * Descendants should override this to return changes to the main settings fragment
     */
    abstract fun navigateBackWithResult()
}
