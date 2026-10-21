#!/bin/bash
# Configure a Jurassic Ninja site as the Maestro lab store.
#
# Connects Jetpack to your own WordPress.com test account, creates WooCommerce
# REST API keys, and writes .maestro/.env.local. Everything site-side runs over
# SSH + wp-cli; only the two WordPress.com calls go over HTTP.
#
# Provision the site first. Either use the /setup-test-stores skill, which does
# this and then calls this script, or create one yourself at
# https://jurassic.ninja/create/?woocommerce&woocommerce-import-sample-data and
# take the admin password from the wp-admin notice the site displays.
#
# With --no-jetpack-site instead of --site, it configures a WooCommerce site
# without Jetpack for the push setup flow: it only creates the API keys and
# writes the MAESTRO_WOO_NO_JETPACK_* values.
#
# Usage:
#   setup-jn-store.sh --site <domain> [--site-password <password>] [--wpcom-user <email>]
#                     [--admin-user <name>] [--admin-id <id>] [--env-out <path>] [--app-creds <path>]
#   setup-jn-store.sh --no-jetpack-site <domain> [--site-password <password>]
#                     [--admin-user <name>] [--admin-id <id>] [--env-out <path>]
#
# Credentials are resolved in this order, so a second run needs no arguments
# beyond the site:
#   1. command-line flags
#   2. existing values in .maestro/.env.local (the site password only when
#      the site is the store already written there)
#   3. an interactive prompt (never echoed, never in shell history)
#
# The WordPress.com account must not have two-factor authentication enabled:
# the OAuth password grant cannot answer a challenge non-interactively.

set -uo pipefail

SITE="" NO_JETPACK_SITE="" SITE_PASS="" WPCOM_USER="" WPCOM_PASS=""
ADMIN_USER="demo"
ADMIN_ID="1"
ENV_OUT=""
APP_CREDS="$HOME/.configure/woocommerce-android/secrets/secrets.properties"

die() { printf '\033[31merror:\033[0m %s\n' "$*" >&2; exit 1; }
STEP=0
step() { STEP=$(( STEP + 1 )); printf '\n\033[1m[%s/%s]\033[0m %s\n' "$STEP" "$TOTAL_STEPS" "$1"; }
ok() { printf '  \033[32mok\033[0m %s\n' "$*"; }

while [ $# -gt 0 ]; do
  case "$1" in
    --site) SITE="$2"; shift 2 ;;
    --no-jetpack-site) NO_JETPACK_SITE="$2"; shift 2 ;;
    --site-password) SITE_PASS="$2"; shift 2 ;;
    --wpcom-user) WPCOM_USER="$2"; shift 2 ;;
    --admin-user) ADMIN_USER="$2"; shift 2 ;;
    --admin-id) ADMIN_ID="$2"; shift 2 ;;
    --env-out) ENV_OUT="$2"; shift 2 ;;
    --app-creds) APP_CREDS="$2"; shift 2 ;;
    -h|--help) sed -n '2,31p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) die "unknown argument: $1" ;;
  esac
done

if [ -n "$NO_JETPACK_SITE" ]; then
  [ -z "$SITE" ] || die "--site and --no-jetpack-site configure different stores, run the script once for each"
  SITE="$NO_JETPACK_SITE"
  TOTAL_STEPS=3
else
  [ -n "$SITE" ] || die "--site or --no-jetpack-site is required"
  TOTAL_STEPS=7
fi
command -v expect >/dev/null || die "expect is required for password-based SSH"
command -v python3 >/dev/null || die "python3 is required"
[ -n "$NO_JETPACK_SITE" ] || [ -f "$APP_CREDS" ] \
  || die "app credentials not found: $APP_CREDS (see Environment Setup in AGENTS.md)"

if [ -z "$ENV_OUT" ]; then
  REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && git rev-parse --show-toplevel 2>/dev/null)" || REPO_ROOT=""
  ENV_OUT="${REPO_ROOT:-.}/.maestro/.env.local"
fi

# Read one value out of an env file, tolerating quoting and inline comments.
env_value() {
  [ -f "$ENV_OUT" ] || return 0
  ENV_FILE="$ENV_OUT" ENV_KEY="$1" python3 -c '
import os, re, shlex
path, key = os.environ["ENV_FILE"], os.environ["ENV_KEY"]
for line in open(path, encoding="utf-8"):
    m = re.match(r"\s*" + re.escape(key) + r"=(.*)$", line)
    if m:
        raw = m.group(1).strip()
        try:
            parts = shlex.split(raw, comments=True)
        except ValueError:
            parts = [raw]
        print(parts[0] if parts else "")
        break
'
}

# Secrets are prompted rather than accepted as flags: command-line arguments are
# recorded in shell history and are visible to any process that can run `ps`.
prompt_secret() {
  local __var="$1" __label="$2" __value=""
  printf '  %s: ' "$__label" >&2
  read -rs __value
  printf '\n' >&2
  printf -v "$__var" '%s' "$__value"
}

# Resolution order per value: flag, then environment, then .env.local, then an
# interactive prompt.
[ -n "$SITE_PASS" ]   || SITE_PASS="${JN_SSH_PASS:-}"
if [ -n "$NO_JETPACK_SITE" ]; then
  STORED_URL_KEY=MAESTRO_WOO_NO_JETPACK_SITE_URL
  STORED_PASS_KEY=MAESTRO_WOO_NO_JETPACK_SITE_ADMIN_PASSWORD
else
  STORED_URL_KEY=MAESTRO_WOO_LAB_JETPACK_STORE_URL
  STORED_PASS_KEY=MAESTRO_WOO_LAB_JETPACK_SITE_ADMIN_PASSWORD
fi
# The stored password belongs to the stored site, so a new site is asked for its own.
if [ -z "$SITE_PASS" ] && [ "$(env_value "$STORED_URL_KEY")" = "https://$SITE" ]; then
  SITE_PASS="$(env_value "$STORED_PASS_KEY")"
fi
if [ -z "$NO_JETPACK_SITE" ]; then
  [ -n "$WPCOM_USER" ]  || WPCOM_USER="${MAESTRO_WOO_LAB_WPCOM_EMAIL:-}"
  [ -n "$WPCOM_USER" ]  || WPCOM_USER="$(env_value MAESTRO_WOO_LAB_WPCOM_EMAIL)"
  [ -n "$WPCOM_PASS" ]  || WPCOM_PASS="${MAESTRO_WOO_LAB_WPCOM_PASSWORD:-}"
  [ -n "$WPCOM_PASS" ]  || WPCOM_PASS="$(env_value MAESTRO_WOO_LAB_WPCOM_PASSWORD)"
fi

# Prompting needs a terminal. Without one (an agent, a pipeline) the reads would
# take EOF and fail several steps later with a misleading message, so say
# exactly what is missing and how to supply it.
if [ -z "$SITE_PASS" ] || { [ -z "$NO_JETPACK_SITE" ] && { [ -z "$WPCOM_USER" ] || [ -z "$WPCOM_PASS" ]; }; }; then
  if [ ! -t 0 ]; then
    printf 'error: missing credentials and no terminal to prompt on.\n' >&2
    [ -n "$SITE_PASS" ]  || printf '  site admin password: pass --site-password or set JN_SSH_PASS\n' >&2
    [ -n "$NO_JETPACK_SITE" ] || [ -n "$WPCOM_USER" ] || printf '  WordPress.com account: set MAESTRO_WOO_LAB_WPCOM_EMAIL or add it to %s\n' "$(basename "$ENV_OUT")" >&2
    [ -n "$NO_JETPACK_SITE" ] || [ -n "$WPCOM_PASS" ] || printf '  WordPress.com password: set MAESTRO_WOO_LAB_WPCOM_PASSWORD or add it to %s\n' "$(basename "$ENV_OUT")" >&2
    printf 'Or run this script directly in a terminal and it will prompt.\n' >&2
    exit 1
  fi
  printf '\nCredentials not found in %s, enter them now.\n' "$(basename "$ENV_OUT")"
fi

[ -n "$SITE_PASS" ] || prompt_secret SITE_PASS "site admin password for $SITE"
[ -n "$SITE_PASS" ] || die "no site admin password supplied"
if [ -z "$NO_JETPACK_SITE" ]; then
  if [ -z "$WPCOM_USER" ]; then
    printf '  WordPress.com test account email: ' >&2
    read -r WPCOM_USER
  fi
  [ -n "$WPCOM_PASS" ] || prompt_secret WPCOM_PASS "WordPress.com password for $WPCOM_USER"
  [ -n "$WPCOM_USER" ] || die "no WordPress.com account supplied"
  [ -n "$WPCOM_PASS" ] || die "no WordPress.com password supplied"
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
chmod 700 "$WORK"

cat > "$WORK/ssh.exp" <<'EXP'
#!/usr/bin/expect -f
set timeout 120
set site [lindex $argv 0]
set pass $env(JN_SSH_PASS)
set cmd  [lindex $argv 1]
log_user 0
spawn ssh -o StrictHostKeyChecking=accept-new -o PubkeyAuthentication=no \
          -o NumberOfPasswordPrompts=1 $site@sftp.wp.com $cmd
expect {
  -re {[Pp]assword:} { send "$pass\r"; log_user 1; exp_continue }
  eof
}
EXP
chmod +x "$WORK/ssh.exp"

# The password goes through the environment rather than argv so it never
# appears in `ps` output.
remote() { JN_SSH_PASS="$SITE_PASS" "$WORK/ssh.exp" "$SITE" "$1" 2>/dev/null | tr -d '\r'; }

# Run PHP on the site. The source is base64-encoded so quoting survives the
# shell/ssh/wp-cli layers, and staged as a file because `wp eval-file -` does
# not read stdin in the wp-cli build JN ships.
remote_php() {
  local php b64
  php="${1//__ADMIN_ID__/$ADMIN_ID}"
  b64="$(printf '%s' "$php" | base64 | tr -d '\n')"
  remote "echo $b64 | base64 -d > /tmp/.jnsetup.php && wp eval-file /tmp/.jnsetup.php; rm -f /tmp/.jnsetup.php"
}

# WooCommerce exposes no REST endpoint for API keys, so insert the row the same
# way its own admin-ajax handler does.
create_api_keys() {
  local keys verify
  keys="$(remote_php '<?php
$ck = "ck_" . wc_rand_hash();
$cs = "cs_" . wc_rand_hash();
global $wpdb;
$ok = $wpdb->insert( $wpdb->prefix . "woocommerce_api_keys", array(
  "user_id"         => __ADMIN_ID__,
  "description"     => "maestro-smoke",
  "permissions"     => "read_write",
  "consumer_key"    => wc_api_hash( $ck ),
  "consumer_secret" => $cs,
  "truncated_key"   => substr( $ck, -7 ),
), array( "%d", "%s", "%s", "%s", "%s", "%s" ) );
if ( ! $ok ) { echo "ERR:insert failed\n"; exit( 1 ); }
echo "CK:" . $ck . "\n" . "CS:" . $cs . "\n";')"
  CK="$(printf '%s' "$keys" | sed -n 's/^CK:\(.*\)$/\1/p' | head -1)"
  CS="$(printf '%s' "$keys" | sed -n 's/^CS:\(.*\)$/\1/p' | head -1)"
  [ -n "$CK" ] && [ -n "$CS" ] || die "could not create API keys: $(printf '%s' "$keys" | head -3)"

  verify="$(curl -s -o /dev/null -w '%{http_code}' -u "$CK:$CS" "https://$SITE/wp-json/wc/v3/products?per_page=1")"
  [ "$verify" = "200" ] || die "the API keys do not authenticate (http=$verify)"
  ok "keys created and verified as read_write"
}

# Each argument maps an env file name to the shell variable holding its value,
# for example MAESTRO_WOO_LAB_CONSUMER_KEY=CK. Only the names go on the command
# line; the values reach Python through the environment.
write_env() {
  local pair
  if [ -f "$ENV_OUT" ]; then
    BACKUP_DIR="${TMPDIR:-/tmp}"; BACKUP_DIR="${BACKUP_DIR%/}/maestro-env-backups"
    mkdir -p "$BACKUP_DIR"; chmod 700 "$BACKUP_DIR"
    BACKUP="$BACKUP_DIR/$(basename "$ENV_OUT").$(date +%Y%m%d-%H%M%S).$$"
    cp "$ENV_OUT" "$BACKUP" || die "could not back up $ENV_OUT"
    chmod 600 "$BACKUP"
    # Deliberately outside the repository: .gitignore matches ".env.local"
    # only, so a ".env.local.bak" sibling would not be ignored and could be
    # committed with credentials in it.
    ok "backed up the previous env file to $BACKUP"
  fi

  mkdir -p "$(dirname "$ENV_OUT")"
  for pair in "$@"; do export "${pair#*=}"; done
  python3 - "$ENV_OUT" "$@" <<'PY' || die "could not write $ENV_OUT${BACKUP:+; the previous copy is $BACKUP}"
import os, re, sys, pathlib

path = pathlib.Path(sys.argv[1])
values = {}
for pair in sys.argv[2:]:
    name, _, variable = pair.partition("=")
    values[name] = os.environ[variable]

def quote(value):
    if re.search(r"[\s()\"'!$&|;<>`\\#]", value):
        return "'" + value.replace("'", "'\\''") + "'"
    return value

# Rewrite in place so hand-maintained lines and comments survive.
lines = path.read_text(encoding="utf-8").splitlines() if path.exists() else []
seen, out = set(), []
for line in lines:
    match = re.match(r"^([A-Z_]+)=", line)
    if match and match.group(1) in values:
        out.append(f"{match.group(1)}={quote(values[match.group(1)])}")
        seen.add(match.group(1))
    else:
        out.append(line)
missing = [k for k in values if k not in seen]
if missing:
    if out and out[-1].strip():
        out.append("")
    out.append("# Written by .maestro/scripts/setup-jn-store.sh")
    out.extend(f"{k}={quote(values[k])}" for k in missing)
path.write_text("\n".join(out).rstrip() + "\n", encoding="utf-8")
path.chmod(0o600)
PY
  ok "wrote $ENV_OUT (mode 600)"
}

# JN provisions asynchronously and answers HTTP well before its plugins finish
# installing, so poll for the state actually needed rather than for a 200.
step "Waiting for $SITE to finish provisioning"
DEADLINE=$(( $(date +%s) + 900 ))
STATE=""
while :; do
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "https://$SITE/wp-json/" || echo 000)"
  if [ "$code" = "200" ]; then
    STATE="$(remote_php '<?php
echo "WPCLI:ok\n";
echo "WC:" . ( function_exists( "wc_rand_hash" ) ? "active" : "pending" ) . "\n";
echo "JP:" . ( class_exists( "Jetpack" ) ? "active" : "pending" ) . "\n";
echo "USER:" . ( get_user_by( "id", __ADMIN_ID__ ) ? "ok" : "MISSING" ) . "\n";')"
    if ! printf '%s' "$STATE" | grep -q "WPCLI:ok"; then
      ssh_failures=$(( ${ssh_failures:-0} + 1 ))
      [ "$ssh_failures" -lt 3 ] || die "could not run wp-cli over SSH. Check the site admin password."
    fi
    printf '%s' "$STATE" | grep -q "WC:active" \
      && { [ -n "$NO_JETPACK_SITE" ] || printf '%s' "$STATE" | grep -q "JP:active"; } && break
  fi
  if [ "$(date +%s)" -ge "$DEADLINE" ]; then
    printf '  last state: http=%s %s\n' "$code" "$(printf '%s' "$STATE" | tr '\n' ' ')" >&2
    die "site did not finish provisioning within 15 minutes"
  fi
  printf '  still provisioning (http=%s), waiting...\n' "$code"
  sleep 15
done
if [ -n "$NO_JETPACK_SITE" ]; then
  ok "site is up, WooCommerce active"
else
  ok "site is up, WooCommerce and Jetpack active"
fi

step "Checking SSH and admin user"
if ! printf '%s' "$STATE" | grep -q "WPCLI:ok"; then
  printf '  remote output: %s\n' "$(printf '%s' "$STATE" | head -3 | tr '\n' ' ')" >&2
  die "could not run wp-cli over SSH. Check the site admin password."
fi
printf '%s' "$STATE" | grep -q "USER:ok" || die "no user with id $ADMIN_ID (try --admin-id)"
ok "ssh and wp-cli working, admin user id $ADMIN_ID present"

# The push setup flow signs in with the site credentials and creates its order
# with the API keys, so the site needs nothing else.
if [ -n "$NO_JETPACK_SITE" ]; then
  step "Creating WooCommerce API keys and writing $(basename "$ENV_OUT")"
  create_api_keys
  STORE_URL="https://$SITE"
  write_env \
    MAESTRO_WOO_NO_JETPACK_SITE_URL=STORE_URL \
    MAESTRO_WOO_NO_JETPACK_SITE_ADMIN_USERNAME=ADMIN_USER \
    MAESTRO_WOO_NO_JETPACK_SITE_ADMIN_PASSWORD=SITE_PASS \
    MAESTRO_WOO_NO_JETPACK_CONSUMER_KEY=CK \
    MAESTRO_WOO_NO_JETPACK_CONSUMER_SECRET=CS
  printf '\n\033[32mDone.\033[0m No-Jetpack store ready: https://%s\n' "$SITE"
  exit 0
fi

# rest_do_request runs as an authenticated admin inside wp-cli, so no cookie or
# REST nonce is needed for the two site-side Jetpack calls.
step "Registering the site with Jetpack"
REG="$(remote_php '<?php
wp_set_current_user( __ADMIN_ID__ );
$res = rest_do_request( new WP_REST_Request( "POST", "/jetpack/v4/connection/register" ) );
$d = $res->get_data();
if ( $res->is_error() ) { echo "ERR:" . json_encode( $d ) . "\n"; exit( 1 ); }
$q = array();
parse_str( (string) parse_url( $d["authorizeUrl"], PHP_URL_QUERY ), $q );
echo "BLOGID:" . ( isset( $q["client_id"] ) ? $q["client_id"] : "" ) . "\n";')"
BLOG_ID="$(printf '%s' "$REG" | sed -n 's/^BLOGID:\([0-9][0-9]*\).*/\1/p' | head -1)"
[ -n "$BLOG_ID" ] || die "could not register the site: $(printf '%s' "$REG" | head -3)"
ok "registered, blogID=$BLOG_ID"

step "Provisioning the user connection"
PROV="$(remote_php '<?php
wp_set_current_user( __ADMIN_ID__ );
$res = rest_do_request( new WP_REST_Request( "POST", "/jetpack/v4/remote_provision" ) );
$d = $res->get_data();
if ( $res->is_error() ) { echo "ERR:" . json_encode( $d ) . "\n"; exit( 1 ); }
echo "UID:"    . $d["user_id"] . "\n";
echo "SCOPE:"  . $d["scope"]   . "\n";
echo "SECRET:" . $d["secret"]  . "\n";')"
EXT_UID="$(printf '%s' "$PROV" | sed -n 's/^UID:\(.*\)$/\1/p'    | head -1)"
SCOPE="$(printf   '%s' "$PROV" | sed -n 's/^SCOPE:\(.*\)$/\1/p'  | head -1)"
SECRET="$(printf  '%s' "$PROV" | sed -n 's/^SECRET:\(.*\)$/\1/p' | head -1)"
[ -n "$SECRET" ] || die "remote_provision failed: $(printf '%s' "$PROV" | head -3)"
ok "scope and secret obtained (the secret is short-lived)"

# --data-urlencode is required, not stylistic: curl -d sends the body raw, so a
# plus-alias address arrives with the + decoded as a space and the grant fails
# with a misleading "Incorrect username or password".
step "Connecting Jetpack to $WPCOM_USER"
app_cred() {
  python3 -c '
import sys
for line in open(sys.argv[1], encoding="utf-8"):
    key, _, value = line.partition("=")
    if key.strip() == sys.argv[2]:
        print(value.strip())
        break
' "$APP_CREDS" "$1"
}
CID="$(app_cred wc.oauth.app_id)"
CSEC="$(app_cred wc.oauth.app_secret)"
[ -n "$CID" ] && [ -n "$CSEC" ] || die "wc.oauth.app_id or wc.oauth.app_secret is missing from $APP_CREDS"
TOKRESP="$(curl -s -X POST https://public-api.wordpress.com/oauth2/token \
  --data-urlencode "client_id=$CID" --data-urlencode "client_secret=$CSEC" \
  -d grant_type=password \
  --data-urlencode "username=$WPCOM_USER" --data-urlencode "password=$WPCOM_PASS" \
  -d wpcom_supports_2fa=true -d with_auth_types=true)"
TOKEN="$(printf '%s' "$TOKRESP" | python3 -c "import sys,json;print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null)"
if [ -z "$TOKEN" ]; then
  printf '%s\n' "$TOKRESP" | python3 -m json.tool 2>/dev/null | head -6 >&2
  die "could not obtain a WordPress.com token. The account must exist and have 2FA disabled."
fi
# The app falls back to a username login when WordPress.com refuses the email one.
WPCOM_USERNAME="$(curl -s -H "Authorization: Bearer $TOKEN" https://public-api.wordpress.com/rest/v1.1/me \
  | python3 -c "import sys,json;print(json.load(sys.stdin).get('username',''))" 2>/dev/null)"
[ -n "$WPCOM_USERNAME" ] || die "could not read the WordPress.com username for $WPCOM_USER"

curl -s -o /dev/null -X POST "https://public-api.wordpress.com/wpcom/v2/sites/$BLOG_ID/jetpack-remote-connect-user" \
  -H "Authorization: Bearer $TOKEN" \
  --data-urlencode "secret=$SECRET" --data-urlencode "scope=$SCOPE" \
  --data-urlencode "external_user_id=$EXT_UID" --data-urlencode "redirect_uri=https://$SITE"

# That call can return remote_request_timeout and still have succeeded, so
# trust the site's own connection state rather than the response body.
CONN="$(remote_php '<?php
wp_set_current_user( __ADMIN_ID__ );
$d = rest_do_request( new WP_REST_Request( "GET", "/jetpack/v4/connection" ) )->get_data();
echo "USERCONNECTED:" . ( ! empty( $d["isUserConnected"] ) ? "yes" : "no" ) . "\n";
echo "OWNER:" . ( ! empty( $d["hasConnectedOwner"] ) ? "yes" : "no" ) . "\n";')"
printf '%s' "$CONN" | grep -q "USERCONNECTED:yes" || die "the Jetpack user connection did not complete"
ok "isUserConnected=yes, hasConnectedOwner=yes"

# The order list, analytics and order creation flows need orders and a customer,
# and POS needs a coupon. The pagination checks need more orders and products than
# one page of the app's lists holds, so the store is topped up to 100 of each, and a
# re-run against the same store only adds what is missing. Numbered names, oldest
# first, let a flow look for one that only a later page of the list shows.
# The app reads customers and stats from the Analytics tables, which WooCommerce
# fills in a scheduled batch, so import the new rows right away.
step "Adding orders, products, a customer and a coupon"
SEED="$(remote_php '<?php
wp_set_current_user( __ADMIN_ID__ );
$target = 100;
$product_ids = wc_get_products( array( "limit" => -1, "status" => "publish", "return" => "ids" ) );
$added_products = 0;
for ( $i = count( $product_ids ); $i < $target; $i++ ) {
  $product = new WC_Product_Simple();
  $product->set_name( sprintf( "Maestro Product %03d", $i + 1 ) );
  $product->set_regular_price( (string) ( 5 + $i % 20 ) );
  $product->set_status( "publish" );
  $product->save();
  $added_products++;
}
$products = wc_get_products( array( "limit" => 10, "status" => "publish", "type" => "simple" ) );
if ( ! $products ) { echo "ERR:no published simple products\n"; return; }
$email = "maestro.customer@example.com";
$user = get_user_by( "email", $email );
$customer_id = $user ? $user->ID : wc_create_new_customer( $email, "", wp_generate_password(), array( "first_name" => "Maestro", "last_name" => "Customer" ) );
if ( is_wp_error( $customer_id ) ) { echo "ERR:" . $customer_id->get_error_message() . "\n"; return; }
$billing = array( "first_name" => "Maestro", "last_name" => "Customer", "email" => $email );
if ( ! $user ) {
  $customer = new WC_Customer( $customer_id );
  $customer->set_billing_first_name( $billing["first_name"] );
  $customer->set_billing_last_name( $billing["last_name"] );
  $customer->set_billing_email( $email );
  $customer->save();
  \Automattic\WooCommerce\Internal\Admin\Schedulers\CustomersScheduler::import( $customer_id );
}
if ( ! wc_get_coupon_id_by_code( "maestro10" ) ) {
  $coupon = new WC_Coupon();
  $coupon->set_code( "maestro10" );
  $coupon->set_discount_type( "percent" );
  $coupon->set_amount( 10 );
  $coupon->save();
}
$order_ids = wc_get_orders( array( "limit" => -1, "status" => array_keys( wc_get_order_statuses() ), "return" => "ids" ) );
$added_orders = 0;
for ( $i = count( $order_ids ); $i < $target; $i++ ) {
  $order = wc_create_order( array( "customer_id" => $customer_id ) );
  $order->add_product( $products[ $i % count( $products ) ], 1 + $i % 3 );
  $order->set_address( array_merge( $billing, array( "last_name" => sprintf( "Order %03d", $i + 1 ) ) ), "billing" );
  $order->calculate_totals();
  $order->update_status( "completed" );
  \Automattic\WooCommerce\Internal\Admin\Schedulers\OrdersScheduler::import( $order->get_id() );
  $added_orders++;
}
echo "SEEDED:" . $added_orders . " orders, " . $added_products . " products\n";')"
printf '%s' "$SEED" | grep -q "^SEEDED:" || die "could not add the store data: $(printf '%s' "$SEED" | head -3)"
ok "$(printf '%s' "$SEED" | sed -n 's/^SEEDED://p') added; the store has 100 of each, 1 customer and the maestro10 coupon"

step "Creating WooCommerce API keys and writing $(basename "$ENV_OUT")"
create_api_keys

# WordPress core rejects the WooCommerce keys, so cleanup deletes the images the
# flows upload with an application password. A re-run replaces the previous one.
APP_PASSWORD_OUT="$(remote_php '<?php
foreach ( WP_Application_Passwords::get_user_application_passwords( __ADMIN_ID__ ) as $item ) {
  if ( "maestro-smoke" === $item["name"] ) {
    WP_Application_Passwords::delete_application_password( __ADMIN_ID__, $item["uuid"] );
  }
}
$created = WP_Application_Passwords::create_new_application_password( __ADMIN_ID__, array( "name" => "maestro-smoke" ) );
if ( is_wp_error( $created ) ) { echo "ERR:" . $created->get_error_message() . "\n"; exit( 1 ); }
echo "AP:" . $created[0] . "\n";')"
APP_PASSWORD="$(printf '%s' "$APP_PASSWORD_OUT" | sed -n 's/^AP:\(.*\)$/\1/p' | head -1)"
[ -n "$APP_PASSWORD" ] || die "could not create an application password: $(printf '%s' "$APP_PASSWORD_OUT" | head -3)"

VERIFY="$(curl -s -o /dev/null -w '%{http_code}' -u "$ADMIN_USER:$APP_PASSWORD" "https://$SITE/wp-json/wp/v2/users/me")"
[ "$VERIFY" = "200" ] || die "the application password does not authenticate (http=$VERIFY)"
ok "application password created and verified"

STORE_URL="https://$SITE"
write_env \
  MAESTRO_WOO_LAB_JETPACK_STORE_URL=STORE_URL \
  MAESTRO_WOO_LAB_WPCOM_EMAIL=WPCOM_USER \
  MAESTRO_WOO_LAB_WPCOM_PASSWORD=WPCOM_PASS \
  MAESTRO_WOO_LAB_WPCOM_USERNAME=WPCOM_USERNAME \
  MAESTRO_WOO_LAB_JETPACK_SITE_ADMIN_USERNAME=ADMIN_USER \
  MAESTRO_WOO_LAB_JETPACK_SITE_ADMIN_PASSWORD=SITE_PASS \
  MAESTRO_WOO_LAB_CONSUMER_KEY=CK \
  MAESTRO_WOO_LAB_CONSUMER_SECRET=CS \
  MAESTRO_WOO_LAB_APPLICATION_PASSWORD=APP_PASSWORD

printf '\n\033[32mDone.\033[0m Lab store ready: https://%s (blogID %s)\n' "$SITE" "$BLOG_ID"
printf 'Verify with:\n'
printf '  .maestro/scripts/lint-env.py\n'
