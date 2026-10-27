/* ES5: webOS 4.5 runs Node 0.12. No DOM, eval or external entity parsing. */
"use strict";
function text(s) {
  return String(s || "")
    .replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, "$1")
    .replace(/<[^>]*>/g, "")
    .replace(/&#x([a-f0-9]+);/gi, function (_, n) {
      return code(parseInt(n, 16));
    })
    .replace(/&#(\d+);/g, function (_, n) {
      return code(parseInt(n, 10));
    })
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, "&")
    .replace(/\s+/g, " ")
    .trim();
}
function code(n) {
  return n > 0 && n <= 65535 ? String.fromCharCode(n) : "";
}
function tag(s, name) {
  var m = new RegExp(
    "<" + name + "(?:\\s[^>]*)?>([\\s\\S]*?)</" + name + ">",
    "i"
  ).exec(s);
  return m ? text(m[1]) : "";
}
function date(s) {
  var n = Date.parse(s);
  return isFinite(n) ? n : 0;
}
function news(xml, topic) {
  var items = [],
    re = /<item(?:\s[^>]*)?>([\s\S]*?)<\/item>/gi,
    m;
  while ((m = re.exec(xml)) && items.length < 16) {
    var s = m[1],
      title = tag(s, "title"),
      url = tag(s, "link"),
      source = tag(s, "source") || "Google Notícias";
    if (title && /^https:\/\//.test(url)) {
      var suffix = " - " + source;
      if (title.slice(-suffix.length) === suffix)
        title = title.slice(0, -suffix.length);
      items.push({
        title: title,
        source: source,
        topic: topic,
        url: url,
        publishedAt: date(tag(s, "pubDate"))
      });
    }
  }
  return items;
}
function videos(xml) {
  var items = [],
    re = /<entry(?:\s[^>]*)?>([\s\S]*?)<\/entry>/gi,
    m;
  while ((m = re.exec(xml)) && items.length < 16) {
    var s = m[1],
      id = tag(s, "yt:videoId"),
      title = tag(s, "title");
    if (/^[A-Za-z0-9_-]{11}$/.test(id) && title)
      items.push({
        id: id,
        title: title,
        channel: tag(s, "name"),
        publishedAt: date(tag(s, "published")),
        thumbnail: "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg",
        url: "https://www.youtube.com/watch?v=" + id,
        provider: "YouTube"
      });
  }
  return items;
}
function topics(v) {
  var out = [];
  String(v || "telecom, tecnologia, inteligência artificial")
    .split(/[,\n]/)
    .forEach(function (s) {
      s = s.trim().slice(0, 80);
      if (s && out.indexOf(s) < 0 && out.length < 8) out.push(s);
    });
  return out;
}
function channels(v) {
  var out = [];
  String(v || "")
    .split(/[,\s]+/)
    .forEach(function (s) {
      if (
        /^UC[A-Za-z0-9_-]{22}$/.test(s) &&
        out.indexOf(s) < 0 &&
        out.length < 8
      )
        out.push(s);
    });
  return out;
}
function interleave(groups, max, key) {
  var out = [],
    seen = {},
    row = 0,
    more = true;
  while (more && out.length < max) {
    more = false;
    groups.forEach(function (g) {
      if (g[row]) {
        more = true;
        var id = g[row][key];
        if (!seen[id] && out.length < max) {
          seen[id] = true;
          out.push(g[row]);
        }
      }
    });
    row++;
  }
  return out;
}
function bridgeURL(value) {
  var match = /^(https?):\/\/([^\/:?#]+)(?::(\d{1,5}))?\/?$/.exec(
    String(value || "")
  );
  if (!match) return "";
  var host = match[2].toLowerCase(),
    parts = host.split(".").map(Number);
  var local =
    host === "localhost" ||
    /\.local$/.test(host) ||
    (parts.length === 4 &&
      parts.every(function (n) {
        return n >= 0 && n <= 255;
      }) &&
      (parts[0] === 10 ||
        parts[0] === 127 ||
        (parts[0] === 192 && parts[1] === 168) ||
        (parts[0] === 172 && parts[1] >= 16 && parts[1] <= 31) ||
        (parts[0] === 100 && parts[1] >= 64 && parts[1] <= 127)));
  if (!local && match[1] !== "https") return "";
  if (match[3] && (Number(match[3]) < 1 || Number(match[3]) > 65535)) return "";
  return match[1] + "://" + host + (match[3] ? ":" + match[3] : "");
}
module.exports = {
  text: text,
  news: news,
  videos: videos,
  topics: topics,
  channels: channels,
  interleave: interleave,
  bridgeURL: bridgeURL
};
