# frozen_string_literal: true

source 'https://rubygems.org'

gem 'danger-dangermattic', '~> 1.2'
gem 'fastlane', '~> 2.216'
gem 'nokogiri'
gem 'rubocop', '~> 1.65'

### Fastlane Plugins

# The '>= 13.5.1' after '~> 13.5' ensures that we resolve to any version compatible with 13.5 starting from 13.5.1
# We need 13.5.1 because of a fix in fetching issues / PRs with a fine-grained GitHub token
gem 'fastlane-plugin-wpmreleasetoolkit', '~> 13.5', '>= 13.5.1'
# gem 'fastlane-plugin-wpmreleasetoolkit', path: '../../release-toolkit'
# gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', branch: ''

### Gems needed only for generating Promo Screenshots
group :screenshots, optional: true do
  gem 'rmagick', '~> 4.1'
end
