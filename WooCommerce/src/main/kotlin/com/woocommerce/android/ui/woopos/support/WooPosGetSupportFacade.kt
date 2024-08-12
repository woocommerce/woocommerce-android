package com.woocommerce.android.ui.woopos.support

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosGetSupportFacade @Inject constructor(): DefaultLifecycleObserver {
    private var activity: AppCompatActivity? = null

    override fun onCreate(owner: LifecycleOwner) {
        this.activity = owner as AppCompatActivity
    }

    override fun onDestroy(owner: LifecycleOwner) {
        this.activity = null
    }

    fun openSupportForm() {
        val intent = SupportRequestFormActivity.createIntent(
            context = activity!!,
            origin = HelpOrigin.POS,
            extraTags = ArrayList()
        )
        activity!!.startActivity(intent)
    }
}