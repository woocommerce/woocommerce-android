package com.woocommerce.android.ui.prefs

import android.R.attr
import android.content.Context
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

class WCSettingsToggleOptionView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.settingsToggleOptionStyle,
    @StyleRes defStyleRes: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr), Checkable {
    private val binding = ViewSettingsToggleOptionBinding.inflate(LayoutInflater.from(ctx), this)

    init {
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
                ).let { UiHelpers.setTextOrHide(binding.toggleSettingTitle, it) }

                // Set the view description if it exists, or hide
                StyleAttrUtils.getString(
                    a,
                    isInEditMode,
                    R.styleable.WCSettingsToggleOptionView_toggleOptionDesc,
                    R.styleable.WCSettingsToggleOptionView_tools_toggleOptionDesc
                ).let { UiHelpers.setTextOrHide(binding.toggleSettingDesc, it) }

                // Set the view icon if exists, or hide
                StyleAttrUtils.getResourceId(
                    a,
                    isInEditMode,
                    R.styleable.WCSettingsToggleOptionView_toggleOptionIcon,
                    R.styleable.WCSettingsToggleOptionView_tools_toggleOptionIcon
                ).let {
                    UiHelpers.setImageOrHideInLandscapeOnNonExpandedScreenSizes(
                        binding.toggleSettingIcon,
                        it,
                        setInvisible = true
                    )
                }

                // Set the view checked state
                binding.toggleSettingSwitch.isChecked = StyleAttrUtils.getBoolean(
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

    var title: String?
        get() = binding.toggleSettingTitle.text.toString()
        set(value) { UiHelpers.setTextOrHide(binding.toggleSettingTitle, value) }

    var description: String?
        get() = binding.toggleSettingDesc.text.toString()
        set(value) { UiHelpers.setTextOrHide(binding.toggleSettingDesc, value) }

    private val checkable: CompoundButton by lazy { binding.toggleSettingSwitch }
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
