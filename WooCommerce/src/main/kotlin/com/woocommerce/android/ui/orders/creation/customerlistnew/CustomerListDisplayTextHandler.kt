package com.woocommerce.android.ui.orders.creation.customerlistnew

import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.creation.customerlistnew.CustomerListViewState.CustomerList.Item.Customer
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class CustomerListDisplayTextHandler @Inject constructor(
    private val highlighter: CustomerListSearchResultsHighlighter,
    private val resourceProvider: ResourceProvider,
) {
    operator fun invoke(param: CustomerParam, matchBy: String) =
        when (param) {
            is CustomerParam.Email -> handleEmail(param.text, matchBy)
            is CustomerParam.Name -> handleName(param.text, matchBy)
            is CustomerParam.Username -> handleUsername(param.text, matchBy)
        }

    private fun handleName(name: String, matchBy: String) =
        if (name.isEmpty()) {
            Customer.Text.Placeholder(
                resourceProvider.getString(R.string.order_creation_customer_search_empty_name)
            )
        } else {
            highlighter(name, matchBy)
        }

    private fun handleEmail(email: String, matchBy: String) =
        if (email.isEmpty()) {
            Customer.Text.Placeholder(
                resourceProvider.getString(R.string.order_creation_customer_search_empty_email)
            )
        } else {
            highlighter(email, matchBy)
        }

    private fun handleUsername(username: String, matchBy: String) =
        if (username.isEmpty()) {
            Customer.Text.Placeholder("")
        } else {
            highlighter("· $username", matchBy)
        }

    sealed class CustomerParam(val text: String) {
        data class Name(val value: String) : CustomerParam(value)
        data class Email(val value: String) : CustomerParam(value)
        data class Username(val value: String) : CustomerParam(value)
    }
}
