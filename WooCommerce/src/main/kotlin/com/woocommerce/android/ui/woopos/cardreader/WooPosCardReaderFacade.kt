package com.woocommerce.android.ui.woopos.cardreader

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.util.parcelable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class WooPosCardReaderFacade @Inject constructor(cardReaderManager: CardReaderManager) : DefaultLifecycleObserver {
    private var paymentContinuation: Continuation<WooPosCardReaderPaymentResult>? = null
    private var paymentResultLauncher: ActivityResultLauncher<Intent>? = null
    private var activity: AppCompatActivity? = null

    override fun onCreate(owner: LifecycleOwner) {
        activity = owner as AppCompatActivity
        paymentResultLauncher = activity!!.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val paymentResult = result.data!!.parcelable<WooPosCardReaderPaymentResult>(
                WooPosCardReaderActivity.WOO_POS_CARD_PAYMENT_RESULT_KEY
            )
            paymentContinuation?.resume(paymentResult!!)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        activity = null
        paymentContinuation = null
        paymentResultLauncher = null
    }

    val readerStatus: Flow<CardReaderStatus> = cardReaderManager.readerStatus

    fun connectToReader() {
        activity!!.startActivity(WooPosCardReaderActivity.buildIntentForCardReaderConnection(activity!!))
    }

    suspend fun collectPayment(orderId: Long): WooPosCardReaderPaymentResult {
        return suspendCancellableCoroutine { continuation ->
            paymentContinuation = continuation
            paymentResultLauncher!!.launch(WooPosCardReaderActivity.buildIntentForPayment(activity!!, orderId))
        }
    }
}
