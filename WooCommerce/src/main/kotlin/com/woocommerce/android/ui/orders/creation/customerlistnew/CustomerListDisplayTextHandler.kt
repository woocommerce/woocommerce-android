package com.woocommerce.android.ui.orders.creation.customerlistnew

import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.creation.customerlistnew.CustomerListViewState.CustomerList.Item.Customer
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class CustomerListDisplayTextHandler @Inject constructor(
    private val highlighter: CustomerListSearchResultsHighlighter,
    private val resourceProvider: ResourceProvider,
) {
    operator fun invoke(param: CustomerParam, matchBy: String, searchType: SearchType) =
        when (param) {
            is CustomerParam.Email -> handleEmail(
                param.text,
                matchBy,
                searchType == SearchType.ALL || searchType == SearchType.EMAIL
            )

            is CustomerParam.Name -> handleName(
                param.text,
                matchBy,
                searchType == SearchType.ALL || searchType == SearchType.NAME
            )

            is CustomerParam.Username -> handleUsername(
                param.text,
                matchBy,
                searchType == SearchType.ALL || searchType == SearchType.USERNAME
            )
        }

    private fun handleName(name: String, matchBy: String, highlight: Boolean) =
        if (name.isEmpty()) {
            Customer.Text.Placeholder(
                resourceProvider.getString(R.string.order_creation_customer_search_empty_name)
            )
        } else {
            if (highlight) {
                highlighter(name, matchBy)
            } else {
                Customer.Text.Highlighted(name, 0, 0)
            }
        }

    private fun handleEmail(email: String, matchBy: String, highlight: Boolean) =
        if (email.isEmpty()) {
            Customer.Text.Placeholder(
                resourceProvider.getString(R.string.order_creation_customer_search_empty_email)
            )
        } else {
            if (highlight) {
                highlighter(email, matchBy)
            } else {
                Customer.Text.Highlighted(email, 0, 0)
            }
        }

    private fun handleUsername(username: String, matchBy: String, highlight: Boolean) =
        if (username.isEmpty()) {
            Customer.Text.Placeholder("")
        } else {
            if (highlight) {
                highlighter("· $username", matchBy)
            } else {
                Customer.Text.Highlighted("· $username", 0, 0)
            }
        }

    sealed class CustomerParam(val text: String) {
        data class Name(val value: String) : CustomerParam(value)
        data class Email(val value: String) : CustomerParam(value)
        data class Username(val value: String) : CustomerParam(value)
    }

    enum class SearchType {
        NAME,
        EMAIL,
        USERNAME,
        ALL
    }
}
