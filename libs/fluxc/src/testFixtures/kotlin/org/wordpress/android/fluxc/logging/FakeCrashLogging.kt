package org.wordpress.android.fluxc.logging

object FakeCrashLogging : FluxCCrashLogger {
    override fun recordEvent(message: String, category: String?) = println("FakeCrashLogging: [$category] $message")

    override fun recordException(exception: Throwable, category: String?) =
        println("FakeCrashLogging: [$category] $exception")

    override fun sendReport(
        exception: Throwable?,
        tags: Map<String, String>,
        message: String?
    ) = println("FakeCrashLogging: $tags $message $exception")
}
