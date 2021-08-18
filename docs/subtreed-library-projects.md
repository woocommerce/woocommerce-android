
The login library (aka [WordPress-Login-Flow-Android][wp-login]) is managed as separate open source project and is git-subtree'd into the WooCommerce Android app source tree.

**Important**: Only use **`git version 2.19`** or less for all of these subtree commands! If `2.20.x` is used it will corrupt the source tree!

## Contents

* [Working with the Login Library Submodule](#working-with-the-login-library-submodule)
* [Subtree Cheatsheet](#subtree-cheatsheet)

## Working with the Login Library Submodule

### Before making changes to files in /libs/login

Use the following command to pull in any updates from the `develop` branch of the login library into WooCommerce for Android.

`$ git subtree pull --prefix=libs/login git@github.com:wordpress-mobile/WordPress-Login-Flow-Android.git develop --squash`

### After Main PR containing changes is approved and merged

1. Use the following command to push the changes from this repo to a new branch (`[branch-name]`) in the [WordPress-Login-Flow-Android][wp-login] repo:

    `$ git subtree push --prefix=libs/login git@github.com:wordpress-mobile/WordPress-Login-Flow-Android.git [branch-name]`

2. Navigate to [WordPress-Login-Flow-Android][wp-login-branches] and locate the branch you just created
3. Click the **New pull request** to open a new pull request and submit the login library changes for review. Be sure to reference the PR in `woocommerce-mobile` for additional information on the source of all the changes, as well as specific testing instructions.

(**NOTE**: the remaining steps are optional. They are recommended if changes are really big and require a lot of work in WPAndroid)

4. Switch to the [WordPress-Android][wp-android] repository and create a new branch off of `develop`
5. Use the following command to pull in the remote login library branch (`[branch-name]`) created in step 1 into the current branch:

    `$ git subtree pull --prefix=libs/login git@github.com:wordpress-mobile/WordPress-Login-Flow-Android.git [branch-name] --squash`
6. Fix any conflicts and add any additional code needed in WordPress-Android to support the changes to the login library.
7. Create a draft PR which will then be used for testing to verify the changes do not break the WordPress-Android app.

### After the Login Library PR is approved and merged

1. Open the [WordPress-Android][wp-android] repo and switch to the branch created in step 5 of [the last section](#after-main-pr-containing-changes-is-approved-and-merged).
2. Use the following command to pull in changes to the `develop` branch in the [WordPress-Login-Flow-Android][wp-login] repo into the current branch:

    `$ git subtree pull --prefix=libs/login git@github.com:wordpress-mobile/WordPress-Login-Flow-Android.git develop --squash`
3. Update the PR with any additional notes and mark it as ready for review by taking it out of draft.

## Subtree Cheatsheet

For those who know enough to be dangerous, but just want to refresh their memory. Below we break down how to interact with the **fake** library repository `woo-lib`. Notice how even though the `git subtree` arguments look to be identical between the different actions, their actual purpose and effect changes.

### The `add` command

```bash
git subtree add --prefix=/libs/woo-lib https://github.com/project/woo-lib master --squash
```

* `add` adds the repo as a submodule of the active repo
* `--prefix=/libs/woo-lib` the folder this submodule will be copied into
* `https://github.com/project/woo-lib` the repo to pull in as a submodule
* `master` the branch in the aforementioned repo to pull in
* `--squash` import the state of the repo being imported but not the history

### The `pull` command

```bash
git subtree pull --prefix=/libs/woo-lib https://github.com/project/woo-lib master --squash
```

* `pull` pulls changes from the submodule repo:branch into the current repo branch
* `--prefix=/libs/woo-lib` the folder this submodule is located in
* `https://github.com/project/woo-lib` the repo location of the submodule
* `master` the branch in the aforementioned repo to pull changes from
* `--squash` squash all changes into a single commit

### The `push` command

```bash
git subtree push --prefix==/libs/woo-lib https://github.com/project/woo-lib new-branch
```

* `push` pushes changes from the submodule repo:branch into it's remote repo
* `--prefix=/libs/woo-lib` the folder this submodule is located in
* `https://github.com/project/woo-lib` the repo location of the submodule
* `new-banch` the branch to create on the submodules main git repository to contain the changes

[wp-android]: https://github.com/wordpress-mobile/WordPress-Android
[wp-login]: https://github.com/wordpress-mobile/WordPress-Login-Flow-Android
[wp-login-branches]: https://github.com/wordpress-mobile/WordPress-Login-Flow-Android/branches
