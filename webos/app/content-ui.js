/* OwnTV content UI. ES5, native Luna service, text-only feed rendering. */
(function () {
  "use strict";
  var core,
    news = [],
    videos = [],
    newsIndex = 0,
    newsPage = 0,
    videoPage = 0,
    homeVideoPage = 0,
    current = "",
    pending = [],
    prefs,
    newsStale = false,
    videosStale = false,
    loadingNews = false,
    loadingVideos = false;
  var defaults = {
    newsEnabled: true,
    videoEnabled: true,
    topics: "telecom, tecnologia, inteligência artificial",
    channels:
      "UCMpkcuh4xorlaP8GsmAjMQg, UC_bXJnsgwOqEPA_-6N6faKw, UCdmGjywrxeOPfC7vDllmSgQ, UCXZCJLdBC09xxGZ6gcdrc6A"
  };
  function el(id) {
    return document.getElementById(id);
  }
  function all(q) {
    return Array.prototype.slice.call(document.querySelectorAll(q));
  }
  function read() {
    try {
      var p = JSON.parse(localStorage.getItem("owntv.webos.content")) || {};
      return {
        newsEnabled: p.newsEnabled !== false,
        videoEnabled: p.videoEnabled !== false,
        topics:
          typeof p.topics === "string"
            ? p.topics.slice(0, 640)
            : defaults.topics,
        channels:
          typeof p.channels === "string"
            ? p.channels.slice(0, 320)
            : defaults.channels
      };
    } catch (e) {
      return JSON.parse(JSON.stringify(defaults));
    }
  }
  function save() {
    try {
      localStorage.setItem("owntv.webos.content", JSON.stringify(prefs));
    } catch (e) {
      core.toast("Não foi possível salvar as preferências.");
    }
    labels();
  }
  function call(method, payload, done) {
    var Bridge = window.PalmServiceBridge || window.WebOSServiceBridge;
    if (!Bridge) {
      done({
        returnValue: false,
        errorText: "Este recurso requer o OwnTV instalado na LG."
      });
      return;
    }
    var request = new Bridge(),
      finished = false;
    pending.push(request);
    var timeout = setTimeout(function () {
      finish({
        returnValue: false,
        errorText: "O serviço não respondeu. Tente atualizar."
      });
    }, 25000);
    function finish(result) {
      if (finished) return;
      finished = true;
      clearTimeout(timeout);
      var n = pending.indexOf(request);
      if (n >= 0) pending.splice(n, 1);
      done(result);
    }
    request.onservicecallback = function (raw) {
      try {
        finish(JSON.parse(raw));
      } catch (e) {
        finish({
          returnValue: false,
          errorText: "Resposta inválida do serviço."
        });
      }
    };
    request.call(
      "luna://io.elizandropacheco.owntv.webos.content/" + method,
      JSON.stringify(payload || {})
    );
  }
  function cached(name) {
    try {
      var data = JSON.parse(localStorage.getItem("owntv.cache." + name));
      return data &&
        Array.isArray(data.items) &&
        Date.now() - data.saved >= 0 &&
        Date.now() - data.saved < 86400000 &&
        data.signature === (name === "news" ? prefs.topics : prefs.channels)
        ? data.items
        : [];
    } catch (e) {
      return [];
    }
  }
  function cache(name, items) {
    try {
      localStorage.setItem(
        "owntv.cache." + name,
        JSON.stringify({
          saved: Date.now(),
          items: items,
          signature: name === "news" ? prefs.topics : prefs.channels
        })
      );
    } catch (e) {
      /* Fresh results remain usable when storage is full. */
    }
  }
  function date(value) {
    if (!value) return "";
    var d = new Date(value);
    return (
      ("0" + d.getDate()).slice(-2) +
      "/" +
      ("0" + (d.getMonth() + 1)).slice(-2) +
      "/" +
      d.getFullYear()
    );
  }
  function labels() {
    el("news-setting-value").textContent = prefs.newsEnabled
      ? "Temas e manchetes"
      : "Desativadas";
    el("video-setting-value").textContent = prefs.videoEnabled
      ? "YouTube · canais escolhidos"
      : "Desativados";
  }
  function headline() {
    var node = el("headline"),
      entry =
        prefs.newsEnabled && news.length ? news[newsIndex % news.length] : null;
    if (entry) {
      node.textContent = entry.title;
      el("headline-source").textContent =
        entry.source +
        " · " +
        date(entry.publishedAt) +
        (newsStale ? " · salvo anteriormente" : "");
    } else {
      node.textContent = prefs.newsEnabled
        ? loadingNews
          ? "Buscando notícias dos seus temas…"
          : "Notícias indisponíveis. Abra Notícias para atualizar."
        : "Seus aplicativos e dispositivos. Uma tela com a sua identidade.";
      el("headline-source").textContent = "";
    }
  }
  function fetchNews() {
    if (loadingNews || !prefs.newsEnabled) {
      headline();
      return;
    }
    loadingNews = true;
    var signature = prefs.topics;
    headline();
    call("news", { topics: prefs.topics }, function (r) {
      loadingNews = false;
      if (signature !== prefs.topics) {
        fetchNews();
        return;
      }
      if (r.returnValue && r.items && r.items.length) {
        news = r.items;
        newsStale = !!r.stale;
        newsIndex = 0;
        cache("news", news);
      } else {
        newsStale = news.length > 0;
        if (current === "news")
          core.toast(r.errorText || "Notícias indisponíveis.");
      }
      headline();
      if (current === "news") renderNews(false);
    });
  }
  function fetchVideos() {
    if (loadingVideos || !prefs.videoEnabled) return;
    loadingVideos = true;
    var signature = prefs.channels;
    renderHomeVideos();
    call("videos", { channels: prefs.channels }, function (r) {
      loadingVideos = false;
      if (signature !== prefs.channels) {
        fetchVideos();
        return;
      }
      if (r.returnValue && r.items && r.items.length) {
        videos = r.items;
        videosStale = !!r.stale;
        videoPage = 0;
        homeVideoPage = 0;
        cache("videos", videos);
      } else {
        videosStale = videos.length > 0;
        if (current === "suggestions")
          core.toast(r.errorText || "Sugestões indisponíveis.");
      }
      renderHomeVideos();
      if (current === "suggestions") renderVideos(false);
    });
  }
  function button(label, fn, css) {
    var b = document.createElement("button");
    b.className = css || "";
    b.textContent = label;
    b.onclick = fn;
    return b;
  }
  function titleNode(tag, text, css) {
    var n = document.createElement(tag);
    n.textContent = text;
    if (css) n.className = css;
    return n;
  }
  function pageControl(id, index, total) {
    el(id).textContent = (total ? index + 1 : 0) + " / " + Math.ceil(total / 4);
  }
  function videoCard(item, compact) {
    var b = document.createElement("button");
    b.className = "recommendation" + (compact ? " compact" : "");
    b.setAttribute("data-video-id", item.id);
    b.setAttribute("aria-label", item.title + " · " + item.channel);
    var image = document.createElement("img");
    image.src = item.thumbnail;
    image.alt = "";
    image.onerror = function () {
      image.style.visibility = "hidden";
    };
    b.appendChild(image);
    var info = document.createElement("span");
    info.className = "recommendation-info";
    info.appendChild(titleNode("strong", item.title));
    info.appendChild(titleNode("small", item.channel));
    info.appendChild(
      titleNode(
        "small",
        date(item.publishedAt) + (videosStale ? " · salvo" : ""),
        "metadata"
      )
    );
    b.appendChild(info);
    b.onclick = function () {
      if (!/^[A-Za-z0-9_-]{11}$/.test(item.id)) return;
      webOSDev.launch({
        id: "youtube.leanback.v4",
        params: { contentTarget: "https://www.youtube.com/watch?v=" + item.id },
        onFailure: function () {
          core.toast("Não foi possível abrir este vídeo no YouTube.");
        }
      });
    };
    return b;
  }
  function renderHomeVideos() {
    var section = el("home-suggestions"),
      host = el("home-video-items");
    var focused = host.contains(document.activeElement)
      ? document.activeElement.getAttribute("data-video-id")
      : null;
    section.hidden = !prefs.videoEnabled;
    host.textContent = "";
    if (!prefs.videoEnabled) return;
    videos
      .slice(homeVideoPage * 4, homeVideoPage * 4 + 4)
      .forEach(function (v) {
        host.appendChild(videoCard(v, true));
      });
    el("home-video-status").hidden = videos.length > 0;
    el("home-video-status").textContent = loadingVideos
      ? "Buscando vídeos dos canais escolhidos…"
      : "Sem vídeos disponíveis. Configure seus canais em Sugeridos.";
    pageControl("home-video-page", homeVideoPage, videos.length);
    el("home-video-prev").disabled = homeVideoPage === 0;
    el("home-video-next").disabled = (homeVideoPage + 1) * 4 >= videos.length;
    if (focused) {
      var target =
        host.querySelector('[data-video-id="' + focused + '"]') ||
        host.querySelector("button");
      if (target) target.focus();
    }
  }
  function renderVideos(focus) {
    var host = el("video-items");
    host.textContent = "";
    if (!prefs.videoEnabled)
      el("video-status").textContent =
        "Sugestões desativadas. Você pode ativá-las em Configurar canais.";
    else if (!videos.length)
      el("video-status").textContent = loadingVideos
        ? "Buscando vídeos…"
        : "Nenhum vídeo recebido. Confira os canais e a conexão.";
    else
      el("video-status").textContent =
        "Vídeos públicos dos canais escolhidos · não são recomendações pessoais da sua conta.";
    (prefs.videoEnabled ? videos : [])
      .slice(videoPage * 4, videoPage * 4 + 4)
      .forEach(function (v) {
        host.appendChild(videoCard(v, false));
      });
    pageControl("video-page", videoPage, videos.length);
    el("video-prev").disabled = videoPage === 0;
    el("video-next").disabled = (videoPage + 1) * 4 >= videos.length;
    if (focus) (host.querySelector("button") || el("video-config")).focus();
  }
  function renderNews(focus) {
    var host = el("news-items");
    host.textContent = "";
    el("news-status").textContent = !prefs.newsEnabled
      ? "Notícias desativadas."
      : news.length
        ? "Manchetes dos seus temas · " +
          (newsStale ? "exibindo itens salvos" : "fontes e datas informadas")
        : loadingNews
          ? "Buscando notícias…"
          : "Nenhuma notícia recebida. Confira os temas e a conexão.";
    (prefs.newsEnabled ? news : [])
      .slice(newsPage * 6, newsPage * 6 + 6)
      .forEach(function (n) {
        var b = button(
          "",
          function () {
            el("news-detail-title").textContent = n.title;
            el("news-detail-source").textContent =
              n.source + " · " + n.topic + " · " + date(n.publishedAt);
            el("news-detail").hidden = false;
            el("news-detail-close").focus();
          },
          "news-card"
        );
        b.appendChild(titleNode("span", n.topic, "news-topic"));
        b.appendChild(titleNode("strong", n.title));
        b.appendChild(
          titleNode("small", n.source + " · " + date(n.publishedAt))
        );
        host.appendChild(b);
      });
    el("news-page").textContent =
      (news.length ? newsPage + 1 : 0) + " / " + Math.ceil(news.length / 6);
    el("news-prev").disabled = newsPage === 0;
    el("news-next").disabled = (newsPage + 1) * 6 >= news.length;
    if (focus) (host.querySelector("button") || el("news-config")).focus();
  }
  function form(which) {
    var host = el("content-form");
    host.textContent = "";
    el("content-settings").hidden = false;
    el("content-form-error").textContent = "";
    var title =
      which === "news-settings"
        ? "Notícias e temas"
        : which === "video-settings"
          ? "Sugestões do YouTube"
          : "Conexão das câmeras";
    el("content-settings-title").textContent = title;
    function field(label, id, value, type) {
      var wrap = document.createElement("label");
      wrap.appendChild(titleNode("span", label));
      var input = document.createElement(
        type === "textarea" ? "textarea" : "input"
      );
      input.id = id;
      if (type !== "textarea") input.type = type || "text";
      input.value = value || "";
      input.maxLength = type === "textarea" ? 640 : 256;
      input.autocomplete = "off";
      wrap.appendChild(input);
      host.appendChild(wrap);
      return input;
    }
    if (which === "camera-settings") {
      field("Endereço da ponte de câmeras", "bridge-url", "", "url");
      field("Chave de acesso desta TV", "bridge-token", "", "password");
      host.appendChild(
        titleNode(
          "p",
          "A ponte recebe o RTSP do gravador e entrega prévias e vídeo compatível com a LG. As credenciais do gravador permanecem no servidor.",
          "form-help"
        )
      );
      host.appendChild(
        button("Salvar e conectar", function () {
          call(
            "setBridge",
            {
              url: el("bridge-url").value.trim(),
              token: el("bridge-token").value.trim()
            },
            function (r) {
              if (!r.returnValue) {
                el("content-form-error").textContent = r.errorText;
                return;
              }
              core.toast("Conexão salva.");
              core.show("house");
            }
          );
        })
      );
      host.appendChild(
        button("Desconectar esta TV", function () {
          call("clearBridge", {}, function (r) {
            if (r.returnValue) {
              window.OwnTVCameras.disconnect();
              core.toast("Conexão removida desta TV.");
              core.show("settings");
            }
          });
        })
      );
      call("bridge", {}, function (r) {
        if (current === "camera-settings" && r.returnValue) {
          el("bridge-url").value = r.url || "";
          el("bridge-token").value = r.token || "";
        }
      });
    } else {
      var isNews = which === "news-settings",
        enabled = isNews ? "newsEnabled" : "videoEnabled",
        name = isNews ? "topics" : "channels";
      var toggle = button(
        (prefs[enabled] ? "Ativado" : "Desativado") + " · OK para alternar",
        function () {
          prefs[enabled] = !prefs[enabled];
          toggle.textContent =
            (prefs[enabled] ? "Ativado" : "Desativado") + " · OK para alternar";
          save();
          headline();
          renderHomeVideos();
        }
      );
      host.appendChild(toggle);
      field(
        isNews
          ? "Temas separados por vírgula (até 8)"
          : "IDs dos canais YouTube separados por vírgula (até 8)",
        "content-value",
        prefs[name],
        "textarea"
      );
      host.appendChild(
        titleNode(
          "p",
          isNews
            ? "Exemplos: telecom, tecnologia, inteligência artificial. São exibidas manchetes, fonte e data."
            : "Use o identificador público do canal, começando por UC. O feed fornece título, canal e data; duração e recomendações pessoais não são disponibilizadas por ele.",
          "form-help"
        )
      );
      host.appendChild(
        button("Salvar e atualizar", function () {
          var value = el("content-value").value.trim();
          if (!value) {
            el("content-form-error").textContent =
              "Informe ao menos um " + (isNews ? "tema." : "canal.");
            return;
          }
          if (
            !isNews &&
            value.split(/[,\s]+/).some(function (x) {
              return x && !/^UC[A-Za-z0-9_-]{22}$/.test(x);
            })
          ) {
            el("content-form-error").textContent =
              "Use IDs completos de canal (UC e mais 22 caracteres).";
            return;
          }
          prefs[name] = value;
          save();
          if (isNews) {
            news = [];
            newsPage = 0;
            fetchNews();
            core.show("news");
          } else {
            videos = [];
            videoPage = 0;
            homeVideoPage = 0;
            fetchVideos();
            core.show("suggestions");
          }
        })
      );
    }
    (host.querySelector("button") || host.querySelector("input")).focus();
  }
  function show(which) {
    current = which;
    el("news-detail").hidden = true;
    if (window.OwnTVCameras) window.OwnTVCameras.leave();
    if (which === "news") {
      el("news").hidden = false;
      renderNews(true);
      fetchNews();
      return true;
    }
    if (which === "suggestions") {
      el("suggestions").hidden = false;
      renderVideos(true);
      fetchVideos();
      return true;
    }
    if (/^(news|video|camera)-settings$/.test(which)) {
      form(which);
      return true;
    }
    if (which === "house") {
      el("cameras-page").hidden = false;
      window.OwnTVCameras.open();
      return true;
    }
    return false;
  }
  function key(e) {
    if (!el("news-detail").hidden && (e.keyCode === 461 || e.keyCode === 27)) {
      el("news-detail").hidden = true;
      (el("news-items").querySelector("button") || el("news-config")).focus();
      return true;
    }
    if (!el("news-detail").hidden && e.keyCode >= 37 && e.keyCode <= 40) {
      el("news-detail-close").focus();
      return true;
    }
    var active = document.activeElement;
    if (
      current === "home" &&
      active.parentNode &&
      active.parentNode.id === "home-video-items"
    ) {
      var cards = all("#home-video-items button"),
        index = cards.indexOf(active);
      if (e.keyCode === 38) {
        var favorites = all("#shortcuts button");
        if (favorites[index]) favorites[index].focus();
        return true;
      }
      if (
        e.keyCode === 39 &&
        index === cards.length - 1 &&
        !el("home-video-next").disabled
      ) {
        el("home-video-next").click();
        return true;
      }
      if (e.keyCode === 37 && index === 0 && !el("home-video-prev").disabled) {
        el("home-video-prev").click();
        return true;
      }
    }
    return window.OwnTVCameras && window.OwnTVCameras.key(e);
  }
  function init(api) {
    core = api;
    prefs = read();
    news = cached("news");
    videos = cached("videos");
    newsStale = news.length > 0;
    videosStale = videos.length > 0;
    labels();
    headline();
    renderHomeVideos();
    el("home-video-next").onclick = function () {
      homeVideoPage++;
      renderHomeVideos();
      (
        el("home-video-items").querySelector("button") || el("home-video-next")
      ).focus();
    };
    el("home-video-prev").onclick = function () {
      homeVideoPage = Math.max(0, homeVideoPage - 1);
      renderHomeVideos();
      el("home-video-items").querySelector("button").focus();
    };
    el("video-next").onclick = function () {
      videoPage++;
      renderVideos(true);
    };
    el("video-prev").onclick = function () {
      videoPage = Math.max(0, videoPage - 1);
      renderVideos(true);
    };
    el("news-next").onclick = function () {
      newsPage++;
      renderNews(true);
    };
    el("news-prev").onclick = function () {
      newsPage = Math.max(0, newsPage - 1);
      renderNews(true);
    };
    el("news-refresh").onclick = fetchNews;
    el("video-refresh").onclick = fetchVideos;
    el("news-detail-close").onclick = function () {
      el("news-detail").hidden = true;
      (el("news-items").querySelector("button") || el("news-config")).focus();
    };
    window.OwnTVCameras.init(api, call);
    fetchNews();
    fetchVideos();
    setInterval(function () {
      if (!document.hidden && news.length) {
        newsIndex++;
        headline();
      }
    }, 12000);
    setInterval(function () {
      if (!document.hidden) {
        fetchNews();
        fetchVideos();
      }
    }, 1800000);
  }
  window.OwnTVContent = { init: init, show: show, key: key, call: call };
})();
