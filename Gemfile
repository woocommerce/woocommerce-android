# frozen_string_literal: true

source 'https://rubygems.org'

gem 'danger-dangermattic', '~> 1.1'
gem 'fastlane', '~> 2.216'
gem 'nokogiri'
gem 'rubocop', '~> 1.65'

### Fastlane Plugins

gem 'fastlane-plugin-wpmreleasetoolkit', '~> 11.0', git: 'https://github.com/wordpress-mobile/release-toolkit.git', branch: 'iangmaia/publish-release'
# gem 'fastlane-plugin-wpmreleasetoolkit', path: '../../release-toolkit'
# gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', branch: ''

### Gems needed only for generating Promo Screenshots
group :screenshots, optional: true do
  gem 'rmagick', '~> 4.1'
end
