package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.subscription.WCSubscriptionModel
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

class SubscriptionMapper @Inject constructor() {
    fun toAppModel(model: WCSubscriptionModel): Subscription {
        return Subscription(
            id = model.subscriptionId,
            status = Subscription.Status.fromValue(model.status),
            billingPeriod = Subscription.Period.fromValue(model.billingPeriod),
            billingInterval = model.billingInterval,
            total = model.total.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            startDate = DateTimeUtils.dateUTCFromIso8601(model.startDate) ?: Date(),
            endDate = DateTimeUtils.dateUTCFromIso8601(model.endDate),
            currency = model.currency
        )
    }
}
