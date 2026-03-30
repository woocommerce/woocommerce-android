#!/usr/bin/env python3
"""
Parse Android strings.xml files to/from JSON.

Modes:
  extract      - Read strings.xml, output JSON array to stdout
  extract-keys - Output only string keys (one per line)
  to-xml       - Read JSON translations, write valid strings.xml to stdout

Usage:
  python3 parse_strings.py extract path/to/strings.xml > output.json
  python3 parse_strings.py extract-keys path/to/strings.xml
  python3 parse_strings.py to-xml translations.json > strings.xml
"""

import json
import re
import sys


def _extract_entries(xml_text: str) -> list[dict]:
    """
    Parse strings.xml content using regex to preserve HTML entities as-is.

    Standard XML parsers decode &lt; to < which we do NOT want.
    """
    entries = []

    # Match <string name="..." ...>...</string> (possibly multiline)
    string_pattern = re.compile(
        r'<string\s+name="([^"]+)"([^>]*)>(.*?)</string>',
        re.DOTALL,
    )

    for m in string_pattern.finditer(xml_text):
        key = m.group(1)
        attrs = m.group(2)
        value = m.group(3)

        # Skip translatable="false"
        if 'translatable="false"' in attrs:
            continue

        # Normalize multiline values: collapse internal whitespace
        value = re.sub(r'\s+', ' ', value).strip()

        entries.append({"key": key, "value": value})

    # Match <plurals name="...">...<item quantity="...">...</item>...</plurals>
    plurals_pattern = re.compile(
        r'<plurals\s+name="([^"]+)"([^>]*)>(.*?)</plurals>',
        re.DOTALL,
    )
    item_pattern = re.compile(
        r'<item\s+quantity="([^"]+)">(.*?)</item>',
        re.DOTALL,
    )

    for m in plurals_pattern.finditer(xml_text):
        key = m.group(1)
        attrs = m.group(2)
        body = m.group(3)

        if 'translatable="false"' in attrs:
            continue

        items = {}
        for item_m in item_pattern.finditer(body):
            quantity = item_m.group(1)
            item_value = re.sub(r'\s+', ' ', item_m.group(2)).strip()
            items[quantity] = item_value

        if items:
            entries.append({"key": key, "type": "plurals", "items": items})

    return entries


def cmd_extract_to_list(xml_path: str) -> list:
    """Extract entries and return as a Python list (for import by other scripts)."""
    with open(xml_path, "r", encoding="utf-8") as f:
        xml_text = f.read()
    return _extract_entries(xml_text)


def cmd_extract(xml_path: str) -> None:
    entries = cmd_extract_to_list(xml_path)
    json.dump(entries, sys.stdout, ensure_ascii=False, indent=2)
    sys.stdout.write("\n")


def cmd_extract_keys(xml_path: str) -> None:
    with open(xml_path, "r", encoding="utf-8") as f:
        xml_text = f.read()
    entries = _extract_entries(xml_text)
    for entry in entries:
        print(entry["key"])


def _escape_xml_value(value: str) -> str:
    """
    Escape a string value for Android strings.xml.

    The value already has HTML entities preserved (e.g. &lt;b&gt;),
    and CDATA sections intact, so we only need to handle apostrophes
    and double quotes that aren't already escaped.
    """
    # Don't touch CDATA sections
    if "<![CDATA[" in value:
        return value

    # Escape apostrophes that aren't already escaped
    value = re.sub(r"(?<!\\)'", r"\\'", value)

    return value


def cmd_to_xml(json_path: str) -> None:
    with open(json_path, "r", encoding="utf-8") as f:
        entries = json.load(f)

    print('<?xml version="1.0" encoding="UTF-8"?>')
    print('<resources xmlns:tools="http://schemas.android.com/tools">')

    for entry in entries:
        if entry.get("type") == "plurals":
            print(f'    <plurals name="{entry["key"]}">')
            for quantity, value in entry["items"].items():
                escaped = _escape_xml_value(value)
                print(f'        <item quantity="{quantity}">{escaped}</item>')
            print("    </plurals>")
        else:
            escaped = _escape_xml_value(entry["value"])
            print(f'    <string name="{entry["key"]}">{escaped}</string>')

    print("</resources>")


def main():
    if len(sys.argv) < 3:
        print(__doc__.strip(), file=sys.stderr)
        sys.exit(1)

    command = sys.argv[1]
    path = sys.argv[2]

    if command == "extract":
        cmd_extract(path)
    elif command == "extract-keys":
        cmd_extract_keys(path)
    elif command == "to-xml":
        cmd_to_xml(path)
    else:
        print(f"Unknown command: {command}", file=sys.stderr)
        print(__doc__.strip(), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
