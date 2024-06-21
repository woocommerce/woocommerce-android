package com.woocommerce.android.wear.extensions

import com.google.android.gms.wearable.DataMap
import com.woocommerce.commons.DataParameters
import org.wordpress.android.fluxc.model.SiteModel

fun DataMap.getSiteId(selectedSite: SiteModel?) =
    getLong(DataParameters.SITE_ID.value, -1)
        .takeIf { it != -1L }
        ?: selectedSite?.siteId
        ?: 0L
