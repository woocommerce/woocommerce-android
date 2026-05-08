# frozen_string_literal: true

source 'https://rubygems.org'

gem 'danger-dangermattic', '~> 1.2'
gem 'fastlane', '~> 2.216'
gem 'fastlane-plugin-firebase_app_distribution', '~> 0.10'
gem 'nokogiri', '>= 1.19.3' # GHSA-c4rq-3m3g-8wgx — drop floor once toolkit is on >= 14.4.1
gem 'rubocop', '~> 1.65'

# Security: https://github.com/lostisland/faraday/pull/1665
# Faraday 2.0 is not compatible with Fastlane
gem 'faraday', '~> 1.10', '>= 1.10.5'

### Fastlane Plugins

gem 'fastlane-plugin-wpmreleasetoolkit', '~> 13.8'
# gem 'fastlane-plugin-wpmreleasetoolkit', path: '../../release-toolkit'
# gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', branch: ''

### Gems needed only for generating Promo Screenshots
group :screenshots, optional: true do
  gem 'rmagick', '~> 4.1'
end

# To avoid errors like:
#
# SSL_connect returned=1 errno=0 peeraddr=3.5.132.155:443 state=error: certificate verify failed (unable to get certificate CRL)
#
# See https://github.com/ruby/openssl/issues/949
gem 'openssl', '~> 4.0'
