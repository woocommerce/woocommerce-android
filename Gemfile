# frozen_string_literal: true

source 'https://rubygems.org'

gem 'danger-dangermattic', '~> 1.4'
gem 'fastlane', '~> 2.240'
gem 'fastlane-plugin-firebase_app_distribution', '~> 1.0'
gem 'rubocop', '~> 1.91'

### Fastlane Plugins

# Local workflow pilot: replace sibling paths with released versions before CI adoption.
gem 'a8c-release-workflows', '0.1.0.pre.1', path: '../a8c-release-workflows'
gem 'fastlane-plugin-wpmreleasetoolkit', '~> 15.1', path: '../release-toolkit'
# gem 'fastlane-plugin-wpmreleasetoolkit', path: '../../release-toolkit'
# gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', branch: ''

### Gems needed only for generating Promo Screenshots
group :screenshots, optional: true do
  gem 'rmagick', '~> 7.1'
end

# To avoid errors like:
#
# SSL_connect returned=1 errno=0 peeraddr=3.5.132.155:443 state=error: certificate verify failed (unable to get certificate CRL)
#
# See https://github.com/ruby/openssl/issues/949
gem 'openssl', '~> 4.0'
