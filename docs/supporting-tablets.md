# Tablet Support in WooCommerce Android
Several screens within the WooCommerce Android application have been optimized for larger screens,
such as tablets. Notably, the OrderListFragment and OrderDetailFragment display as a two-pane layout on tablets,
providing a more spacious and user-friendly interface. On phones, these screens revert to a single-pane layout
for optimal use of smaller screen sizes.

# Optimizing Additional Screens for Tablet
To adapt more screens for tablet use and achieve a similar two-pane layout, follow the steps outlined below:
1. FragmentContainerView as NavHostFragment: For the screen requiring a two-pane layout, incorporate a new FragmentContainerView
in your XML layout. This view should also function as a NavHostFragment, containing either the detail or list pane
of your screen. Assign a navigation graph to this NavHostFragment for proper navigation management.
2. Layout Adjustments: Adjust the width attributes of your screen's main layout and the NavHostFragment to ensure
a seamless transition between tablet and phone layouts. This may involve modifying the XML attributes or dynamically
adjusting layout parameters in your code.
3. Device Type Detection: Utilize the isTablet() extension function available for both Fragment and Activity
classes to determine if the application is running on a tablet. This function helps in applying different layouts or logic
based on the device type.

# Reference Screens
To better understand how tablet optimization has been implemented, refer to the OrderListFragment as a primary example.
This fragment demonstrates the use of a two-pane layout on tablets, including the configuration of NavHostFragment and
adjustments to layout parameters for tablet support.
