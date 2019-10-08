group :screenshots, optional: true do
  gem 'rmagick', '~> 3.2.0'
end

source "https://rubygems.org" do 
  gem 'nokogiri'
  gem "fastlane", "2.127.2"
end


plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)