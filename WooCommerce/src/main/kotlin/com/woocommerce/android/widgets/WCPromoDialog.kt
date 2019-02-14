package com.woocommerce.android.widgets

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.app.AlertDialog
import com.woocommerce.android.R
import java.lang.ref.WeakReference

object WCPromoDialog {
    private const val PREF_NAME = "woo_promo_dialog"

    enum class PromoType(val prefKeyName: String) {
        SITE_PICKER("key_site_picker")
    }

    private var dialogRef: WeakReference<AlertDialog>? = null

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

    /**
     * Check whether the promo dialog should be shown or not.
     * @return true if the dialog should be shown
     */
    private fun shouldShowPromoDialog(context: Context, promoType: PromoType): Boolean {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val hasShownDialog = preferences.getBoolean(promoType.prefKeyName, false)
        return !hasShownDialog
    }

    private fun showPromoDialog(context: Context, promoType: PromoType) {
        dialogRef?.get()?.let {
            // Dialog is already present
            return
        }

        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // TODO: preferences.edit().putBoolean(promoType.prefKeyName, true).apply()

        val builder = AlertDialog.Builder(context)
        builder.setView(R.layout.dialog_promo)
                .setCancelable(true)
                .setOnDismissListener { dialogRef?.clear() }
        dialogRef = WeakReference(builder.show())
    }
}
