# frozen_string_literal: true

source 'https://rubygems.org'

gem 'danger-dangermattic', '~> 1.4'
gem 'fastlane', '~> 2.237'
gem 'fastlane-plugin-firebase_app_distribution', '~> 1.0'
gem 'rubocop', '~> 1.89'

# Security: https://github.com/lostisland/faraday/pull/1665
# Faraday 2.0 is not compatible with Fastlane
gem 'faraday', '~> 2.14'

### Fastlane Plugins

gem 'fastlane-plugin-wpmreleasetoolkit', '~> 14.11'
# gem 'fastlane-plugin-wpmreleasetoolkit', path: '../../release-toolkit'
# gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', branch: ''

### Gems needed only for generating Promo Screenshots
group :screenshots, optional: true do
  # Capped below 7: rmagick 7 breaks promo-screenshot generation with the
  # wpmreleasetoolkit 13.8 PromoScreenshots helper. See AINFRA-2482.
  gem 'rmagick', '>= 4.1', '< 7'
end

# To avoid errors like:
#
# SSL_connect returned=1 errno=0 peeraddr=3.5.132.155:443 state=error: certificate verify failed (unable to get certificate CRL)
#
# See https://github.com/ruby/openssl/issues/949
gem 'openssl', '~> 4.0'
