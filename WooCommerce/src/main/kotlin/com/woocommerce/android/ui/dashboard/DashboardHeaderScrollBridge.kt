package com.woocommerce.android.ui.dashboard

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import com.woocommerce.android.ui.compose.designsystem.component.WooPageHeaderScrollBehavior

/**
 * The XML Dashboard places its JITM FragmentContainerView between separate header and body ComposeView roots, so
 * the header's remembered [WooPageHeaderScrollBehavior] cannot be shared directly. This bridge is scoped to the
 * view lifecycle and can be removed when the Dashboard moves to one Compose root.
 */
internal class DashboardHeaderScrollBridge {
    private data class Attachment(
        val identity: Any,
        val nestedScrollConnection: NestedScrollConnection,
        val expand: suspend () -> Unit,
    )

    private val attachmentState = mutableStateOf<Attachment?>(null, referentialEqualityPolicy())

    val nestedScrollConnection: NestedScrollConnection?
        get() = attachmentState.value?.nestedScrollConnection

    fun attach(behavior: WooPageHeaderScrollBehavior) {
        attach(
            identity = behavior,
            nestedScrollConnection = behavior.nestedScrollConnection,
            expand = behavior::expand,
        )
    }

    fun detach(behavior: WooPageHeaderScrollBehavior) {
        detach(identity = behavior)
    }

    internal fun attach(
        identity: Any,
        nestedScrollConnection: NestedScrollConnection,
        expand: suspend () -> Unit,
    ) {
        if (attachmentState.value?.identity !== identity) {
            attachmentState.value = Attachment(identity, nestedScrollConnection, expand)
        }
    }

    internal fun detach(identity: Any) {
        if (attachmentState.value?.identity === identity) {
            attachmentState.value = null
        }
    }

    suspend fun scrollToTop(scrollBodyToTop: suspend () -> Unit) {
        scrollBodyToTop()
        attachmentState.value?.expand?.invoke()
    }
}
