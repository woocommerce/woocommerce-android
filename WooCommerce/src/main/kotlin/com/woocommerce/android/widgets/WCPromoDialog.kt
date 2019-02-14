package com.woocommerce.android.widgets

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatButton
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.woocommerce.android.R
import com.woocommerce.android.widgets.WCPromoDialog.PromoButton.BUTTON_SITE_PICKER_GOT_IT
import com.woocommerce.android.widgets.WCPromoDialog.PromoButton.BUTTON_SITE_PICKER_TRY_IT
import org.wordpress.android.util.DisplayUtils

class WCPromoDialog : DialogFragment() {
    companion object {
        const val TAG: String = "WCPromoDialog"
        private const val PREF_NAME = "woo_promo_dialog"

        fun showIfNeeded(activity: AppCompatActivity, promoType: PromoType) {
            val preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val hasShownDialog = preferences.getBoolean(promoType.prefKeyName, false)
            if (!hasShownDialog) {
                val fragment = WCPromoDialog()
                if (activity is PromoDialogListener) {
                    fragment.listener = activity
                }
                fragment.promoType = promoType
                fragment.show(activity.supportFragmentManager, TAG)
                // TODO: preferences?.edit().putBoolean(promoType.prefKeyName, true).apply()
            }
        }
    }

    enum class PromoButton {
        BUTTON_SITE_PICKER_GOT_IT,
        BUTTON_SITE_PICKER_TRY_IT
    }

    enum class PromoType(
        val prefKeyName: String,
        val button1: PromoButton,
        val button2: PromoButton
    ) {
        SITE_PICKER("key_site_picker", BUTTON_SITE_PICKER_GOT_IT, BUTTON_SITE_PICKER_TRY_IT)
    }

    interface PromoDialogListener {
        fun onPromoButtonClicked(promoButton: PromoButton)
    }

    private var listener: PromoDialogListener? = null
    private lateinit var promoType: PromoType

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        checkOrientation()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        checkOrientation()
    }

    // hide the image in landscape
    private fun checkOrientation() {
        if (isAdded) {
            val isLandscape = DisplayUtils.isLandscape(activity)
            val image = dialog.findViewById<ImageView>(R.id.imagePromo)
            image?.visibility = if (isLandscape) View.GONE else View.VISIBLE
        }
    }

    /**
     * For now we have only one promo so we don't bother to customize the dialog here, but in the
     * future we'll need separate strings and actions for each promo type
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // inflate the custom view and set up the button listeners
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_promo, null)
        dialogView.findViewById<AppCompatButton>(R.id.button1)?.setOnClickListener {
            dialog?.dismiss()
            listener?.onPromoButtonClicked(promoType.button1)
        }
        dialogView.findViewById<AppCompatButton>(R.id.button2)?.setOnClickListener {
            dialog?.dismiss()
            listener?.onPromoButtonClicked(promoType.button2)
        }

        return AlertDialog.Builder(activity as Context)
                .setView(dialogView)
                .setCancelable(true)
                .create()
    }
}
