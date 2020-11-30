package com.woocommerce.android.ui.prefs

import android.R.attr
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Checkable
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ViewSettingsToggleOptionBinding
import com.woocommerce.android.util.StyleAttrUtils
import com.woocommerce.android.util.UiHelpers
import kotlinx.android.synthetic.main.view_settings_toggle_option.view.*

class WCSettingsToggleOptionView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.settingsToggleOptionStyle,
    @StyleRes defStyleRes: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr), Checkable {
    private var _binding: ViewSettingsToggleOptionBinding? = null
    private val binding get() = _binding!!

    init {
        _binding = ViewSettingsToggleOptionBinding.inflate(LayoutInflater.from(ctx), this)

        // Sets the selectable background
        val outValue = TypedValue()
        context.theme.resolveAttribute(attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)

        if (attrs != null) {
            val a = context
                    .obtainStyledAttributes(attrs, R.styleable.WCSettingsToggleOptionView, defStyleAttr, defStyleRes)
            try {
                // Set the view title
                StyleAttrUtils.getString(
                        a,
                        isInEditMode,
                        R.styleable.WCSettingsToggleOptionView_toggleOptionTitle,
                        R.styleable.WCSettingsToggleOptionView_tools_toggleOptionTitle
                ).let { UiHelpers.setTextOrHide(toggleSetting_title, it) }

                // Set the view description if it exists, or hide
                StyleAttrUtils.getString(
                        a,
                        isInEditMode,
                        R.styleable.WCSettingsToggleOptionView_toggleOptionDesc,
                        R.styleable.WCSettingsToggleOptionView_tools_toggleOptionDesc
                ).let { UiHelpers.setTextOrHide(toggleSetting_desc, it) }

                // Set the view icon if exists, or hide
                StyleAttrUtils.getResourceId(
                        a,
                        isInEditMode,
                        R.styleable.WCSettingsToggleOptionView_toggleOptionIcon,
                        R.styleable.WCSettingsToggleOptionView_tools_toggleOptionIcon
                ).let { UiHelpers.setImageOrHide(toggleSetting_icon, it, setInvisible = true) }

                // Set the view checked state
                toggleSetting_switch.isChecked = StyleAttrUtils.getBoolean(
                        a,
                        isInEditMode,
                        R.styleable.WCSettingsToggleOptionView_toggleOptionChecked,
                        R.styleable.WCSettingsToggleOptionView_tools_toggleOptionIcon
                )

                setOnClickListener { toggle() }
            } finally {
                a.recycle()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        _binding = null
    }

    var title: String?
        get() = toggleSetting_title.text.toString()
        set(value) { UiHelpers.setTextOrHide(toggleSetting_title, value) }

    var description: String?
        get() = toggleSetting_desc.text.toString()
        set(value) { UiHelpers.setTextOrHide(toggleSetting_desc, value) }

    var iconImageResource: Int? = null
        set(value) { UiHelpers.setImageOrHide(toggleSetting_icon, value) }

    var iconDrawable: Drawable? = null
        set(value) { UiHelpers.setDrawableOrHide(toggleSetting_icon, value) }

    private val checkable: CompoundButton by lazy { toggleSetting_switch }
    var listener: OnCheckedChangeListener? = null

    override fun isChecked() = checkable.isChecked

    override fun toggle() {
        checkable.toggle()
        onCheckChanged()
    }

    override fun setChecked(checked: Boolean) {
        if (checkable.isChecked != checked) {
            checkable.isChecked = checked
            listener?.onCheckedChanged(checkable, isChecked)
        }
    }

    private fun onCheckChanged() {
        listener?.onCheckedChanged(checkable, isChecked)
    }

    /**
     * Allows for sending in lambdas in place of a listener object.
     */
    fun setOnCheckedChangeListener(onCheckedChangeListener: ((CompoundButton, Boolean) -> Unit)?) {
        listener = onCheckedChangeListener?.let {
            OnCheckedChangeListener { buttonView, isChecked -> onCheckedChangeListener(buttonView!!, isChecked) }
        }
    }

    // region Accessibility settings
    override fun getAccessibilityClassName() = this::javaClass.name

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent?) {
        event?.isChecked = isChecked
        super.onInitializeAccessibilityEvent(event)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo?) {
        info?.isCheckable = true
        info?.isChecked = isChecked
        super.onInitializeAccessibilityNodeInfo(info)
    }
    // endregion
}
