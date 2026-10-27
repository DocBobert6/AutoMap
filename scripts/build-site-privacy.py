#!/usr/bin/env python3
"""Builds the website's privacy page from PRIVACY.md.

PRIVACY.md is the only place the privacy text is written. This renders it into the website's
own layout (site/style.css, the same header and footer as the landing page) so velamaps.org has
a privacy page that cannot drift from the document in the repository.

    python3 scripts/build-site-privacy.py <out.html>

Needs the `markdown` package, which the docs toolchain (site/requirements-docs.txt) installs.
"""
import os
import re
import sys

import markdown

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLOB = "https://github.com/PimpinPumpkin/Vela/blob/main/"

HEAD = """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Privacy · Vela Maps</title>
  <meta name="description" content="What leaves your phone when you use Vela Maps, where it goes, and what does not: every service the app contacts and what each one receives.">
  <link rel="canonical" href="https://velamaps.org/privacy">
  <link rel="icon" href="assets/logo.svg" type="image/svg+xml">
  <link rel="stylesheet" href="style.css">
</head>
<body>

<header class="top">
  <a class="brand" href="./"><img src="assets/logo.svg" alt=""> Vela Maps</a>
  <nav>
    <a href="./#what">What it does</a>
    <a href="privacy.html" aria-current="page">Privacy</a>
    <a href="./#install">Install</a>
    <a href="./#docs">Docs</a>
    <a href="press.html">Press</a>
    <a href="https://github.com/PimpinPumpkin/Vela">GitHub</a>
  </nav>
</header>

<main class="doc">
<div class="hero col">
  <div class="kicker">Privacy</div>
"""

FOOT = """
  <p class="small">This page is built from <a href="https://github.com/PimpinPumpkin/Vela/blob/main/PRIVACY.md"><code>PRIVACY.md</code></a> in the repository, which is where a correction goes.</p>
</div>
</main>

<footer>
  <div class="col">
    <p>Vela Maps is free software under the <a href="https://github.com/PimpinPumpkin/Vela/blob/main/LICENSE">GPLv3</a>. Map data © <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors.</p>
    <p>Vela is not affiliated with or endorsed by Google. Google Maps is a trademark of Google LLC.</p>
    <p>This site sets no cookies and loads nothing from anyone else.</p>
  </div>
</footer>

</body>
</html>
"""


def main():
    out = sys.argv[1]
    src = open(os.path.join(ROOT, "PRIVACY.md"), encoding="utf-8").read()
    # The file's own title carries the app's name; the page already does.
    src = re.sub(r"^# .*\n", "# What Vela sends, and to whom\n", src, count=1)
    html = markdown.markdown(src, extensions=["tables", "sane_lists"])
    # A link into the repository (code, SPEC.md) points at GitHub; the page has no such files.
    html = re.sub(r'href="(?!https?:|#|mailto:)([^"]+)"', lambda m: f'href="{BLOB}{m.group(1)}"', html)
    # The first paragraph is the summary line under the title.
    html = html.replace("<p>", '<p class="lede">', 1)
    # Wide tables scroll inside themselves on a phone instead of widening the page.
    html = html.replace("<table>", '<div class="tablewrap"><table>').replace("</table>", "</table></div>")
    # The section headings sit outside the hero block, in the page's normal text column.
    body, sep, rest = html.partition("<h2")
    page = HEAD + body + '</div>\n<div class="col prose">\n' + sep + rest + FOOT
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    open(out, "w", encoding="utf-8").write(page)
    print(f"privacy page: {len(page):,} bytes -> {out}")


if __name__ == "__main__":
    main()
