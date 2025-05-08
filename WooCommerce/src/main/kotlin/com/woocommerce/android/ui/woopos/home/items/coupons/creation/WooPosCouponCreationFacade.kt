package com.woocommerce.android.ui.woopos.home.items.coupons.creation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosCouponCreationFacade @Inject constructor(
) : DefaultLifecycleObserver {
    private var activity: AppCompatActivity? = null

    override fun onCreate(owner: LifecycleOwner) {
        activity = owner as AppCompatActivity
    }

    override fun onDestroy(owner: LifecycleOwner) {
        activity = null
    }

    fun createCoupon() {
        val intent = WooPosCouponCreationActivity.buildIntentForCardReaderConnection(activity!!).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
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
