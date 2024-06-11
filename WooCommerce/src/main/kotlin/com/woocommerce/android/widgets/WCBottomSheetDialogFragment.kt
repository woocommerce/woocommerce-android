package com.woocommerce.android.widgets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.util.DisplayUtils

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
        dialog?.setOnShowListener {
            val dialog = it as BottomSheetDialog
            dialog.findViewById<View>(org.wordpress.aztec.R.id.design_bottom_sheet)?.let { sheet ->
                // if device height is 32dp bigger than sheet height, show full sheet
                val heightPixels = view.context.resources.displayMetrics.heightPixels
                val topPadding = DisplayUtils.dpToPx(context, TOP_OFFSET_BEFORE_SHOWING_FULL_SHEET_DP)
                if (heightPixels - topPadding > sheet.height) {
                    dialog.behavior.peekHeight = DisplayUtils.getWindowPixelHeight(requireContext())
                }
                sheet.parent.parent.requestLayout()
            }
        }
    }

    private companion object {
        private const val TOP_OFFSET_BEFORE_SHOWING_FULL_SHEET_DP = 32
    }
}
