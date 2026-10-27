"use strict";
var Service = require("webos-service"),
  https = require("https"),
  fs = require("fs"),
  path = require("path"),
  urls = require("url"),
  parser = require("./content");
var id = require("./package.json").name,
  service = new Service(id),
  cache = {},
  inflight = {};
var dataDir = path.join(__dirname, "private-data"),
  configFile = path.join(dataDir, "bridge.json");
function own(message) {
  if (message.sender !== "io.elizandropacheco.owntv.webos") {
    fail(message, "Acesso restrito ao OwnTV.");
    return false;
  }
  return true;
}
function fail(message, text) {
  message.respond({ returnValue: false, errorText: text });
}
function get(url, done) {
  var finished = false,
    total = 0,
    chunks = [],
    request;
  function finish(error, body) {
    if (finished) return;
    finished = true;
    clearTimeout(timer);
    done(error, body);
  }
  var timer = setTimeout(function () {
    if (request) request.abort();
    finish(new Error("timeout"));
  }, 10000);
  try {
    var options = urls.parse(url);
    options.headers = {
      "User-Agent": "OwnTV/0.2 (+https://elizandropacheco.io)"
    };
    request = https.get(options, function (response) {
      if (response.statusCode !== 200) {
        response.resume();
        finish(new Error("upstream"));
        return;
      }
      response.on("data", function (chunk) {
        total += chunk.length;
        if (total > 1048576) {
          request.abort();
          finish(new Error("limit"));
        } else chunks.push(chunk);
      });
      response.on("end", function () {
        finish(null, Buffer.concat(chunks).toString("utf8"));
      });
      response.on("error", function () {
        finish(new Error("network"));
      });
    });
    request.on("error", function () {
      finish(new Error("network"));
    });
  } catch (e) {
    finish(e);
  }
}
function load(key, url, parse, done) {
  var previous = cache[key],
    now = Date.now();
  if (previous && now - previous.fetchedAt < 1800000) {
    done(null, previous.items, false);
    return;
  }
  if (inflight[key]) {
    inflight[key].push(done);
    return;
  }
  inflight[key] = [done];
  get(url, function (error, body) {
    var items = error ? [] : parse(body);
    if (!items.length) error = new Error("empty");
    if (!error) {
      var keys = Object.keys(cache);
      if (!cache[key] && keys.length >= 64) {
        keys.sort(function (a, b) {
          return cache[a].fetchedAt - cache[b].fetchedAt;
        });
        delete cache[keys[0]];
      }
      cache[key] = { items: items, fetchedAt: now };
    }
    var callbacks = inflight[key];
    delete inflight[key];
    callbacks.forEach(function (fn) {
      fn(
        error,
        items.length ? items : previous ? previous.items : [],
        !!error && !!previous
      );
    });
  });
}
function many(values, fetch, done) {
  var groups = [],
    pending = values.length,
    stale = false,
    failed = 0;
  if (!pending) {
    done([], false, 0);
    return;
  }
  values.forEach(function (value, index) {
    fetch(value, function (error, items, cached) {
      groups[index] = items;
      stale = stale || cached;
      if (error) failed++;
      if (--pending === 0) done(groups, stale, failed);
    });
  });
}
service.register("news", function (message) {
  if (!own(message)) return;
  var selected = parser.topics(message.payload.topics);
  many(
    selected,
    function (topic, done) {
      load(
        "news:" + topic,
        "https://news.google.com/rss/search?q=" +
          encodeURIComponent(topic + " when:7d") +
          "&hl=pt-BR&gl=BR&ceid=BR:pt-419",
        function (xml) {
          return parser.news(xml, topic);
        },
        done
      );
    },
    function (groups, stale, failed) {
      var items = parser.interleave(groups, 64, "title");
      message.respond({
        returnValue: items.length > 0,
        items: items,
        stale: stale,
        failedSources: failed,
        fetchedAt: Date.now(),
        errorText: items.length ? "" : "Não foi possível atualizar as notícias."
      });
    }
  );
});
service.register("videos", function (message) {
  if (!own(message)) return;
  var selected = parser.channels(message.payload.channels);
  if (!selected.length) {
    fail(message, "Escolha ao menos um canal válido.");
    return;
  }
  many(
    selected,
    function (channel, done) {
      load(
        "videos:" + channel,
        "https://www.youtube.com/feeds/videos.xml?channel_id=" + channel,
        parser.videos,
        done
      );
    },
    function (groups, stale, failed) {
      var items = parser.interleave(groups, 48, "id");
      message.respond({
        returnValue: items.length > 0,
        items: items,
        stale: stale,
        failedSources: failed,
        errorText: items.length ? "" : "Não foi possível atualizar os vídeos."
      });
    }
  );
});
function bridge() {
  try {
    var value = JSON.parse(fs.readFileSync(configFile, "utf8"));
    if (
      !parser.bridgeURL(value.url) ||
      !/^[-_A-Za-z0-9]{32,128}$/.test(value.token)
    )
      return null;
    return value;
  } catch (e) {
    return null;
  }
}
// This method accepts data only: no executable commands, filenames or camera passwords.
service.register("setBridge", function (message) {
  if (!own(message)) return;
  var p = message.payload,
    url = parser.bridgeURL(p.url),
    token = String(p.token || "");
  if (!url || !/^[-_A-Za-z0-9]{32,128}$/.test(token)) {
    fail(message, "Endereço da ponte ou chave inválidos.");
    return;
  }
  try {
    if (!fs.existsSync(dataDir)) fs.mkdirSync(dataDir, 448);
    fs.writeFileSync(configFile, JSON.stringify({ url: url, token: token }), {
      mode: 384
    });
    fs.chmodSync(configFile, 384);
    message.respond({ returnValue: true });
  } catch (e) {
    fail(message, "Não foi possível salvar a conexão.");
  }
});
service.register("clearBridge", function (message) {
  if (!own(message)) return;
  try {
    if (fs.existsSync(configFile)) fs.unlinkSync(configFile);
    message.respond({ returnValue: true });
  } catch (e) {
    fail(message, "Não foi possível remover a conexão.");
  }
});
service.register("bridge", function (message) {
  if (!own(message)) return;
  var value = bridge();
  message.respond({
    returnValue: true,
    configured: !!value,
    url: value ? value.url : "",
    token: value ? value.token : ""
  });
});
service.register("status", function (message) {
  message.respond({
    returnValue: true,
    version: "0.2.1",
    runtime: process.version
  });
});

// Only this app can access its TV-local pairing. Never accept a caller-supplied host.
var appsKeyFile = path.join(dataDir, "apps-pairing.json");
service.register("appsConnection", function (message) {
  if (!own(message)) return;
  try {
    var interfaces = require("os").networkInterfaces(),
      hosts = [],
      key = "";
    Object.keys(interfaces).forEach(function (name) {
      interfaces[name].forEach(function (a) {
        if (
          !a.internal &&
          (a.family === "IPv4" || a.family === 4) &&
          a.address.indexOf("169.254.") !== 0
        )
          hosts.push(a.address);
      });
    });
    try {
      key = JSON.parse(fs.readFileSync(appsKeyFile, "utf8")).key || "";
    } catch (_) {}
    message.respond({ returnValue: true, hosts: hosts, key: key });
  } catch (_) {
    fail(message, "Não foi possível consultar a rede desta TV.");
  }
});
service.register("setAppsKey", function (message) {
  if (!own(message)) return;
  var key = message.payload.key;
  if (typeof key !== "string" || !/^[A-Za-z0-9_-]{8,256}$/.test(key)) {
    fail(message, "Pareamento inválido.");
    return;
  }
  try {
    if (!fs.existsSync(dataDir)) fs.mkdirSync(dataDir, 448);
    fs.chmodSync(dataDir, 448);
    fs.writeFileSync(appsKeyFile, JSON.stringify({ key: key }), { mode: 384 });
    fs.chmodSync(appsKeyFile, 384);
    message.respond({ returnValue: true });
  } catch (_) {
    fail(message, "Não foi possível salvar o pareamento.");
  }
});
