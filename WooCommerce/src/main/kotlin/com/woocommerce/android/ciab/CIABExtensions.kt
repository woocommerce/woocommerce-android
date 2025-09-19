package com.woocommerce.android.ciab

import com.woocommerce.android.ciab.CIABSiteGateKeeper.Companion.CIAB_GARDEN_NAME
import org.wordpress.android.fluxc.model.SiteModel

fun SiteModel.isCIABSite() = isGardenSite && gardenName == CIAB_GARDEN_NAME
