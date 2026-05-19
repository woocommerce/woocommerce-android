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
