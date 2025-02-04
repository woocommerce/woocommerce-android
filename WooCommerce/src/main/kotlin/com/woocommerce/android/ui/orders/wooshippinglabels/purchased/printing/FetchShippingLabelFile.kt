package com.woocommerce.android.ui.orders.wooshippinglabels.purchased.printing

import android.content.Context
import android.os.Environment
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.util.Base64Decoder
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FetchShippingLabelFile @Inject constructor(
    private val appContext: Context,
    private val selectedSite: SelectedSite,
    private val dispatchers: CoroutineDispatchers,
    private val fileUtils: FileUtils,
    private val base64Decoder: Base64Decoder,
    private val labelRepository: WooShippingLabelRepository
) {
    private val storageDir: File?
        get() = appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

    suspend operator fun invoke(
        labelIds: List<Long>,
        paperSize: String
    ): File? {
        val response = selectedSite.getOrNull()
            ?.let { labelRepository.fetchShippingLabelPrinting(it, labelIds, paperSize) }
            ?.takeIf { it.isError.not() }
            ?.model?.b64Content
            ?.takeIf { it.isNotNullOrEmpty() }
            ?: return null

        return withContext(dispatchers.io) {
            storageDir?.let {
                createShippingLabelFile(it, response)
            }
        }
    }

    private fun createShippingLabelFile(
        storageDirectory: File,
        shippingLabelData: String
    ) = fileUtils.createTempTimeStampedFile(
        storageDir = storageDirectory,
        prefix = PDF_PREFIX,
        fileExtension = PDF_EXTENSION
    )?.let {
        fileUtils.writeContentToFile(
            file = it,
            content = base64Decoder.decode(shippingLabelData, 0)
        )
    }

    companion object {
        const val PDF_PREFIX = "PDF"
        const val PDF_EXTENSION = "pdf"
    }
}
