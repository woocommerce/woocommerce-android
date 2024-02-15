package com.woocommerce.android.widgets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.R
import org.wordpress.android.util.DisplayUtils
import kotlin.math.min

/**
 * The default behavior of a bottom sheet in landscape causes it to be cut off after the first item,
 * which makes sense for phones but not large tablets since there's usually plenty of room to show
 * more items. This simple BottomSheetDialogFragment wrapper resolves this by showing the entire
 * sheet on large landscape tablets.
 */
open class WCBottomSheetDialogFragment : BottomSheetDialogFragment {
    private val contentLayoutId: Int?

    constructor() : super() {
        contentLayoutId = null
    }

    constructor(@LayoutRes contentLayoutId: Int) : super() {
        this.contentLayoutId = contentLayoutId
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return if (contentLayoutId != null) {
            inflater.inflate(contentLayoutId, container, false)
        } else {
            null
        }
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (DisplayUtils.isLandscape(requireContext())) {
            dialog?.setOnShowListener {
                val dialog = it as BottomSheetDialog
                dialog.findViewById<View>(org.wordpress.aztec.R.id.design_bottom_sheet)?.let { sheet ->
                    dialog.behavior.peekHeight = if (DisplayUtils.isXLargeTablet(requireContext())) {
                        sheet.height
                    } else {
                        min(DisplayUtils.getWindowPixelHeight(requireContext()) / 2, sheet.height)
                    }
                    sheet.parent.parent.requestLayout()
                }
            }
        }
    }
}
