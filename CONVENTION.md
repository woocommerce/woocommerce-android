# Development Conventions
* [Naming](#naming)
* [Testing](#testing)
* [Code organization](#code-organization)

## Naming
* ##### Layouts (files)
    ***feature_name*** _ ***layout_type***

    **Examples**
    * feedback_survey_activity
    * shipping_label_package_details_list_item
    * product_tag_list_item
    * product_download_details_fragment
    * feedback_survey_fragment

* ##### View IDs
    ***goal*** ***ViewType***

    **Examples**
    * doneButton
    * headingTextView
    * choice1RadioButton
    * userNameEditText
    * successImageView
* ##### Drawables
    _todo_
* ##### Dimensions
    _todo_
* ##### Text Appearances
    _todo_
* ##### Extension functions
    _todo_

## Testing
### Unit Tests
* ##### General approach
    _todo_

* ##### Naming
    ***given*** something is, ***when*** something happens, ***then*** something is expected

    **Examples**
    ```
    fun `when the button is clicked, then the sum label is updated`() {
        val currentSum = sumLabel.text as Int

        // WHEN
        presenter.onButtonClick()

        // THEN
        assertThat(sumLabel.text).isEqualTo("${currentSum + 1}")
    }
    ```

    ```
    fun `given the button has 9 clicks, when the button is clicked, then the appropriate message is shown`() {
        // GIVEN
        repeat(9) { presenter.onButtonClick() }

        // WHEN
        presenter.onButtonClick()

        // THEN
        assertThat(messageLabel.text).isEqualTo("Wow, 10 clicks")
    }
    ```

#### UI Tests
_todo_
#### E2E
_todo_

## Code organization
_todo_
