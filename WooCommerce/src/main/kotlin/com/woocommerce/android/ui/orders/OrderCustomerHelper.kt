package com.woocommerce.android.ui.orders

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.Order
import org.wordpress.android.util.ToastUtils
import java.util.Locale

object OrderCustomerHelper {
    enum class Action {
        EMAIL,
        CALL,
        SMS
    }

    fun createEmail(
        context: Context,
        order: Order,
        emailAddr: String
    ) {
        AnalyticsTracker.track(
            Stat.ORDER_CONTACT_ACTION, mapOf(
            AnalyticsTracker.KEY_ID to order.remoteId,
            AnalyticsTracker.KEY_STATUS to order.status,
            AnalyticsTracker.KEY_TYPE to Action.EMAIL.name.toLowerCase(Locale.US))
        )

        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:$emailAddr") // only email apps should handle this
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            AnalyticsTracker.track(
                Stat.ORDER_CONTACT_ACTION_FAILED,
                this.javaClass.simpleName,
                e.javaClass.simpleName, "No e-mail app was found")

            ToastUtils.showToast(context, R.string.error_no_email_app)
        }
    }

    fun dialPhone(
        context: Context,
        order: Order,
        phone: String
    ) {
        AnalyticsTracker.track(
            Stat.ORDER_CONTACT_ACTION, mapOf(
            AnalyticsTracker.KEY_ID to order.remoteId,
            AnalyticsTracker.KEY_STATUS to order.status,
            AnalyticsTracker.KEY_TYPE to Action.CALL.name.toLowerCase(Locale.US))
        )

        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phone")
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            AnalyticsTracker.track(
                Stat.ORDER_CONTACT_ACTION_FAILED,
                this.javaClass.simpleName,
                e.javaClass.simpleName, "No phone app was found")

            ToastUtils.showToast(context, R.string.error_no_phone_app)
        }
    }

    fun sendSms(
        context: Context,
        order: Order,
        phone: String
    ) {
        AnalyticsTracker.track(
            Stat.ORDER_CONTACT_ACTION, mapOf(
            AnalyticsTracker.KEY_ID to order.remoteId,
            AnalyticsTracker.KEY_STATUS to order.status,
            AnalyticsTracker.KEY_TYPE to Action.SMS.name.toLowerCase(Locale.US)))

        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:$phone")
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            AnalyticsTracker.track(
                Stat.ORDER_CONTACT_ACTION_FAILED,
                this.javaClass.simpleName,
                e.javaClass.simpleName, "No SMS app was found")

            ToastUtils.showToast(context, R.string.error_no_sms_app)
        }
    }
}
