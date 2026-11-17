package com.woocommerce.android.ui.woopos.roles.pin

import android.util.AtomicFile
import java.io.File

internal class WooPosPINStorageFiles(
    val directory: File,
    private val extension: String,
) {
    fun checkDirectoryAccess() {
        check(!directory.exists() || (directory.isDirectory && directory.canRead() && directory.canExecute()))
    }

    fun exists(file: AtomicFile): Boolean =
        file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()

    fun file(localSiteId: Int): AtomicFile {
        require(localSiteId > 0)
        return AtomicFile(File(directory, "$localSiteId.$extension"))
    }
}
