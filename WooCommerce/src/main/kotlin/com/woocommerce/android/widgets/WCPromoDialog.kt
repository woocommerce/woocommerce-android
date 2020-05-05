package com.woocommerce.android.widgets

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.widgets.WCPromoDialog.PromoButton.SITE_PICKER_GOT_IT
import com.woocommerce.android.widgets.WCPromoDialog.PromoButton.SITE_PICKER_TRY_IT
import org.wordpress.android.util.DisplayUtils

/**
 * Used to advertise a new feature with a single-shot dialog. This was initially added to promote the ability to
 * switch sites but is no longer used. Code is being left here for possible future use.
 */
class WCPromoDialog : androidx.fragment.app.DialogFragment() {
    companion object {
        const val TAG: String = "WCPromoDialog"
        private const val PREF_NAME = "woo_promo_dialog"

        /**
         * Displays the desired promo dialog if it hasn't already been shown, returns True if it gets shown below
         */
        fun showIfNeeded(activity: AppCompatActivity, promoType: PromoType): Boolean {
            val preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val hasShownDialog = preferences.getBoolean(promoType.prefKeyName, false)
            if (hasShownDialog) {
                return false
            }

            val fragment = WCPromoDialog()
            if (activity is PromoDialogListener) {
                fragment.listener = activity
            }
            fragment.promoType = promoType
            fragment.show(activity.supportFragmentManager, TAG)
            preferences.edit().putBoolean(promoType.prefKeyName, true).apply()

            return true
        }
    }

    enum class PromoButton {
        SITE_PICKER_GOT_IT,
        SITE_PICKER_TRY_IT
    }

    enum class PromoType(
        val prefKeyName: String,
        val button1: PromoButton,
        val button2: PromoButton,
        val addedInVersionCode: Int
    ) {
        SITE_PICKER("key_site_picker", SITE_PICKER_GOT_IT, SITE_PICKER_TRY_IT, 34)
    }

    interface PromoDialogListener {
        fun onPromoButtonClicked(promoButton: PromoButton)
    }

    private var listener: PromoDialogListener? = null
    private var promoFrame: View? = null
    private var promoImage: View? = null
    private lateinit var promoType: PromoType

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog
    }

    // hide the image in landscape
    private fun checkOrientation() {
        if (isAdded) {
            val isLandscape = DisplayUtils.isLandscape(activity)
            promoFrame?.visibility = if (isLandscape) View.GONE else View.VISIBLE
            promoImage?.visibility = if (isLandscape) View.GONE else View.VISIBLE
        }
    }

    /**
     * For now we have only one promo so we don't bother to customize the dialog here, but in the
     * future we'll need separate strings and actions for each promo type
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = View.inflate(activity, R.layout.dialog_promo, null)

        dialogView.findViewById<MaterialButton>(R.id.button1)?.setOnClickListener {
            dialog?.dismiss()
            listener?.onPromoButtonClicked(promoType.button1)
        }

        dialogView.findViewById<MaterialButton>(R.id.button2)?.setOnClickListener {
            dialog?.dismiss()
            listener?.onPromoButtonClicked(promoType.button2)
        }

        promoFrame = dialogView.findViewById(R.id.imagePromoFrame)
        promoImage = dialogView.findViewById(R.id.imagePromo)
        checkOrientation()

        return MaterialAlertDialogBuilder(activity as Context)
                .setView(dialogView)
                .setCancelable(true)
                .create()
    }
}
