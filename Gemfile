# frozen_string_literal: true

source 'https://rubygems.org'

gem 'danger-dangermattic', git: 'https://github.com/Automattic/dangermattic'
gem 'fastlane', '~> 2.216'
gem 'nokogiri'
gem 'rubocop', '~> 1.56'

### Fastlane Plugins

gem 'fastlane-plugin-wpmreleasetoolkit', '~> 9.2'
# gem 'fastlane-plugin-wpmreleasetoolkit', path: '../../release-toolkit'
# gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', branch: ''

### Gems needed only for generating Promo Screenshots
group :screenshots, optional: true do
  gem 'rmagick', '~> 4.1'
end
