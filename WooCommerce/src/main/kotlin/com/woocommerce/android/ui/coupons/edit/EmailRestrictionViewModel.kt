package com.woocommerce.android.ui.coupons.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class EmailRestrictionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: EmailRestrictionFragmentArgs by savedState.navArgs()

    private val allowedEmailsDraft = savedStateHandle.getStateFlow(
        viewModelScope,
        navArgs.allowedEmails
    )

    val viewState = allowedEmailsDraft.map {
        ViewState(
            hasChanges = it != navArgs.allowedEmails,
            allowedEmails = allowedEmailsDraft.value
        )
    }.asLiveData()

    fun onAllowedEmailsChanged(inputText: String) {
        allowedEmailsDraft.value = inputText
    }

    fun onBackPressed() {
        val event = viewState.value?.takeIf { it.hasChanges }?.let { viewState ->
            val inputText = viewState.allowedEmails

            // Return early as we allow emptying field
            if (inputText.isEmpty()) {
                ExitWithResult(inputText)
            } else {
                val emails = inputText.split(",").map { it.trim() }

                // To match core, we want to allow:
                // - regular emails, e.g: woo@woocommerce.com
                // - emails with wildcard local part, e.g: *@woocommerce.com
                if (emails.all { StringUtils.isValidEmail(email = it, allowWildCardLocalPart = true) }) {
                    ExitWithResult(inputText)
                } else {
                    ShowSnackbar(R.string.coupon_restrictions_allowed_emails_invalid)
                }
            }
        } ?: Exit

        triggerEvent(event)
    }

    data class ViewState(
        val hasChanges: Boolean,
        val allowedEmails: String
    )
}
