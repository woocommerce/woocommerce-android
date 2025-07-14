package com.woocommerce.android.media

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.Configuration
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Service which uploads device images to the WP media library to be later assigned to a product.
 * This service is used on devices running API 34 and above due to the restrictions on foreground services.
 */
@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class ProductImagesJobService : JobService() {
    companion object {
        private const val MIN_JOB_ID = 0
        private const val MAX_JOB_ID = 10000
    }

    @Inject lateinit var notifHandler: ProductImagesNotificationHandler

    init {
        val builder: Configuration.Builder = Configuration.Builder()
        builder.setJobSchedulerJobIdRange(MIN_JOB_ID, MAX_JOB_ID)
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        notifHandler.attachToService(this, params!!)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        jobFinished(params!!, false)
        return true
    }
}
