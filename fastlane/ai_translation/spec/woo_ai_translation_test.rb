# frozen_string_literal: true

require 'minitest/autorun'
require 'tmpdir'
require 'fileutils'
require_relative '../lib/woo_ai_translation'

FIXTURE = File.join(__dir__, 'fixtures', 'strings_sample.xml')

class AndroidResourcesTest < Minitest::Test
  P = WooAiTranslation::AndroidResources::Parser
  W = WooAiTranslation::AndroidResources::Writer

  def test_parses_all_units_and_filters_non_translatable
    doc = P.parse_file(FIXTURE)
    assert_equal 10, doc.units.size
    assert_equal 9, doc.translatable_units.size
    refute_includes doc.translatable_names, 'pref_key_storage'
  end

  def test_cdata_and_apostrophe_resolve_to_logical_value
    doc = P.parse_file(FIXTURE)
    assert_equal 'Please <a href="support">contact us</a> now.', doc.find('html_note').entries.first[:source]
    assert_equal "Your store's orders", doc.find('possessive').entries.first[:source]
  end

  def test_array_and_plurals_shape
    doc = P.parse_file(FIXTURE)
    arr = doc.find('date_selectors')
    assert_equal :array, arr.type
    assert_equal %w[Today], [arr.entries.first[:source]]
    pl = doc.find('cart_items')
    assert_equal :plurals, pl.type
    assert_equal %w[one other], pl.entries.map { |e| e[:quantity] }
  end

  def test_escape_rules
    assert_equal '&lt;b&gt;hi&lt;/b&gt;', W.escape('<b>hi</b>')
    assert_equal 'a &amp; b', W.escape('a & b')
    assert_equal "Your store\\'s orders", W.escape("Your store's orders")
    assert_equal '\@home', W.escape('@home')
    assert_equal "\\\"q\\\"", W.escape('"q"')
  end

  def test_escape_unescape_round_trip_android_layer
    %w[plain @lead ?lead].each do |s|
      assert_equal s, W.unescape(W.escape(s))
    end
    assert_equal %(a "q" 's), W.unescape(W.escape(%(a "q" 's)))
  end

  def test_writer_preserves_order_and_formatted_attr
    doc = P.parse_file(FIXTURE)
    doc.translatable_units.each { |u| u.entries.each { |e| e[:value] = e[:source] } }
    Dir.mktmpdir do |dir|
      out = File.join(dir, 'values-xx', 'strings.xml')
      W.write(out, doc.translatable_units, 'xx')
      raw = File.read(out)
      assert_includes raw, 'formatted="false"'
      reparsed = P.parse_file(out)
      assert_equal doc.translatable_names, reparsed.translatable_names
      assert_equal 'Please <a href="support">contact us</a> now.', reparsed.find('html_note').entries.first[:source]
    end
  end
end

class XmlCommentContextTest < Minitest::Test
  P = WooAiTranslation::AndroidResources::Parser

  def test_preceding_comment_is_attached_sticky_until_next_comment
    doc = P.parse_file(FIXTURE)
    # First comment "Greetings on the main dashboard." applies to every string
    # up to (but not including) the next comment.
    assert_equal 'Greetings on the main dashboard.', doc.find('app_name').comment
    assert_equal 'Greetings on the main dashboard.', doc.find('greeting').comment
    assert_equal 'Greetings on the main dashboard.', doc.find('raw_percent').comment
    # New comment switches the context for everything below.
    assert_equal 'Help & support strings.', doc.find('html_note').comment
    assert_equal 'Help & support strings.', doc.find('possessive').comment
    assert_equal 'Help & support strings.', doc.find('cart_items').comment
    assert_equal 'Help & support strings.', doc.find('files_value_multiple').comment
  end
end

class GlossaryAndStyleTest < Minitest::Test
  def test_glossary_text_renders_when_file_present
    Dir.mktmpdir do |dir|
      g = File.join(dir, 'glossary.json')
      File.write(g, JSON.generate('terms' => [
                                    { 'term' => 'Woo', 'rule' => 'Brand; never translate.' },
                                    { 'term' => 'SKU', 'rule' => 'Keep as SKU.' }
                                  ]))
      ctx = WooAiTranslation::ContextProvider.from_file(nil, glossary_path: g)
      text = ctx.glossary_text
      assert_includes text, 'Brand & domain glossary'
      assert_includes text, '- Woo: Brand; never translate.'
      assert_includes text, '- SKU: Keep as SKU.'
    end
  end

  def test_glossary_text_is_empty_when_absent
    assert_equal '', WooAiTranslation::ContextProvider.new({}).glossary_text
  end

  def test_style_for_reads_locale_markdown
    Dir.mktmpdir do |dir|
      File.write(File.join(dir, 'de.md'), 'Use informal Du.')
      ctx = WooAiTranslation::ContextProvider.new({}, style_dir: dir)
      assert_equal 'Use informal Du.', ctx.style_for('de')
      assert_equal '', ctx.style_for('xx')
    end
  end

  def test_translator_includes_glossary_as_cached_system_block
    captured = []
    client = Class.new do
      define_method(:available?) { true }
      define_method(:complete) do |model:, system_blocks:, user_content:, max_tokens: 8192|
        captured << system_blocks.dup
        '{}'
      end
    end.new

    t = WooAiTranslation::Translator.new(client: client, glossary: 'Brand & domain glossary (applies to every locale):\n- Woo: Brand; never translate.')
    t.translate(locale: 'fr', items: [{ id: 'k', source: 's', context: '' }], model: WooAiTranslation::DEFAULT_MODEL, style: 'fr style.')

    blocks = captured.first
    assert_equal 3, blocks.size, 'rules + glossary + per-locale style'
    assert_includes blocks[0], 'professional software localizer'
    assert_includes blocks[1], 'Brand & domain glossary'
    assert_includes blocks[2], 'fr style.'
  end
end

class ClaudeCliClientTest < Minitest::Test
  def test_invokes_binary_strips_fences_and_handles_utf8
    Dir.mktmpdir do |dir|
      bin = File.join(dir, 'fake_claude')
      File.write(bin, <<~SH)
        #!/bin/bash
        # Discard stdin (the prompt), emit fenced JSON with UTF-8 chars.
        cat > /dev/null
        printf '```json\\n{"k":"Cześć świecie"}\\n```\\n'
      SH
      File.chmod(0o755, bin)
      client = WooAiTranslation::ClaudeCliClient.new(bin: bin)
      assert client.available?
      out = client.complete(model: 'm', system_blocks: ['rule'], user_content: 'translate')
      assert_equal '{"k":"Cześć świecie"}', out
      assert_equal 'UTF-8', out.encoding.to_s
    end
  end

  def test_raises_when_binary_exits_non_zero
    Dir.mktmpdir do |dir|
      bin = File.join(dir, 'broken')
      File.write(bin, "#!/bin/bash\necho 'kaboom' >&2\nexit 7\n")
      File.chmod(0o755, bin)
      client = WooAiTranslation::ClaudeCliClient.new(bin: bin)
      err = assert_raises(WooAiTranslation::ClaudeCliClient::Error) do
        client.complete(model: 'm', system_blocks: [], user_content: '?')
      end
      assert_includes err.message, 'exit 7'
      assert_includes err.message, 'kaboom'
    end
  end
end

class FormattedFalsePlaceholderExemptionTest < Minitest::Test
  include WooAiTranslation

  def test_formatted_false_strings_are_not_placeholder_validated
    Dir.mktmpdir do |dir|
      # "50% off" with formatted="false" must translate freely — the placeholder
      # regex would otherwise see "% o" as a printf-style specifier and reject
      # any locale that does not contain the same literal characters.
      stub = StubClient.new { |loc, src| "[#{loc}] #{src.sub('50%', '90%').sub('off', 'rabatu')}" }
      Engine.new(
        source_path: FIXTURE, res_dir: dir,
        manifest: Manifest.load(File.join(dir, 'm.json')),
        manifest_path: File.join(dir, 'm.json'),
        translator: Translator.new(client: stub),
        context: ContextProvider.new({})
      ).run_strings(locales: %w[xx])

      raw = AndroidResources::Parser.parse_file(File.join(dir, 'values-xx', 'strings.xml'))
                                    .find('raw_percent')
      refute_nil raw, 'raw_percent must ship (no false-positive placeholder failure)'
      assert_includes raw.entries.first[:source], '90%'
    end
  end
end

class ManifestTest < Minitest::Test
  M = WooAiTranslation::Manifest

  def test_cache_key_is_deterministic_and_sensitive
    m = M.new
    a = m.cache_key(source: 's', context: 'c', locale: 'fr', model: 'X')
    assert_equal a, m.cache_key(source: 's', context: 'c', locale: 'fr', model: 'X')
    refute_equal a, m.cache_key(source: 's', context: 'c', locale: 'fr', model: 'Y')
    refute_equal a, m.cache_key(source: 's', context: 'c', locale: 'de', model: 'X')
    refute_equal a, m.cache_key(source: 's2', context: 'c', locale: 'fr', model: 'X')
  end

  def test_stale_record_origin_and_persistence
    Dir.mktmpdir do |dir|
      path = File.join(dir, 'm.json')
      m = M.new
      ck = m.cache_key(source: 's', context: '', locale: 'fr', model: 'X')
      assert m.stale?(name: 'k', locale: 'fr', expected_cache_key: ck)
      m.record(name: 'k', locale: 'fr', cache_key: ck, model: 'X', origin: 'ai', source_sha: 'abc')
      refute m.stale?(name: 'k', locale: 'fr', expected_cache_key: ck)
      assert m.stale?(name: 'k', locale: 'fr', expected_cache_key: 'different')
      assert_equal 'ai', m.origin(name: 'k', locale: 'fr')
      m.save(path)

      reloaded = M.load(path)
      refute reloaded.stale?(name: 'k', locale: 'fr', expected_cache_key: ck)
    end
  end
end

class ValidatorsTest < Minitest::Test
  V = WooAiTranslation::Validators

  def test_placeholder_parity
    assert_empty V.placeholder_parity('Hi %1$s', 'Hola %1$s')
    assert_empty V.placeholder_parity('a %% b', 'x %% y')
    refute_empty V.placeholder_parity('Hi %1$s and %2$d', 'Hola %1$s')
    refute_empty V.placeholder_parity('Plain', 'Now %d')
  end

  def test_plural_pairs_is_informational_only
    assert_empty V.plural_pairs(%w[x_single x_multiple y])
    refute_empty V.plural_pairs(%w[x_single y])
    refute_empty V.plural_pairs(%w[x_multiple y])
  end

  def test_plural_pair_integrity_gate
    src = %w[x_single x_multiple lonely_single z]
    # Real pair, both shipped -> ok.
    assert_empty V.plural_pair_integrity(source_names: src, output_names: %w[x_single x_multiple z])
    # Real pair, one side collapsed -> blocking error.
    refute_empty V.plural_pair_integrity(source_names: src, output_names: %w[x_single z])
    # Intentionally-unpaired source key -> not enforced, no error.
    assert_empty V.plural_pair_integrity(source_names: src, output_names: %w[lonely_single z])
  end

  def test_key_parity_and_char_cap
    assert_empty V.key_parity(source_names: %w[a b], output_names: %w[a b])
    refute_empty V.key_parity(source_names: %w[a b], output_names: %w[a b c])
    assert_empty V.char_cap(field: 'title.txt', text: 'short', cap: 50)
    assert_empty V.char_cap(field: 'full_description.txt', text: 'x' * 9999, cap: 0)
    refute_empty V.char_cap(field: 'title.txt', text: 'x' * 51, cap: 50)
  end
end

class EngineTest < Minitest::Test
  include WooAiTranslation

  def build(dir, source: FIXTURE, client: StubClient.new)
    Engine.new(
      source_path: source,
      res_dir: dir,
      manifest: Manifest.load(File.join(dir, 'manifest.json')),
      manifest_path: File.join(dir, 'manifest.json'),
      translator: Translator.new(client: client),
      context: ContextProvider.new({})
    )
  end

  def test_full_translation_then_idempotent_reuse_then_delta
    Dir.mktmpdir do |dir|
      c1 = StubClient.new
      r1 = build(dir, client: c1).run_strings(locales: %w[fr pl])
      assert_equal 2, r1.size
      fr = r1.find { |r| r.locale == 'fr' }
      assert_equal 9, fr.translated
      assert_equal 0, fr.reused
      assert_empty fr.failed
      assert_empty fr.gate_errors
      assert c1.calls.positive?

      doc = AndroidResources::Parser.parse_file(File.join(dir, 'values-fr', 'strings.xml'))
      assert_nil doc.find('pref_key_storage')
      assert_equal '[fr] Woo', doc.find('app_name').entries.first[:source]
      assert_equal '[fr] Please <a href="support">contact us</a> now.',
                   doc.find('html_note').entries.first[:source]
      assert_equal %w[one other], doc.find('cart_items').entries.map { |e| e[:quantity] }

      # Reuse: nothing stale, existing files present -> zero model calls.
      c2 = StubClient.new
      r2 = build(dir, client: c2).run_strings(locales: %w[fr pl])
      assert_equal 0, c2.calls
      assert_equal 9, r2.find { |r| r.locale == 'fr' }.reused
      assert_equal 0, r2.find { |r| r.locale == 'fr' }.translated

      # Delta: change one source string -> only that key retranslates.
      modified = File.join(dir, 'modified.xml')
      File.write(modified, File.read(FIXTURE).sub('Hello %1$s, you have %2$d items',
                                                  'Hi %1$s, %2$d items await'))
      c3 = StubClient.new
      r3 = build(dir, source: modified, client: c3).run_strings(locales: %w[fr])
      d = r3.first
      assert_equal 1, d.translated
      assert_equal 8, d.reused
      assert_equal 1, c3.calls
    end
  end

  def test_metadata_engine_caps_release_notes_gating_and_reuse
    Dir.mktmpdir do |dir|
      src = File.join(dir, 'en-US')
      FileUtils.mkdir_p(File.join(src, 'changelogs'))
      File.write(File.join(src, 'title.txt'), 'X' * 60) # forces over-cap -> English fallback
      File.write(File.join(src, 'short_description.txt'), 'Sell anywhere')
      File.write(File.join(src, 'full_description.txt'), 'A long description.')
      File.write(File.join(src, 'changelogs', 'default.txt'), 'Bug fixes')
      out = File.join(dir, 'out')
      mpath = File.join(dir, 'm.json')

      eng = MetadataEngine.new(translator: Translator.new(client: StubClient.new),
                               manifest: Manifest.load(mpath), manifest_path: mpath)
      r = eng.run(source_dir: src, out_base: out, locales: %w[de-DE],
                  include_release_notes: false).first

      assert_equal 'X' * 60, File.read(File.join(out, 'de-DE', 'title.txt')), 'over-cap -> English fallback'
      assert_includes r.fallback, 'title'
      assert_equal '[de-DE] Sell anywhere', File.read(File.join(out, 'de-DE', 'short_description.txt'))
      refute File.exist?(File.join(out, 'de-DE', 'changelogs', 'default.txt')), 'release notes not at PR-time'

      r2 = eng.run(source_dir: src, out_base: out, locales: %w[de-DE], include_release_notes: false).first
      assert_equal 0, r2.translated
      assert r2.reused.positive?
    end
  end

  def test_baseline_import_preserves_human_strings_and_ai_fills_gaps
    Dir.mktmpdir do |dir|
      FileUtils.mkdir_p(File.join(dir, 'values-fr'))
      File.write(File.join(dir, 'values-fr', 'strings.xml'),
                 "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<resources>\n" \
                 "    <string name=\"app_name\">Woo!</string>\n</resources>\n")
      mpath = File.join(dir, 'm.json')

      seed = Manifest.new
      imp = Importer.new(source_path: FIXTURE, res_dir: dir,
                         manifest: seed, context: ContextProvider.new({}))
      rep = imp.import(locales: %w[fr pl]).find { |r| r.locale == 'fr' }
      assert_equal 1, rep.imported
      assert rep.gaps.positive?
      assert_equal 'glotpress-import', seed.origin(name: 'app_name', locale: 'fr')
      seed.save(mpath)

      stub = StubClient.new
      Engine.new(source_path: FIXTURE, res_dir: dir,
                 manifest: Manifest.load(mpath), manifest_path: mpath,
                 translator: Translator.new(client: stub),
                 context: ContextProvider.new({})).run_strings(locales: %w[fr pl])

      doc = AndroidResources::Parser.parse_file(File.join(dir, 'values-fr', 'strings.xml'))
      assert_equal 'Woo!', doc.find('app_name').entries.first[:source], 'human string preserved, not re-translated'
      assert_equal '[fr] Hello %1$s, you have %2$d items', doc.find('greeting').entries.first[:source]

      final = Manifest.load(mpath)
      assert_equal 'glotpress-import', final.origin(name: 'app_name', locale: 'fr')
      assert_equal 'ai', final.origin(name: 'greeting', locale: 'fr')
      assert stub.calls.positive?
    end
  end

  def test_shadow_diff_reports_without_touching_repo
    Dir.mktmpdir do |dir|
      FileUtils.mkdir_p(File.join(dir, 'values-fr'))
      committed = File.join(dir, 'values-fr', 'strings.xml')
      File.write(committed,
                 "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<resources>\n" \
                 "    <string name=\"app_name\">Woo humain</string>\n</resources>\n")
      before = File.read(committed)

      report = ShadowDiff.new(source_path: FIXTURE, res_dir: dir,
                              translator: Translator.new(client: StubClient.new),
                              context: ContextProvider.new({})).run(locales: %w[fr])

      assert_includes report, '## fr'
      assert_includes report, 'changed='
      assert_equal before, File.read(committed), 'shadow diff must not modify committed files'
    end
  end

  def test_changing_an_xml_comment_only_retranslates_its_section
    Dir.mktmpdir do |dir|
      mpath = File.join(dir, 'm.json')

      # Initial run seeds everything.
      c1 = StubClient.new
      build(dir, client: c1).run_strings(locales: %w[fr])

      # Rewrite the source: change ONLY the first comment ("Greetings ...").
      # Sticky propagation means the 3 strings in that section invalidate;
      # the 6 strings under the second comment must reuse.
      modified = File.join(dir, 'modified.xml')
      File.write(modified, File.read(FIXTURE).sub('Greetings on the main dashboard.',
                                                  'Dashboard greetings, shown above the order list.'))

      c2 = StubClient.new
      report = build(dir, source: modified, client: c2).run_strings(locales: %w[fr]).first

      assert_equal 3, report.translated, 'only the section above the changed comment retranslates'
      assert_equal 6, report.reused
      assert_equal 1, c2.calls
    end
  end

  def test_placeholder_failure_is_dropped_not_shipped
    Dir.mktmpdir do |dir|
      bad = StubClient.new { |loc, src| "[#{loc}] #{src.gsub('%2$d', '')}" }
      report = build(dir, client: bad).run_strings(locales: %w[fr]).first
      assert(report.failed.any? { |f| f.start_with?('greeting') })

      doc = AndroidResources::Parser.parse_file(File.join(dir, 'values-fr', 'strings.xml'))
      assert_nil doc.find('greeting'), 'broken translation must be omitted, not shipped'
      assert_equal '[fr] Woo', doc.find('app_name').entries.first[:source]

      manifest = Manifest.load(File.join(dir, 'manifest.json'))
      assert_nil manifest.origin(name: 'greeting', locale: 'fr')
    end
  end
end
