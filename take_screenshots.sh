# important! don't use a shabang here, otherwise the script will run on a
# different shell and you might not have Ruby etc.

set -e

wait_for_emulator_to_be_online() {
  echo "Waiting for emulator to come online, this can take some time..."

  local interval=5 # 5 seconds
  local elapsed=0
  local timeout=300 # 5 minutes

  while [[ $(adb devices | sed -n 2p | grep 'device' | wc -l | tr -d ' ') -eq 0 && $elapsed -lt $timeout ]]
  do
    echo "Emulator not online, trying again in $interval seconds..."
    sleep $interval
    elapsed=$(expr $elapsed + $interval)
  done
  echo "Emulator online"
}

function toggle_dark_mode() {
  # not sure if this is too much or too little, it works and don't wanna tinker
  # with it for the moment
  local sleep_interval=10

  # 2 turns dark mode on, 1 truns light mode on
  local mode=2
  [ $1 = true ] || mode=1

  local message=''
  [ $mode -eq 1 ] && message="light mode 🌞" || message="dark mode 🌚"
  echo "Enabling $message and rebooting the emulator."

  adb shell settings put secure ui_night_mode $mode
  # if we don't sleep, it doesn't work
  # no idea why?!
  sleep $sleep_interval

  # need to reboot for the change to take place
  adb reboot

  wait_for_emulator_to_be_online

  echo "All good! Taking screenshots now 📸"
}

function fastlane_screenshots() {
  FASTLANE_SKIP_UPDATE_CHECK=1 \
    bundle exec fastlane screenshots \
    skip_ui_modes:true \
    #locales:en,es # use this to specify a subset of locales to run
}

# with -l, we're telling the script the device is in light mode, so skip
# toggling for it
while getopts ":l" opt
do
  case $opt in
    l)
      mode='light'
      ;;
    \?)
      mode='dark'
      ;;
  esac
done

if [ "$mode" = 'light' ]
then
  wait_for_emulator_to_be_online

  echo "Device in light mode 🌞. Taking screenshots..."
  fastlane_screenshots

  toggle_dark_mode true
  fastlane_screenshots
else
  wait_for_emulator_to_be_online

  echo "Device in dark mode 🌚. Taking screenshots..."
  fastlane_screenshots

  toggle_dark_mode false
  fastlane_screenshots
fi

echo "Final step. Composing screenshots 👩‍🎨"
bundle exec fastlane create_promo_screenshots
