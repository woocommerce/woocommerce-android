#!/usr/bin/env ruby

require 'nokogiri'

MAIN_STRINGS_PATH = "./WooCommerce/src/main/res/values/strings.xml"
LIBRARIES_STRINGS_PATH = [
  {library: "Login Library", strings_path: "./libs/login/WordPressLoginFlow/src/main/res/values/strings.xml", exclusions: ["default_web_client_id", "login_notification_channel_id", "enter_site_address", "enter_email_wordpress_com"]}
]

def skip_string(library, string_name)
  if (!library.key?(:exclusions)) 
    return false
  end

  skip = library[:exclusions].include?(string_name)
  if (skip) 
    puts " - Skipping #{string_name} string"
    return true
  end

  return false
end

def add_or_update_string(main_strings, string_name, string_content)
  main_strings.xpath('//string').each { | this_string | (if (this_string.content == string_content) then return :found else this_string.content = string_content ; return :updated end) if this_string.attr("name") == string_name } 
  new_element = Nokogiri::XML::Node.new "string", main_strings
  new_element['name'] = string_name
  new_element.content = string_content
  main_strings.xpath('//string').last().add_next_sibling("\n\t" + new_element.to_xml)
  return :added
end

def merge_string(main_strings, library, string_line)
  string_name = string_line.attr("name")
  return :skipped if skip_string(library, string_name)
  add_or_update_string(main_strings, string_name, string_line.content)
end

def merge_lib(main, library)
  puts "Merging #{library[:library]} strings into #{main}"
  main_strings = File.open(MAIN_STRINGS_PATH) { |f| Nokogiri::XML(f, nil, Encoding::UTF_8.to_s) }
  lib_strings = File.open(library[:strings_path]) { |f| Nokogiri::XML(f, nil, Encoding::UTF_8.to_s) }
 
  updated_count = 0
  untouched_count = 0
  added_count = 0
  skipped_count = 0
  lib_strings.xpath('//string').each do |string_line|
    res = merge_string(main_strings, library, string_line) 
    case res
      when :updated
        puts "#{string_line.attr("name")} updated."
        updated_count = updated_count + 1 
      when :found
        untouched_count = untouched_count + 1
      when :added
        puts "#{string_line.attr("name")} added."
        added_count = added_count + 1
      when :skipped
        skipped_count = skipped_count + 1
      else 
        puts "Internal Error!"
        puts res
        return
      end
  end

  File.open(MAIN_STRINGS_PATH, "w:UTF-8") do | f |
    f.write(main_strings.to_xml)
  end

  puts "Done (#{added_count} added, #{updated_count} updated, #{untouched_count} untouched, #{skipped_count} skipped)."
end

if Dir.pwd =~ /tools/
  puts "Must run script from root folder"
  exit
end


LIBRARIES_STRINGS_PATH.each do | lib |
  merge_lib(MAIN_STRINGS_PATH, lib)
end