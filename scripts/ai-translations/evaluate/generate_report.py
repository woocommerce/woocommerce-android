#!/usr/bin/env python3
"""
Generate a self-contained HTML report with interactive Plotly charts from metrics JSON files.

Usage:
  python3 generate_report.py \\
    --metrics-dir results/metrics/ \\
    --output results/report.html \\
    [--judge-file results/llm_judge_scores.json]
"""

import argparse
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

import plotly.graph_objects as go
import plotly.io as pio


def load_metrics(metrics_dir: str) -> list[dict]:
    """Load all metrics JSON files from a directory."""
    result = []
    for path in sorted(Path(metrics_dir).glob("*.json")):
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        result.append(data)
    return result


def load_judge(judge_file: str) -> dict | None:
    if not judge_file or not os.path.exists(judge_file):
        return None
    with open(judge_file, "r", encoding="utf-8") as f:
        return json.load(f)


def combo_label(m: dict) -> str:
    return f"{m['strategy']}/{m['llm']}"


def build_summary_table(metrics: list[dict]) -> str:
    """Build an HTML summary table grouped by strategy+LLM."""
    groups: dict[str, list[dict]] = {}
    for m in metrics:
        label = combo_label(m)
        groups.setdefault(label, []).append(m)

    rows_html = ""
    for label, group in sorted(groups.items()):
        bleu = sum(g["aggregates"]["bleu_mean"] for g in group) / len(group)
        chrf = sum(g["aggregates"]["chrf_mean"] for g in group) / len(group)
        lev = sum(g["aggregates"]["levenshtein_sim_mean"] for g in group) / len(group)
        strategy, llm = label.split("/")
        rows_html += (
            f"<tr>"
            f"<td>{strategy}</td>"
            f"<td>{llm}</td>"
            f"<td>{bleu:.2f}</td>"
            f"<td>{chrf:.2f}</td>"
            f"<td>{lev:.4f}</td>"
            f"</tr>\n"
        )

    return f"""
<table>
  <thead>
    <tr>
      <th>Strategy</th><th>LLM</th><th>Avg BLEU</th><th>Avg chrF</th><th>Avg Levenshtein Similarity</th>
    </tr>
  </thead>
  <tbody>
    {rows_html}
  </tbody>
</table>
"""


def build_grouped_bar(metrics: list[dict], metric_key: str, title: str) -> str:
    """Grouped bar chart: languages on X-axis, bars per strategy+LLM combo."""
    languages = sorted({m["language"] for m in metrics})
    combos = sorted({combo_label(m) for m in metrics})

    # Build lookup: (combo, lang) -> value
    lookup: dict[tuple[str, str], float] = {}
    for m in metrics:
        lookup[(combo_label(m), m["language"])] = m["aggregates"][f"{metric_key}_mean"]

    color_palette = ["#4C78A8", "#F58518", "#E45756", "#72B7B2"]
    traces = []
    for i, combo in enumerate(combos):
        values = [lookup.get((combo, lang), 0) for lang in languages]
        traces.append(
            go.Bar(
                name=combo,
                x=languages,
                y=values,
                marker_color=color_palette[i % len(color_palette)],
            )
        )

    fig = go.Figure(data=traces)
    fig.update_layout(
        title=title,
        barmode="group",
        xaxis_title="Language",
        yaxis_title=metric_key.upper(),
        template="plotly_dark",
        legend_title="Strategy/LLM",
    )
    return pio.to_html(fig, full_html=False, include_plotlyjs=False)


def build_heatmap(metrics: list[dict]) -> str:
    """Heatmap: rows=languages, cols=strategy+LLM combos, values=BLEU mean."""
    languages = sorted({m["language"] for m in metrics})
    combos = sorted({combo_label(m) for m in metrics})

    lookup: dict[tuple[str, str], float] = {}
    for m in metrics:
        lookup[(combo_label(m), m["language"])] = m["aggregates"]["bleu_mean"]

    z = [[lookup.get((combo, lang), 0) for combo in combos] for lang in languages]

    fig = go.Figure(
        data=go.Heatmap(
            z=z,
            x=combos,
            y=languages,
            colorscale=[[0, "red"], [0.5, "yellow"], [1, "green"]],
            text=[[f"{v:.1f}" for v in row] for row in z],
            texttemplate="%{text}",
            colorbar=dict(title="BLEU"),
        )
    )
    fig.update_layout(
        title="BLEU Score Heatmap",
        xaxis_title="Strategy/LLM",
        yaxis_title="Language",
        template="plotly_dark",
    )
    return pio.to_html(fig, full_html=False, include_plotlyjs=False)


def build_box_plots(metrics: list[dict]) -> str:
    """Box plots: distribution of per-string BLEU across all languages per combo."""
    combos = sorted({combo_label(m) for m in metrics})

    color_palette = ["#4C78A8", "#F58518", "#E45756", "#72B7B2"]
    traces = []
    for i, combo in enumerate(combos):
        bleu_values = []
        for m in metrics:
            if combo_label(m) == combo:
                bleu_values.extend(s["bleu"] for s in m["per_string"])
        traces.append(
            go.Box(
                name=combo,
                y=bleu_values,
                marker_color=color_palette[i % len(color_palette)],
                boxpoints="outliers",
            )
        )

    fig = go.Figure(data=traces)
    fig.update_layout(
        title="Per-String BLEU Distribution",
        yaxis_title="BLEU Score",
        template="plotly_dark",
        legend_title="Strategy/LLM",
    )
    return pio.to_html(fig, full_html=False, include_plotlyjs=False)


def build_scatter(metrics: list[dict]) -> str:
    """Scatter: English string length vs BLEU score, one trace per strategy (claude only)."""
    strategies = sorted({m["strategy"] for m in metrics})
    color_palette = ["#4C78A8", "#F58518", "#E45756", "#72B7B2"]

    traces = []
    for i, strategy in enumerate(strategies):
        x_vals, y_vals, hover = [], [], []
        for m in metrics:
            if m["strategy"] == strategy and m["llm"] == "claude":
                for s in m["per_string"]:
                    eng = s.get("english_value", "")
                    x_vals.append(len(eng))
                    y_vals.append(s["bleu"])
                    hover.append(s.get("key", ""))
        if not x_vals:
            # fallback: use any LLM if claude not present
            for m in metrics:
                if m["strategy"] == strategy:
                    for s in m["per_string"]:
                        eng = s.get("english_value", "")
                        x_vals.append(len(eng))
                        y_vals.append(s["bleu"])
                        hover.append(s.get("key", ""))
                    break
        if x_vals:
            traces.append(
                go.Scatter(
                    x=x_vals,
                    y=y_vals,
                    mode="markers",
                    name=strategy,
                    text=hover,
                    marker=dict(
                        color=color_palette[i % len(color_palette)],
                        opacity=0.5,
                        size=5,
                    ),
                )
            )

    fig = go.Figure(data=traces)
    fig.update_layout(
        title="English String Length vs BLEU Score (claude)",
        xaxis_title="English String Length (chars)",
        yaxis_title="BLEU Score",
        template="plotly_dark",
        legend_title="Strategy",
    )
    return pio.to_html(fig, full_html=False, include_plotlyjs=False)


def build_radar(metrics: list[dict], judge_data: dict) -> str:
    """Radar chart with LLM judge dimensions per strategy+LLM combo."""
    combos = sorted({combo_label(m) for m in metrics})
    dimensions = ["accuracy_mean", "naturalness_mean", "length_fit_mean"]
    dim_labels = ["Accuracy", "Naturalness", "Length Fit"]

    results = judge_data.get("results", {})

    color_palette = ["#4C78A8", "#F58518", "#E45756", "#72B7B2"]
    traces = []
    for i, combo in enumerate(combos):
        strategy, llm = combo.split("/")
        # Aggregate judge scores across languages for this combo
        scores = {d: [] for d in dimensions}
        for lang_data in results.values():
            agg = lang_data.get("aggregates", {})
            # Try strategy-specific then top-level
            src = agg.get(strategy, agg) if strategy in agg else agg
            for d in dimensions:
                v = src.get(d)
                if v is not None:
                    scores[d].append(v)

        values = [
            sum(scores[d]) / len(scores[d]) if scores[d] else 0
            for d in dimensions
        ]
        # Close the radar
        values_closed = values + [values[0]]
        labels_closed = dim_labels + [dim_labels[0]]

        traces.append(
            go.Scatterpolar(
                r=values_closed,
                theta=labels_closed,
                fill="toself",
                name=combo,
                line=dict(color=color_palette[i % len(color_palette)]),
            )
        )

    fig = go.Figure(data=traces)
    fig.update_layout(
        title="LLM Judge Scores by Strategy/LLM",
        polar=dict(radialaxis=dict(visible=True, range=[0, 5])),
        template="plotly_dark",
        legend_title="Strategy/LLM",
    )
    return pio.to_html(fig, full_html=False, include_plotlyjs=False)


def build_worst_strings_table(metrics: list[dict]) -> str:
    """Bottom 20 strings by BLEU from the best strategy+LLM combo."""
    # Find best combo by average BLEU
    combo_scores: dict[str, list[float]] = {}
    for m in metrics:
        label = combo_label(m)
        combo_scores.setdefault(label, []).append(m["aggregates"]["bleu_mean"])

    best_combo = max(combo_scores, key=lambda c: sum(combo_scores[c]) / len(combo_scores[c]))

    all_strings = []
    for m in metrics:
        if combo_label(m) == best_combo:
            all_strings.extend(m["per_string"])

    worst = sorted(all_strings, key=lambda s: s["bleu"])[:20]

    rows_html = ""
    for s in worst:
        key = s.get("key", "")
        eng = s.get("english_value", "")
        ai_val = s.get("ai_value", "")
        human_val = s.get("human_value", "")
        bleu = s.get("bleu", 0)
        rows_html += (
            f"<tr>"
            f"<td>{key}</td>"
            f"<td>{bleu:.2f}</td>"
            f"<td>{eng}</td>"
            f"<td>{ai_val}</td>"
            f"<td>{human_val}</td>"
            f"</tr>\n"
        )

    return f"""
<p class="section-note">From best combo: <strong>{best_combo}</strong></p>
<table>
  <thead>
    <tr>
      <th>Key</th><th>BLEU</th><th>English</th><th>AI Translation</th><th>Human Reference</th>
    </tr>
  </thead>
  <tbody>
    {rows_html}
  </tbody>
</table>
"""


def section(anchor: str, heading: str, content: str) -> str:
    return f"""
<section id="{anchor}">
  <h2>{heading}</h2>
  {content}
</section>
"""


def generate_report(metrics_dir: str, output_path: str, judge_file: str | None) -> None:
    metrics = load_metrics(metrics_dir)
    if not metrics:
        print(f"No metrics JSON files found in {metrics_dir}", file=sys.stderr)
        sys.exit(1)

    judge_data = load_judge(judge_file) if judge_file else None
    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    nav_items = [
        ("summary", "Summary"),
        ("bleu-bar", "BLEU by Language"),
        ("chrf-bar", "chrF by Language"),
        ("heatmap", "BLEU Heatmap"),
        ("box-plots", "Score Distributions"),
        ("scatter", "Length vs Quality"),
        ("worst", "Worst Strings"),
    ]
    if judge_data:
        nav_items.insert(-1, ("radar", "LLM Judge Radar"))

    nav_html = " | ".join(f'<a href="#{a}">{label}</a>' for a, label in nav_items)

    sections_html = ""
    sections_html += section("summary", "Summary", build_summary_table(metrics))
    sections_html += section("bleu-bar", "BLEU Score by Language", build_grouped_bar(metrics, "bleu", "Average BLEU Score per Language"))
    sections_html += section("chrf-bar", "chrF Score by Language", build_grouped_bar(metrics, "chrf", "Average chrF Score per Language"))
    sections_html += section("heatmap", "BLEU Score Heatmap", build_heatmap(metrics))
    sections_html += section("box-plots", "Per-String BLEU Distributions", build_box_plots(metrics))
    sections_html += section("scatter", "English Length vs BLEU Score", build_scatter(metrics))
    if judge_data:
        sections_html += section("radar", "LLM Judge Radar", build_radar(metrics, judge_data))
    sections_html += section("worst", "Worst Strings (Bottom 20 by BLEU)", build_worst_strings_table(metrics))

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>AI Translation Quality Report</title>
  <script src="https://cdn.plot.ly/plotly-2.27.0.min.js"></script>
  <style>
    *, *::before, *::after {{ box-sizing: border-box; }}
    body {{
      background: #1a1a2e;
      color: #e0e0e0;
      font-family: 'Segoe UI', system-ui, sans-serif;
      margin: 0;
      padding: 0 1rem 4rem;
    }}
    header {{
      padding: 2rem 0 1rem;
      border-bottom: 1px solid #333;
      margin-bottom: 2rem;
    }}
    h1 {{ margin: 0 0 0.25rem; font-size: 1.8rem; color: #fff; }}
    .subtitle {{ color: #888; font-size: 0.9rem; }}
    nav {{
      margin-bottom: 2.5rem;
      font-size: 0.9rem;
      line-height: 2;
    }}
    nav a {{ color: #7eb3ff; text-decoration: none; }}
    nav a:hover {{ text-decoration: underline; }}
    section {{
      margin-bottom: 3rem;
    }}
    h2 {{
      font-size: 1.3rem;
      color: #c9d1d9;
      border-left: 4px solid #4C78A8;
      padding-left: 0.75rem;
      margin-bottom: 1rem;
    }}
    table {{
      border-collapse: collapse;
      width: 100%;
      font-size: 0.875rem;
    }}
    th, td {{
      padding: 0.5rem 0.75rem;
      border: 1px solid #333;
      text-align: left;
      vertical-align: top;
      word-break: break-word;
      max-width: 300px;
    }}
    th {{
      background: #16213e;
      color: #c9d1d9;
      font-weight: 600;
    }}
    tr:nth-child(even) {{ background: #16213e; }}
    tr:hover {{ background: #0f3460; }}
    .section-note {{ color: #888; font-size: 0.85rem; margin-bottom: 0.5rem; }}
  </style>
</head>
<body>
  <header>
    <h1>AI Translation Quality Report</h1>
    <p class="subtitle">Generated: {timestamp}</p>
  </header>
  <nav>{nav_html}</nav>
  {sections_html}
</body>
</html>
"""

    os.makedirs(os.path.dirname(output_path), exist_ok=True) if os.path.dirname(output_path) else None
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(html)

    print(f"Report written to {output_path}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate HTML translation quality report.")
    parser.add_argument("--metrics-dir", required=True, help="Directory containing metrics JSON files")
    parser.add_argument("--output", required=True, help="Output HTML file path")
    parser.add_argument("--judge-file", default=None, help="Optional LLM judge scores JSON file")
    args = parser.parse_args()

    generate_report(
        metrics_dir=args.metrics_dir,
        output_path=args.output,
        judge_file=args.judge_file,
    )


if __name__ == "__main__":
    main()
