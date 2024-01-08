package com.woocommerce.android

import android.content.Context
import android.content.Intent
import android.os.Looper
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.tools.SelectedSite.SelectedSiteResetException
import com.woocommerce.android.ui.main.MainActivity
import java.lang.Thread.UncaughtExceptionHandler
import kotlin.system.exitProcess

/**
 * Handles uncaught exceptions where there are chances to recover from them.
 */
class UncaughtErrorsHandler(
    private val context: Context,
    private val baseHandler: UncaughtExceptionHandler?,
    private val crashLogger: CrashLogging
) : UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        when (e.getOriginalCause()) {
            is SelectedSiteResetException -> {
                // This might thrown on specific cases when the user can't access the site:
                // - The user has been removed from the site
                // - The site doesn't have WooCommerce anymore
                // - During logout flow...

                // Send an error report.
                crashLogger.sendReport(e)

                recoverIfNeeded()
            }

            else -> baseHandler?.uncaughtException(t, e) ?: throw e
        }
    }

    private fun Throwable.getOriginalCause(): Throwable {
        return cause?.getOriginalCause() ?: this
    }

    /**
     * If the crash happens on the main thread, the only way to recover is by restarting the app.
     */
    private fun recoverIfNeeded() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )

            exitProcess(0)
        }
    }
}
