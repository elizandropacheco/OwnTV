const assert = require("node:assert/strict");
const content = require("../service/content");
const news = content.news(
  "<rss><item><title><![CDATA[Uma &amp; outra - Fonte]]></title><link>https://example.com/a</link><source>Fonte</source><pubDate>Tue, 27 Oct 2026 12:00:00 GMT</pubDate></item><item><title>unsafe</title><link>javascript:alert(1)</link></item></rss>",
  "tecnologia",
);
assert.equal(news.length, 1);
assert.equal(news[0].title, "Uma & outra");
assert.equal(news[0].source, "Fonte");
assert.ok(news[0].publishedAt > 0);
const videos = content.videos(
  "<entry><yt:videoId>abcdefghijk</yt:videoId><title>Vídeo</title><author><name>Canal</name></author><published>2026-10-26T12:00:00Z</published></entry>",
);
assert.equal(videos[0].channel, "Canal");
assert.equal(videos[0].url, "https://www.youtube.com/watch?v=abcdefghijk");
assert.equal(
  content.videos(
    "<entry><yt:videoId>../../bad</yt:videoId><title>bad</title></entry>",
  ).length,
  0,
);
assert.deepEqual(content.topics(" IA, IA, telecom "), ["IA", "telecom"]);
assert.equal(
  content.channels("https://evil.invalid UCXZCJLdBC09xxGZ6gcdrc6A").length,
  1,
);
assert.equal(content.bridgeURL("http://example.com"), "");
assert.equal(content.bridgeURL("file:///etc/passwd"), "");
assert.equal(content.bridgeURL("http://user:pass@192.168.1.2"), "");
assert.equal(
  content.bridgeURL("http://192.168.1.2:8788"),
  "http://192.168.1.2:8788",
);
assert.deepEqual(
  content
    .interleave(
      [
        [{ id: 1 }, { id: 3 }],
        [{ id: 2 }, { id: 1 }],
      ],
      4,
      "id",
    )
    .map((i) => i.id),
  [1, 2, 3],
);
console.log("Content parsers, bounds, deduplication and URL policy passed.");
