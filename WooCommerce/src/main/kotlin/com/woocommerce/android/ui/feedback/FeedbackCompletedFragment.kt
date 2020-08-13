package com.woocommerce.android.ui.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R

class FeedbackCompletedFragment : androidx.fragment.app.Fragment() {
    companion object {
        const val TAG = "survey_completed"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_feedback_completed, container, false)
    }
}