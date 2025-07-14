package com.woocommerce.android.ui.orders.creation.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.DimenRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.use
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderCreationSectionBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show

class OrderCreateEditSectionView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderCreationSectionBinding.inflate(LayoutInflater.from(ctx), this, true)

    var isEachAddButtonEnabled
        get() = binding.addButtonsLayout.children.all { it.isEnabled }
        set(value) {
            fun View.adjustState(isEnabled: Boolean) {
                if (this is ViewGroup) {
                    this.children.forEach {
                        it.adjustState(isEnabled)
                    }
                } else {
                    this.isEnabled = isEnabled
                }
            }

            binding.addButtonsLayout.children.forEach {
                it.adjustState(value)
            }
        }

    var header: CharSequence
        get() = binding.headerLabel.text
        set(value) {
            binding.headerLabel.text = value
        }

    var content: View?
        get() = binding.contentLayout.children.firstOrNull()
        set(value) {
            updateContent(value)
        }

    val barcodeIcon: ImageView
        get() = binding.barcodeIcon

    val addIcon: ImageView
        get() = binding.addIcon

    private var keepAddButtons: Boolean = false
    private var hasEditButton: Boolean = true

    var isLocked: Boolean = false
        set(value) {
            field = value
            binding.lockIcon.isVisible = value
        }

    init {
        attrs?.let {
            context.obtainStyledAttributes(attrs, R.styleable.OrderCreateEditSectionView, defStyleAttr, 0)
                .use { a ->
                    header = a.getString(R.styleable.OrderCreateEditSectionView_header).orEmpty()
                    keepAddButtons = a.getBoolean(R.styleable.OrderCreateEditSectionView_keepAddButtons, keepAddButtons)
                    hasEditButton = a.getBoolean(R.styleable.OrderCreateEditSectionView_hasEditButton, hasEditButton)
                }
        }
    }

    fun showAddProductsHeaderActions() {
        binding.addIcon.show()
        binding.barcodeIcon.show()
    }

    fun showScanProductsHeaderAction() {
        binding.barcodeIcon.show()
    }

    fun showAddAction() {
        binding.addIcon.show()
    }

    fun hideAddAction() {
        binding.addIcon.hide()
    }

    fun hideAddProductsHeaderActions() {
        binding.addIcon.hide()
        binding.barcodeIcon.hide()
    }
    fun hideHeader() {
        binding.headerLabel.hide()
    }

    fun showHeader() {
        binding.headerLabel.show()
    }

    fun removeProductsButtons() {
        binding.addButtonsLayout.removeAllViews()
    }

    fun setAddButtons(buttons: List<AddButton>) {
        binding.addButtonsLayout.removeAllViews()
        buttons.forEach { buttonModel ->
            val button = MaterialButton(context, null, R.attr.secondaryTextButtonStyle)
            button.text = buttonModel.text
            button.icon = AppCompatResources.getDrawable(context, R.drawable.ic_add)
            button.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            button.setOnClickListener { buttonModel.onClickListener() }
            binding.addButtonsLayout.addView(
                button,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    fun setProductSectionButtons(
        addProductsButton: AddButton? = null,
        addCustomAmountsButton: AddButton? = null,
        addProductsViaScanIconButton: AddButton? = null,
        addProductsViaScanButton: AddButton? = null,
    ) {
        binding.addButtonsLayout.removeAllViews()
        val container = RelativeLayout(context)
        val addProductsManuallyButtonId = View.generateViewId()
        addProductsButton(
            addProductsButton = addProductsButton,
            container = container,
            id = addProductsManuallyButtonId
        )
        addProductsViaScanIconButton(
            addProductsViaScanIconButton = addProductsViaScanIconButton,
            container = container
        )
        val addProductsButtonId = if (addProductsButton == null) null else addProductsManuallyButtonId
        addProductsViaScanButton(
            addProductsViaScanButton = addProductsViaScanButton,
            container = container,
            addingProductsManuallyButtonId = addProductsButtonId,
            id = addProductsManuallyButtonId,
        )
        addCustomAmountsButton(
            addCustomAmountsButton = addCustomAmountsButton,
            container = container,
            addingProductsManuallyButtonId = addProductsManuallyButtonId,
        )
        binding.addButtonsLayout.addView(container)
    }

    fun setCustomAmountsSectionButtons(
        addCustomAmountsButton: AddButton
    ) {
        binding.addButtonsLayout.removeAllViews()
        val container = RelativeLayout(context)
        addCustomAmountsButton(addCustomAmountsButton, container, null)
        binding.addButtonsLayout.addView(container)
    }

    fun removeCustomSectionButtons() {
        binding.addButtonsLayout.removeAllViews()
    }

    private fun addCustomAmountsButton(
        addCustomAmountsButton: AddButton?,
        container: RelativeLayout,
        addingProductsManuallyButtonId: Int?
    ) {
        addCustomAmountsButton?.let {
            val addingCustomAmountsButton = MaterialButton(context, null, R.attr.secondaryTextButtonStyle)
            addingCustomAmountsButton.text = addCustomAmountsButton.text
            addingCustomAmountsButton.icon = AppCompatResources.getDrawable(context, R.drawable.ic_add)
            addingCustomAmountsButton.setOnClickListener { addCustomAmountsButton.onClickListener() }
            val addCustomAmountsButtonParams = RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            addCustomAmountsButtonParams.addRule(RelativeLayout.ALIGN_PARENT_START)
            addingProductsManuallyButtonId?.let {
                addCustomAmountsButtonParams.addRule(RelativeLayout.BELOW, it)
            }
            addingCustomAmountsButton.layoutParams = addCustomAmountsButtonParams
            container.addView(addingCustomAmountsButton)
        }
    }

    private fun addProductsViaScanIconButton(
        addProductsViaScanIconButton: AddButton?,
        container: RelativeLayout
    ) {
        addProductsViaScanIconButton?.let {
            val addingProductsViaScanningButton = ImageView(context, null)
            addingProductsViaScanningButton.setImageResource(R.drawable.ic_barcode)
            val margins = resources.getDimensionPixelSize(R.dimen.major_100)
            addingProductsViaScanningButton.setOnClickListener { addProductsViaScanIconButton.onClickListener() }
            val addProductsViaScanningButtonParams = RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            addProductsViaScanningButtonParams.setMargins(margins)
            addProductsViaScanningButtonParams.addRule(RelativeLayout.ALIGN_PARENT_END)
            addingProductsViaScanningButton.layoutParams = addProductsViaScanningButtonParams
            container.addView(addingProductsViaScanningButton)
        }
    }

    private fun addProductsViaScanButton(
        addProductsViaScanButton: AddButton?,
        container: RelativeLayout,
        addingProductsManuallyButtonId: Int?,
        id: Int,
    ) {
        addProductsViaScanButton ?: return
        val addProductButtonsParams = RelativeLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        addProductButtonsParams.addRule(RelativeLayout.ALIGN_PARENT_START)
        val scanToAddProductButton = MaterialButton(context, null, R.attr.secondaryTextButtonStyle)
        scanToAddProductButton.text = addProductsViaScanButton.text
        scanToAddProductButton.icon = AppCompatResources.getDrawable(context, R.drawable.ic_barcode)
        scanToAddProductButton.id = id
        addingProductsManuallyButtonId?.let {
            addProductButtonsParams.addRule(RelativeLayout.BELOW, it)
        }
        scanToAddProductButton.layoutParams = addProductButtonsParams
        scanToAddProductButton.setOnClickListener { addProductsViaScanButton.onClickListener() }

        container.addView(scanToAddProductButton)
    }

    private fun addProductsButton(
        addProductsButton: AddButton?,
        container: RelativeLayout,
        id: Int,
    ) {
        addProductsButton ?: return
        val addProductButtonsParams = RelativeLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        addProductButtonsParams.addRule(RelativeLayout.ALIGN_PARENT_START)
        val addingProductsManuallyButton = MaterialButton(context, null, R.attr.secondaryTextButtonStyle)
        addingProductsManuallyButton.text = addProductsButton.text
        addingProductsManuallyButton.icon = AppCompatResources.getDrawable(context, R.drawable.ic_add)
        addingProductsManuallyButton.id = id
        addingProductsManuallyButton.layoutParams = addProductButtonsParams
        addingProductsManuallyButton.setOnClickListener { addProductsButton.onClickListener() }

        container.addView(addingProductsManuallyButton)
    }

    private fun updateContent(content: View?) {
        binding.editButton.isVisible = content != null && hasEditButton
        binding.contentLayout.isVisible = content != null
        binding.contentLayout.removeAllViews()
        content?.let {
            binding.contentLayout.addView(
                content,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        binding.addButtonsLayout.isVisible = keepAddButtons || content == null
    }

    fun setContentHorizontalPadding(@DimenRes padding: Int) {
        val paddingSize = context.resources.getDimensionPixelSize(padding)
        binding.contentLayout.setPadding(
            paddingSize,
            binding.contentLayout.paddingTop,
            paddingSize,
            binding.contentLayout.paddingBottom
        )
    }

    fun setOnEditButtonClicked(listener: () -> Unit) {
        binding.editButton.setOnClickListener { listener() }
    }

    fun setEditButtonContentDescription(contentDescription: String) {
        binding.editButton.contentDescription = contentDescription
    }

    data class AddButton(
        val text: CharSequence,
        val onClickListener: () -> Unit
    )
}
