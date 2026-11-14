"""
Графіки й таблиці результатів для пояснювальної записки.

Читає найновіші звіти бенчмарку з docs/experiments/results і пише:
  docs/diploma/figures/*.png    — рисунки (300 dpi, для Word)
  docs/experiments/results-tables.md — ті самі числа таблицями

Запуск із кореня репозиторію:  python docs/diploma/tools/make_figures.py
Потрібно: Python 3.10+, matplotlib (pip install matplotlib).
Після нового прогону бенчмарку достатньо перезапустити скрипт: числа в рисунках і таблицях оновляться разом.
"""
from __future__ import annotations

import glob
import json
import os
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import matplotlib.ticker  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
RESULTS = ROOT / "docs" / "experiments" / "results"
OUT_FIG = ROOT / "docs" / "diploma" / "figures"
OUT_MD = ROOT / "docs" / "experiments" / "results-tables.md"

# Колір закріплений за режимом і однаковий на всіх рисунках (перші три слоти перевіреної палітри)
MODE_COLORS = {"lexical": "#2a78d6", "semantic": "#eb6834", "hybrid": "#1baf7a"}
MODE_LABELS = {"lexical": "Лексичний", "semantic": "Семантичний", "hybrid": "Гібридний"}
TYPE_LABELS = {
    "exact": "Точний\nтермін",
    "wordform": "Інша\nсловоформа",
    "paraphrase": "Перефра-\nзування",
    "crosslingual": "Інша\nмова",
    "all": "Усі\nзапити",
}
SOURCE_LABELS = {"author": "Автор", "claude": "Claude", "chatgpt": "ChatGPT", "deepseek": "DeepSeek"}
TEXT = "#1f1f1d"
MUTED = "#52514e"
GRID = "#e4e2dc"

plt.rcParams.update(
    {
        "font.family": "DejaVu Sans",  # має кирилицю на будь-якій системі з matplotlib
        "font.size": 10,
        "axes.edgecolor": GRID,
        "axes.labelcolor": MUTED,
        "axes.titlesize": 11,
        "axes.titleweight": "bold",
        "axes.titlecolor": TEXT,
        "xtick.color": MUTED,
        "ytick.color": MUTED,
        "axes.spines.top": False,
        "axes.spines.right": False,
        "legend.frameon": False,
        "savefig.dpi": 300,
        "savefig.bbox": "tight",
    }
)


def latest(pattern: str) -> Path:
    files = sorted(glob.glob(str(RESULTS / pattern)))
    if not files:
        raise SystemExit(f"Немає файлів {pattern} у {RESULTS}")
    return Path(files[-1])


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def summary(report: dict, mode: str, group: str) -> dict:
    for s in report["summary"]:
        if s["mode"] == mode and s["group"] == group:
            return s
    raise KeyError((mode, group))


def style_axis(ax, ylabel: str, ymax: float | None = 1.0):
    ax.set_ylabel(ylabel)
    if ymax is not None:
        ax.set_ylim(0, ymax)
    ax.grid(axis="y", color=GRID, linewidth=0.8)
    ax.set_axisbelow(True)
    ax.tick_params(axis="x", length=0)


def legend_below(ax):
    # Під віссю: легенда не накладається на стовпці зі значенням 1.0
    ax.legend(ncol=3, loc="upper center", bbox_to_anchor=(0.5, -0.2))


def grouped_bars(ax, groups, series, values, colors, labels, hatches=None, value_fmt="{:.2f}"):
    """values[s][g]; тонкі стовпці з проміжком і підписом значення над кожним."""
    n = len(series)
    width = 0.8 / n
    for i, s in enumerate(series):
        xs = [g + (i - (n - 1) / 2) * width for g in range(len(groups))]
        bars = ax.bar(
            xs,
            values[s],
            width=width * 0.9,
            color=colors[s],
            label=labels[s],
            hatch=(hatches or {}).get(s),
            edgecolor="white",
            linewidth=0.6,
        )
        for b, v in zip(bars, values[s]):
            ax.annotate(
                value_fmt.format(v),
                (b.get_x() + b.get_width() / 2, b.get_height()),
                xytext=(0, 2),
                textcoords="offset points",
                ha="center",
                va="bottom",
                fontsize=7,
                color=MUTED,
            )
    ax.set_xticks(range(len(groups)))


def fig_mrr_by_type(run_and: dict):
    types = ["exact", "wordform", "paraphrase", "crosslingual", "all"]
    modes = ["lexical", "semantic", "hybrid"]
    vals = {m: [summary(run_and, m, "all" if t == "all" else f"type:{t}")["mrr10"] for t in types] for m in modes}
    fig, ax = plt.subplots(figsize=(7.2, 3.6))
    grouped_bars(ax, types, modes, vals, MODE_COLORS, MODE_LABELS)
    ax.set_xticklabels([TYPE_LABELS[t] for t in types])
    style_axis(ax, "MRR@10", 1.12)
    ax.set_title("Якість пошуку за типами запитів (MRR@10)", loc="left")
    legend_below(ax)
    save(fig, "fig-mrr-by-query-type.png")


def fig_hit1_by_source(run_and: dict):
    sources = ["author", "claude", "chatgpt", "deepseek"]
    modes = ["lexical", "semantic", "hybrid"]
    vals = {m: [summary(run_and, m, f"source:{s}")["hit1"] for s in sources] for m in modes}
    fig, ax = plt.subplots(figsize=(7.2, 3.4))
    grouped_bars(ax, sources, modes, vals, MODE_COLORS, MODE_LABELS)
    counts = {s: summary(run_and, "semantic", f"source:{s}")["queries"] for s in sources}
    ax.set_xticklabels([f"{SOURCE_LABELS[s]}\n(n={counts[s]})" for s in sources])
    style_axis(ax, "Hit@1", 1.12)
    ax.set_title("Частка запитів з релевантним записом на 1-му місці, за джерелом запитів", loc="left")
    legend_below(ax)
    save(fig, "fig-hit1-by-query-source.png")


def fig_operator_ablation(run_and: dict, run_or: dict):
    modes = ["lexical", "hybrid"]
    ops = ["and", "or"]
    vals = {
        "and": [summary(run_and, m, "all")["mrr10"] for m in modes],
        "or": [summary(run_or, m, "all")["mrr10"] for m in modes],
    }
    fig, ax = plt.subplots(figsize=(6.4, 3.4))
    n = len(ops)
    width = 0.8 / n
    for i, op in enumerate(ops):
        for g, m in enumerate(modes):
            x = g + (i - (n - 1) / 2) * width
            v = vals[op][g]
            ax.bar(
                x,
                v,
                width=width * 0.9,
                color=MODE_COLORS[m],
                alpha=1.0 if op == "and" else 0.55,
                hatch=None if op == "and" else "///",
                edgecolor="white",
                linewidth=0.6,
            )
            ax.annotate(
                f"{op.upper()}\n{v:.3f}",
                (x, v),
                xytext=(0, 2),
                textcoords="offset points",
                ha="center",
                va="bottom",
                fontsize=7,
                color=MUTED,
            )
    sem = summary(run_and, "semantic", "all")["mrr10"]
    ax.axhline(sem, color=MODE_COLORS["semantic"], linewidth=1.5, linestyle="--")
    ax.annotate(
        f"семантичний пошук: {sem:.3f}",
        (-0.45, sem),
        xytext=(0, 3),
        textcoords="offset points",
        ha="left",
        fontsize=8,
        color=MUTED,
    )
    ax.set_xticks(range(len(modes)))
    ax.set_xticklabels([MODE_LABELS[m] for m in modes])
    style_axis(ax, "MRR@10, усі запити", 1.05)
    ax.set_title("Вплив оператора лексичного запиту (AND / OR)", loc="left")
    save(fig, "fig-lexical-operator-ablation.png")


def fig_latency(run_and: dict):
    modes = ["lexical", "semantic", "hybrid"]
    p50 = [summary(run_and, m, "all")["latencyP50Ms"] for m in modes]
    p95 = [summary(run_and, m, "all")["latencyP95Ms"] for m in modes]
    fig, ax = plt.subplots(figsize=(6.4, 2.6))
    ys = range(len(modes))
    ax.barh(list(ys), p50, height=0.5, color=[MODE_COLORS[m] for m in modes])
    ax.scatter(p95, list(ys), marker="|", s=260, color=TEXT, linewidths=1.6, zorder=3)
    for y, a, b in zip(ys, p50, p95):
        ax.annotate(f"p50 {a:.1f} мс · p95 {b:.1f} мс", (b, y), xytext=(8, -3), textcoords="offset points",
                    fontsize=8, color=MUTED)
    ax.set_xscale("log")
    ax.set_xlim(0.8, 3000)
    ax.set_yticks(list(ys))
    ax.set_yticklabels([MODE_LABELS[m] for m in modes])
    ax.invert_yaxis()
    ax.grid(axis="x", color=GRID, linewidth=0.8)
    ax.set_axisbelow(True)
    ax.set_xlabel("Затримка пошуку, мс (логарифмічна шкала); стовпець — медіана, риска — 95-й перцентиль")
    ax.set_title("Затримка пошуку за режимами", loc="left")
    save(fig, "fig-search-latency.png")


def fig_ask(ask: dict):
    s = ask["summary"]
    ans, una = s["answerable"], s["unanswerable"]
    correct = round(s["correctAnswerRate"] * ans)
    false_ref = ans - correct
    correct_ref = round(s["correctRefusalRate"] * una)
    unwarranted = una - correct_ref
    good, bad = "#1baf7a", "#e34948"
    fig, ax = plt.subplots(figsize=(6.4, 2.4))
    rows = [("Питання, на які\nє відповідь", correct, false_ref, "правильна відповідь", "хибна відмова"),
            ("Питання, на які\nнемає відповіді", correct_ref, unwarranted, "правильна відмова", "відповідь без підстав")]
    for y, (label, ok, wrong, ok_l, wrong_l) in enumerate(rows):
        ax.barh(y, ok, color=good, height=0.5, edgecolor="white")
        ax.barh(y, wrong, left=ok, color=bad, height=0.5, edgecolor="white", hatch="///")
        # Підпис праворуч від стовпця: на вузькому стовпці текст усередині не вміщається
        ax.annotate(f"{ok_l}: {ok} · {wrong_l}: {wrong}", (ok + wrong, y), xytext=(6, 0),
                    textcoords="offset points", va="center", fontsize=8, color=MUTED)
    ax.set_yticks([0, 1])
    ax.set_yticklabels([r[0] for r in rows])
    ax.invert_yaxis()
    ax.set_xlim(0, max(ans, una) * 2.1)
    ax.xaxis.set_major_locator(matplotlib.ticker.MaxNLocator(integer=True))
    ax.set_xlabel("Кількість питань")
    ax.grid(axis="x", color=GRID, linewidth=0.8)
    ax.set_axisbelow(True)
    ax.set_title(
        f"Ask Pastline ({ask['config']['chatModel']}): вигаданих посилань {s['rawFabricatedCitations']} з {s['rawCitations']}",
        loc="left")
    save(fig, "fig-ask-outcomes.png")


def save(fig, name: str):
    OUT_FIG.mkdir(parents=True, exist_ok=True)
    fig.savefig(OUT_FIG / name)
    plt.close(fig)
    print("рисунок:", (OUT_FIG / name).relative_to(ROOT))


def tables(run_and: dict, run_or: dict, ask: dict, files: list[Path]) -> str:
    out = ["# Таблиці результатів",
           "",
           "Згенеровано скриптом `docs/diploma/tools/make_figures.py`; не редагувати вручну.",
           "",
           "Джерела: " + ", ".join(f"`{f.relative_to(ROOT).as_posix()}`" for f in files),
           ""]
    cfg = run_and["config"]
    out += ["## Умови експерименту пошуку", "",
            "| Параметр | Значення |", "|---|---|",
            f"| Модель ембедінгів | {cfg['embeddingModel']} |",
            f"| Корпус | {cfg['corpusEntries']} записів, SHA-256 `{cfg['corpusSha256'][:12]}…` |",
            f"| Запитів | {cfg['queries']} |",
            f"| RRF k | {cfg['rrfK']} |",
            f"| Глибина метрик | top-{cfg['depth']} |",
            f"| Прогрів / повтори | {cfg['warmup']} / {cfg['repeats']} |",
            f"| Java | {cfg['javaVersion']} |", ""]

    def mode_table(run, title):
        rows = [f"## {title}", "",
                "| Режим | Група | n | Hit@1 | Hit@5 | Hit@10 | MRR@10 | Recall@10 | p50, мс | p95, мс |",
                "|---|---|---|---|---|---|---|---|---|---|"]
        for s in run["summary"]:
            rows.append(f"| {s['mode']} | {s['group']} | {s['queries']} | {s['hit1']:.3f} | {s['hit5']:.3f} | "
                        f"{s['hit10']:.3f} | {s['mrr10']:.3f} | {s['recall10']:.3f} | {s['latencyP50Ms']:.1f} | "
                        f"{s['latencyP95Ms']:.1f} |")
        return rows + [""]

    out += mode_table(run_and, "Пошук: лексичний оператор AND (основний прогін)")
    out += mode_table(run_or, "Пошук: лексичний оператор OR (абляція)")
    s = ask["summary"]
    c = ask["config"]
    out += ["## Ask Pastline (H5)", "",
            f"Модель відповідей `{c['chatModel']}`, ембедінги `{c['embeddingModel']}`, top-{c['topK']}, "
            f"temperature {c['temperature']}, {c['questions']} питань.", "",
            "| Метрика | Значення |", "|---|---|",
            f"| Питань з відповіддю / без відповіді | {s['answerable']} / {s['unanswerable']} |",
            f"| Правильні відповіді | {s['correctAnswerRate']:.3f} |",
            f"| Хибні відмови | {s['falseRefusalRate']:.3f} |",
            f"| Правильні відмови | {s['correctRefusalRate']:.3f} |",
            f"| Відповіді без підстав | {s['unwarrantedAnswerRate']:.3f} |",
            f"| Посилань у сирих відповідях | {s['rawCitations']} |",
            f"| З них вигаданих | {s['rawFabricatedCitations']} |",
            f"| Речень, прибраних перевіркою | {s['removedSentences']} |",
            f"| Відповідей, заблокованих перевіркою | {s['answersBlockedByValidation']} |",
            f"| Точність процитованих джерел | {s['citedSourcePrecision']:.3f} |",
            f"| Релевантний запис у контексті | {s['contextRecall']:.3f} |",
            f"| Затримка p50 / p95, с | {s['latencyP50Ms'] / 1000:.1f} / {s['latencyP95Ms'] / 1000:.1f} |", ""]
    return "\n".join(out)


def main():
    f_and, f_or, f_ask = latest("run-*-and.json"), latest("run-*-or.json"), latest("run-ask-*.json")
    run_and, run_or, ask = load(f_and), load(f_or), load(f_ask)
    fig_mrr_by_type(run_and)
    fig_hit1_by_source(run_and)
    fig_operator_ablation(run_and, run_or)
    fig_latency(run_and)
    fig_ask(ask)
    OUT_MD.parent.mkdir(parents=True, exist_ok=True)
    OUT_MD.write_text(tables(run_and, run_or, ask, [f_and, f_or, f_ask]), encoding="utf-8")
    print("таблиці:", OUT_MD.relative_to(ROOT))


if __name__ == "__main__":
    os.chdir(ROOT)
    main()
