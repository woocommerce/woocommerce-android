package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ViewModelFactory
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

abstract class BaseProductEditorFragment(@LayoutRes private val layoutRes: Int) : BaseFragment(), BackPressListener {
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    companion object {
        const val KEY_SHIPPING_DIALOG_RESULT = "key_shipping_dialog_result"
        const val KEY_PRICING_DIALOG_RESULT = "key_pricing_dialog_result"
        const val KEY_INVENTORY_DIALOG_RESULT = "key_inventory_dialog_result"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(layoutRes, container, false)
    }

    protected var doneButton: MenuItem? = null

    abstract fun onDoneButtonClicked()
    abstract fun onExit()

    abstract val isDoneButtonVisible: Boolean
    abstract val isDoneButtonEnabled: Boolean
    abstract val lastEvent: Event?

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_done, menu)
        doneButton = menu.findItem(R.id.menu_done)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                onDoneButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        doneButton?.isVisible = isDoneButtonVisible
        doneButton?.isEnabled = isDoneButtonEnabled
    }

    override fun onRequestAllowBackPress(): Boolean {
        return (lastEvent == Exit).also { if (it.not()) onExit() }
    }
}
