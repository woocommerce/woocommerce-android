# Using Android Resources

## Drawable Resources

Adding a vector drawable (to `WooCommerce/src/main/res/drawable/`) should be the first option when adding assets. Only if a vector drawable is not available should PNG files be added to the project. Make sure to use `android:src` in place of `app:srcCompat` in XML files. Use existing white 24dp variations of vector drawables (i.e. `ic_*_white_24dp`) and tint the drawables statically (i.e. XML) or dynamically (i.e. Java or Kotlin) as necessary. Set values for `android:height` and `android:width` attributes for views with icons to scale the 24dp icon for that view.

Some vector drawables may come from a SVG file and they are not the easiest file type to edit. If the SVG file is specific to the WooCommerce-Android project (like a banner image or unlike a gridicon), then add the SVG source in `WooCommerce/src/future/svg/`. This will make sure we can find and edit the SVG file and then export it in vector drawable format.

*Tip:* If you’re given an SVG from a designer, try using [the SVGOMG tool](https://href.li/?https://jakearchibald.github.io/svgomg/) to optimize it before importing it as a vector. It usually significantly reduces the size of the SVG and cures the “path too large” warnings in Android Studio.
*Tip 2:* If you're exporting the SVG from Figma, and you notice that the exported file has some issues or distortions, try to disable the option [Simplify stroke](https://help.figma.com/hc/en-us/articles/360040028114-Guide-to-exports-in-Figma#h_fee7100a-1540-4b62-bd45-e7ad6fa943b7) in the export tool.

Please use the following naming convention for drawables:

* Use `ic_` for icons (i.e. simple, usually single color, usually square shape) and `img_` for images (i.e. complex, usually multiple colors).
* Use the [gridicon](http://automattic.github.io/gridicons/) name if applicable (examples: `ic_my_sites` or `ic_reply`).
* Use the color of the icon (example: `ic_reply_white`).
* Use the width in dp (example: `ic_reply_white_24dp`).

#### Valid
`ic_reply_white_24dp` (white reply icon 24dp)
`ic_stats_black_32dp` (black stats icon 32dp)
#### Invalid
`reply_white` (missing `ic_` and width)
`ic_confetti_284dp` (uses `ic_`, but should use `img_`)
`img_confetti_98dp` (uses height, but should use width)
