# frozen_string_literal: true

require 'minitest/autorun'
require 'tmpdir'
require 'fileutils'
require 'English'
require_relative '../lib/woo_ai_translation'

FIXTURE = File.join(__dir__, 'fixtures', 'strings_sample.xml')

class SourceEncodingTest < Minitest::Test
  def test_ai_translation_ruby_sources_do_not_contain_null_bytes
    paths = Dir.glob(File.expand_path('../**/*.rb', __dir__))
    offenders = paths.select { |path| File.binread(path).include?("\u0000") }

    assert_empty offenders
  end
end

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
                                    { 'term' => 'Woo', 'rule' => 'Brand; never translate.', 'preserve' => true },
                                    { 'term' => 'SKU', 'rule' => 'Keep as SKU.' }
                                  ]))
      ctx = WooAiTranslation::ContextProvider.from_file(nil, glossary_path: g)
      text = ctx.glossary_text
      assert_includes text, 'Brand & domain glossary'
      assert_includes text, '- Woo: Brand; never translate. [hard gate: preserve exact term]'
      assert_includes text, '- SKU: Keep as SKU.'
    end
  end

  def test_preserved_terms_returns_only_hard_gated_terms
    ctx = WooAiTranslation::ContextProvider.new({}, glossary: {
                                                  'terms' => [
                                                    { 'term' => 'WooPayments', 'preserve' => true },
                                                    { 'term' => 'Coupon', 'rule' => 'Translate naturally.' },
                                                    { 'term' => 'SKU', 'preserve' => true }
                                                  ]
                                                })
    assert_equal %w[WooPayments SKU], ctx.preserved_terms
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

class AnthropicTemperatureFallbackTest < Minitest::Test
  AC = WooAiTranslation::AnthropicClient

  # Fake Net::HTTP: rejects `temperature` with HTTP 400 on the first request
  # (mimicking claude-opus-4-7), then succeeds. Records each request body so we
  # can assert the retry dropped temperature and later calls skip it entirely.
  class FakeHttp
    Resp = Struct.new(:code, :body)
    attr_reader :bodies

    def initialize
      @bodies = []
      @calls = 0
    end

    def request(req)
      @bodies << JSON.parse(req.body)
      @calls += 1
      if @calls == 1
        Resp.new('400', '{"type":"error","error":{"type":"invalid_request_error",' \
                        '"message":"`temperature` is deprecated for this model."}}')
      else
        Resp.new('200', '{"content":[{"text":"{\"k\":\"ok\"}"}]}')
      end
    end
  end

  def test_drops_temperature_and_retries_on_deprecation_400
    fake = FakeHttp.new
    out = AC.new(api_key: 'k', http: fake)
            .complete(model: 'claude-opus-4-7', system_blocks: ['rules'], user_content: 'translate')
    assert_equal '{"k":"ok"}', out
    assert_equal 2, fake.bodies.size, 'should retry exactly once'
    assert fake.bodies[0].key?('temperature'), 'first attempt includes temperature: 0'
    refute fake.bodies[1].key?('temperature'), 'retry omits temperature'
  end

  def test_remembers_model_and_skips_temperature_on_subsequent_calls
    fake = FakeHttp.new
    client = AC.new(api_key: 'k', http: fake)
    client.complete(model: 'claude-opus-4-7', system_blocks: ['r'], user_content: 'a') # 400 -> retry
    client.complete(model: 'claude-opus-4-7', system_blocks: ['r'], user_content: 'b') # learned: no temp
    assert_equal 3, fake.bodies.size
    refute fake.bodies[2].key?('temperature'), 'second call skips temperature from the start (no wasted 400)'
  end
end

class TranslatorJsonRepairTest < Minitest::Test
  def client_returning(raw)
    Class.new do
      define_method(:available?) { true }
      define_method(:complete) { |model:, system_blocks:, user_content:, max_tokens: 8192| raw }
    end.new
  end

  def test_repairs_raw_newlines_and_tabs_inside_json_string_values
    # The model emitted a multi-paragraph value with LITERAL newlines/tab
    # (invalid JSON) — exactly the full_description / release_notes failure
    # seen on pl-PL and cs-CZ during the metadata backfill.
    raw = %({"full_description":"Para one.\n\nPara two with a\ttab."})
    t = WooAiTranslation::Translator.new(client: client_returning(raw))
    out = t.translate(locale: 'pl', model: 'm',
                      items: [{ id: 'full_description', source: 'x', context: '' }])
    assert_equal "Para one.\n\nPara two with a\ttab.", out['full_description']
  end

  def test_valid_json_still_parses_unchanged
    raw = '{"k":"already \\n escaped fine"}'
    t = WooAiTranslation::Translator.new(client: client_returning(raw))
    out = t.translate(locale: 'pl', model: 'm', items: [{ id: 'k', source: 'x', context: '' }])
    assert_equal "already \n escaped fine", out['k']
  end

  def test_raw_text_mode_takes_response_verbatim_no_json_parsing
    # Metadata path: a multi-paragraph response with quotes/newlines that would
    # break a JSON envelope is taken as-is (after fence stripping) in raw mode.
    raw = "```\nZarządzaj sklepem \"od ręki\"\n\nDodawaj produkty.\n```"
    t = WooAiTranslation::Translator.new(client: client_returning(raw))
    out = t.translate(locale: 'pl', model: 'm', raw_text: true,
                      items: [{ id: 'full_description', source: 'x', context: 'full_description' }])
    assert_equal "Zarządzaj sklepem \"od ręki\"\n\nDodawaj produkty.", out['full_description']
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

  def test_legacy_cache_key_helper_is_deterministic_and_sensitive
    # Metadata staleness still leans on this multi-component hash; app-string
    # translations don't. Locked here so the metadata path doesn't silently
    # change.
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
      sha = 'abc'
      # Empty manifest: no key entry yet -> trust whatever's on disk -> not stale.
      refute m.stale?(name: 'k', locale: 'fr', expected_source_sha: sha)
      m.record(name: 'k', locale: 'fr', model: 'X', origin: 'ai', source_sha: sha)
      refute m.stale?(name: 'k', locale: 'fr', expected_source_sha: sha)
      # Same key, locale that was never recorded for it: stale (locale missing).
      assert m.stale?(name: 'k', locale: 'de', expected_source_sha: sha)
      # Source mismatch: stale even though the locale is recorded.
      assert m.stale?(name: 'k', locale: 'fr', expected_source_sha: 'different')
      assert_equal 'ai', m.origin(name: 'k', locale: 'fr')
      m.save(path)

      reloaded = M.load(path)
      refute reloaded.stale?(name: 'k', locale: 'fr', expected_source_sha: sha)
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

  def test_glossary_preservation_uses_longest_match_wins
    terms = %w[Woo WooCommerce WooPayments SKU PIN]
    assert_empty V.glossary_preservation('Use WooCommerce SKU', 'Utiliser WooCommerce SKU', terms)
    assert_empty V.glossary_preservation('Use WooPayments', 'Utiliser WooPayments', terms)
    assert_empty V.glossary_preservation('Connect Jetpack', 'Yhdistä Jetpackin', %w[Jetpack])
    assert_empty V.glossary_preservation('SHIPPING', 'LIVRAISON', terms)

    errors = V.glossary_preservation('Use WooCommerce SKU', 'Utiliser Woo commerce UGS', terms)
    assert_includes errors, 'glossary term not preserved: WooCommerce'
    assert_includes errors, 'glossary term not preserved: SKU'
    refute_includes errors, 'glossary term not preserved: Woo'
    assert_empty V.glossary_preservation('Use Woo', 'Pidätkö Woosta?', terms)
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
    assert_empty V.key_parity(source_names: %w[a b], output_names: %w[a])
    refute_empty V.key_parity(source_names: %w[a b], output_names: %w[a], require_all: true)
    assert_empty V.char_cap(field: 'title.txt', text: 'short', cap: 50)
    assert_empty V.char_cap(field: 'full_description.txt', text: 'x' * 9999, cap: 0)
    refute_empty V.char_cap(field: 'title.txt', text: 'x' * 51, cap: 50)
  end

  def test_no_unselected_key_loss
    assert_empty V.no_unselected_key_loss(
      before_names: %w[existing touched],
      output_names: %w[existing touched new],
      selected_names: %w[touched new]
    )
    assert_empty V.no_unselected_key_loss(
      before_names: %w[existing touched],
      output_names: %w[existing],
      selected_names: %w[touched]
    )
    refute_empty V.no_unselected_key_loss(
      before_names: %w[existing touched],
      output_names: %w[touched],
      selected_names: %w[touched]
    )
  end
end

class EngineTest < Minitest::Test
  include WooAiTranslation

  def build(dir, source: FIXTURE, client: StubClient.new, context: ContextProvider.new({}), only_names: nil)
    Engine.new(
      source_path: source,
      res_dir: dir,
      manifest: Manifest.load(File.join(dir, 'manifest.json')),
      manifest_path: File.join(dir, 'manifest.json'),
      translator: Translator.new(client: client),
      context: context,
      only_names: only_names
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
      # French CLDR plural categories are one/many/other; the engine reshapes
      # the source's [one, other] plurals block to cover all three.
      assert_equal %w[one many other], doc.find('cart_items').entries.map { |e| e[:quantity] }

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

  def test_changing_an_xml_comment_does_not_retranslate_anything
    # Under the source-only invalidation contract, dev-authored context edits
    # (XML comments, AINFRA-1707 entries) are NOT triggers. Adding a clarifying
    # section header must not cause a 31-locale re-run of already-translated
    # keys. To force re-translation, the operator uses --keys / --key-pattern.
    Dir.mktmpdir do |dir|
      # Initial run seeds everything.
      c1 = StubClient.new
      build(dir, client: c1).run_strings(locales: %w[fr])

      # Rewrite the source: change the first XML comment. Existing source TEXT
      # is unchanged for every key, so source_sha matches and nothing
      # invalidates.
      modified = File.join(dir, 'modified.xml')
      File.write(modified, File.read(FIXTURE).sub('Greetings on the main dashboard.',
                                                  'Dashboard greetings, shown above the order list.'))

      c2 = StubClient.new
      report = build(dir, source: modified, client: c2).run_strings(locales: %w[fr]).first

      assert_equal 0, report.translated, 'comment changes must not auto-invalidate'
      assert_equal 9, report.reused
      assert_equal 0, c2.calls, 'no model calls when nothing is stale'
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

  def test_glossary_preservation_failure_is_dropped_not_shipped
    Dir.mktmpdir do |dir|
      context = ContextProvider.new({}, glossary: {
                                      'terms' => [
                                        { 'term' => 'Woo', 'rule' => 'Brand; never translate.', 'preserve' => true }
                                      ]
                                    })
      bad = StubClient.new { |loc, src| "[#{loc}] #{src.gsub('Woo', 'Oua')}" }
      report = build(dir, client: bad, context: context).run_strings(locales: %w[fr]).first
      assert(report.failed.any? { |f| f.start_with?('app_name') })

      doc = AndroidResources::Parser.parse_file(File.join(dir, 'values-fr', 'strings.xml'))
      assert_nil doc.find('app_name'), 'brand-unsafe translation must be omitted, not shipped'
      assert_equal '[fr] Hello %1$s, you have %2$d items', doc.find('greeting').entries.first[:source]

      manifest = Manifest.load(File.join(dir, 'manifest.json'))
      assert_nil manifest.origin(name: 'app_name', locale: 'fr')
    end
  end

  def test_polish_plurals_get_all_four_cldr_quantities_synthesized
    Dir.mktmpdir do |dir|
      build(dir).run_strings(locales: %w[pl])
      doc = AndroidResources::Parser.parse_file(File.join(dir, 'values-pl', 'strings.xml'))
      assert_equal %w[one few many other],
                   doc.find('cart_items').entries.map { |e| e[:quantity] }
    end
  end

  def test_thai_plurals_drop_irrelevant_one_quantity
    Dir.mktmpdir do |dir|
      build(dir).run_strings(locales: %w[th])
      doc = AndroidResources::Parser.parse_file(File.join(dir, 'values-th', 'strings.xml'))
      # Thai CLDR uses only `other`; the engine drops the irrelevant `one` form.
      assert_equal %w[other], doc.find('cart_items').entries.map { |e| e[:quantity] }
    end
  end

  # ---- Source-only invalidation contract -------------------------------------
  # These tests encode the rows of the table the team agreed on: source-text
  # changes are the ONLY automatic re-translation trigger. Model, prompt,
  # context-comment, and attribute changes don't auto-propagate. On-demand
  # re-translation runs through Manifest#invalidate (the --keys / --key-pattern
  # CLI flags).

  def test_model_bump_does_not_invalidate_existing_translations
    # Run 1 seeds translations with the default model. Run 2 forces a different
    # model -- under the source-only contract the existing translations stay
    # put (force_model is only consulted for keys that would translate anyway,
    # which here is none).
    Dir.mktmpdir do |dir|
      build(dir, client: StubClient.new).run_strings(locales: %w[fr])

      bumped_client = StubClient.new
      engine = Engine.new(
        source_path: FIXTURE, res_dir: dir,
        manifest: Manifest.load(File.join(dir, 'manifest.json')),
        manifest_path: File.join(dir, 'manifest.json'),
        translator: Translator.new(client: bumped_client),
        context: ContextProvider.new({}),
        force_model: 'claude-opus-4-7'
      )
      r2 = engine.run_strings(locales: %w[fr]).first
      assert_equal 0, r2.translated, 'a model bump must not auto-invalidate existing translations'
      assert_equal 9, r2.reused
      assert_equal 0, bumped_client.calls
    end
  end

  def test_attribute_only_change_does_not_invalidate
    # Toggling a non-text attribute (e.g. adding formatted="false") must not
    # invalidate. source_signature is built from entry text only.
    Dir.mktmpdir do |dir|
      build(dir, client: StubClient.new).run_strings(locales: %w[fr])

      modified = File.join(dir, 'modified.xml')
      File.write(modified, File.read(FIXTURE).sub('<string name="app_name">',
                                                  '<string name="app_name" formatted="false">'))

      c2 = StubClient.new
      r2 = build(dir, source: modified, client: c2).run_strings(locales: %w[fr]).first
      assert_equal 0, r2.translated, 'attribute-only change must not invalidate'
      assert_equal 9, r2.reused
      assert_equal 0, c2.calls
    end
  end

  def test_manifest_loss_trusts_existing_localized_files
    # Bootstrap path: an operator wipes translation-manifest.json by accident
    # (or merges across branches and ends up with an empty one). The engine
    # must NOT decide every existing locale file is stale and clobber the
    # human translations -- it should treat the on-disk files as authoritative
    # for keys whose source it has never seen.
    Dir.mktmpdir do |dir|
      build(dir, client: StubClient.new).run_strings(locales: %w[fr])
      File.delete(File.join(dir, 'manifest.json'))

      bad_client = StubClient.new { |loc, src| "[#{loc}] DESTRUCTIVE: #{src}" }
      r2 = build(dir, client: bad_client).run_strings(locales: %w[fr]).first
      assert_equal 0, r2.translated, 'empty manifest + file present must trust the file'
      assert_equal 9, r2.reused
      assert_equal 0, bad_client.calls, 'no model calls when the file is trusted'

      doc = AndroidResources::Parser.parse_file(File.join(dir, 'values-fr', 'strings.xml'))
      refute_includes doc.find('app_name').entries.first[:source], 'DESTRUCTIVE'
    end
  end

  def test_keys_invalidation_still_forces_retranslation
    # --keys / --key-pattern (via Manifest#invalidate) is the operator's only
    # auto-invalidation path under the new contract. Removing the locale entry
    # must make build_plan re-translate that (name, locale).
    Dir.mktmpdir do |dir|
      manifest_path = File.join(dir, 'manifest.json')
      build(dir, client: StubClient.new).run_strings(locales: %w[fr])

      m = Manifest.load(manifest_path)
      assert_equal 1, m.invalidate(name: 'app_name', locales: %w[fr])
      m.save(manifest_path)

      c2 = StubClient.new
      r2 = build(dir, client: c2).run_strings(locales: %w[fr]).first
      assert_equal 1, r2.translated, '--keys must force re-translation of the invalidated key'
      assert_equal 8, r2.reused
    end
  end

  def test_only_names_translates_pr_delta_without_filling_old_gaps
    Dir.mktmpdir do |dir|
      FileUtils.mkdir_p(File.join(dir, 'values-fr'))
      File.write(File.join(dir, 'values-fr', 'strings.xml'),
                 "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<resources>\n" \
                 "    <string name=\"app_name\">Woo humain</string>\n</resources>\n")

      report = build(dir, client: StubClient.new, only_names: %w[greeting]).run_strings(locales: %w[fr]).first

      assert_equal 1, report.translated
      assert_equal 1, report.reused
      assert_empty report.failed
      assert_empty report.gate_errors

      doc = AndroidResources::Parser.parse_file(File.join(dir, 'values-fr', 'strings.xml'))
      assert_equal 'Woo humain', doc.find('app_name').entries.first[:source]
      assert_equal '[fr] Hello %1$s, you have %2$d items', doc.find('greeting').entries.first[:source]
      assert_nil doc.find('raw_percent'), 'unselected missing keys must remain gaps'
    end
  end

  def test_only_names_preserves_existing_file_order_and_header
    Dir.mktmpdir do |dir|
      localized = File.join(dir, 'values-fr', 'strings.xml')
      FileUtils.mkdir_p(File.dirname(localized))
      File.write(localized, <<~XML)
        <?xml version="1.0" encoding="UTF-8"?>
        <!--
        Generator: GlotPress/2.4.0-alpha
        Language: fr
        -->
        <resources xmlns:tools="http://schemas.android.com/tools">
            <string name="raw_percent" formatted="false">50 % de réduction</string>
            <string name="app_name">Woo humain</string>
        </resources>
      XML
      before = File.read(localized)

      report = build(dir, client: StubClient.new, only_names: %w[greeting]).run_strings(locales: %w[fr]).first

      assert_equal 1, report.translated
      assert_empty report.gate_errors
      after = File.read(localized)
      assert_includes after, "Generator: GlotPress/2.4.0-alpha\n"
      assert_includes after, %(    <string name="raw_percent" formatted="false">50 % de réduction</string>\n)
      assert_includes after, %(    <string name="app_name">Woo humain</string>\n)
      assert_includes after, %(    <string name="greeting">[fr] Hello %1$s, you have %2$d items</string>\n)
      assert_operator after.index('raw_percent'), :<, after.index('app_name')
      assert_equal before.lines[0, 7], after.lines[0, 7], 'unrelated file prefix must stay byte-for-byte'
    end
  end

  def test_only_names_does_not_drop_unselected_plural_that_cannot_be_reused
    Dir.mktmpdir do |dir|
      localized = File.join(dir, 'values-pl', 'strings.xml')
      FileUtils.mkdir_p(File.dirname(localized))
      raw_plural = <<~XML.chomp
            <plurals name="cart_items">
                <item quantity="one">%d artykuł</item>
                <item quantity="other">%d artykułów</item>
            </plurals>
      XML
      File.write(localized, <<~XML)
        <?xml version="1.0" encoding="UTF-8"?>
        <resources>
        #{raw_plural}
            <string name="app_name">Woo człowiek</string>
        </resources>
      XML

      report = build(dir, client: StubClient.new, only_names: %w[greeting]).run_strings(locales: %w[pl]).first

      assert_equal 1, report.translated
      assert_empty report.gate_errors
      after = File.read(localized)
      assert_includes after, raw_plural
      assert_includes after, %(    <string name="app_name">Woo człowiek</string>\n)
      assert_includes after, %(    <string name="greeting">[pl] Hello %1$s, you have %2$d items</string>\n)
    end
  end

  def test_full_generated_locale_reports_missing_key_parity
    Dir.mktmpdir do |dir|
      bad = StubClient.new { |loc, src| "[#{loc}] #{src.gsub('%2$d', '')}" }
      report = build(dir, client: bad).run_strings(locales: %w[fr]).first

      assert(report.failed.any? { |f| f.start_with?('greeting') })
      assert(report.gate_errors.any? { |e| e.include?('missing keys from output') })
    end
  end

  def test_malformed_generated_xml_is_reported_without_replacing_existing_file
    Dir.mktmpdir do |dir|
      localized = File.join(dir, 'values-fr', 'strings.xml')
      FileUtils.mkdir_p(File.dirname(localized))
      File.write(localized, <<~XML)
        <?xml version="1.0" encoding="UTF-8"?>
        <resources>
            <string name="app_name">Woo humain</string>
        </resources>
      XML
      before = File.read(localized)

      writer = lambda do |path, _units, _locale|
        FileUtils.mkdir_p(File.dirname(path))
        File.write(path, '<resources><string name="broken">unterminated')
      end

      report = nil
      WooAiTranslation::AndroidResources::Writer.stub(:write, writer) do
        report = build(dir, client: StubClient.new).run_strings(locales: %w[fr]).first
      end

      assert(report.gate_errors.any? { |e| e.include?('not well-formed') })
      assert_equal before, File.read(localized), 'bad temp output must not replace existing translations'
    end
  end

  def test_source_text_change_invalidates_only_the_changed_key
    # The single auto-invalidation rule: an English source text change for
    # ONE key re-translates exactly that one key, leaves the other 8 untouched.
    # This is also covered by the big idempotency test; pinned here as the
    # canonical positive case for the contract.
    Dir.mktmpdir do |dir|
      build(dir, client: StubClient.new).run_strings(locales: %w[fr])

      modified = File.join(dir, 'modified.xml')
      File.write(modified, File.read(FIXTURE).sub('Hello %1$s, you have %2$d items',
                                                  'Hi %1$s, %2$d items await'))

      c2 = StubClient.new
      r2 = build(dir, source: modified, client: c2).run_strings(locales: %w[fr]).first
      assert_equal 1, r2.translated
      assert_equal 8, r2.reused
      assert_equal 1, c2.calls
    end
  end
end

class CldrPluralsTest < Minitest::Test
  CP = WooAiTranslation::CldrPlurals

  def test_loads_data_and_drops_comment_keys
    data = CP.load_data
    assert data.key?('pl')
    refute data.key?('_comment'), 'underscore-prefixed keys are dropped'
  end

  def test_quantities_for_returns_full_cldr_list_for_polish
    assert_equal %w[one few many other], CP.quantities_for('pl')
  end

  def test_quantities_for_portuguese_portugal_includes_many
    assert_equal %w[one many other], CP.quantities_for('pt-rPT')
  end

  def test_quantities_for_thai_is_only_other
    assert_equal %w[other], CP.quantities_for('th')
  end

  def test_quantities_for_unknown_locale_is_nil
    assert_nil CP.quantities_for('xx-rZZ')
  end

  def test_prompt_line_mentions_required_categories
    line = CP.prompt_line_for('pl')
    assert_includes line, 'pl'
    %w[one few many other].each { |q| assert_includes line, q }
  end

  def test_prompt_line_for_unknown_locale_is_empty
    assert_equal '', CP.prompt_line_for('xx-rZZ')
  end
end

class TextNormalizerTest < Minitest::Test
  TN = WooAiTranslation::TextNormalizer

  def test_three_dot_run_becomes_unicode_ellipsis
    assert_equal 'Loading…', TN.normalize('Loading...')
  end

  def test_does_not_touch_already_unicode_ellipsis
    assert_equal 'Loading…', TN.normalize('Loading…')
  end

  def test_four_or_more_dots_are_left_alone_conservative
    assert_equal 'Wait.....', TN.normalize('Wait.....')
  end

  def test_numeric_range_dash_becomes_endash
    assert_equal '8–20%', TN.normalize('8-20%')
  end

  def test_time_range_with_spaces_becomes_endash
    assert_equal '12:00–13:00', TN.normalize('12:00 - 13:00')
  end

  def test_compound_word_hyphen_is_preserved
    assert_equal 'plug-in', TN.normalize('plug-in')
    assert_equal 'step-by-step', TN.normalize('step-by-step')
  end

  def test_prose_dash_between_words_is_preserved
    assert_equal 'Open - close cycle', TN.normalize('Open - close cycle')
  end

  def test_placeholders_themselves_are_preserved_verbatim
    # `...` between placeholders still normalizes to `…`; only the placeholder
    # spans themselves (%1$d, %s) are untouched.
    assert_equal '%1$d… %s', TN.normalize('%1$d... %s')
    # `%d-%d` is two placeholders glued together; the dash sits inside the
    # protected region and is left alone.
    assert_equal '%d-%d', TN.normalize('%d-%d')
  end

  def test_escape_sequences_are_preserved
    assert_equal "line 1\\nline 2", TN.normalize("line 1\\nline 2")
  end

  def test_handles_empty_and_nil
    assert_equal '', TN.normalize('')
    assert_nil TN.normalize(nil)
  end
end

class PluralCoverageValidatorTest < Minitest::Test
  V = WooAiTranslation::Validators

  def test_passes_when_required_categories_all_present
    errs = V.plural_form_coverage(
      output_plurals_quantities: { 'items' => %w[one few many other] },
      required: %w[one few many other]
    )
    assert_empty errs
  end

  def test_flags_missing_categories
    errs = V.plural_form_coverage(
      output_plurals_quantities: { 'items' => %w[one other] },
      required: %w[one few many other]
    )
    assert_equal 1, errs.size
    assert_includes errs.first, 'missing CLDR quantities'
    assert_includes errs.first, 'few'
    assert_includes errs.first, 'many'
  end

  def test_flags_irrelevant_categories
    errs = V.plural_form_coverage(
      output_plurals_quantities: { 'items' => %w[one other] },
      required: %w[other]
    )
    assert_equal 1, errs.size
    assert_includes errs.first, 'irrelevant'
    assert_includes errs.first, 'one'
  end

  def test_skips_gate_when_required_is_nil_or_empty
    assert_empty V.plural_form_coverage(output_plurals_quantities: { 'k' => %w[other] }, required: nil)
    assert_empty V.plural_form_coverage(output_plurals_quantities: { 'k' => %w[other] }, required: [])
  end
end

class NonTranslatableStringReferenceValidatorTest < Minitest::Test
  P = WooAiTranslation::AndroidResources::Parser
  V = WooAiTranslation::Validators

  def test_non_translatable_string_references_must_exist_in_source
    source_doc = P.parse(<<~XML)
      <resources>
          <string name="date_timeframe_last_quarter">Last Quarter</string>
          <string-array name="timeframe_titles" translatable="false">
              <item>@string/date_timeframe_last_quarter</item>
          </string-array>
      </resources>
    XML
    output_doc = P.parse(<<~XML)
      <resources>
          <string-array name="timeframe_titles" translatable="false">
              <item>@string/date_timeframe_quarter</item>
          </string-array>
      </resources>
    XML

    errors = V.non_translatable_string_reference_integrity(
      source_names: source_doc.units.map(&:name),
      output_units: output_doc.units
    )

    assert_equal [
      'non-translatable timeframe_titles references missing source string: @string/date_timeframe_quarter'
    ], errors
  end
end

class AttributePreservationTest < Minitest::Test
  P = WooAiTranslation::AndroidResources::Parser
  W = WooAiTranslation::AndroidResources::Writer

  def test_tools_namespace_prefix_survives_parse_and_write
    xml = <<~XML
      <resources xmlns:tools="http://schemas.android.com/tools">
        <string name="copy" tools:override="true">Copy</string>
      </resources>
    XML
    doc = P.parse(xml)
    copy_unit = doc.find('copy')
    assert_equal 'true', copy_unit.attributes['tools:override'],
                 'namespace-prefixed attribute name must survive parse intact'

    copy_unit.entries.first[:value] = 'Kopia'
    Dir.mktmpdir do |dir|
      out = File.join(dir, 'values-xx', 'strings.xml')
      W.write(out, [copy_unit], 'xx')
      raw = File.read(out)
      assert_includes raw, 'tools:override="true"',
                      'Writer must emit the namespace prefix'
      refute_match(/(?<!tools:)override="true"/, raw,
                   'Writer must not emit bare override="true"')
    end
  end
end

class TranslatorRecoverableErrorsTest < Minitest::Test
  # Persistent client failures must NOT crash the run; they fall through the
  # split-retry loop and a single bad key is left untranslated (Android falls
  # back to default at runtime). This locks the "required from day one"
  # behavior: a CLI / API hiccup mid-batch never blocks PRs.

  def make_translator(error_class:, message: 'boom')
    stub = Class.new do
      define_method(:available?) { true }
      define_method(:complete) do |model:, system_blocks:, user_content:, max_tokens: 8192|
        raise error_class, message
      end
    end.new
    WooAiTranslation::Translator.new(client: stub)
  end

  def items(n)
    (1..n).map { |i| { id: "k#{i}", source: "s#{i}", context: "" } }
  end

  def test_persistent_claude_cli_error_returns_empty_does_not_raise
    t = make_translator(error_class: WooAiTranslation::ClaudeCliClient::Error)
    result = nil
    assert_silent { result = t.translate(locale: 'pl', items: items(8), model: 'sonnet') }
    assert_equal({}, result)
  end

  def test_persistent_anthropic_error_returns_empty_does_not_raise
    t = make_translator(error_class: WooAiTranslation::AnthropicClient::Error)
    result = t.translate(locale: 'pl', items: items(4), model: 'sonnet')
    assert_equal({}, result)
  end

  def test_persistent_parser_error_returns_empty_does_not_raise
    # Malformed JSON (key with no value, then another key) is what Sonnet
    # occasionally emits in long payloads.
    bad_json = '{"k1":"good","k2","k3":"also good"}'
    stub = Class.new do
      define_method(:available?) { true }
      define_method(:complete) do |model:, system_blocks:, user_content:, max_tokens: 8192|
        bad_json
      end
    end.new
    t = WooAiTranslation::Translator.new(client: stub)
    result = t.translate(locale: 'pl', items: items(8), model: 'sonnet')
    assert_equal({}, result)
  end

  def test_non_string_json_values_return_empty_do_not_raise
    bad_json = '{"k1":["not a string"],"k2":{"text":"also wrong"}}'
    stub = Class.new do
      define_method(:available?) { true }
      define_method(:complete) do |model:, system_blocks:, user_content:, max_tokens: 8192|
        bad_json
      end
    end.new
    t = WooAiTranslation::Translator.new(client: stub)
    result = t.translate(locale: 'pl', items: items(2), model: 'sonnet')
    assert_equal({}, result)
  end

  def test_unknown_error_class_still_propagates
    # NoMethodError, SystemCallError, etc. are NOT in the rescue list because
    # they indicate engine bugs we want to surface, not transient failures.
    stub = Class.new do
      define_method(:available?) { true }
      define_method(:complete) do |model:, system_blocks:, user_content:, max_tokens: 8192|
        raise NoMethodError, 'this is a real bug'
      end
    end.new
    t = WooAiTranslation::Translator.new(client: stub)
    assert_raises(NoMethodError) do
      t.translate(locale: 'pl', items: items(2), model: 'sonnet')
    end
  end
end

class AnthropicClientTransportErrorsTest < Minitest::Test
  def test_transport_errors_are_wrapped_after_retries
    http = Class.new do
      attr_reader :calls

      def initialize
        @calls = 0
      end

      def request(_req)
        @calls += 1
        raise Errno::ECONNRESET, 'connection reset'
      end
    end.new

    client = WooAiTranslation::AnthropicClient.new(api_key: 'dummy', http: http)
    client.define_singleton_method(:backoff_seconds) { |_attempt| 0 }
    client.define_singleton_method(:sleep) { |_seconds| nil }

    error = assert_raises(WooAiTranslation::AnthropicClient::Error) do
      client.complete(model: 'test-model', system_blocks: ['rules'], user_content: '[]')
    end

    assert_equal WooAiTranslation::AnthropicClient::MAX_RETRIES + 1, http.calls
    assert_includes error.message, 'ECONNRESET'
  end
end

class ManifestInvalidationTest < Minitest::Test
  M = WooAiTranslation::Manifest

  def test_source_change_invalidation_is_per_locale_for_legacy_key_level_manifests
    m = M.new(
      {
        'keys' => {
          'app_name' => {
            'source_sha' => 'old-sha',
            'locales' => {
              'pl' => { 'origin' => 'ai' },
              'cs' => { 'origin' => 'ai' }
            }
          }
        },
        'metadata' => {}
      }
    )

    assert m.stale?(name: 'app_name', locale: 'pl', expected_source_sha: 'new-sha')
    m.record(name: 'app_name', locale: 'pl', model: 'test-model', origin: 'ai', source_sha: 'new-sha')

    refute m.stale?(name: 'app_name', locale: 'pl', expected_source_sha: 'new-sha')
    assert m.stale?(name: 'app_name', locale: 'cs', expected_source_sha: 'new-sha')
  end

  def test_invalidate_clears_recorded_locales_for_a_key
    m = M.new
    m.record(name: 'app_name', locale: 'pl', model: 'test-model', origin: 'ai', source_sha: 'sha')
    m.record(name: 'app_name', locale: 'fr', model: 'test-model', origin: 'ai', source_sha: 'sha')

    cleared = m.invalidate(name: 'app_name', locales: %w[pl])
    assert_equal 1, cleared
    assert m.stale?(name: 'app_name', locale: 'pl', expected_source_sha: 'sha'), 'pl is stale after invalidation'
    refute m.stale?(name: 'app_name', locale: 'fr', expected_source_sha: 'sha'), 'fr is unaffected'
  end

  def test_invalidate_on_unknown_key_is_noop
    m = M.new
    assert_equal 0, m.invalidate(name: 'never_recorded', locales: %w[pl])
  end

  def test_known_keys_reports_recorded_names
    m = M.new
    m.record(name: 'a', locale: 'pl', model: 'test-model', origin: 'ai', source_sha: 'sha')
    m.record(name: 'b', locale: 'pl', model: 'test-model', origin: 'ai', source_sha: 'sha')
    assert_equal %w[a b].sort, m.known_keys.sort
  end
end

class CliStrictModeTest < Minitest::Test
  Report = WooAiTranslation::Engine::Report

  def test_prtime_strict_allows_recoverable_translation_failures
    reports = [
      Report.new(locale: 'pl', translated: 0, reused: 0, written: 0, failed: ['app_name'], gate_errors: [])
    ]

    assert_equal 0, WooAiTranslation::CLI.strict_exit_code(reports, mode: 'prtime', strict: true)
  end

  def test_prtime_strict_blocks_gate_errors
    reports = [
      Report.new(locale: 'pl', translated: 0, reused: 0, written: 0, failed: [], gate_errors: ['missing keys'])
    ]

    assert_equal 1, WooAiTranslation::CLI.strict_exit_code(reports, mode: 'prtime', strict: true)
  end

  def test_non_prtime_strict_still_blocks_recoverable_translation_failures
    reports = [
      Report.new(locale: 'pl', translated: 0, reused: 0, written: 0, failed: ['app_name'], gate_errors: [])
    ]

    assert_equal 1, WooAiTranslation::CLI.strict_exit_code(reports, mode: 'sweep', strict: true)
  end
end

class CliShimTest < Minitest::Test
  # The .gitignore at repo root has a blanket `bin/` rule, so the executable
  # entry point must be force-added. These tests catch a regression where the
  # shim is on disk locally but not committed, or loses its exec bit.
  SHIM = File.expand_path('../bin/woo-ai-translate', __dir__)

  def test_shim_is_present_and_executable
    assert File.exist?(SHIM), "shim missing at #{SHIM} (was it force-added?)"
    assert File.executable?(SHIM), "shim not executable at #{SHIM}"
  end

  def test_shim_boots_the_cli
    out = IO.popen([SHIM, '--help'], err: %i[child out], &:read)
    assert_equal 0, $CHILD_STATUS.exitstatus, "shim exited non-zero: #{out}"
    assert_includes out, 'Usage: woo-ai-translate'
  end
end

class BaselineReaderTest < Minitest::Test
  BR = WooAiTranslation::AndroidResources::BaselineReader

  def write_baseline(dir, locale, body)
    path = File.join(dir, "values-#{locale}", 'strings.xml')
    FileUtils.mkdir_p(File.dirname(path))
    File.write(path, body)
    path
  end

  def test_captures_single_line_string_with_attributes_verbatim
    Dir.mktmpdir do |dir|
      raw = <<~XML
        <?xml version="1.0" encoding="UTF-8"?>
        <resources xmlns:tools="http://schemas.android.com/tools">
            <string name="app_name">Mein Laden</string>
            <string name="raw_percent" formatted="false">50 % rabatt</string>
        </resources>
      XML
      path = write_baseline(dir, 'de', raw)
      blocks = BR.read(path)
      assert_equal 2, blocks.size
      assert_equal %(    <string name="app_name">Mein Laden</string>\n), blocks['app_name']
      assert_equal %(    <string name="raw_percent" formatted="false">50 % rabatt</string>\n), blocks['raw_percent']
    end
  end

  def test_captures_multi_line_plurals_and_arrays_verbatim
    Dir.mktmpdir do |dir|
      raw = <<~XML
        <?xml version="1.0" encoding="UTF-8"?>
        <resources xmlns:tools="http://schemas.android.com/tools">
            <plurals name="cart_items">
                <item quantity="one">1 Artikel</item>
                <item quantity="other">%d Artikel</item>
            </plurals>
            <string-array name="weekdays">
                <item>Montag</item>
                <item>Dienstag</item>
            </string-array>
        </resources>
      XML
      path = write_baseline(dir, 'de', raw)
      blocks = BR.read(path)
      assert_includes blocks['cart_items'], '<plurals name="cart_items">'
      assert_includes blocks['cart_items'], '<item quantity="one">1 Artikel</item>'
      assert_includes blocks['cart_items'], '</plurals>'
      assert_includes blocks['weekdays'], '<item>Montag</item>'
      assert_includes blocks['weekdays'], '</string-array>'
    end
  end

  def test_returns_empty_for_missing_file
    assert_equal({}, BR.read('/tmp/does-not-exist-baseline-strings.xml'))
    assert_equal({}, BR.read(nil))
  end

  def test_ignores_nested_item_lines_when_matching_top_level_units
    # An <item> child at 8-space indent must not be mistaken for a unit start.
    Dir.mktmpdir do |dir|
      raw = <<~XML
        <?xml version="1.0" encoding="UTF-8"?>
        <resources>
            <string-array name="picker">
                <item>Eins</item>
                <item>Zwei</item>
            </string-array>
        </resources>
      XML
      blocks = BR.read(write_baseline(dir, 'de', raw))
      assert_equal %w[picker], blocks.keys
    end
  end
end

class PreservedLineWriterTest < Minitest::Test
  P = WooAiTranslation::AndroidResources::Parser
  W = WooAiTranslation::AndroidResources::Writer

  def test_writer_emits_preserved_xml_verbatim_when_set
    Dir.mktmpdir do |dir|
      doc = P.parse_file(FIXTURE)
      doc.translatable_units.each { |u| u.entries.each { |e| e[:value] = e[:source] } }
      # Pick one unit and pin its preserved_xml to something we can recognize
      # in the output (a human-edited line with idiosyncratic spacing the
      # render_unit path would never produce).
      target = doc.find('app_name')
      target.preserved_xml = %(    <string  name="app_name"   formatted="false"  >Verbatim&#x20;Wert</string>\n)
      out = File.join(dir, 'values-zz', 'strings.xml')
      W.write(out, doc.translatable_units, 'zz')
      raw = File.read(out)
      assert_includes raw, %(    <string  name="app_name"   formatted="false"  >Verbatim&#x20;Wert</string>)
      refute_includes raw, '<string name="app_name">' # the render_unit form
    end
  end

  def test_writer_falls_back_to_render_unit_when_preserved_xml_is_nil
    Dir.mktmpdir do |dir|
      doc = P.parse_file(FIXTURE)
      doc.translatable_units.each { |u| u.entries.each { |e| e[:value] = e[:source] } }
      assert doc.translatable_units.all? { |u| u.preserved_xml.nil? }
      out = File.join(dir, 'values-zz', 'strings.xml')
      W.write(out, doc.translatable_units, 'zz')
      raw = File.read(out)
      # render_unit produces the canonical single-space form.
      assert_includes raw, '<string name="app_name"'
    end
  end
end

class EnginePreservedLineIntegrationTest < Minitest::Test
  include WooAiTranslation

  # End-to-end: import the baseline as glotpress-import, then run an engine
  # sweep that should reuse those keys AND emit them from the sidecar
  # baseline (byte-for-byte). Proves the writer doesn't re-serialize through
  # render_unit when origin=glotpress-import is preserved across runs.
  def test_glotpress_import_keys_emit_from_sidecar_after_sweep
    Dir.mktmpdir do |root|
      source = File.join(root, 'values', 'strings.xml')
      FileUtils.mkdir_p(File.dirname(source))
      File.write(source, <<~XML)
        <?xml version="1.0" encoding="UTF-8"?>
        <resources xmlns:tools="http://schemas.android.com/tools">
            <string name="app_name">My Store</string>
            <string name="raw_percent" formatted="false">50% off</string>
        </resources>
      XML
      # Idiosyncratic spacing the engine's render_unit would never emit.
      human_line = %(    <string name="app_name"   >Mein   Laden</string>)
      existing = File.join(root, 'values-de', 'strings.xml')
      FileUtils.mkdir_p(File.dirname(existing))
      File.write(existing, <<~XML)
        <?xml version="1.0" encoding="UTF-8"?>
        <resources xmlns:tools="http://schemas.android.com/tools">
        #{human_line}
            <string name="raw_percent" formatted="false">50% rabatt</string>
        </resources>
      XML
      # Sidecar is the byte-for-byte snapshot the writer should emit from.
      baseline_path = File.join(root, 'baseline', 'values-de', 'strings.xml')
      FileUtils.mkdir_p(File.dirname(baseline_path))
      FileUtils.cp(existing, baseline_path)

      manifest = Manifest.new
      context = ContextProvider.new({})

      Importer.new(source_path: source, res_dir: root, manifest: manifest, context: context).import(locales: %w[de])

      Engine.new(
        source_path: source, res_dir: root,
        manifest: manifest, manifest_path: File.join(root, 'm.json'),
        translator: Translator.new(client: StubClient.new),
        context: context, baseline_dir: File.join(root, 'baseline')
      ).run_strings(locales: %w[de])

      out = File.read(existing)
      assert_includes out, human_line,
                      'expected the idiosyncratic baseline spacing to survive the writer'
      # Verifying the negative is what proves the preserved-line path actually
      # fired -- render_unit would have collapsed the spacing.
      refute_includes out, '<string name="app_name">Mein   Laden</string>'
    end
  end

  def test_ai_origin_keys_still_route_through_render_unit
    Dir.mktmpdir do |root|
      source = File.join(root, 'values', 'strings.xml')
      FileUtils.mkdir_p(File.dirname(source))
      File.write(source, <<~XML)
        <?xml version="1.0" encoding="UTF-8"?>
        <resources>
            <string name="newly_added">Brand new</string>
        </resources>
      XML
      # Sidecar is empty for this locale; nothing for the writer to preserve.
      manifest = Manifest.new
      Engine.new(
        source_path: source, res_dir: root,
        manifest: manifest, manifest_path: File.join(root, 'm.json'),
        translator: Translator.new(client: StubClient.new),
        context: ContextProvider.new({}),
        baseline_dir: File.join(root, 'baseline')
      ).run_strings(locales: %w[de])

      out = File.read(File.join(root, 'values-de', 'strings.xml'))
      assert_includes out, '<string name="newly_added">[de] Brand new</string>'
      assert_equal 'ai', manifest.origin(name: 'newly_added', locale: 'de')
    end
  end
end
