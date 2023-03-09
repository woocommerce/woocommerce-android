package com.woocommerce.android.ui.plans.trial

import com.woocommerce.android.ui.plans.repository.SitePlanRepository.Companion.FREE_TRIAL_PLAN_ID
import org.wordpress.android.fluxc.model.SiteModel

val SiteModel?.isFreeTrial: Boolean
    get() = this?.planId == FREE_TRIAL_PLAN_ID
