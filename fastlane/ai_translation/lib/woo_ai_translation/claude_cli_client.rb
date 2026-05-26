# frozen_string_literal: true

require 'open3'

module WooAiTranslation
  # Adapter that shells out to the local `claude` CLI (Claude Code) instead of
  # calling the Anthropic API directly. Useful when the developer has a Claude
  # Pro/Max account but no separate `ANTHROPIC_API_KEY`. Matches
  # `AnthropicClient`'s interface so it slots in via the CLI `--claude-cli`
  # flag with no other engine changes.
  #
  # Prompt caching is not available through the CLI -- the constant prologue
  # (rules + glossary + per-locale style) is re-sent each call. That is fine on
  # the user's Pro/Max plan (no per-token billing visible) and the experiment
  # post used exactly this path.
  class ClaudeCliClient
    class Error < StandardError; end

    DEFAULT_BIN = 'claude'
    MAX_RETRIES = 3

    def initialize(bin: DEFAULT_BIN, logger: nil)
      @bin = bin
      @logger = logger || ->(_m) {}
    end

    def available?
      !system("command -v #{@bin} > /dev/null 2>&1").nil? &&
        !`command -v #{@bin}`.strip.empty?
    end

    # Same signature as AnthropicClient#complete; ignores max_tokens (the CLI
    # controls that itself) but collapses the system_blocks + user_content into
    # a single prompt fed via stdin.
    def complete(model:, system_blocks:, user_content:, max_tokens: 8192)
      prompt = (system_blocks + ["User request:\n#{user_content}"]).join("\n\n")
      with_retries { call_cli(model, prompt) }
    end

    private

    def call_cli(model, prompt)
      cmd = [@bin, '--print', '--no-session-persistence',
             '--model', model, '--output-format', 'text']
      stdout, stderr, status = Open3.capture3(*cmd, stdin_data: prompt.encode('UTF-8'))
      # The CLI returns UTF-8 text; Open3 hands it back as ASCII-8BIT, which
      # blows up on `.strip` once translations contain non-ASCII characters
      # (every non-English locale).
      stdout.force_encoding('UTF-8')
      stderr.force_encoding('UTF-8')
      unless status.success?
        # On failure, dump the prompt to /tmp for offline reproduction and
        # include BOTH stderr and stdout in the error: the CLI often writes
        # diagnostics to stdout when --output-format=text, and stderr is
        # often empty. Truncate generously so a real error message fits.
        dump = "/tmp/claude-cli-fail-#{Time.now.to_i}-#{$PROCESS_ID}.prompt.txt"
        File.write(dump, prompt) rescue nil
        raise Error, "claude CLI exit #{status.exitstatus} (model=#{model}, prompt=#{prompt.bytesize}B, dumped=#{dump}): " \
                     "stderr=#{stderr.strip[0, 600].inspect} stdout=#{stdout.strip[0, 600].inspect}"
      end

      strip_fences(stdout.strip)
    end

    # The CLI sometimes wraps JSON output in ```json ... ``` fences. The
    # Translator already tolerates this, but pre-stripping helps the diagnostics.
    def strip_fences(text)
      text.sub(/\A```(?:json)?\s*/m, '').sub(/```\s*\z/, '').strip
    end

    def with_retries
      attempt = 0
      begin
        yield
      rescue Error => e
        attempt += 1
        @logger.call("claude-cli retry #{attempt}/#{MAX_RETRIES}: #{e.message[0, 200]}")
        raise if attempt > MAX_RETRIES

        sleep(2**attempt)
        retry
      end
    end
  end
end
