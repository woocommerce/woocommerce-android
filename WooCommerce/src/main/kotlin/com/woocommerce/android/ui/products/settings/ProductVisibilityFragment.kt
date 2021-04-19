package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.View.OnClickListener
import android.widget.CheckedTextView
import androidx.annotation.IdRes
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductVisibilityBinding
import com.woocommerce.android.ui.products.settings.ProductVisibility.PASSWORD_PROTECTED
import com.woocommerce.android.ui.products.settings.ProductVisibility.PRIVATE
import com.woocommerce.android.ui.products.settings.ProductVisibility.PUBLIC
import kotlinx.parcelize.Parcelize
import org.wordpress.android.util.ActivityUtils

/**
 * Settings screen which enables choosing a product's visibility
 */
class ProductVisibilityFragment : BaseProductSettingsFragment(R.layout.fragment_product_visibility), OnClickListener {
    companion object {
        const val ARG_VISIBILITY = "visibility"
        const val ARG_PASSWORD = "password"
        const val PRODUCT_VISIBILITY_RESULT = "product-visibility"
    }

    private var _binding: FragmentProductVisibilityBinding? = null
    private val binding get() = _binding!!

    private val navArgs: ProductVisibilityFragmentArgs by navArgs()
    private var selectedVisibility: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductVisibilityBinding.bind(view)

        setHasOptionsMenu(true)

        selectedVisibility = savedInstanceState?.getString(ARG_VISIBILITY) ?: navArgs.visibility
        selectedVisibility?.let {
            getButtonForVisibility(it)?.isChecked = true
        }

        binding.btnPublic.setOnClickListener(this)
        binding.btnPasswordProtected.setOnClickListener(this)
        binding.btnPrivate.setOnClickListener(this)

        if (selectedVisibility == PASSWORD_PROTECTED.toString()) {
            (savedInstanceState?.getString(ARG_PASSWORD) ?: navArgs.password)?.let { password ->
                binding.editPassword.setText(password)
                showPassword(password.isNotBlank())
            }
        }

        binding.editPassword.setOnTextChangedListener {
            if (it.toString().isNotBlank()) {
                binding.editPassword.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_VISIBILITY, selectedVisibility)
    }

    override fun onClick(view: View?) {
        (view as? CheckedTextView)?.let {
            binding.btnPublic.isChecked = it == binding.btnPublic
            binding.btnPrivate.isChecked = it == binding.btnPrivate
            binding.btnPasswordProtected.isChecked = it == binding.btnPasswordProtected

            selectedVisibility = getVisibilityForButtonId(it.id)
            showPassword(it == binding.btnPasswordProtected)
        }
    }

    private fun showPassword(show: Boolean) {
        if (show && binding.editPassword.visibility != View.VISIBLE) {
            binding.editPassword.visibility = View.VISIBLE
            binding.editPassword.requestFocus()
            ActivityUtils.showKeyboard(binding.editPassword)
        } else if (!show && binding.editPassword.visibility == View.VISIBLE) {
            binding.editPassword.visibility = View.GONE
            ActivityUtils.hideKeyboardForced(binding.editPassword)
        }
    }

    override fun getChangesResult(): Pair<String, Any> {
        val password = if (selectedVisibility == PASSWORD_PROTECTED.toString()) {
            getPassword()
        } else {
            ""
        }
        return PRODUCT_VISIBILITY_RESULT to ProductVisibilityResult(selectedVisibility ?: "", password)
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

    override fun validateChanges(): Boolean {
        if (selectedVisibility == PASSWORD_PROTECTED.toString() && getPassword().isEmpty()) {
            binding.editPassword.error = getString(R.string.product_visibility_password_required)
            return false
        }
        return true
    }

    private fun getButtonForVisibility(visibility: String): CheckedTextView? {
        return when (ProductVisibility.fromString(visibility)) {
            PUBLIC -> binding.btnPublic
            PRIVATE -> binding.btnPrivate
            PASSWORD_PROTECTED -> binding.btnPasswordProtected
            else -> null
        }
    }

    private fun getVisibilityForButtonId(@IdRes buttonId: Int): String? {
        return when (buttonId) {
            R.id.btnPublic -> PUBLIC.toString()
            R.id.btnPrivate -> PRIVATE.toString()
            R.id.btnPasswordProtected -> PASSWORD_PROTECTED.toString()
            else -> null
        }
    }

    private fun getPassword() = binding.editPassword.getText()
}

@Parcelize
data class ProductVisibilityResult(val selectedVisiblity: String, val password: String) : Parcelable
