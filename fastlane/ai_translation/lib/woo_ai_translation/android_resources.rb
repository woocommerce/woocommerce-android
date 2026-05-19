# frozen_string_literal: true

require 'rexml/document'
require 'digest'
require 'fileutils'

module WooAiTranslation
  # Order-preserving reader/writer for Android `strings.xml` resources.
  #
  # Uses only the Ruby stdlib (REXML) so the engine is self-contained and
  # cross-platform-extractable -- no Bundler, no Nokogiri at runtime.
  #
  # The localized files this writer emits only need to be valid Android resource
  # XML with correct escaping and placeholders preserved -- byte-for-byte parity
  # with the wpmreleasetoolkit/GlotPress output is NOT a goal. The baseline
  # import of existing human translations is a file copy (rollout phase), so no
  # re-serialization happens there.
  module AndroidResources
    UNIT_SEPARATOR = "␟"

    # A single translatable resource entry: a <string>, <string-array> or
    # <plurals>. `entries` is the ordered list of translatable text fragments.
    class Unit
      attr_reader :type, :name, :attributes
      attr_accessor :entries # [{ id:, source:, value:, quantity? }]

      def initialize(type:, name:, attributes:)
        @type = type
        @name = name
        @attributes = attributes
        @entries = []
      end

      def translatable?
        @attributes['translatable'] != 'false'
      end

      # Stable signature of the source content; any change re-translates the
      # whole unit (simple and correct).
      def source_signature
        @entries.map { |e| "#{e[:id]}=#{e[:source]}" }.join(UNIT_SEPARATOR)
      end

      def translation_requests
        @entries.map { |e| { id: e[:id], source: e[:source] } }
      end

      # Missing ids are left nil on purpose so a failed/partial translation is
      # omitted from the localized file (Android falls back to the default
      # resource) instead of shipping the English source as if translated.
      def apply!(translations)
        @entries.each { |e| e[:value] = translations[e[:id]] }
      end

      def fully_translated?
        @entries.any? && @entries.all? { |e| !e[:value].nil? }
      end

      # An output-side copy with the same shape but no translated values yet.
      def dup_shell
        copy = Unit.new(type: @type, name: @name, attributes: @attributes.dup)
        copy.entries = @entries.map { |e| e.dup.tap { |x| x[:value] = nil } }
        copy
      end
    end

    # Parsed document; keeps every unit in source order.
    class Document
      attr_reader :units

      def initialize(units)
        @units = units
      end

      def translatable_units
        @units.select(&:translatable?)
      end

      def translatable_names
        translatable_units.map(&:name)
      end

      def find(name)
        @units.find { |u| u.name == name }
      end
    end

    module Parser
      module_function

      def parse_file(path)
        parse(File.read(path))
      end

      def parse(xml)
        doc = REXML::Document.new(xml)
        root = doc.root
        raise ArgumentError, 'No <resources> root element' if root.nil? || root.name != 'resources'

        units = []
        root.elements.each do |node|
          case node.name
          when 'string' then units << string_unit(node)
          when 'string-array' then units << array_unit(node)
          when 'plurals' then units << plurals_unit(node)
          end
        end
        Document.new(units)
      end

      def string_unit(node)
        u = Unit.new(type: :string, name: node.attributes['name'], attributes: attrs(node))
        u.entries = [{ id: node.attributes['name'], source: text_of(node), value: nil }]
        u
      end

      def array_unit(node)
        u = Unit.new(type: :array, name: node.attributes['name'], attributes: attrs(node))
        u.entries = node.get_elements('item').each_with_index.map do |item, i|
          { id: "#{u.name}[#{i}]", source: text_of(item), value: nil }
        end
        u
      end

      def plurals_unit(node)
        u = Unit.new(type: :plurals, name: node.attributes['name'], attributes: attrs(node))
        u.entries = node.get_elements('item').map do |item|
          q = item.attributes['quantity']
          { id: "#{u.name}{#{q}}", quantity: q, source: text_of(item), value: nil }
        end
        u
      end

      def attrs(node)
        h = {}
        node.attributes.each_attribute { |a| h[a.name] = a.value }
        h
      end

      # The logical, human-readable value the app displays: CDATA raw, XML
      # entities decoded, and Android escapes reversed so the value round-trips
      # through the Writer idempotently (reuse never double-escapes). Inline
      # child elements (e.g. xliff:g) are uncommon here and are captured as
      # serialized markup so they round-trip untranslated.
      def text_of(node)
        return node.children.map(&:to_s).join if node.has_elements?

        raw = node.children.map do |c|
          c.is_a?(REXML::CData) ? c.value : (c.respond_to?(:value) ? c.value : c.to_s)
        end.join
        Writer.unescape(raw)
      end
    end

    module Writer
      module_function

      XML_ESCAPES = { '&' => '&amp;', '<' => '&lt;', '>' => '&gt;' }.freeze

      # Android string-resource escaping, matching the conventions in the
      # existing localized files (XML-escape, then \" \' , then leading @/?).
      # Block-form gsub keeps literal backslashes out of gsub replacement rules.
      def escape(str)
        s = str.to_s.gsub(/[&<>]/, XML_ESCAPES)
        s = s.gsub('"') { '\"' }
        s = s.gsub("'") { "\\'" }
        s = s.sub(/\A@/) { '\@' }
        s.sub(/\A\?/) { '\?' }
      end

      # Inverse of #escape (XML entities are already decoded by the parser).
      # A no-op on English source, which carries no Android escapes.
      def unescape(str)
        s = str.to_s
        s = s.sub(/\A\\@/) { '@' }
        s = s.sub(/\A\\\?/) { '?' }
        s = s.gsub(/\\"/) { '"' }
        s.gsub(/\\'/) { "'" }
      end

      def header(locale)
        <<~HDR
          <?xml version="1.0" encoding="UTF-8"?>
          <!--
          Generator: WooAiTranslation/#{VERSION}
          Prompt-Version: #{PROMPT_VERSION}
          Language: #{locale}
          Warning: Machine-translated. Spot-checked, non-blocking review.
          -->
          <resources xmlns:tools="http://schemas.android.com/tools">
        HDR
      end

      def attr_string(unit)
        unit.attributes
            .reject { |k, _| %w[translatable name].include?(k) }
            .map { |k, v| %( #{k}="#{escape(v)}") }
            .join
      end

      def render_unit(unit)
        case unit.type
        when :string
          %(    <string name="#{unit.name}"#{attr_string(unit)}>#{escape(unit.entries.first[:value])}</string>)
        when :array
          items = unit.entries.map { |e| %(        <item>#{escape(e[:value])}</item>) }
          ["    <string-array name=\"#{unit.name}\"#{attr_string(unit)}>", *items, '    </string-array>'].join("\n")
        when :plurals
          items = unit.entries.map { |e| %(        <item quantity="#{e[:quantity]}">#{escape(e[:value])}</item>) }
          ["    <plurals name=\"#{unit.name}\"#{attr_string(unit)}>", *items, '    </plurals>'].join("\n")
        end
      end

      # `units` must already carry translated values; only fully-translated
      # units are written (Android falls back to the default resource).
      def write(path, units, locale)
        body = units.select(&:fully_translated?).map { |u| render_unit(u) }.join("\n")
        FileUtils.mkdir_p(File.dirname(path))
        File.write(path, "#{header(locale)}#{body}\n</resources>\n")
      end
    end
  end
end
