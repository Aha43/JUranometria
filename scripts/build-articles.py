#!/usr/bin/env python3
"""Build the two reviewed article collections into the Pages artifact."""
from __future__ import annotations
import html, re, shutil, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = "https://github.com/Aha43/JUranometria/blob/main/"

PAGES = [
    (ROOT / "docs/articles/README.md", "index.html", "JUranometria articles"),
    (ROOT / "docs/articles/from-celestial-sphere-to-page.md",
     "celestial-sphere-to-page.html", "Exploring the sky"),
    (ROOT / "docs/articles/where-the-sun-is.md",
     "where-the-sun-is.html", "Exploring the sky"),
    (ROOT / "docs/articles/which-way-the-light-falls.md",
     "which-way-the-light-falls.html", "Exploring the sky"),
    (ROOT / "docs/how/language.md", "language.html", "How JUranometria works"),
    (ROOT / "docs/how/projections.md", "projections.html", "How JUranometria works"),
]

SPECIAL_LINKS = {
    "from-celestial-sphere-to-page.md": "celestial-sphere-to-page.html",
    "../articles/from-celestial-sphere-to-page.md": "celestial-sphere-to-page.html",
    "where-the-sun-is.md": "where-the-sun-is.html",
    "which-way-the-light-falls.md": "which-way-the-light-falls.html",
    "../how/language.md": "language.html",
    "../how/projections.md": "projections.html",
    "language.md": "language.html",
    "projections.md": "projections.html",
    "../articles/README.md": "index.html",
}

def href(target: str, source: Path) -> str:
    if target in SPECIAL_LINKS:
        return SPECIAL_LINKS[target]
    if target.startswith(("http://", "https://", "#", "images/")):
        return target
    resolved = (source.parent / target).resolve()
    try:
        rel = resolved.relative_to(ROOT).as_posix()
    except ValueError:
        return target
    return REPO + rel

def inline(text: str, source: Path) -> str:
    stash: list[str] = []
    def keep(value: str) -> str:
        stash.append(value); return f"\x00{len(stash)-1}\x00"
    text = re.sub(r"`([^`]+)`", lambda m: keep("<code>" + html.escape(m.group(1)) + "</code>"), text)
    text = html.escape(text, quote=False)
    text = re.sub(r"!\[([^]]*)\]\(([^)]+)\)", lambda m: keep(f'<img src="{html.escape(href(html.unescape(m.group(2)), source), quote=True)}" alt="{html.escape(html.unescape(m.group(1)), quote=True)}">'), text)
    text = re.sub(r"\[([^]]+)\]\(([^)]+)\)", lambda m: keep(f'<a href="{html.escape(href(html.unescape(m.group(2)), source), quote=True)}">{m.group(1)}</a>'), text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", text)
    text = re.sub(r"(?<!\*)\*([^*]+)\*(?!\*)", r"<em>\1</em>", text)
    token = re.compile(r"\x00(\d+)\x00")
    while token.search(text):
        text = token.sub(lambda m: stash[int(m.group(1))], text)
    return text

def render_markdown(source: Path) -> tuple[str, str]:
    raw = source.read_text(encoding="utf-8").splitlines()
    lines: list[str] = []
    for line in raw:
        if (lines and re.match(r"^\s{2,}\S", line)
                and re.match(r"^\s*([-*]|\d+\.)\s+", lines[-1])):
            lines[-1] += " " + line.strip()
        else:
            lines.append(line)
    title = next(line[2:].strip() for line in lines if line.startswith("# "))
    out: list[str] = []
    paragraph: list[str] = []
    in_code = False; code: list[str] = []
    list_kind: str | None = None
    quote: list[str] = []

    def flush_paragraph():
        nonlocal paragraph
        if paragraph:
            out.append("<p>" + inline(" ".join(x.strip() for x in paragraph), source) + "</p>")
            paragraph=[]
    def close_list():
        nonlocal list_kind
        if list_kind:
            out.append(f"</{list_kind}>"); list_kind=None
    def flush_quote():
        nonlocal quote
        if quote:
            out.append("<blockquote><p>" + inline(" ".join(quote), source) + "</p></blockquote>"); quote=[]

    i=0
    while i<len(lines):
        line=lines[i]
        if line.startswith("```"):
            flush_paragraph(); close_list(); flush_quote()
            if in_code:
                out.append("<pre><code>"+html.escape("\n".join(code))+"</code></pre>"); code=[]; in_code=False
            else: in_code=True
            i+=1; continue
        if in_code: code.append(line); i+=1; continue
        if line.startswith("|") and i+1<len(lines) and re.match(r"^\|?\s*:?-+", lines[i+1]):
            flush_paragraph(); close_list(); flush_quote()
            rows=[]
            while i<len(lines) and lines[i].startswith("|"):
                rows.append([c.strip() for c in lines[i].strip("|").split("|")]); i+=1
            head=rows[0]; body=rows[2:]
            out.append("<div class=\"table-wrap\"><table><thead><tr>"+"".join("<th>"+inline(c,source)+"</th>" for c in head)+"</tr></thead><tbody>")
            for row in body: out.append("<tr>"+"".join("<td>"+inline(c,source)+"</td>" for c in row)+"</tr>")
            out.append("</tbody></table></div>"); continue
        if not line.strip(): flush_paragraph(); close_list(); flush_quote(); i+=1; continue
        if line.strip()=="---": flush_paragraph(); close_list(); flush_quote(); out.append("<hr>"); i+=1; continue
        m=re.match(r"^(#{1,4})\s+(.+)$",line)
        if m:
            flush_paragraph(); close_list(); flush_quote(); level=len(m.group(1)); text=m.group(2)
            slug=re.sub(r"[^a-z0-9]+","-",text.lower()).strip("-")
            out.append(f'<h{level} id="{slug}">{inline(text,source)}</h{level}>'); i+=1; continue
        if line.startswith(">"):
            flush_paragraph(); close_list(); quote.append(line[1:].strip()); i+=1; continue
        m=re.match(r"^\s*([-*]|\d+\.)\s+(.+)$",line)
        if m:
            flush_paragraph(); flush_quote(); kind="ol" if m.group(1)[0].isdigit() else "ul"
            if list_kind!=kind: close_list(); out.append(f"<{kind}>"); list_kind=kind
            out.append("<li>"+inline(m.group(2),source)+"</li>"); i+=1; continue
        paragraph.append(line); i+=1
    flush_paragraph(); close_list(); flush_quote()
    return title, "\n".join(out)

def page(title: str, series: str, body: str, filename: str) -> str:
    return f'''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(title)} — JUranometria</title><link rel="stylesheet" href="articles.css"></head>
<body><nav><a href="../index.html">Gallery</a><a href="index.html">Articles</a><a href="https://github.com/Aha43/JUranometria">Repository</a></nav>
<main><p class="series">{html.escape(series)}</p>{body}</main>
<footer><p>JUranometria 3.0 · <a href="https://github.com/Aha43/JUranometria/releases/tag/v3.0.0">Download the atlas</a></p></footer></body></html>\n'''

def validate_site(out: Path, articles: Path) -> None:
    from html.parser import HTMLParser
    class Links(HTMLParser):
        def __init__(self): super().__init__(); self.values=[]
        def handle_starttag(self, tag, attrs):
            for key,value in attrs:
                if key in ("href","src") and value: self.values.append(value)
    for page_path in articles.glob("*.html"):
        parser=Links(); parser.feed(page_path.read_text(encoding="utf-8"))
        for value in parser.values:
            if value.startswith(("http://","https://","#","mailto:")): continue
            target=(page_path.parent/value.split("#",1)[0]).resolve()
            if not target.exists(): raise RuntimeError(f"{page_path}: missing local target {value}")
        text=page_path.read_text(encoding="utf-8")
        if "/private/tmp" in text: raise RuntimeError(f"{page_path}: temporary path escaped into publication")

def main() -> None:
    if len(sys.argv)!=2: raise SystemExit("usage: build-articles.py OUT")
    out=Path(sys.argv[1]); articles=out/"articles"; articles.mkdir(parents=True,exist_ok=True)
    shutil.copy2(ROOT/"docs/articles/articles.css",articles/"articles.css")
    shutil.copytree(ROOT/"docs/articles/images",articles/"images",dirs_exist_ok=True)
    for source,name,series in PAGES:
        title,body=render_markdown(source)
        (articles/name).write_text(page(title,series,body,name),encoding="utf-8")
    gallery=out/"index.html"
    if gallery.exists():
        text=gallery.read_text(encoding="utf-8")
        marker='</header>'
        link='<p class="articles-link"><a href="articles/">Read the articles</a></p>\n'
        if marker not in text: raise RuntimeError("gallery index has no header boundary")
        gallery.write_text(text.replace(marker,link+marker,1),encoding="utf-8")
    validate_site(out,articles)
    print(f"articles: {len(PAGES)} pages assembled and checked in {articles}")
if __name__=="__main__": main()
