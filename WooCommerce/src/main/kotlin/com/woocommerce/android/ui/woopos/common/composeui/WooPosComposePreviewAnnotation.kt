@file:Suppress("MatchingDeclarationName", "Filename")

package com.woocommerce.android.ui.woopos.common.composeui

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@Preview(
    name = "Tablet Big",
    device = Devices.TABLET,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(
    name = "Tablet Small",
    showSystemUi = true,
    device = "spec:width=440dp,height=920dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_TYPE_NORMAL
)
annotation class WooPosPreview
