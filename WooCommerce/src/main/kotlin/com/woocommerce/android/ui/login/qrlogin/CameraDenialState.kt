package com.woocommerce.android.ui.login.qrlogin

/**
 * Dialog state shown on the QR login prologue when the user denies the camera permission.
 *
 * The two states map directly to Android's permission lifecycle: [FirstDenial] when the system
 * will re-prompt on the next request (i.e. `shouldShowRequestPermissionRationale` returns
 * `true`), and [PermanentlyDenied] once the user has selected "Don't ask again" or denied twice
 * — Settings is then the only path back. The "Enter store address" fallback is offered in both
 * states.
 */
enum class CameraDenialState { Hidden, FirstDenial, PermanentlyDenied }
