# Baseline Profile Module

The module provides two main functionalities:
* Generation of the baseline based on the full login flow (`woo_generate_baseline_profile.sh`)
* Testing performance of the baseline (`woo_startup_benchmark.sh`)

#### woo_generate_baseline_profile.sh
The script that generates baseline profile for the login flow and main script

Comments in the script contain a manual on how to use that

Input params: `login`, `password`, and a `website` that will be used to run the login flow

###### Script steps:
* Removes installed app (if any)
* Roots connected emulator. The emulator should not have Google Services installed
* Builds the app
* Run the test which performs login flow
* Pulls generated baseline profile from the emulator
* Renames it and moves it into the main app module so it will be used automatically

#### woo_startup_benchmark.sh
The script that performs benchmarking of the generated baseline

Comments in the script contain a manual on how to use that

Input params: login, password, and website that will be used to run the login flow

#### How to use
* Every now and then (it depends to the code changes in the login flow and main screen) generate a new baseline profile using `woo_generate_baseline_profile.sh` script
In order to make a decision if a new baseline needed, run benchmark tests and see if gain is still withing 10-15% interval
* In order to verify that the new profile performs well, use `woo_startup_benchmark.sh` script, or/and tests `CleanStartupBenchmark`, `LoggedInStartupBenchmark`
Expected gain is about 10-15%
