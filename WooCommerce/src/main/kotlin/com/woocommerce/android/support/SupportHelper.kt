package com.woocommerce.android.support

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.widgets.WCMaterialOutlinedEditTextView
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel

class SupportHelper {
    /**
     * This is a helper function that shows the support identity input dialog and runs the provided function with
     * the input from it.
     *
     * @param context Context the dialog will be showed from
     * @param email Initial value for the email field
     * @param name Initial value for the name field
     * @param isNameInputHidden Whether the name input field should be shown or not
     * @param emailAndNameSelected Function to run with the email and name inputs from the dialog. Even if the
     * [isNameInputHidden] parameter is true, the input in the name field will be provided and it's up to the caller to
     * ignore the name parameter.
     */
    fun showSupportIdentityInputDialog(
        context: Context,
        email: String?,
        name: String? = null,
        isNameInputHidden: Boolean = false,
        emailAndNameSelected: (String, String) -> Unit
    ) {
        val (layout, emailEditText, nameEditText) =
                supportIdentityInputDialogLayout(context, isNameInputHidden, email, name)

        val dialog = MaterialAlertDialogBuilder(context)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val newEmail = emailEditText.getText()
                val newName = nameEditText.getText()
                if (StringUtils.isValidEmail(newEmail)) {
                    emailAndNameSelected(newEmail, newName)
                    dialog.dismiss()
                } else {
                    emailEditText.error = context.getString(R.string.invalid_email_message)
                }
            }
        }
        dialog.show()
    }

    /**
     * This is a helper function to returns suggested email and name values to be used in the support identity dialog.
     *
     * @param account WordPress.com account
     * @param selectedSite Selected site of the user which will be used if the [account] is null
     *
     * @return a Pair with email and name suggestion
     */
    fun getSupportEmailAndNameSuggestion(
        account: AccountModel?,
        selectedSite: SiteModel?
    ): Pair<String?, String?> {
        val accountEmail = account?.email
        val accountDisplayName = account?.displayName
        val emailSuggestion = if (!accountEmail.isNullOrEmpty()) accountEmail else selectedSite?.email
        val nameSuggestion = if (!accountDisplayName.isNullOrEmpty()) accountDisplayName else selectedSite?.username
        return Pair(emailSuggestion, nameSuggestion)
    }
}

/**
 * This is a helper function that inflates the support identity dialog layout.
 *
 * @param context Context to use to inflate the layout
 * @param isNameInputHidden Whether the name EditText should be visible or not
 * @param emailSuggestion Initial value for the email EditText
 * @param nameSuggestion Initial value for the name EditText
 *
 * @return a Triple with layout View, email EditText and name EditText
 */
private fun supportIdentityInputDialogLayout(
    context: Context,
    isNameInputHidden: Boolean,
    emailSuggestion: String?,
    nameSuggestion: String?
): Triple<View, WCMaterialOutlinedEditTextView, WCMaterialOutlinedEditTextView> {
    val layout = LayoutInflater.from(context).inflate(R.layout.support_email_and_name_dialog, null)

    val messageText = layout.findViewById<MaterialTextView>(R.id.support_identity_input_dialog_message)
    val message = if (isNameInputHidden) {
        R.string.support_identity_input_dialog_enter_email
    } else {
        R.string.support_identity_input_dialog_enter_email_and_name
    }
    messageText.setText(message)

    val emailEditText = layout.findViewById<WCMaterialOutlinedEditTextView>(
            R.id.support_identity_input_dialog_email_edit_text
    )
    emailSuggestion?.let {
        emailEditText.setText(it)
        emailEditText.setSelection(0, it.length)
    }

    val nameEditText = layout.findViewById<WCMaterialOutlinedEditTextView>(
            R.id.support_identity_input_dialog_name_edit_text
    )
    nameSuggestion?.let {
        nameEditText.setText(it)
    }
    nameEditText.visibility = if (isNameInputHidden) View.GONE else View.VISIBLE

    return Triple(layout, emailEditText, nameEditText)
}
