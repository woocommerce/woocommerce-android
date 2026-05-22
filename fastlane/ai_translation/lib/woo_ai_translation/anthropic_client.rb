# frozen_string_literal: true

require 'net/http'
require 'json'
require 'uri'

module WooAiTranslation
  # Thin Anthropic Messages API client.
  #
  # - Key from ENV (never committed). Prefer the Automattic AI gateway by setting
  #   WOO_AI_TRANSLATION_BASE_URL; otherwise the Anthropic API directly.
  # - System blocks are marked with cache_control so the large constant prefix
  #   (rules + per-locale style + glossary) is prompt-cached across the thousands
  #   of calls; only the per-batch user message varies.
  # - Retries with exponential backoff on timeouts / 429 / 5xx.
  class AnthropicClient
    class Error < StandardError; end

    DEFAULT_BASE_URL = 'https://api.anthropic.com'
    MAX_RETRIES = 5
    TRANSPORT_ERRORS = [
      EOFError,
      Net::OpenTimeout,
      Net::ReadTimeout,
      OpenSSL::SSL::SSLError,
      SocketError,
      SystemCallError
    ].freeze

    def self.from_env
      new(
        api_key: ENV['ANTHROPIC_API_KEY'],
        base_url: ENV['WOO_AI_TRANSLATION_BASE_URL'] || DEFAULT_BASE_URL
      )
    end

    def initialize(api_key:, base_url: DEFAULT_BASE_URL, http: nil)
      @api_key = api_key
      @base_url = base_url
      @http = http
      # Some newer models (e.g. claude-opus-4-7) reject the `temperature`
      # parameter outright (HTTP 400). We send temperature: 0 for determinism
      # where it's accepted, learn which models reject it on the first 400, and
      # omit it for those from then on (per-instance, so a long run pays the
      # 400 at most once per model).
      @no_temperature_models = {}
    end

    def available?
      !@api_key.to_s.empty?
    end

    # system_blocks: array of strings; the last is cache-flagged.
    # Returns the assistant text content (String).
    def complete(model:, system_blocks:, user_content:, max_tokens: 8192)
      raise Error, 'ANTHROPIC_API_KEY is not set' unless available?

      body = {
        model: model,
        max_tokens: max_tokens,
        system: cacheable_system(system_blocks),
        messages: [{ role: 'user', content: user_content }]
      }
      body[:temperature] = 0 unless @no_temperature_models[model]

      begin
        with_retries { post_messages(body) }
      rescue Error => e
        # First time we see a model reject `temperature`: drop it, remember the
        # model, and retry once. Any other error propagates unchanged.
        raise unless body.key?(:temperature) && temperature_rejected?(e)

        @no_temperature_models[model] = true
        body.delete(:temperature)
        with_retries { post_messages(body) }
      end
    end

    private

    def temperature_rejected?(error)
      msg = error.message.to_s
      msg.include?('HTTP 400') && msg.downcase.include?('temperature')
    end

    def cacheable_system(blocks)
      blocks.each_with_index.map do |text, i|
        block = { type: 'text', text: text }
        block[:cache_control] = { type: 'ephemeral' } if i == blocks.length - 1
        block
      end
    end

    def post_messages(body)
      uri = URI.join(@base_url + '/', 'v1/messages')
      req = Net::HTTP::Post.new(uri)
      req['content-type'] = 'application/json'
      req['x-api-key'] = @api_key
      req['anthropic-version'] = ANTHROPIC_VERSION
      req['anthropic-beta'] = 'prompt-caching-2024-07-31'
      req.body = JSON.generate(body)

      res = http_client(uri).request(req)
      raise Error, "HTTP #{res.code}: #{res.body}" unless res.code.to_i.between?(200, 299)

      json = JSON.parse(res.body)
      Array(json['content']).map { |c| c['text'] }.compact.join
    end

    def http_client(uri)
      return @http if @http

      http = Net::HTTP.new(uri.host, uri.port)
      http.use_ssl = uri.scheme == 'https'
      http.open_timeout = 30
      http.read_timeout = 120
      http
    end

    def with_retries
      attempt = 0
      begin
        yield
      rescue Error => e
        attempt += 1
        raise if attempt > MAX_RETRIES
        raise if e.is_a?(Error) && client_error_no_retry?(e)

        sleep(backoff_seconds(attempt))
        retry
      rescue *TRANSPORT_ERRORS => e
        attempt += 1
        raise Error, "#{e.class}: #{e.message}" if attempt > MAX_RETRIES

        sleep(backoff_seconds(attempt))
        retry
      end
    end

    def client_error_no_retry?(error)
      m = error.message[/HTTP (\d+)/, 1]
      return false if m.nil?

      code = m.to_i
      code.between?(400, 499) && code != 429
    end

    def backoff_seconds(attempt)
      (2**attempt) + rand
    end
  end

  # Deterministic offline stand-in used by tests and `--offline` dry runs.
  # Echoes a locale-tagged source so the full pipeline (delta, validation,
  # manifest, writer) is exercised without network or spend.
  class StubClient
    def initialize(&transform)
      @transform = transform || ->(loc, src) { "[#{loc}] #{src}" }
      @calls = 0
    end

    attr_reader :calls

    def available?
      true
    end

    def complete(model:, system_blocks:, user_content:, max_tokens: 8192)
      @calls += 1
      # JSON batched mode (strings) vs raw-text mode (metadata) are told apart by
      # the JSON-mode instruction the Translator emits.
      if user_content.include?('respond with the JSON object only')
        locale = user_content[/locale:\s*([\w-]+)/, 1] || '??'
        payload = JSON.parse(user_content[/\[.*\]/m] || '[]')
        out = payload.to_h { |item| [item['id'], @transform.call(locale, item['source'])] }
        JSON.generate(out)
      else
        locale = user_content[/Locale:\s*([\w-]+)/, 1] || '??'
        source = user_content.split("\n\n", 2).last.to_s
        @transform.call(locale, source)
      end
    end
  end
end
