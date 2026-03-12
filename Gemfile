# frozen_string_literal: true

source 'https://rubygems.org'

gem 'danger-dangermattic', git: 'https://github.com/Automattic/dangermattic.git', branch: 'iangmaia/add-llm-review-plugin'
gem 'fastlane', '~> 2.216'
gem 'fastlane-plugin-firebase_app_distribution', '~> 0.10'
gem 'nokogiri'
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
