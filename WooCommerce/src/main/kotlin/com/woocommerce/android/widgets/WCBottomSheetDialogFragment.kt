package com.woocommerce.android.widgets

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.R
import org.wordpress.android.util.DisplayUtils

/**
 * The default behavior of a bottom sheet in landscape causes it to be cut off after the first item,
 * which makes sense for phones but not large tablets since there's usually plenty of room to show
 * more items. This simple BottomSheetDialogFragment wrapper resolves this by showing the entire
 * sheet on large landscape tablets.
 */
open class WCBottomSheetDialogFragment : BottomSheetDialogFragment() {
    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (DisplayUtils.isXLargeTablet(requireContext()) && DisplayUtils.isLandscape(requireContext())) {
            dialog?.setOnShowListener {
                val dialog = it as BottomSheetDialog
                dialog.findViewById<View>(R.id.design_bottom_sheet)?.let { sheet ->
                    dialog.behavior.peekHeight = sheet.height
                    sheet.parent.parent.requestLayout()
                }
            }
        }
    }
}
