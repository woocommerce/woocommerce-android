package com.woocommerce.android.ui.sitepicker

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.sitepicker.SitePickerAdapter.OnSiteClickListener
import kotlinx.android.synthetic.main.site_picker_view.view.*
import org.wordpress.android.fluxc.generated.WCCoreActionBuilder
import org.wordpress.android.fluxc.model.SiteModel

class SitePiickerView  @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs), OnSiteClickListener {
    private lateinit var siteAdapter: SitePickerAdapter

    init {
        View.inflate(context, R.layout.site_picker_view, this)

        sites_recycler.layoutManager = LinearLayoutManager(context)
        siteAdapter = SitePickerAdapter(context, this)
        sites_recycler.adapter = siteAdapter
    }

    override fun onSiteClick(siteId: Long) {
        // TODO
    }

    fun showSiteList(wcSites: List<SiteModel>, selectedSiteId: Long = 0) {
        if (wcSites.isEmpty()) {
            // TODO: showNoStoresView()
            return
        }

        site_list_label.text = if (wcSites.size == 1)
            context.getString(R.string.login_connected_store)
        else
            context.getString(R.string.login_pick_store)

        siteAdapter.siteList = wcSites

        if (selectedSiteId != 0L) {
            siteAdapter.selectedSiteId = selectedSiteId
        } else {
            siteAdapter.selectedSiteId = wcSites[0].siteId
        }

        // TODO
        /*button_continue.text = getString(R.string.continue_button)
        button_continue.isEnabled = true
        button_continue.setOnClickListener { _ ->
            val site = presenter.getSiteBySiteId(siteAdapter.selectedSiteId)
            site?.let { it ->
                AnalyticsTracker.track(
                        Stat.LOGIN_EPILOGUE_STORE_PICKER_CONTINUE_TAPPED,
                        mapOf(AnalyticsTracker.KEY_SELECTED_STORE_ID to site.id))
                loginProgressDialog = ProgressDialog.show(this, null, getString(R.string.login_verifying_site))
                presenter.verifySiteApiVersion(it)
                // Preemptively also update the site settings so we have them available sooner
                presenter.updateWooSiteSettings(it)
            }
        }*/
    }

    private fun verifySiteApiVersion(site: SiteModel) {
        dispatcher.dispatch(WCCoreActionBuilder.newFetchSiteApiVersionAction(site))
    }
}
