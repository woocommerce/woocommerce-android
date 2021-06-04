package com.woocommerce.android.util

import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.io.InterruptedIOException
import java.net.URL
import javax.inject.Inject

class FileDownloader @Inject constructor(
    private val dispatchers: CoroutineDispatchers
) {
    /**
     * Download the content of the specified url to the [destinationFile]
     *
     * @return true if the download succeeded and false otherwise
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun downloadFile(url: String, destinationFile: File): Boolean {
        return try {
            if (destinationFile.exists()) destinationFile.delete()
            runInterruptible(dispatchers.io) {
                URL(url).openConnection().inputStream.use { inputStream ->
                    destinationFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            }
        } catch (e: InterruptedIOException) {
            WooLog.d(T.UTILS, "Downloading file cancelled")
            destinationFile.delete()
            false
        } catch (e: Exception) {
            WooLog.e(T.UTILS, "Downloading file failed", e)
            destinationFile.delete()
            false
        }
    }
}
