package com.woocommerce.android.ui.notifications

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_review_detail.*
import javax.inject.Inject

class ReviewDetailFragment : Fragment(), ReviewDetailContract.View {
    companion object {
        const val TAG = "ReviewDetailFragment"

        fun newInstance(): ReviewDetailFragment {
            return ReviewDetailFragment()
        }
    }

    @Inject lateinit var presenter: ReviewDetailContract.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_review_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        presenter.takeView(this)

        review_approve.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> disapproveReview()
                false -> approveReview()
            }
        }

        review_spam.setOnClickListener { spamReview() }
        review_trash.setOnClickListener { trashReview() }

        // TODO - this is just temp data to display the view properly for review of the UI
        review_product_name.text = "Left Handed Candlestick"
        review_gravatar.setImageResource(R.drawable.ic_gridicons_user_circle_100dp)
        review_user_name.text = "Ursula K. LeGuin"
        review_time.text = "23 hrs ago"
        review_rating_bar.rating = 4f
        review_description.text = "Great product! Definitely what I was looking for. Great quality, and " +
                "looks exactly like the product image on the website. Would highly recommend to anyone " +
                "who is looking for something like this!"
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun showLoadingSkeleton() {
        // TODO add loading skeleton
    }

    private fun trashReview() {
        uiMessageResolver.showSnack("Trash logic not implemented")
    }

    private fun spamReview() {
        uiMessageResolver.showSnack("Spam logic not implemented")
    }

    private fun approveReview() {
        uiMessageResolver.showSnack("Approve logic not implemented")
    }

    private fun disapproveReview() {
        uiMessageResolver.showSnack("Disapprove logic not implemented")
    }
}
