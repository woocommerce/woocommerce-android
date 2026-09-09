package com.woocommerce.android.ui.ageeligibility

import android.content.Context
import com.woocommerce.android.support.help.HelpActivity
import com.woocommerce.android.support.help.HelpOrigin
import javax.inject.Inject

class AgeRestrictionSupportLauncher @Inject constructor() {
    fun open(context: Context) {
        context.startActivity(
            HelpActivity.createIntent(context, HelpOrigin.AGE_RESTRICTION, extraSupportTags = null)
        )
    }
}
