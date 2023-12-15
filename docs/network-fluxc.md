[Fluxc](https://github.com/wordpress-mobile/WordPress-FluxC-Android) is a library we use to communicate with the backend.
On top of that it provides caching of the response

### How to use it
During developing a new feature that requires changes in fluxc, the easiest way to test the changes would be using of composite builds.
To do that you need to add the following to your `local-builds.gradle` file:
```
ext {
 localFluxCPath = "path_to_fluxc/WordPress-FluxC-Android"
}
```

After doing a gradle sync, the project will be built using the local fluxc sources

Another way of using the specified and `compiled` version of fluxc is changing `fluxCVersion` in `build.gradle` file, for instance:
* `2.59.0` will be using specific version of the library
* `trunk-3f65981193242842166b7428f409fd2d290076ac` will be using the specified commit from the branch
* `2912-de56923eec0f052e2f55c2ffaf0561fd52b11471` will be using the specified commit from the pull request

Make sure to merge the changes in fluxc to the `trunk` branch before merging the changes in the main project that expect the changes in fluxc

