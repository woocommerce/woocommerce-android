package com.woocommerce.android.media

import android.content.Intent

import com.woocommerce.android.util.WooLog
import androidx.core.app.JobIntentService

/**
 * service which uploads photos to the WP media library
 */

class MediaUploadService : JobIntentService() {
    override fun onCreate() {
        super.onCreate()
        WooLog.i(WooLog.T.MEDIA, "media upload service > created")
    }

    override fun onDestroy() {
        WooLog.i(WooLog.T.MEDIA, "media upload service > destroyed")
        super.onDestroy()
    }

    override fun onHandleWork(intent: Intent) {
        // TODO
    }

    override fun onStopCurrentWork(): Boolean {
        // this Service was failing silently if it couldn't get to update its data, so
        // that hints us that we shouldn't really care about rescheduling this job
        // in the case something failed.
        return false
    }
}
