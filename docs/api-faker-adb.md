# ApiFaker ADB Commands

The ApiFaker module can be controlled via ADB broadcast commands, enabling automated testing scenarios driven by scripts or AI agents.

## Prerequisites

- Debug build installed on the device/emulator
- ADB connected to the device

**Note:** All commands include `-p com.woocommerce.android.dev` to target the debug app package. This is required on Android 8.0+ because implicit broadcasts are not delivered to manifest-registered receivers without a package qualifier.

## Commands

### Enable / Disable

```bash
# Enable
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.SET_STATUS --ez enabled true

# Disable
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.SET_STATUS --ez enabled false
```

Note: ApiFaker can only be enabled when at least one endpoint is configured. If no endpoints exist, the status will remain disabled even after sending `enabled true`.

### Add Endpoint

```bash
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.ADD_ENDPOINT \
  --es api_type "<type>" \
  --es path "<path>" \
  [--es http_method "<method>"] \
  [--es query_params "<params>"] \
  [--es request_body "<body>"] \
  [--es request_body_file "<file_path>"] \
  [--ei response_status_code <code>] \
  [--es response_body "<body>"] \
  [--es response_body_file "<file_path>"]
```

**Required extras:**
| Extra | Type | Description |
|-------|------|-------------|
| `api_type` | string | One of: `wp-api`, `wp-com`, `custom` |
| `path` | string | The API path to match (e.g., `/wc/v3/products`) |

**Optional extras:**
| Extra | Type | Default | Description |
|-------|------|---------|-------------|
| `custom_host` | string | - | Required when `api_type` is `custom`. The host to match (e.g., `api.stripe.com`) |
| `http_method` | string | any | HTTP method: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`, `HEAD`, `TRACE`, `CONNECT` |
| `query_params` | string | - | Comma-separated `key=value` pairs (e.g., `per_page=10,status=publish`). Note: values cannot contain commas |
| `request_body` | string | - | Request body pattern to match |
| `request_body_file` | string | - | Path to file containing request body pattern (takes priority over `request_body`) |
| `response_status_code` | int | 200 | HTTP status code for the mocked response |
| `response_body` | string | - | JSON body for the mocked response |
| `response_body_file` | string | - | Path to file containing response body (takes priority over `response_body`) |

#### API Types

**WP-API** (`wp-api`): WordPress REST API endpoints accessed via `/wp-json`.
```bash
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.ADD_ENDPOINT \
  --es api_type "wp-api" \
  --es path "/wc/v3/products" \
  --es http_method "GET" \
  --ei response_status_code 200 \
  --es response_body '[{"id":1,"name":"Test Product"}]'
```

**WP.COM** (`wp-com`): WordPress.com Public API endpoints on `public-api.wordpress.com`.
```bash
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.ADD_ENDPOINT \
  --es api_type "wp-com" \
  --es path "/rest/v1.1/me" \
  --es http_method "GET" \
  --ei response_status_code 200 \
  --es response_body '{"ID":123,"display_name":"Test User"}'
```

**Custom** (`custom`): Third-party API endpoints with an arbitrary host.
```bash
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.ADD_ENDPOINT \
  --es api_type "custom" \
  --es custom_host "api.stripe.com" \
  --es path "/v1/tokens" \
  --es http_method "POST" \
  --ei response_status_code 200 \
  --es response_body '{"id":"tok_test"}'
```

### Edit Endpoint

Partially updates an existing endpoint. Only the extras you provide will be changed; all other fields are preserved.

```bash
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.EDIT_ENDPOINT \
  --el endpoint_id <id> \
  [--es path "<new_path>"] \
  [--ei response_status_code <new_code>] \
  [--es response_body "<new_body>"]
```

**Required extras:**
| Extra | Type | Description |
|-------|------|-------------|
| `endpoint_id` | long | The ID of the endpoint to edit (use `LIST_ENDPOINTS` to find IDs) |

All other extras from `ADD_ENDPOINT` are accepted and optional.

### Remove Endpoint

```bash
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.REMOVE_ENDPOINT \
  --el endpoint_id <id>
```

### Clear All Endpoints

```bash
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.CLEAR_ENDPOINTS
```

### List Endpoints

Outputs all configured endpoints to logcat as JSON.

```bash
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.LIST_ENDPOINTS
```

Read the output:
```bash
adb logcat -s WCApiFaker -d
```

The output is bracketed by markers for easy parsing:
```
I/WCApiFaker: ADB: === ENDPOINTS_START (2 endpoints) ===
I/WCApiFaker: ADB: ENDPOINT: {"request":{"id":1,...},"response":{...}}
I/WCApiFaker: ADB: ENDPOINT: {"request":{"id":2,...},"response":{...}}
I/WCApiFaker: ADB: === ENDPOINTS_END ===
```

### Bulk Import

Import multiple endpoints from a JSON file. The file format matches the ApiFaker export format.

```bash
# Push the file to the device
adb push endpoints.json /data/local/tmp/endpoints.json

# Import it
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.IMPORT_ENDPOINTS \
  --es file "/data/local/tmp/endpoints.json"
```

**JSON file format:**
```json
[
  {
    "request": {
      "id": 0,
      "type": {"type": "wp-api"},
      "path": "/wc/v3/products",
      "httpMethod": "GET",
      "queryParameters": [],
      "body": null
    },
    "response": {
      "endpointId": 0,
      "statusCode": 200,
      "body": "[{\"id\":1,\"name\":\"Test Product\"}]"
    }
  }
]
```

The `type` field is a JSON object:
- WP-API: `{"type": "wp-api"}`
- WP.COM: `{"type": "wp-com"}`
- Custom: `{"type": "custom", "host": "api.example.com"}`

## Using Large Response Bodies

For responses larger than a few hundred characters, use the file-based approach to avoid shell escaping issues:

```bash
# Write the response body to a file
cat > /tmp/response.json << 'EOF'
{"products": [{"id": 1, "name": "Product 1"}, {"id": 2, "name": "Product 2"}]}
EOF

# Push to device
adb push /tmp/response.json /data/local/tmp/response.json

# Reference the file in the broadcast
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.ADD_ENDPOINT \
  --es api_type "wp-api" \
  --es path "/wc/v3/products" \
  --es http_method "GET" \
  --ei response_status_code 200 \
  --es response_body_file "/data/local/tmp/response.json"
```

## Typical Workflow

```bash
# 1. Clear any existing endpoints
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.CLEAR_ENDPOINTS

# 2. Add mock endpoints
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.ADD_ENDPOINT \
  --es api_type "wp-api" --es path "/wc/v3/products" --es http_method "GET" \
  --ei response_status_code 200 --es response_body '[]'

# 3. Enable ApiFaker
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.SET_STATUS --ez enabled true

# 4. Run your test scenario...

# 5. Disable and clean up
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.SET_STATUS --ez enabled false
adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.CLEAR_ENDPOINTS
```

## Feedback and Debugging

All actions log their results to logcat under the `WCApiFaker` tag:

```bash
# Watch ApiFaker logs in real-time
adb logcat -s WCApiFaker

# Dump recent logs
adb logcat -s WCApiFaker -d
```

Success messages are logged at INFO level, errors at ERROR level.
