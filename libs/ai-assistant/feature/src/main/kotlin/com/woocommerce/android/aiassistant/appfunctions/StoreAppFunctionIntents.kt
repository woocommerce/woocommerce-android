package com.woocommerce.android.aiassistant.appfunctions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject

internal const val EXTRA_REQUEST_ID = "appfunctions_request_id"
internal const val EXTRA_TOOL_NAME = "appfunctions_tool_name"
internal const val EXTRA_TOOL_ARGUMENTS = "appfunctions_tool_arguments"
internal const val EXTRA_ORDER_ID = "appfunctions_order_id"
internal const val EXTRA_SITE_LOCAL_ID = "appfunctions_site_local_id"
internal const val EXTRA_STORE_URL = "appfunctions_store_url"
internal const val EXTRA_STORE_NAME = "appfunctions_store_name"

internal fun SavedStateHandle.requestedStore(): StoreIdentity? {
    val localSiteId: Int? = get(EXTRA_SITE_LOCAL_ID)
    val url: String? = get(EXTRA_STORE_URL)
    return if (localSiteId == null || url == null) null else StoreIdentity(localSiteId, url)
}

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
internal class StoreAppFunctionIntents @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun confirmation(call: ToolCall, store: StoreIdentity, storeName: String): PendingIntent = pendingActivity(
        intent = Intent(context, AppFunctionConfirmationActivity::class.java)
            .putExtra(EXTRA_REQUEST_ID, call.id)
            .putExtra(EXTRA_TOOL_NAME, call.name)
            .putExtra(EXTRA_TOOL_ARGUMENTS, call.arguments.toString())
            .putStore(store, storeName)
            .setIdentifier(call.id),
        flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT,
    )

    fun openOrder(orderId: Long, store: StoreIdentity, storeName: String): PendingIntent = pendingActivity(
        intent = Intent(context, AppFunctionOpenOrderActivity::class.java)
            .putExtra(EXTRA_ORDER_ID, orderId)
            .putStore(store, storeName)
            .setIdentifier(UUID.randomUUID().toString()),
        flags = PendingIntent.FLAG_IMMUTABLE,
    )

    private fun pendingActivity(intent: Intent, flags: Int): PendingIntent =
        PendingIntent.getActivity(context, 0, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags)

    private fun Intent.putStore(store: StoreIdentity, storeName: String): Intent =
        putExtra(EXTRA_SITE_LOCAL_ID, store.localSiteId)
            .putExtra(EXTRA_STORE_URL, store.url)
            .putExtra(EXTRA_STORE_NAME, storeName)
}
