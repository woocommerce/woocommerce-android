package com.woocommerce.android.media

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.collection.LongSparseArray
import androidx.core.app.JobIntentService
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_IMAGE_UPLOAD_FAILED
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCProductStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

/**
 * Service which uploads device images to the WP media library to be later assigned to a product
 */
@AndroidEntryPoint
class ProductImagesService : Service() {
    @Inject lateinit var notifHandler: ProductImagesNotificationHandler

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notifHandler.attachToService(this)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
