package com.woocommerce.android.ui.woopos.cardreader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosCardReaderFacade @Inject constructor(
    private val cardReaderManager: CardReaderManager
) : DefaultLifecycleObserver {
    private var activity: AppCompatActivity? = null

    val readerStatus: StateFlow<CardReaderStatus> = cardReaderManager.readerStatus

    override fun onCreate(owner: LifecycleOwner) {
        activity = owner as AppCompatActivity
    }

    override fun onDestroy(owner: LifecycleOwner) {
        activity = null
    }

    fun connectToReader() {
        val intent = WooPosCardReaderActivity.buildIntentForCardReaderConnection(activity!!).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    suspend fun disconnectFromReader() {
        cardReaderManager.disconnectReader()
    }

    private fun startActivity(intent: Intent) {
        val options = ActivityOptionsCompat.makeCustomAnimation(
            activity!!,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        ActivityCompat.startActivity(activity!!, intent, options.toBundle())
    }
}
