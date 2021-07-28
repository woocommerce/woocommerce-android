# Themes & Styling Practices
The goal of this document is to give an overview of how the material dark/light mode styling has been implemented in the WCAndroid app to act as a reference and introduction.

### Themes
#### **File**: `themes.xml`
* Theme definitions used throughout the app such as main themes, dialog, toolbar, and component themes.
* Naming convention should follow `Theme.Woo.*`
* Main themes of interest:
	* `Theme.Woo.DayNight` - the main application theme
	* `Theme.Woo.Splash` - theme used for the loading/splash screen
	* `Theme.Woo.DayNight.Zendesk` - theme used for the views provided by the Zendesk integration

### Styles
#### File: `styles_base.xml`
* Style definitions used throughout the app
* Use the following naming convention:
	* `Woo.[component-name]`: Overrides various style properties for standard Android components (ex: `Woo.Card`).
		* Note that these styles override a component and *should not be used on views not of that component type*. So for example, `Woo.Card` would only be used on a `MaterialCardView`.
	* `Woo.[component-name].[variation]`: Variation of a standard component style of some sort (ex. `Woo.Button.Toggle`, `Woo.Button.Colored`) .
	* `Woo.TextView.[text-type]`: `TextView` styles configured with the appropriate `textAppearance` definitions to match `[text-type]` (ex. `Woo.TextView.Subtitle1`, `Woo.TextView.Body2`).
	* `Woo.[view]`: Styles covering a specific view-type, typically a view group (ex. `Woo.Divider`, `Woo.ListItem`, `Woo.ListHeader`)
	* `Woo.[view].[text-type]`: Styles for `TextView` items inside those views (ex. `Woo.ListItem.Title`)
	* `Woo.Skeleton.[*]`: Skeleton style definitions.
	* `Widget.Woo.*`: custom Woo view components. Typically these styles also have custom style attributes defined in `attrs.xml`. These style definitions should already be applied to the custom component so no need to use them directly.

#### File: `styles_login.xml`
* Overrides style definitions for the login views (WordPressLoginFlow)

## Subsystem Implementation
> For many of the resource definitions we follow Google’s lead by naming resources on a *relative* scale. This is to prevent scenarios where we have to edit values across the app due to minor changes in designs for example color palette or dimension values. Names are calculated starting with a baseline, then defined as a percentage of that baseline. The percentage doesn’t have to be exact, its main purpose is to describe the relationship of the dimension to the baseline. So in the case of dimensions 100 would be larger, and for colors the numeric labels indicate light to darker values.

### Colors

> See [Color palette](material-theme-designs.md#design-colors) for theme color designs
#### File: `wc_colors_base.xml`
* Contains raw color values for the WooCommerce color palette. Color definitions added here should be named very specifically to identify the color (ex. *woo_purple_30*), and should only contain hexadecimal color values.

#### Files: `colors_base.xml` & `values-night/colors_base.xml`
* Generic theme-level color definitions such as `color_primary`, `color_on_surface`, and descriptive component-specific items such as `product_status_fg_pending`.
* **Raw color values should not be added here**. The goal is to have a relatively small palette of colors to work with, all defined in `wc_colors_base.xml`. If absolutely necessary, first add the raw value to `wc_colors_base.xml`.

### Dimensions
#### File: `dimens.xml`
* Dimension definitions such as margins, paddings, borders, etc.
* Uses generic names combined with a numeric value to signify a percentage of that base. Use `major` for larger definitions and `minor`  for smaller definitions. The `100` in the label signifies the baseline - most commonly used value. For example:
	* `major_100` : commonly used for margins/padding for fragments and activities (`16dp`)
	* `minor_100`: commonly used for margins/paddings between components in a view (`8dp`)

### Elevation
#### File: `elevations.xml`
* Elevation definitions for applying elevation on various components. Not heavily used, but handy none-the-less.

### Shapes
#### File: `shape.xml`
* Corner radius and edge definitions for the 3 shape styles. Changes made to these styles will be applied across the app.

### TextAppearance
> See [Typography](material-theme-designs.md#design-typography) for theme typography designs
#### File: `type.xml`
* Text/font style definitions that override the material `TextAppearance` styles. These definitions are all used in the main `Theme.Woo.DayNight` theme.
* It would be rare to use these directly in a view layout, but if you did, favor the theme attribute version (ex: Use `?attr/textAppearanceBody2` instead of `@style/TextAppearance.Woo.Body2`)

### Use Material Components when Available
* `MaterialTextView`
* `MaterialCardView`
* `MaterialButton`
* `SwitchMaterial`

### Padding/Margins
As a rule of thumb:
* Outer margins of a main view (ex. Card, Page): **16 dp** (`@dimen/major_100`)
* Vertical margin/padding between sections in a view: **12 dp** (`@dimen/major_75`)
* Horizontal margin/padding between elements in a view: **8 dp** (`@dimen/minor_100`)
* Vertical margin/padding between elements in a view: **4 dp** (`@dimen/minor_50`)
	* Sometimes if stacking text elements you don’t need to add this padding. Depends on the type (title, body, etc).

### Dividers
We use two different types of dividers in this app and there is a style definition for both. Below are examples of how they are used:

**Regular Divider**
```
<!— Divider —>
<View
    android:id=“@+id/paymentInfo_total-paid_divider”
    style=“@style/Woo.Divider”
    android:layout_marginStart=“@dimen/major_100”
    android:layout_marginEnd=“@dimen/minor_00”/>

```

**Title-aligned Divider**

```
<View style=“@style/Woo.Divider.TitleAligned”/>
```

### Settings
There are several custom widgets for handling the main types of options offered in the settings/prefs views. These custom widgets have their styles already applied so there is no need to worry about configuring them for light/dark mode. Here are the widgets:
* `WCSettingsCategoryHeaderView` - Header view for each setting section
* `WCSettingsOptionValueView` - Setting that displays the setting option and an optional current value or a description below it.
* `WCSettingsToggleOptionView` - Toggled setting capable of optionally displaying an icon and description.

[[images/best-practices-settings.png]]
￼
### When in doubt
* When in doubt, reference an existing view. Great examples:
	* List-type fragment styling: l`ayout/fragment_order_list.xml`
	* List Item styling: `layout/order_list_item.xml`
	* Detail-type fragment styling: `layout/fragment_order_detail.xml`
	* Card contents styling: `layout/order_detail_customer_info.xml`
	* Settings option: `layout/fragment_settings_main.xml`
* Prefer resource files over defining/implementing styles in code.
