package com.woocommerce.android.ui.orders.shippinglabels.creation

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreatePackageViewModel.PackageType

class ShippingLabelCreatePackageViewPagerAdapter(container: Fragment) : FragmentStateAdapter(container) {
    override fun getItemCount(): Int = PackageType.values().size

    override fun createFragment(position: Int): Fragment {
        return when (PackageType.values()[position]) {
            PackageType.CUSTOM -> ShippingLabelCreateCustomPackageFragment()
            PackageType.SERVICE -> ShippingLabelCreateServicePackageFragment()
        }
    }
}
