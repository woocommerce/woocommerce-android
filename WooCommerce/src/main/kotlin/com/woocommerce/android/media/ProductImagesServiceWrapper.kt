package com.woocommerce.android.media

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.woocommerce.android.util.SystemVersionUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for the product images service - this allows clients to control the service without having a reference
 * to the [Context]
 */
@Singleton
class ProductImagesServiceWrapper @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val JOB_ID = 112233
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun startJobService() {
        val networkRequestBuilder = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, ProductImagesJobService::class.java))
            .setUserInitiated(true)
            .setRequiredNetwork(networkRequestBuilder.build())
            .build()

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(jobInfo)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun stopJobService() {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancel(JOB_ID)
    }

    fun startService() {
        // we can't use foreground services on devices running >API 34
        if (SystemVersionUtils.isAtLeastU()) {
            startJobService()
        } else {
            ContextCompat.startForegroundService(context, Intent(context, ProductImagesService::class.java))
        }
    }

    fun stopService() {
        if (SystemVersionUtils.isAtLeastU()) {
            stopJobService()
        } else {
            context.stopService(Intent(context, ProductImagesService::class.java))
        }
    }
}
