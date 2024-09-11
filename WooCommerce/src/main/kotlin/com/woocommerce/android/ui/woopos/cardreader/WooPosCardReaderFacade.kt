package com.woocommerce.android.ui.woopos.cardreader

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderActivity.Companion.WOO_POS_CARD_PAYMENT_RESULT_KEY
import com.woocommerce.android.util.parcelable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosCardReaderFacade @Inject constructor(
    private val cardReaderManager: CardReaderManager
) : DefaultLifecycleObserver {
    private var paymentResultLauncher: ActivityResultLauncher<Intent>? = null
    private var activity: AppCompatActivity? = null

    val readerStatus: Flow<CardReaderStatus> = cardReaderManager.readerStatus

    private val _paymentStatus = MutableStateFlow<WooPosCardReaderPaymentStatus>(
        WooPosCardReaderPaymentStatus.Unknown
    )
    val paymentStatus: Flow<WooPosCardReaderPaymentStatus> = _paymentStatus

    override fun onCreate(owner: LifecycleOwner) {
        activity = owner as AppCompatActivity
        paymentResultLauncher = activity!!.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val paymentResult = if (result.data != null && result.resultCode == AppCompatActivity.RESULT_OK) {
                result.data!!.parcelable<WooPosCardReaderPaymentStatus>(
                    WOO_POS_CARD_PAYMENT_RESULT_KEY
                )
            } else {
                WooPosCardReaderPaymentStatus.Failure
            }
            _paymentStatus.value = paymentResult!!
            _paymentStatus.value = WooPosCardReaderPaymentStatus.Unknown
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        activity = null
        paymentResultLauncher = null
    }

    fun connectToReader() {
        val intent = WooPosCardReaderActivity.buildIntentForCardReaderConnection(activity!!).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        activity!!.startActivity(intent)
    }

    fun collectPayment(orderId: Long) {
        _paymentStatus.value = WooPosCardReaderPaymentStatus.Unknown
        val intent = WooPosCardReaderActivity.buildIntentForPayment(activity!!, orderId).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        paymentResultLauncher!!.launch(intent)
    }

    suspend fun disconnectFromReader() {
        cardReaderManager.disconnectReader()
    }
}
