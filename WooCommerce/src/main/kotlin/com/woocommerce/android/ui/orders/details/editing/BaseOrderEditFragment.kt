package com.woocommerce.android.ui.orders.details.editing

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent
import org.wordpress.android.util.ActivityUtils

abstract class BaseOrderEditFragment : BaseFragment, BackPressListener {
    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    protected val viewModel: OrderEditingSharedViewModel by viewModels()

    private var doneMenuItem: MenuItem? = null

    /**
     * This TextWatcher can be used to detect EditText changes in any descendant
     */
    protected val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            // noop
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // noop
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            updateDoneMenuItem()
        }
    }

    /**
     * These are the key and the result we use in navigateBackWithResult() when user taps Done
     */
    abstract val resultKey: String
    abstract fun getResult(): Any

    /**
     * Descendants should return true if the user made any changes
     */
    abstract fun hasChanges(): Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        updateDoneMenuItem()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                navigateBackWithResult(resultKey, getResult())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateDoneMenuItem() {
        doneMenuItem?.isVisible = hasChanges()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return if (hasChanges()) {
            confirmDiscard()
            false
        } else {
            true
        }
    }

    private fun confirmDiscard() {
        MultiLiveEvent.Event.ShowDialog.buildDiscardDialogEvent(
            positiveBtnAction = { _, _ ->
                findNavController().navigateUp()
            }
        ).showDialog()
    }
}
