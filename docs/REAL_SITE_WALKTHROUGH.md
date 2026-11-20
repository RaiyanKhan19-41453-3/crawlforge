# Real-site walkthrough: CodeMarkeet

This is the story of how we pointed CrawlForge at a real Laravel site
(`https://codemarkeet.com`) and built working scrapers for four different page
layouts. Keep this file as a record of *how*, not just *what*.

## Ground rule

We only crawled this site because we own it. Before touching any other site, check its
`robots.txt` and make sure you are authorised.

CodeMarkeet's `robots.txt` was:

```
User-agent: *
Disallow:
```

Empty `Disallow` = crawling allowed. ✅

## The repeatable method

1. **Fetch the page HTML** and look for the repeating container (cards, articles, divs).
2. **Count matches** — make sure the selector matches only what you actually want.
3. **Pick selectors** for each field you want (CSS in `fields`, XPath in `xpathFields`).
4. **Check pagination** (`rel="next"`, `.pagination a`, `?page=`).
5. **Write a JSON recipe** into `config/sites/`.
6. **Run** `./scripts/run-docker.sh --site <name> --output /app/output/<name>.csv`.
7. **Inspect the output**. If a field is empty, fix that one selector and rerun.

## Recipe 1 — Store (the hard one)

**Page:** `https://codemarkeet.com/store`

**Discovery:**
- 12 repeating `<div class="product-card ...">` elements per page
- Pagination via `<a rel="next" href="...?page=2">`
- Server-rendered HTML → static crawler is enough, no Selenium

**Recipe:** `config/sites/codemarkeet-store.json`

```json
{
  "name": "codemarkeet-store",
  "sourceType": "HTML",
  "baseUrl": "https://codemarkeet.com",
  "seedUrls": ["https://codemarkeet.com/store"],
  "itemSelector": ".product-card",
  "nextPageSelector": "a[rel='next']",
  "fields": {
    "title": "h3",
    "description": "p.line-clamp-2",
    "price": "span.text-xl",
    "oldPrice": "span.line-through",
    "discount": "span.bg-rose-600",
    "category": "attr:data-categories"
  },
  "xpathFields": {
    "url": ".//a[contains(@href, '/product/')]/@href"
  },
  "maxPages": 3,
  "maxDepth": 5,
  "followLinks": false
}
```

**Command:**

```bash
./scripts/run-docker.sh --site codemarkeet-store \
  --output /app/output/codemarkeet.csv
```

**Result:** 3 pages, **36 products**, quality `1.000`, `Health: OK`.

### Bug found #1: pagination was ignored

The first run returned only 12 records (page 1). Cause: `AbstractCrawler` gated link
discovery behind `followLinks`, so an explicit `nextPageSelector` was skipped when
`followLinks: false`. Fixed by always calling `discoverLinks()`; `HtmlParser.links()`
already only adds general anchors when `followLinks` is true.

Also raised `maxDepth` from 1 to 5 so the next-link chain could continue.

## Recipe 2 — Blog

**Page:** `https://codemarkeet.com/blog`

**Discovery:** 3 `<article>` elements; no pagination.

**Recipe:** `config/sites/codemarkeet-blog.json`

```json
{
  "itemSelector": "article",
  "fields": {
    "title": "h2",
    "summary": "p.line-clamp-3",
    "category": "span.bg-charcoal",
    "author": "span.font-semibold",
    "date": "div.gap-2 span:nth-child(3)"
  },
  "xpathFields": { "url": ".//h2/a/@href" }
}
```

**Result:** **3 posts** — titles, summaries, categories (Meta/Tutorial/Business),
authors, dates and URLs.

Note: exported columns named `attr_title` / `attr_url` are intentional. When a scraped
field collides with a metadata key, the exporter prefixes it with `attr_` so your data
isn't overwritten.

## Recipe 3 — Our Work

**Page:** `https://codemarkeet.com/work`

**Discovery:** 6 `<article>` elements — 3 case studies and 3 service cards. Service
cards have an Alpine.js `x-show` attribute; case studies don't.

Selector evolution (a good lesson):

| Selector | Rows | Verdict |
| --- | --- | --- |
| `article` | 6 | wrong — included service cards |
| `article.reveal` | 6 | wrong — all six have `reveal` |
| `article:not([x-show])` | 3 | correct |

**Recipe:** `config/sites/codemarkeet-work.json`

```json
{
  "itemSelector": "article:not([x-show])",
  "fields": {
    "title": "h3",
    "summary": "p.text-sm",
    "category": "div.bg-charcoal",
    "platform": "div.grid-cols-2 span:nth-child(2)"
  },
  "xpathFields": {
    "label": ".//span[starts-with(normalize-space(.),'0')]/text()"
  }
}
```

**Result:** **3 case studies**, e.g.
`01 / Full-Stack Product | Wholesale Pharmaceutical Ordering Platform`.

### Bug found #2: Alpine.js attributes broke the XPath bridge

Parsing failed with `DOMException: INVALID_CHARACTER_ERR`. The cause: when copying
Jsoup HTML into a W3C DOM for XPath, attributes like `@click.away` and `:class` are
**not legal XML attribute names**, so the DOM rejected them.

Fixed in `XPathSelector.copyInto()`:
- skip attribute names that don't match `[A-Za-z_:][A-Za-z0-9_.:-]*`
- strip characters forbidden by XML 1.0 from text and attribute values

## Recipe 4 — Team

**Page:** `https://codemarkeet.com/team`

**Discovery:** 7 divs with `class="reveal group text-center flex flex-col items-center"`.
Name in `h3`, role in `p.font-medium`, photo in `img`.

**Recipe:** `config/sites/codemarkeet-team.json`

```json
{
  "itemSelector": "div.reveal.group.text-center",
  "fields": { "name": "h3", "role": "p.font-medium" },
  "xpathFields": { "photo": ".//img/@src" }
}
```

**Result:** **7 members** with names, roles and photo URLs.

## Summary

| Recipe | Records | Notable |
| --- | --- | --- |
| `codemarkeet-store` | 36 | pagination + product links |
| `codemarkeet-blog` | 3 | dates and categories |
| `codemarkeet-work` | 3 | needed `:not([x-show])` filtering |
| `codemarkeet-team` | 7 | photos via XPath |

One engine, four different layouts, all customisation in JSON recipes. The two real
bugs found here (pagination gating, Alpine attributes in XPath) had never shown up in
the offline demo — which is exactly why testing against a real site matters.

## Useful commands

```bash
# list all available recipes
./scripts/run-docker.sh

# run one recipe
./scripts/run-docker.sh --site codemarkeet-store --output /app/output/codemarkeet.csv

# repeat every 5 minutes
./scripts/run-docker.sh --site codemarkeet-store --interval 300 --output /app/output/store.csv
```
