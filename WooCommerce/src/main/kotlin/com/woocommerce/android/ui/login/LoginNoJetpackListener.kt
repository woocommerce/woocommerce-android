package com.woocommerce.android.ui.login

interface LoginNoJetpackListener {
    fun showJetpackInstructions()
    fun showJetpackTroubleshootingTips()
    fun showWhatIsJetpackDialog()
    fun showEmailLoginScreen(siteAddress: String? = null)
    fun showUsernamePasswordScreen(
        siteAddress: String?,
        endpointAddress: String?,
        inputUsername: String?,
        inputPassword: String?
    )
}
