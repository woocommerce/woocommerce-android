package com.woocommerce.android.ui.aisupportchat.diagnostics

/**
 * Identifies an individual diagnostic check run by [SupportDiagnosticsService].
 * Each value maps to a use case under `ui/troubleshooting/useCases/`.
 */
enum class DiagnosticTest {
    INTERNET_CONNECTION,
    WPCOM_SERVERS,
    STORE_CONNECTION,
    STORE_ORDERS,
    STORE_PRODUCTS,
    ANALYTICS_SETTING,
    NOTIFICATION_PERMISSION,
    APP_NOTIFICATIONS_ENABLED,
    NOTIFICATION_CHANNELS_ENABLED,
    PUSH_NOTIFICATION_TOKEN,
    PUSH_NOTIFICATION_REGISTRATION
}
