package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.annotation.IdRes
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility.HIDDEN
import com.woocommerce.android.ui.products.settings.ProductVisibility.PASSWORD_PROTECTED
import com.woocommerce.android.ui.products.settings.ProductVisibility.PRIVATE
import com.woocommerce.android.ui.products.settings.ProductVisibility.PUBLIC
import com.woocommerce.android.util.WooAnimUtils
import kotlinx.android.synthetic.main.fragment_product_visibility.*
import org.wordpress.android.util.ActivityUtils

/**
 * Settings screen which enables choosing a product's visibility
 */
class ProductVisibilityFragment : BaseProductSettingsFragment(), OnClickListener {
    companion object {
        const val ARG_VISIBILITY = "visibility"
        const val ARG_PASSWORD = "password"
    }

    override val requestCode = RequestCodes.PRODUCT_SETTINGS_VISIBLITY

    private val navArgs: ProductVisibilityFragmentArgs by navArgs()
    private var selectedVisibility: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_visibility, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedVisibility = savedInstanceState?.getString(ARG_VISIBILITY) ?: navArgs.visibility
        selectedVisibility?.let {
            getButtonForVisibility(it)?.isChecked = true
        }

        btnPublic.setOnClickListener(this)
        btnPrivate.setOnClickListener(this)
        btnPasswordProtected.setOnClickListener(this)

        val password = savedInstanceState?.getString(ARG_PASSWORD) ?: navArgs.password
        editPassword.setText(password)
        showPassword(if (password.isNullOrBlank()) false else true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_VISIBILITY, selectedVisibility)
    }

    override fun onClick(view: View?) {
        (view as? CheckedTextView)?.let {
            btnPublic.isChecked = it == btnPublic
            btnPrivate.isChecked = it == btnPrivate
            btnPasswordProtected.isChecked = it == btnPasswordProtected

            selectedVisibility = getVisibilityForButtonId(it.id)
            showPassword(it == btnPasswordProtected)
        }
    }

    private fun showPassword(show: Boolean) {
        if (show && editPassword.visibility != View.VISIBLE) {
            WooAnimUtils.scaleIn(editPassword)
            editPassword.requestFocus()
            ActivityUtils.showKeyboard(editPassword)
        } else if (!show && editPassword.visibility == View.VISIBLE) {
            WooAnimUtils.scaleOut(editPassword)
            ActivityUtils.hideKeyboardForced(editPassword)
        }
    }

    override fun getChangesBundle(): Bundle {
        return Bundle().also {
            it.putString(ARG_VISIBILITY, selectedVisibility)
            if (selectedVisibility == PASSWORD_PROTECTED.toString()) {
                it.putString(ARG_PASSWORD, getPassword())
            }
        }
    }

    override fun hasChanges(): Boolean {
        return navArgs.visibility != selectedVisibility ||
                navArgs.password != getPassword()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_visibility)

    private fun getButtonForVisibility(visibility: String): CheckedTextView? {
        return when (ProductVisibility.fromString(visibility)) {
            PUBLIC -> btnPublic
            PRIVATE -> btnPrivate
            PASSWORD_PROTECTED -> btnPasswordProtected
            else -> null
        }
    }

    private fun getVisibilityForButtonId(@IdRes buttonId: Int): String? {
        return when (buttonId) {
            R.id.btnPublic -> PUBLIC.toString()
            R.id.btnPrivate -> PRIVATE.toString()
            R.id.btnPasswordProtected -> PASSWORD_PROTECTED.toString()
            R.id.btnVisibilityHidden -> HIDDEN.toString()
            else -> null
        }
    }

    private fun getPassword() = editPassword.text.toString()
}
