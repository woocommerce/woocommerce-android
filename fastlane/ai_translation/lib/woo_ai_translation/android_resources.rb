# frozen_string_literal: true

require 'rexml/document'
require 'digest'
require 'fileutils'
require 'set'

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
    # <plurals>. `entries` is the ordered list of translatable text fragments;
    # `comment` is the immediately-preceding XML comment (sticky within a
    # section, propagating to subsequent strings until the next comment) -- the
    # cheapest form of per-string translator context, dev-authored.
    class Unit
      attr_reader :type, :name, :attributes
      attr_accessor :entries, :comment # entries: [{ id:, source:, value:, quantity? }]
      # When set, the Writer emits this verbatim instead of calling render_unit.
      # Populated by the Engine for keys with origin=glotpress-import and
      # unchanged source, sourced from fastlane/ai_translation/baseline/. This
      # is the preserved-line writer's only contract.
      attr_accessor :preserved_xml

      def initialize(type:, name:, attributes:, comment: '')
        @type = type
        @name = name
        @attributes = attributes
        @entries = []
        @comment = comment.to_s
        @preserved_xml = nil
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
        copy = Unit.new(type: @type, name: @name, attributes: @attributes.dup, comment: @comment)
        copy.entries = @entries.map { |e| e.dup.tap { |x| x[:value] = nil } }
        copy
      end

      # An output-side copy where, for `:plurals` units, the entries are
      # reshaped to match the target locale's CLDR-required quantity categories
      # (`one`, `few`, `many`, `other`, …). Source quantities that aren't in
      # `quantities` are dropped; required quantities not present in source are
      # added, seeded with the source's `other` entry (or the first available)
      # so the model has the meaning to work from. For non-plural units this
      # behaves exactly like {#dup_shell}.
      def dup_shell_for_locale(quantities)
        return dup_shell if @type != :plurals
        return dup_shell if quantities.nil? || quantities.empty?

        seed = @entries.find { |e| e[:quantity] == 'other' } || @entries.first
        new_entries = quantities.map do |q|
          existing = @entries.find { |e| e[:quantity] == q }
          src = (existing || seed)[:source]
          { id: "#{@name}{#{q}}", quantity: q, source: src, value: nil }
        end

        copy = Unit.new(type: @type, name: @name, attributes: @attributes.dup, comment: @comment)
        copy.entries = new_entries
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

        # Walk children in document order so we can attach the most recent XML
        # comment as per-string context. Comments stick across whitespace and
        # propagate to subsequent strings until the next comment (so section
        # headers like `<!-- Payment methods -->` apply to every string in the
        # section, not only the first).
        units = []
        last_comment = ''
        root.children.each do |node|
          case node
          when REXML::Comment
            last_comment = node.string.to_s.strip
          when REXML::Element
            case node.name
            when 'string'        then units << string_unit(node, last_comment)
            when 'string-array'  then units << array_unit(node, last_comment)
            when 'plurals'       then units << plurals_unit(node, last_comment)
            end
          end
        end
        Document.new(units)
      end

      def string_unit(node, comment)
        u = Unit.new(type: :string, name: node.attributes['name'], attributes: attrs(node), comment: comment)
        u.entries = [{ id: node.attributes['name'], source: text_of(node), value: nil }]
        u
      end

      def array_unit(node, comment)
        u = Unit.new(type: :array, name: node.attributes['name'], attributes: attrs(node), comment: comment)
        u.entries = node.get_elements('item').each_with_index.map do |item, i|
          { id: "#{u.name}[#{i}]", source: text_of(item), value: nil }
        end
        u
      end

      def plurals_unit(node, comment)
        u = Unit.new(type: :plurals, name: node.attributes['name'], attributes: attrs(node), comment: comment)
        u.entries = node.get_elements('item').map do |item|
          q = item.attributes['quantity']
          { id: "#{u.name}{#{q}}", quantity: q, source: text_of(item), value: nil }
        end
        u
      end

      def attrs(node)
        # Use expanded_name so namespace-prefixed attributes (e.g. `tools:override`)
        # keep their prefix. Without this REXML returns just the local name
        # (`override`), the Writer reflects that on output, and Android Lint flags
        # `<string name="copy" override="true">` as a private-symbol override that
        # doesn't actually opt out of the lint check.
        h = {}
        node.attributes.each_attribute { |a| h[a.expanded_name] = a.value }
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

    # Reads a baseline strings.xml as raw text and returns { name => raw_xml_block }
    # for every top-level translatable unit. The raw block includes the start
    # tag, all content (whitespace, escapes, etc.), and the end tag, exactly as
    # they appear in the file. Empty when the file does not exist.
    #
    # The format we rely on is the convention used by the committed
    # values-<locale>/strings.xml files: top-level units indented with 4 spaces,
    # <string>...</string> always on a single line, <string-array> and <plurals>
    # spanning multiple lines with <item> children at 8-space indent, no CDATA,
    # no inline child elements inside <string>. The 16 baseline locales captured
    # in fastlane/ai_translation/baseline/ all follow this convention.
    module BaselineReader
      module_function

      # Line patterns are anchored at the 4-space indent so we ignore nested
      # <item> children of arrays/plurals which sit at 8-space indent.
      OPEN_LINE = /^    <(string|string-array|plurals)\s+[^>]*\bname="([^"]+)"/.freeze
      STRING_CLOSE_ON_SAME_LINE = /<\/string>\s*$/.freeze

      def read(path)
        return {} unless path && File.exist?(path)

        blocks = {}
        in_unit = nil # { name:, kind:, start_idx: }
        lines = File.readlines(path)

        lines.each_with_index do |line, i|
          if in_unit
            blocks[in_unit[:name]] = lines[in_unit[:start_idx]..i].join if line.start_with?("    </#{in_unit[:kind]}>")
            in_unit = nil if line.start_with?("    </#{in_unit[:kind]}>")
            next
          end

          m = line.match(OPEN_LINE)
          next unless m

          kind = m[1]
          name = m[2]
          # Single-line <string> elements close on the same line; capture and move on.
          if kind == 'string' && line.match?(STRING_CLOSE_ON_SAME_LINE)
            blocks[name] = line
          else
            in_unit = { name: name, kind: kind, start_idx: i }
          end
        end

        blocks
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
      # Units with `preserved_xml` set are emitted byte-for-byte from that
      # string -- the preserved-line path used for keys that are still
      # origin=glotpress-import. Everything else goes through `render_unit`.
      def write(path, units, locale)
        body = units.select(&:fully_translated?).map { |u| emit_unit(u) }.join("\n")
        FileUtils.mkdir_p(File.dirname(path))
        File.write(path, "#{header(locale)}#{body}\n</resources>\n")
      end

      # The preserved block, when present, may carry a trailing newline (because
      # we captured whole lines from the baseline); strip it so the join("\n")
      # in `write` produces uniform single-newline separators.
      def emit_unit(unit)
        return unit.preserved_xml.chomp if unit.preserved_xml

        render_unit(unit)
      end
    end

    # Patch writer for PR-time `--only-keys` runs. Unlike Writer.write, this does
    # not reorder the whole localized file to match the English source. It edits
    # only selected top-level resource blocks and preserves every unrelated line
    # byte-for-byte so PR diffs stay scoped to the source strings that changed.
    module PartialWriter
      module_function

      def write(path, units, locale, selected_names:)
        selected = selected_names.to_set
        rendered = units.select { |u| selected.include?(u.name) && u.fully_translated? }
                        .to_h { |u| [u.name, Writer.emit_unit(u)] }

        unless File.exist?(path)
          Writer.write(path, units.select { |u| selected.include?(u.name) && u.fully_translated? }, locale)
          return
        end

        lines = File.readlines(path)
        out = []
        replaced = Set.new
        i = 0

        while i < lines.size
          match = lines[i].match(BaselineReader::OPEN_LINE)
          if match && selected.include?(match[2])
            name = match[2]
            block_end = unit_block_end(lines, i, match[1])
            if rendered.key?(name)
              out << with_trailing_newline(rendered[name])
              replaced << name
            end
            i = block_end + 1
            next
          end

          out << lines[i]
          i += 1
        end

        insert_missing_blocks(out, rendered.reject { |name, _| replaced.include?(name) })
        File.write(path, out.join)
      end

      def unit_block_end(lines, start, kind)
        return start if kind == 'string' && lines[start].match?(BaselineReader::STRING_CLOSE_ON_SAME_LINE)

        close = /^\s*<\/#{Regexp.escape(kind)}>\s*$/
        found = (start...lines.size).find { |i| lines[i].match?(close) }
        found || start
      end

      def insert_missing_blocks(lines, blocks)
        return if blocks.empty?

        insert_at = lines.rindex { |line| line.match?(%r{^\s*</resources>\s*$}) } || lines.size
        lines.insert(insert_at, *blocks.values.map { |block| with_trailing_newline(block) })
      end

      def with_trailing_newline(block)
        block.end_with?("\n") ? block : "#{block}\n"
      end
    end
  end
end
