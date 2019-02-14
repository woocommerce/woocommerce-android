package com.woocommerce.android.widgets

import android.content.Context
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatButton
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.woocommerce.android.R
import com.woocommerce.android.widgets.WCPromoDialog.PromoButton.BUTTON_GOT_IT
import com.woocommerce.android.widgets.WCPromoDialog.PromoButton.BUTTON_TRY_IT
import org.wordpress.android.util.DisplayUtils
import java.lang.ref.WeakReference

object WCPromoDialog {
    private const val PREF_NAME = "woo_promo_dialog"

    enum class PromoType(val prefKeyName: String) {
        SITE_PICKER("key_site_picker")
    }

    enum class PromoButton {
        BUTTON_GOT_IT,
        BUTTON_TRY_IT
    }

    private var dialogRef: WeakReference<AlertDialog>? = null

    interface PromoDialogListener {
        fun onPromoButtonClicked(promoType: PromoType, promoButton: PromoButton)
    }

    /**
     * Show the desired dialog if the criteria is satisfied.
     * @return true if shown, false otherwise.
     */
    fun showPromoDialogIfNeeded(context: Context, promoType: PromoType): Boolean {
        return if (shouldShowPromoDialog(context, promoType)) {
            showPromoDialog(context, promoType)
            true
        } else {
            false
        }
    }

    // hide the image in landscape TODO: handle rotation
    fun checkOrientation(context: Context) {
        dialogRef?.get()?.let { dialog ->
            val isLandsccape = DisplayUtils.isLandscape(context)
            val image = dialog.findViewById<ImageView>(R.id.imagePromo)
            image?.visibility = if (isLandsccape) View.GONE else View.VISIBLE
        }
    }

    /**
     * Check whether the promo dialog should be shown or not.
     * @return true if the dialog should be shown
     */
    private fun shouldShowPromoDialog(context: Context, promoType: PromoType): Boolean {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val hasShownDialog = preferences.getBoolean(promoType.prefKeyName, false)
        return !hasShownDialog
    }

    /**
     * For now we have only one promo so we don't bother to customize the dialog here, but in the
     * future we'll need separate strings and actions for each promo type
     */
    private fun showPromoDialog(context: Context, promoType: PromoType) {
        dialogRef?.get()?.let {
            // Dialog is already present
            return
        }

        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // TODO: preferences.edit().putBoolean(promoType.prefKeyName, true).apply()

        val listener: PromoDialogListener? = if (context is PromoDialogListener) {
            context
        } else {
            null
        }

        // inflate the custom view and set up the button listeners
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_promo, null)
        dialogView.findViewById<AppCompatButton>(R.id.btnGotIt)?.setOnClickListener {
            dialogRef?.let {
                it.get()?.dismiss()
                it.clear()
                listener?.onPromoButtonClicked(promoType, BUTTON_GOT_IT)
            }
        }
        dialogView.findViewById<AppCompatButton>(R.id.btnTryIt)?.setOnClickListener {
            dialogRef?.let {
                it.get()?.dismiss()
                it.clear()
                listener?.onPromoButtonClicked(promoType, BUTTON_TRY_IT)
            }
        }


        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)
                .setCancelable(true)
                .setOnDismissListener { dialogRef?.clear() }
        dialogRef = WeakReference(builder.show())
        checkOrientation(context)
    }
}
