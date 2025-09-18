package com.woocommerce.android.ciab

import com.woocommerce.android.ciab.CIABSiteGateKeeper.Companion.CIAB_GARDEN_NAME
import com.woocommerce.android.tools.SelectedSite

fun SelectedSite.isCurrentSiteCIAB(): Boolean =
    this.getOrNull()?.let { it.isGardenSite && it.gardenName == CIAB_GARDEN_NAME } ?: false
