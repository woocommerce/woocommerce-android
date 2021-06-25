package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.navigateBackWithResult

class ShippingLabelCustomPackageTypeDialog : DialogFragment() {
    enum class ShippingLabelCustomPackageType(@StringRes val stringResource: Int) {
        BOX(string.shipping_label_create_custom_package_field_type_box),
        ENVELOPE(string.shipping_label_create_custom_package_field_type_envelope)
    }

    companion object {
        const val KEY_CUSTOM_PACKAGE_TYPE_RESULT = "key_custom_package_type_result"
    }
    private val navArgs: ShippingLabelCustomPackageTypeDialogArgs by navArgs()
    private val customPackageTypes: Array<String> by lazy {
        ShippingLabelCustomPackageType.values().map { getString(it.stringResource)}.toTypedArray()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(resources.getString(string.shipping_label_create_custom_package_field_type_info))
            .setSingleChoiceItems(customPackageTypes, getCurrentCustomPackageTypeIndex()) { _, which ->
                navigateBackWithResult(
                    key = KEY_CUSTOM_PACKAGE_TYPE_RESULT,
                    result = ShippingLabelCustomPackageType.values()[which]
                )
            }
            .create()
    }

    private fun getCurrentCustomPackageTypeIndex() = ShippingLabelCustomPackageType
        .values().indexOfFirst { it == navArgs.currentCustomPackageType }
}
