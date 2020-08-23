package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.use
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.view_file_picker_chooser.view.*

class FilePickerChooserView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    var title: CharSequence
        get() = chooser_title.text
        set(value) {
            chooser_title.text = value
        }

    init {
        View.inflate(context, R.layout.view_file_picker_chooser, this)
        orientation = VERTICAL
        attrs.let {
            context.obtainStyledAttributes(it, R.styleable.FilePickerChooserView, 0, 0).use { typedArray ->
                val titleText = typedArray.getString(R.styleable.FilePickerChooserView_title)
                if (!titleText.isNullOrEmpty()) {
                    title = titleText
                }
            }
        }
    }

    fun setOnDeviceClickListener(listener: () -> Unit) {
        chooser_device.setOnClickListener { listener.invoke() }
    }

    fun setOnCameraClickListener(listener: () -> Unit) {
        chooser_camera.setOnClickListener { listener.invoke() }
    }

    fun setOnWPMediaLibraryClickListener(listener: () -> Unit) {
        chooser_media_library.setOnClickListener { listener.invoke() }
    }
}
