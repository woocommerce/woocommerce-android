package com.woocommerce.android.ui.woopos.home.items.coupons.creation

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * A facade that integrates Coupons Creation flow into the POS.
 * This class manages the lifecycle of an `ActivityResultLauncher` and provides a suspending function
 * to launch the coupon creation activity and retrieve the resulting coupon ID or null if the flow was cancelled.
 *
 * Note: This class currently doesn't handle orientation changes and process death - considering POS is locked in
 * landscape and not receiving result of the newly created coupon isn't critical.
 */
@ActivityRetainedScoped
class WooPosCouponCreationFacade @Inject constructor() : DefaultLifecycleObserver {
    private var activityWR: WeakReference<AppCompatActivity>? = null

    private var couponCreationLauncher: ActivityResultLauncher<Intent>? = null
    private var cancellableContinuation: CancellableContinuation<Long?>? = null

    override fun onCreate(owner: LifecycleOwner) {
        activityWR = WeakReference(owner as AppCompatActivity)
        couponCreationLauncher = owner.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val couponId = extractCouponIdFromResult(result)
            cancellableContinuation?.resume(couponId)
            cancellableContinuation = null
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cancellableContinuation?.resume(null)
        cancellableContinuation = null
        couponCreationLauncher?.unregister()
        couponCreationLauncher = null
        activityWR = null
    }

    suspend fun createCoupon(): Long? = withContext(Dispatchers.Main) {
        val currentActivityWR = activityWR ?: return@withContext null

        suspendCancellableCoroutine { continuation ->
            cancellableContinuation = continuation
            currentActivityWR.get()?.let { launchCouponCreationActivity(it) } ?: continuation.resume(null)
        }
    }

    private fun launchCouponCreationActivity(
        activity: AppCompatActivity,
    ) {
        val intent = WooPosCouponCreationActivity.buildIntentForCouponCreation(activity).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val options = ActivityOptionsCompat.makeCustomAnimation(
            activity,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        couponCreationLauncher!!.launch(intent, options)
    }

    private fun extractCouponIdFromResult(result: androidx.activity.result.ActivityResult): Long? {
        return if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
            val id = result.data?.getLongExtra(
                WooPosCouponCreationActivity.WOO_POS_COUPON_CREATION_NEW_COUPON_ID,
                -1L
            )
            if (id != -1L) id else null
        } else {
            null
        }
    }
}
