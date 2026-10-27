(function () {
  "use strict";
  var pageSize = 6,
    core,
    call,
    base = "",
    generation = 0,
    page = 0,
    count = 0,
    active = false,
    expanded = 0,
    quality = "main",
    fit = "cover",
    timer,
    retryTimer,
    retryCount = 0,
    watchdog,
    lastProgress = 0,
    lastTime = 0,
    hasPlayed = false,
    requests = [],
    objectURLs = [],
    lastFocus = 1,
    labels = [];
  function el(id) {
    return document.getElementById(id);
  }
  function label(id, text) {
    var node = el(id).querySelector("span");
    if (node) node.textContent = text;
    else el(id).textContent = text;
  }
  function stopRequests() {
    var cancelled = requests;
    requests = [];
    cancelled.forEach(function (r) {
      try {
        r.abort();
      } catch (e) {}
    });
    objectURLs.forEach(function (url) {
      URL.revokeObjectURL(url);
    });
    objectURLs = [];
  }
  function request(route, blob, done) {
    var xhr = new XMLHttpRequest(),
      token = generation;
    xhr.open("GET", base + "/" + route, true);
    xhr.timeout = 16000;
    if (blob) xhr.responseType = "blob";
    requests.push(xhr);
    function end(ok) {
      var i = requests.indexOf(xhr);
      if (i >= 0) requests.splice(i, 1);
      if (token !== generation) return;
      if (ok) {
        if (blob) done(null, xhr.response);
        else
          try {
            done(null, JSON.parse(xhr.responseText));
          } catch (e) {
            done("Resposta inválida.");
          }
      } else done("Não foi possível conectar à ponte.");
    }
    xhr.onload = function () {
      end(xhr.status === 200);
    };
    xhr.onabort =
      xhr.onerror =
      xhr.ontimeout =
        function () {
          end(false);
        };
    xhr.send();
  }
  function focusGrid() {
    var target =
      el("camera-grid").querySelector('[data-camera="' + lastFocus + '"]') ||
      el("camera-grid").querySelector("button") ||
      el("camera-config");
    target.focus();
  }
  function framePreview(image) {
    if (!image.naturalWidth || !image.naturalHeight) return;
    var card = image.parentNode,
      w = card.clientWidth,
      h = card.clientHeight;
    var scale = (fit === "cover" ? Math.max : Math.min)(
      w / image.naturalWidth,
      h / image.naturalHeight
    );
    var width = Math.round(image.naturalWidth * scale),
      height = Math.round(image.naturalHeight * scale);
    image.style.width = width + "px";
    image.style.height = height + "px";
    image.style.left = Math.round((w - width) / 2) + "px";
    image.style.top = Math.round((h - height) / 2) + "px";
  }
  function refresh() {
    if (!active || expanded) return;
    Array.prototype.forEach.call(
      el("camera-grid").querySelectorAll("button"),
      function (card) {
        if (card._busy) return;
        card._busy = true;
        var channel = Number(card.getAttribute("data-camera")),
          image = card.querySelector("img"),
          status = card.querySelector("small");
        request(
          "snapshot/" + channel + ".jpg?time=" + Date.now(),
          true,
          function (error, blob) {
            card._busy = false;
            if (error) {
              status.textContent = image.getAttribute("src")
                ? "Prévia anterior · sem atualização"
                : "Câmera indisponível · OK para tentar vídeo";
              card.classList.add("unavailable");
              return;
            }
            var url = URL.createObjectURL(blob);
            objectURLs.push(url);
            var previous = image.getAttribute("src");
            image.onload = function () {
              framePreview(image);
              status.textContent = "Prévia atualizada · OK para vídeo ao vivo";
              card.classList.remove("unavailable");
              if (previous) {
                URL.revokeObjectURL(previous);
                var index = objectURLs.indexOf(previous);
                if (index >= 0) objectURLs.splice(index, 1);
              }
            };
            image.onerror = function () {
              status.textContent = "Imagem indisponível";
            };
            image.src = url;
          }
        );
      }
    );
  }
  function render() {
    stopRequests();
    var host = el("camera-grid");
    host.textContent = "";
    var start = page * pageSize + 1;
    for (
      var channel = start;
      channel <= Math.min(count, start + pageSize - 1);
      channel++
    ) {
      (function (n) {
        var card = document.createElement("button");
        card.className = "camera-tile";
        card.setAttribute("data-camera", n);
        var image = document.createElement("img");
        image.alt = "";
        image.style.objectFit = fit;
        card.appendChild(image);
        var caption = document.createElement("span"),
          title = document.createElement("strong"),
          status = document.createElement("small");
        title.textContent = labels[n - 1] || "Câmera " + n;
        status.textContent = "Carregando prévia…";
        caption.appendChild(title);
        caption.appendChild(status);
        card.appendChild(caption);
        card.onclick = function () {
          lastFocus = n;
          play(n, "main");
        };
        host.appendChild(card);
      })(channel);
    }
    el("camera-pagination").textContent =
      (count ? page + 1 : 0) + " / " + Math.ceil(count / pageSize);
    el("camera-prev").disabled = page === 0;
    el("camera-next").disabled = (page + 1) * pageSize >= count;
    el("camera-status").textContent =
      count +
      " câmeras · prévias atualizadas a cada 3 segundos · selecione para vídeo ao vivo";
    refresh();
    focusGrid();
  }
  function open() {
    active = true;
    generation++;
    clearInterval(timer);
    el("camera-status").textContent = "Conectando às suas câmeras…";
    el("camera-grid").textContent = "";
    el("camera-config").focus();
    call("bridge", {}, function (r) {
      if (!active) return;
      if (!r.returnValue || !r.configured) {
        el("camera-status").textContent =
          "Configure a ponte para acessar suas câmeras. Nenhuma senha do gravador é incluída no aplicativo.";
        el("camera-pagination").textContent = "";
        el("camera-prev").disabled = true;
        el("camera-next").disabled = true;
        return;
      }
      base = r.url + "/t/" + encodeURIComponent(r.token);
      request("status", false, function (error, status) {
        if (error) {
          el("camera-status").textContent =
            error + " Confira se o servidor está ligado.";
          return;
        }
        count = Math.max(0, Math.min(64, Number(status.channels) || 0));
        labels = Array.isArray(status.labels) ? status.labels : [];
        page = Math.min(page, Math.max(0, Math.ceil(count / pageSize) - 1));
        render();
        timer = setInterval(refresh, 3000);
      });
    });
  }
  function videoStatus(text) {
    el("camera-live-status").textContent = text;
  }
  function play(channel, mode, retrying) {
    clearTimeout(retryTimer);
    if (!retrying) retryCount = 0;
    clearInterval(watchdog);
    lastProgress = Date.now();
    lastTime = 0;
    hasPlayed = false;
    watchdog = setInterval(function () {
      var v = el("camera-video");
      if (!expanded || !active || document.hidden || v.paused) return;
      if (Math.abs(v.currentTime - lastTime) > 0.1) {
        lastTime = v.currentTime;
        lastProgress = Date.now();
      } else if (Date.now() - lastProgress > (hasPlayed ? 20000 : 45000)) {
        if (retryCount < 2) {
          retryCount++;
          play(expanded, quality, true);
        } else {
          clearInterval(watchdog);
          videoStatus("Conexão interrompida. Tente Reconectar ou Modo leve.");
        }
      }
    }, 5000);
    expanded = channel;
    quality = mode;
    clearInterval(timer);
    stopRequests();
    document.documentElement.classList.add("camera-live");
    el("camera-player").hidden = false;
    el("camera-live-title").textContent =
      (labels[channel - 1] || "Câmera " + channel) +
      " · " +
      (mode === "main" ? "fluxo principal" : "modo leve");
    label(
      "camera-quality",
      mode === "main" ? "Modo leve" : "Qualidade principal"
    );
    var video = el("camera-video");
    video.pause();
    video.removeAttribute("src");
    video.load();
    video.style.width = window.innerWidth + "px";
    video.style.height = window.innerHeight + "px";
    video.setAttribute("width", window.innerWidth);
    video.setAttribute("height", window.innerHeight);
    video.muted = true;
    label("camera-audio", "Ativar áudio");
    label("camera-fit", fit === "cover" ? "Ajustar imagem" : "Preencher tela");
    label("camera-pause", "Pausar");
    videoStatus("Conectando ao vídeo ao vivo…");
    video.src =
      base +
      "/live/" +
      channel +
      "/" +
      mode +
      "/" +
      fit +
      "/index.m3u8?attempt=" +
      Date.now();
    video.load();
    try {
      var promise = video.play();
      if (promise && promise.catch)
        promise.catch(function () {
          videoStatus("Pressione Reproduzir para iniciar o vídeo.");
          label("camera-pause", "Reproduzir");
        });
    } catch (e) {
      videoStatus("Não foi possível iniciar. Tente Modo leve.");
    }
    el("camera-close").focus();
  }
  function close() {
    clearTimeout(retryTimer);
    clearInterval(watchdog);
    var video = el("camera-video");
    video.pause();
    video.removeAttribute("src");
    video.load();
    el("camera-player").hidden = true;
    document.documentElement.classList.remove("camera-live");
    expanded = 0;
    if (active) {
      refresh();
      timer = setInterval(refresh, 3000);
      focusGrid();
    }
  }
  function leave() {
    active = false;
    generation++;
    clearInterval(timer);
    stopRequests();
    if (expanded) close();
  }
  function disconnect() {
    leave();
    base = "";
    count = 0;
  }
  function key(e) {
    if (!expanded) return false;
    var code = e.keyCode;
    if (code === 461 || code === 27) {
      close();
      return true;
    }
    if (code >= 37 && code <= 40) {
      var controls = Array.prototype.slice.call(
          el("camera-player").querySelectorAll("button")
        ),
        n = controls.indexOf(document.activeElement);
      n =
        (n + (code === 37 || code === 38 ? -1 : 1) + controls.length) %
        controls.length;
      controls[n].focus();
      return true;
    }
    return false;
  }
  function init(api, service) {
    core = api;
    call = service;
    // LG renders video in a separate hardware plane. Keep it outside the
    // transformed design canvas before loading a source, including scale(1).
    document.body.appendChild(el("camera-player"));
    try {
      fit =
        localStorage.getItem("owntv.camera.fit") === "contain"
          ? "contain"
          : "cover";
    } catch (e) {}
    el("camera-prev").onclick = function () {
      page = Math.max(0, page - 1);
      lastFocus = page * pageSize + 1;
      generation++;
      render();
    };
    el("camera-next").onclick = function () {
      page++;
      lastFocus = page * pageSize + 1;
      generation++;
      render();
    };
    el("camera-refresh").onclick = function () {
      if (count) refresh();
      else open();
    };
    el("camera-close").onclick = close;
    el("camera-reconnect").onclick = function () {
      play(expanded, quality);
    };
    el("camera-quality").onclick = function () {
      play(expanded, quality === "main" ? "sub" : "main");
    };
    el("camera-fit").onclick = function () {
      fit = fit === "cover" ? "contain" : "cover";
      try {
        localStorage.setItem("owntv.camera.fit", fit);
      } catch (e) {}
      play(expanded, quality);
      label(
        "camera-fit",
        fit === "cover" ? "Ajustar imagem" : "Preencher tela"
      );
      Array.prototype.forEach.call(
        el("camera-grid").querySelectorAll("img"),
        function (im) {
          framePreview(im);
        }
      );
    };
    el("camera-audio").onclick = function () {
      var v = el("camera-video");
      v.muted = !v.muted;
      label("camera-audio", v.muted ? "Ativar áudio" : "Silenciar");
    };
    el("camera-pause").onclick = function () {
      var v = el("camera-video");
      if (v.paused) {
        v.play();
        label("camera-pause", "Pausar");
      } else {
        v.pause();
        label("camera-pause", "Reproduzir");
      }
    };
    var video = el("camera-video");
    video.onplaying = function () {
      hasPlayed = true;
      lastProgress = Date.now();
      videoStatus(
        "Ao vivo · " +
          (video.videoWidth
            ? video.videoWidth + " × " + video.videoHeight
            : "vídeo conectado")
      );
    };
    video.onwaiting = function () {
      if (expanded) videoStatus("Carregando vídeo…");
    };
    video.onerror = function () {
      if (!expanded) return;
      if (retryCount < 2) {
        retryCount++;
        videoStatus("Preparando o vídeo · reconectando…");
        retryTimer = setTimeout(function () {
          if (expanded && active) play(expanded, quality, true);
        }, retryCount * 2500);
      } else videoStatus("Vídeo indisponível. Tente Reconectar ou Modo leve.");
    };
    document.addEventListener("visibilitychange", function () {
      if (document.hidden) {
        clearInterval(timer);
        stopRequests();
        if (expanded) video.pause();
      } else if (active) {
        if (expanded) play(expanded, quality);
        else {
          refresh();
          clearInterval(timer);
          timer = setInterval(refresh, 3000);
        }
      }
    });
  }
  window.OwnTVCameras = {
    init: init,
    open: open,
    leave: leave,
    key: key,
    disconnect: disconnect
  };
})();
