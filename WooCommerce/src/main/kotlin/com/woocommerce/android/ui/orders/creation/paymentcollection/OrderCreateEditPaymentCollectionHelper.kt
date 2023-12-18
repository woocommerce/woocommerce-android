package com.woocommerce.android.ui.orders.creation.paymentcollection

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.TabletOrdersFeatureFlagWrapper
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class OrderCreateEditPaymentCollectionHelper @Inject constructor(
    private val isTabletOrdersM1Enabled: TabletOrdersFeatureFlagWrapper,
    private val resourceProvider: ResourceProvider,
) {
    fun mapToPaymentCollectionSectionsState(order: Order): PaymentCollectionSectionsState {
        return if (isTabletOrdersM1Enabled()) {
            PaymentCollectionSectionsState.Shown(
                button = PaymentCollectionSectionsState.Button(
                    text = resourceProvider.getString(R.string.order_creation_collect_payment_button),
                    onClick = {
                        // TODO
                    }
                )
            )
        } else {
            PaymentCollectionSectionsState.Hidden
        }
    }
}

sealed class PaymentCollectionSectionsState {
    data class Shown(
        val button: Button
    ): PaymentCollectionSectionsState()

    object Hidden : PaymentCollectionSectionsState()

    data class Button(
        val text: String,
        val onClick: () -> Unit
    )
}
