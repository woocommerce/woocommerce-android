package com.woocommerce.android.ui.products

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.media.MediaUploadService
import com.woocommerce.android.media.MediaUploadService.Companion.OnProductMediaUploadEvent
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.roundToInt

@OpenClassOnDebug
class ProductDetailViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val productRepository: ProductDetailRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(mainDispatcher) {
    private var remoteProductId = 0L

    private val product = MutableLiveData<Product>()
    private val parameters = MutableLiveData<Parameters>()

    private val _productData = MediatorLiveData<ProductWithParameters>()
    val productData: LiveData<ProductWithParameters> = _productData

    private val _isSkeletonShown = MutableLiveData<Boolean>()
    val isSkeletonShown: LiveData<Boolean> = _isSkeletonShown

    private val _shareProduct = SingleLiveEvent<Product>()
    val shareProduct: LiveData<Product> = _shareProduct

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _isUploadingProductImage = MutableLiveData<Boolean>()
    val isUploadingProductImage: LiveData<Boolean> = _isUploadingProductImage

    private val _exit = SingleLiveEvent<Unit>()
    val exit: LiveData<Unit> = _exit

    var capturedPhotoPath: String? = null

    init {
        _productData.addSource(product) { prod ->
            parameters.value?.let { params ->
                _productData.value = combineData(prod, params)
            }
        }
        _productData.addSource(parameters) { params ->
            product.value?.let { prod ->
                _productData.value = combineData(prod, params)
            }
        }

        EventBus.getDefault().register(this)
    }

    fun start(remoteProductId: Long) {
        loadProduct(remoteProductId)
    }

    fun onShareButtonClicked() {
        _shareProduct.value = product.value
    }

    override fun onCleared() {
        super.onCleared()

        productRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    fun reloadProduct() {
        product.value = productRepository.getProduct(remoteProductId)
    }

    private fun loadProduct(remoteProductId: Long) {
        loadParameters()

        _isUploadingProductImage.value = MediaUploadService.isUploadingForProduct(remoteProductId)

        val shouldFetch = remoteProductId != this.remoteProductId
        this.remoteProductId = remoteProductId

        launch {
            val productInDb = productRepository.getProduct(remoteProductId)
            if (productInDb != null) {
                product.value = productInDb
                if (shouldFetch) {
                    fetchProduct(remoteProductId)
                }
            } else {
                _isSkeletonShown.value = true
                fetchProduct(remoteProductId)
            }
            _isSkeletonShown.value = false
        }
    }

    private fun loadParameters() {
        val currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
        val (weightUnit, dimensionUnit) = wooCommerceStore.getProductSettings(selectedSite.get())?.let { settings ->
            return@let Pair(settings.weightUnit, settings.dimensionUnit)
        } ?: Pair(null, null)
        parameters.value = Parameters(currencyCode, weightUnit, dimensionUnit)
    }

    private suspend fun fetchProduct(remoteProductId: Long) {
        if (networkStatus.isConnected()) {
            val fetchedProduct = productRepository.fetchProduct(remoteProductId)
            if (fetchedProduct != null) {
                product.value = fetchedProduct
            } else {
                _showSnackbarMessage.value = R.string.product_detail_fetch_product_error
                _exit.call()
            }
        } else {
            _showSnackbarMessage.value = R.string.offline_error
            _isSkeletonShown.value = false
        }
    }

    private fun formatCurrency(amount: BigDecimal?, currencyCode: String?): String {
        return currencyCode?.let {
            currencyFormatter.formatCurrency(amount ?: BigDecimal.ZERO, it)
        } ?: amount.toString()
    }

    private fun combineData(product: Product, parameters: Parameters): ProductWithParameters {
        val weight = if (product.weight > 0) "${format(product.weight)}${parameters.weightUnit ?: ""}" else ""

        val hasLength = product.length > 0
        val hasWidth = product.width > 0
        val hasHeight = product.height > 0
        val unit = parameters.dimensionUnit ?: ""
        val size = if (hasLength && hasWidth && hasHeight) {
            "${format(product.length)} x ${format(product.width)} x ${format(product.height)} $unit"
        } else if (hasWidth && hasHeight) {
            "${format(product.width)} x ${format(product.height)} $unit"
        } else {
            ""
        }.trim()

        return ProductWithParameters(
                product,
                weight,
                size,
                formatCurrency(product.price, parameters.currencyCode),
                formatCurrency(product.salePrice, parameters.currencyCode),
                formatCurrency(product.regularPrice, parameters.currencyCode)
        )
    }

    private fun format(number: Float): String {
        val int = number.roundToInt()
        return if (number != int.toFloat()) {
            number.toString()
        } else {
            int.toString()
        }
    }

    fun uploadCapturedImage(context: Context, remoteProductId: Long) {
        Uri.parse(capturedPhotoPath)?.let { uri ->
            uploadProductMedia(context, remoteProductId, uri)
        }
        capturedPhotoPath = null
    }

    fun uploadProductMedia(context: Context, remoteProductId: Long, localImageUri: Uri) {
        // TODO: at some point we want to support uploading multiple product images
        if (MediaUploadService.isBusy()) {
            _showSnackbarMessage.value = R.string.product_image_already_uploading
            return
        }
        _isUploadingProductImage.value = true
        MediaUploadService.uploadProductMedia(context, remoteProductId, localImageUri)
    }

    /**
     * Create an intent for capturing a device photo
     */
    fun createCaptureImageIntent(context: Context): Intent? {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            // Ensure that there's a camera activity to handle the intent
            intent.resolveActivity(context.packageManager)?.also {
                createCaptureImageFile(context)?.also { file ->
                    capturedPhotoPath = file.absolutePath
                    val authority = context.applicationContext.packageName + ".provider"
                    val photoURI: Uri = FileProvider.getUriForFile(
                            context,
                            authority,
                            file
                    )
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    return intent
                }
            }
        }

        WooLog.w(T.MEDIA, "ProductDetailViewModel > unable to create capture intent")
        _showSnackbarMessage.value = R.string.product_image_capture_failed
        return null
    }

    /**
     * Creates a temporary file for captured photos
     */
    private fun createCaptureImageFile(context: Context): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile(
                    "JPEG_${timeStamp}_",
                    ".jpg",
                    storageDir
            )
        } catch (ex: IOException) {
            WooLog.e(T.MEDIA, ex)
            null
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductMediaUploadEvent) {
        _isUploadingProductImage.value = false
        if (event.isError) {
            _showSnackbarMessage.value = R.string.product_image_upload_error
        } else {
            reloadProduct()
        }
    }

    data class Parameters(
        val currencyCode: String?,
        val weightUnit: String?,
        val dimensionUnit: String?
    )

    data class ProductWithParameters(
        val product: Product,
        val weightWithUnits: String,
        val sizeWithUnits: String,
        val priceWithCurrency: String,
        val salePriceWithCurrency: String,
        val regularPriceWithCurrency: String
    )
}
