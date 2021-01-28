package com.woocommerce.android.widgets

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
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ViewToggleSingleOptionBinding

/**
 * Custom toggle component that mimics a [android.preference.SwitchPreference]. This view will display
 * a title, a summary, and a switch. The entire view acts as a single component. This is especially useful for
 * TalkBack.
 *
 * This class could eventually be further genericized for even more flexibility.
 */
class WCToggleSingleOptionView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(ctx, attrs, defStyleAttr), Checkable {
    private val binding = ViewToggleSingleOptionBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL

        // Sets the selectable background
        val outValue = TypedValue()
        context.theme.resolveAttribute(attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.WCToggleSingleOptionView)
            try {
                // Set the view title
                binding.switchSettingTitle.text = a.getString(R.styleable.WCToggleSingleOptionView_switchTitle)
                    .orEmpty()

                // Set the summary and switch state
                binding.switchSettingSwitch.isChecked =
                        a.getBoolean(R.styleable.WCToggleSingleOptionView_switchChecked, false)
                binding.switchSettingSwitch.text =
                        a.getString(R.styleable.WCToggleSingleOptionView_switchSummary).orEmpty()

                // Set the component state text to something meaningful - this text is announced to the user after
                // the content description.
                binding.switchSettingSwitch.textOn = resources.getString(R.string.toggle_option_checked)
                binding.switchSettingSwitch.textOff = resources.getString(R.string.toggle_option_not_checked)

                setOnClickListener { toggle() }
            } finally {
                a.recycle()
            }
        }
    }

    private val checkable: CompoundButton by lazy {
        binding.switchSettingSwitch
    }
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

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        binding.switchSettingSwitch.isEnabled = enabled
        binding.switchSettingTitle.isEnabled = enabled
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
