package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import kotlinx.android.synthetic.main.fragment_product_menu_order.*
import org.wordpress.android.util.StringUtils

/**
 * Settings screen which enables editing a product's menu order
 */
class ProductMenuOrderFragment : BaseProductSettingsFragment() {
    companion object {
        const val ARG_MENU_ORDER = "menu_order"
    }

    override val requestCode = RequestCodes.PRODUCT_SETTINGS_MENU_ORDER
    private val navArgs: ProductMenuOrderFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_menu_order, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        product_menu_order.setText(navArgs.menuOrder.toString())
        product_menu_order.setOnTextChangedListener {
            changesMade()
        }
    }

    override fun hasChanges() = getMenuOrder() != navArgs.menuOrder

    override fun validateChanges() = true

    override fun getChangesBundle(): Bundle {
        return Bundle().also {
            it.putInt(ARG_MENU_ORDER, getMenuOrder())
        }
    }

    private fun getMenuOrder() = StringUtils.stringToInt(product_menu_order.getText())

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_menu_order)
}
