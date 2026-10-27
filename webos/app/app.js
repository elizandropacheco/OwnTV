(function () {
  "use strict";
  var model = window.OwnTVModel,
    state = model.read(localStorage),
    page = "home",
    appsMode = "open",
    appsOffset = 0,
    favoriteOffset = 0,
    movingId = null,
    appsStatus = "",
    toastTimer,
    lastTile = null,
    photoIndex = state.wallpaper;
  function el(id) {
    return document.getElementById(id);
  }
  function each(items, fn) {
    Array.prototype.forEach.call(items, fn);
  }
  function toast(text) {
    clearTimeout(toastTimer);
    el("toast").textContent = text;
    el("toast").hidden = false;
    toastTimer = setTimeout(function () {
      el("toast").hidden = true;
    }, 6500);
  }
  function save(next) {
    try {
      state = model.write(localStorage, next);
      photoIndex = state.wallpaper;
      apply();
    } catch (e) {
      toast(
        "Não foi possível salvar. Suas escolhas anteriores foram mantidas."
      );
    }
  }
  function item(id) {
    return model.appsFor(state).filter(function (a) {
      return a.id === id;
    })[0];
  }
  function firstTile() {
    var b = el("shortcuts").querySelector("button");
    if (b) b.focus();
    else document.querySelector('[data-page="favorites"]').focus();
  }
  function selectedNav() {
    return document.querySelector("nav .selected") || el("home-nav");
  }
  function show(next) {
    page = next;
    movingId = null;
    each(document.querySelectorAll(".page"), function (p) {
      p.hidden = true;
    });
    each(document.querySelectorAll("nav button"), function (b) {
      b.classList.toggle("selected", b.getAttribute("data-page") === next);
    });
    var contentHandled = window.OwnTVContent && window.OwnTVContent.show(next);
    if (contentHandled) {
      return;
    }
    if (next === "home") {
      el("home").hidden = false;
      firstTile();
    } else if (next === "apps" || next === "favorites") {
      el("apps-page").hidden = false;
      appsMode = next === "favorites" ? "select" : "open";
      appsOffset = 0;
      renderApps();
      (el("apps-items").firstChild || el("apps-refresh")).focus();
    } else if (next === "settings") {
      el("settings").hidden = false;
      el("brand-option").focus();
    } else if (next === "about") {
      el("about").hidden = false;
      el("about").querySelector("button").focus();
    } else {
      el("category").hidden = false;
      renderCategory(next);
      var first = el("category-items").querySelector("button");
      if (first) first.focus();
      else el("category").querySelector("button").focus();
    }
  }
  function photo() {
    var entry = window.OwnTVWallpapers[photoIndex];
    el("wallpaper").style.backgroundImage = entry
      ? 'url("' + entry.file + '")'
      : "none";
    var level = [0.2, 0.35, 0.5, 0.65, 0.8][state.dim];
    el("shade").style.background =
      "linear-gradient(180deg,rgba(3,11,18," +
      Math.min(1, level + 0.15) +
      "),rgba(3,11,18," +
      Math.max(0, level - 0.18) +
      "),rgba(3,11,18," +
      Math.min(1, level + 0.26) +
      "))";
    el("wallpaper-value").textContent = entry ? entry.label : "Fundo escuro";
    el("photo-credit").textContent = entry
      ? "Foto: " + entry.author + " · " + entry.license + " · " + entry.source
      : "";
  }
  function apply() {
    el("brand").src = "assets/" + state.brand + ".png";
    el("splash").querySelector("img").src = "assets/" + state.brand + ".png";
    el("brand-value").textContent =
      state.brand === "ep" ? "Elizandro Pacheco" : "Monograma OwnTV";
    el("color-value").textContent = state.colored
      ? "Coloridos (padrão)"
      : "Monocromáticos";
    el("dim-value").textContent = [
      "Suave",
      "Leve",
      "Equilibrado",
      "Escuro",
      "Muito escuro"
    ][state.dim];
    el("rotate-value").textContent = state.rotate
      ? "A cada 1 minuto"
      : "Desativado";
    el("hdmi-value").textContent = state.hdmi
      ? "HDMI " + state.hdmi
      : "Escolher ao abrir";
    el("console-caption").textContent = state.hdmi
      ? "Abrir HDMI " + state.hdmi
      : "Escolha sua entrada HDMI";
    el("effects-value").textContent = state.effects
      ? "Transições suaves"
      : "Sem animações";
    document.body.classList.toggle("mono", !state.colored);
    document.body.classList.toggle("no-effects", !state.effects);
    photo();
    renderFavorites();
  }
  function renderTiles(host, ids) {
    host.textContent = "";
    ids.forEach(function (id) {
      var a = item(id);
      if (!a) return;
      var b = document.createElement("button"),
        icon = document.createElement("span"),
        text = document.createElement("span"),
        name = document.createElement("strong"),
        desc = document.createElement("small");
      b.className = "tile" + (a.nativeIcon ? " native-app" : "");
      b.setAttribute("data-id", id);
      b.setAttribute("aria-label", a.name);
      icon.className = "tile-icon";
      if (a.icon) {
        var image = document.createElement("img");
        image.src = a.icon;
        image.alt = "";
        image.onerror = function () {
          icon.textContent = a.glyph;
        };
        icon.appendChild(image);
      } else icon.textContent = a.glyph;
      name.textContent = a.name;
      desc.textContent = a.detail;
      text.appendChild(name);
      text.appendChild(desc);
      b.appendChild(icon);
      b.appendChild(text);
      b.addEventListener("click", function () {
        lastTile = id;
        if (host.id === "apps-items" && appsMode === "select") {
          var next = model.toggle(state, id);
          save(next);
          renderApps(id);
        } else if (host.id === "apps-items" && appsMode === "order") {
          movingId = movingId === id ? null : id;
          renderApps(id);
        } else launch(a.id, a.name);
      });
      host.appendChild(b);
    });
  }
  function renderFavorites() {
    var focused = document.activeElement,
      restore =
        focused && focused.parentNode && focused.parentNode.id === "shortcuts"
          ? focused.getAttribute("data-id")
          : null;
    favoriteOffset = Math.max(
      0,
      Math.min(
        favoriteOffset,
        Math.floor(Math.max(0, state.order.length - 1) / 4) * 4
      )
    );
    renderTiles(
      el("shortcuts"),
      state.order.slice(favoriteOffset, favoriteOffset + 4)
    );
    el("favorites-count").textContent = state.order.length
      ? favoriteOffset +
        1 +
        "–" +
        Math.min(favoriteOffset + 4, state.order.length) +
        " / " +
        state.order.length
      : "Escolha seus apps";
    el("favorites-prev").disabled = favoriteOffset === 0;
    el("favorites-next").disabled = favoriteOffset + 4 >= state.order.length;
    if (restore) {
      var previous = el("shortcuts").querySelector(
        '[data-id="' + restore + '"]'
      );
      if (previous) previous.focus();
    }
    if (!state.order.length) {
      var b = document.createElement("button");
      b.className = "tile";
      b.textContent = "＋ Adicionar favoritos";
      b.onclick = function () {
        show("favorites");
      };
      el("shortcuts").appendChild(b);
    }
  }
  function appIds() {
    return appsMode === "order"
      ? state.order
      : model.appsFor(state).map(function (a) {
          return a.id;
        });
  }
  function renderApps(focusId) {
    var ids = appIds();
    appsOffset = Math.max(
      0,
      Math.min(appsOffset, Math.floor(Math.max(0, ids.length - 1) / 9) * 9)
    );
    el("apps-title").textContent =
      appsMode === "select"
        ? "Escolher favoritos"
        : appsMode === "order"
          ? "Ordenar favoritos"
          : "Todos os apps";
    el("apps-help").textContent =
      appsMode === "select"
        ? "OK adiciona ou remove. ★ indica favorito. Suas escolhas são salvas automaticamente."
        : appsMode === "order"
          ? movingId
            ? "Movendo: use as setas. OK confirma a posição; Voltar solta o item."
            : "Escolha um favorito e pressione OK para mover. Depois use as setas e OK para concluir."
          : "OK abre o aplicativo. Use Escolher favoritos para personalizar o início.";
    el("apps-status").textContent =
      appsStatus ||
      (state.apps.length
        ? state.apps.length + " aplicativos encontrados nesta TV"
        : "Atualize a lista para descobrir os aplicativos instalados nesta TV.");
    renderTiles(el("apps-items"), ids.slice(appsOffset, appsOffset + 9));
    each(el("apps-items").querySelectorAll("button"), function (b) {
      var id = b.getAttribute("data-id"),
        selected = state.order.indexOf(id) >= 0;
      b.classList.toggle("favorite-selected", selected);
      b.classList.toggle("moving", movingId === id);
      if (appsMode === "select")
        b.setAttribute("aria-pressed", String(selected));
      b.querySelector("small").textContent =
        appsMode === "order"
          ? "Posição " +
            (state.order.indexOf(id) + 1) +
            (movingId === id ? " · movendo" : " · OK mover")
          : selected
            ? "★ Favorito"
            : appsMode === "select"
              ? "＋ Adicionar"
              : "OK abrir";
    });
    el("apps-page-number").textContent = ids.length
      ? appsOffset +
        1 +
        "–" +
        Math.min(appsOffset + 9, ids.length) +
        " de " +
        ids.length
      : "Nenhum favorito — use Escolher favoritos";
    el("apps-prev").disabled = appsOffset === 0;
    el("apps-next").disabled = appsOffset + 9 >= ids.length;
    ["open", "select", "order"].forEach(function (mode) {
      el("apps-" + mode).classList.toggle("selected", appsMode === mode);
    });
    if (focusId) {
      var b = el("apps-items").querySelector('[data-id="' + focusId + '"]');
      if (b) b.focus();
    }
  }
  function refreshApps(interactive) {
    appsStatus = "Consultando os aplicativos desta TV…";
    if (page === "apps" || page === "favorites") renderApps();
    window.OwnTVApps.discover(
      interactive,
      function (message) {
        appsStatus = message;
        if (page === "apps" || page === "favorites") renderApps();
      },
      function (error, apps) {
        var active = document.activeElement,
          focusId =
            active && active.parentNode.id === "apps-items"
              ? active.getAttribute("data-id")
              : null;
        if (error) appsStatus = error;
        else {
          save(model.updateApps(state, apps));
          appsStatus =
            state.apps.length + " aplicativos encontrados · lista atualizada";
        }
        if (page === "apps" || page === "favorites") renderApps(focusId);
        if (interactive && error) toast(error);
      }
    );
  }
  ["open", "select", "order"].forEach(function (mode) {
    el("apps-" + mode).onclick = function () {
      appsMode = mode;
      appsOffset = 0;
      movingId = null;
      renderApps();
      (el("apps-items").firstChild || el("apps-select")).focus();
    };
  });
  el("apps-refresh").onclick = function () {
    refreshApps(true);
  };
  [-1, 1].forEach(function (delta) {
    el(delta < 0 ? "favorites-prev" : "favorites-next").onclick = function () {
      favoriteOffset += delta * 4;
      renderFavorites();
      firstTile();
    };
    el(delta < 0 ? "apps-prev" : "apps-next").onclick = function () {
      appsOffset += delta * 9;
      renderApps();
      if (el("apps-items").firstChild) el("apps-items").firstChild.focus();
    };
  });
  function launch(id, name) {
    if (id === "com.webos.app.inputpicker") {
      show("inputs");
      return;
    }
    if (id === "settings") {
      show("settings");
      return;
    }
    if (!window.PalmServiceBridge && !window.WebOSServiceBridge) {
      toast(
        "Este atalho abre " + name + " quando o OwnTV está instalado na LG."
      );
      return;
    }
    webOSDev.launch({
      id: id,
      params: {},
      onSuccess: function () {},
      onFailure: function () {
        toast(
          "Não foi possível abrir " +
            name +
            ". Confira se o aplicativo está disponível nesta TV."
        );
      }
    });
  }
  function renderCategory(which) {
    if (which === "inputs") {
      renderInputs();
      return;
    }
    var titles = {
        movies: "Filmes",
        music: "Música",
        games: "Jogos",
        house: "Casa",
        apps: "Todos os apps"
      },
      ids = [],
      note = "";
    el("category-title").textContent = titles[which] || "Aplicativos";
    el("category-description").textContent = "Atalhos desta versão piloto.";
    if (which === "movies")
      ids = ["youtube.leanback.v4", "com.webos.app.livetv"];
    if (which === "games") ids = ["com.webos.app.inputpicker"];
    if (which === "music")
      note =
        "Os aplicativos de música ainda não foram integrados neste piloto.";
    if (which === "house")
      note =
        "A página de câmeras está em preparação para webOS. Nenhuma configuração das suas câmeras foi importada para este aplicativo.";
    el("category-note").hidden = !note;
    el("category-note").textContent = note;
    renderTiles(el("category-items"), ids);
  }
  function renderInputs() {
    el("category-title").textContent = "Entradas";
    el("category-description").textContent =
      "Escolha HDMI 1, 2 ou 3, ou abra a central de entradas da LG.";
    el("category-note").hidden = true;
    var host = el("category-items");
    host.textContent = "";
    [
      { id: "com.webos.app.hdmi1", name: "HDMI 1" },
      { id: "com.webos.app.hdmi2", name: "HDMI 2" },
      { id: "com.webos.app.hdmi3", name: "HDMI 3" },
      { id: "com.webos.app.homeconnect", name: "Central de entradas" }
    ].forEach(function (input) {
      var b = document.createElement("button");
      b.className = "tile";
      b.setAttribute("data-input", input.id);
      var icon = document.createElement("span");
      icon.className = "tile-icon";
      var im = document.createElement("img");
      im.src = "assets/input.svg";
      im.alt = "";
      icon.appendChild(im);
      b.appendChild(icon);
      var label = document.createElement("strong");
      label.textContent = input.name;
      b.appendChild(label);
      b.addEventListener("click", function () {
        launch(input.id, input.name);
      });
      host.appendChild(b);
    });
  }
  each(document.querySelectorAll("[data-page]"), function (b) {
    b.addEventListener("click", function () {
      show(b.getAttribute("data-page"));
    });
  });
  each(document.querySelectorAll("[data-action]"), function (b) {
    b.addEventListener("click", function () {
      var action = b.getAttribute("data-action"),
        next = model.normalize(state);
      if (action === "console") {
        launch(
          state.hdmi
            ? "com.webos.app.hdmi" + state.hdmi
            : "com.webos.app.inputpicker",
          state.hdmi ? "HDMI " + state.hdmi : "Entradas"
        );
        return;
      }
      if (action === "games-settings") {
        show("settings");
        document.querySelector('[data-action="hdmi"]').focus();
        return;
      }
      if (action === "order") {
        show("favorites");
        return;
      }
      if (action === "brand")
        next.brand = state.brand === "ep" ? "owntv" : "ep";
      if (action === "colors") next.colored = !state.colored;
      if (action === "wallpaper")
        next.wallpaper =
          state.wallpaper >= window.OwnTVWallpapers.length - 1
            ? -1
            : state.wallpaper + 1;
      if (action === "dim") next.dim = (state.dim + 1) % 5;
      if (action === "rotate") next.rotate = !state.rotate;
      if (action === "effects") next.effects = !state.effects;
      if (action === "hdmi") next.hdmi = (state.hdmi + 1) % 4;
      save(next);
      b.focus();
    });
  });
  function nearby(code) {
    var current = document.activeElement,
      rect = current.getBoundingClientRect(),
      cx = rect.left + rect.width / 2,
      cy = rect.top + rect.height / 2,
      best = null,
      score = Infinity;
    each(document.querySelectorAll("button, input, textarea"), function (b) {
      if (b === current || b.disabled || !b.offsetWidth || !b.offsetHeight)
        return;
      var r = b.getBoundingClientRect(),
        dx = r.left + r.width / 2 - cx,
        dy = r.top + r.height / 2 - cy,
        along = code === 37 ? -dx : code === 39 ? dx : code === 38 ? -dy : dy,
        cross = code === 37 || code === 39 ? Math.abs(dy) : Math.abs(dx);
      if (along > 8) {
        var value = along + cross * 4;
        if (value < score) {
          score = value;
          best = b;
        }
      }
    });
    if (best) best.focus();
  }
  document.addEventListener("keydown", function (e) {
    if (window.OwnTVContent && window.OwnTVContent.key(e)) {
      e.preventDefault();
      return;
    }
    var code = e.keyCode,
      current = document.activeElement;
    if (code === 461 || code === 27) {
      e.preventDefault();
      if (movingId) {
        var dropped = movingId;
        movingId = null;
        renderApps(dropped);
      } else if (page !== "home") show("home");
      else toast("Use Home no controle para voltar à tela da LG.");
      return;
    }
    if (code < 37 || code > 40) return;
    if (
      (current.tagName === "INPUT" || current.tagName === "TEXTAREA") &&
      (code === 37 || code === 39)
    )
      return;
    e.preventDefault();
    if ((page === "apps" || page === "favorites") && movingId) {
      save(
        model.move(
          state,
          movingId,
          code === 37 ? -1 : code === 39 ? 1 : code === 38 ? -3 : 3
        )
      );
      appsOffset = Math.floor(state.order.indexOf(movingId) / 9) * 9;
      renderApps(movingId);
      return;
    }
    if (
      (page === "apps" || page === "favorites") &&
      current.parentNode.id === "apps-items"
    ) {
      var allIds = appIds(),
        at = allIds.indexOf(current.getAttribute("data-id"));
      var nextAt =
        at + (code === 37 ? -1 : code === 39 ? 1 : code === 38 ? -3 : 3);
      if (nextAt >= 0 && nextAt < allIds.length) {
        appsOffset = Math.floor(nextAt / 9) * 9;
        renderApps(allIds[nextAt]);
        return;
      }
    }
    if (page === "home") {
      if (
        (code === 37 || code === 39) &&
        current.parentNode.id === "shortcuts"
      ) {
        var pos =
          state.order.indexOf(current.getAttribute("data-id")) +
          (code === 37 ? -1 : 1);
        if (pos >= 0 && pos < state.order.length) {
          favoriteOffset = Math.floor(pos / 4) * 4;
          renderFavorites();
          el("shortcuts")
            .querySelector('[data-id="' + state.order[pos] + '"]')
            .focus();
        }
        return;
      }
      if (code === 40 && current.parentNode.tagName === "NAV") {
        favoriteOffset = 0;
        renderFavorites();
        firstTile();
        return;
      }
      if (code === 40 && current.parentNode.id === "shortcuts") {
        var favorites = Array.prototype.slice.call(
          el("shortcuts").querySelectorAll("button")
        );
        var suggested = document.querySelectorAll("#home-video-items button");
        var target = suggested[favorites.indexOf(current)] || suggested[0];
        if (target) {
          target.focus();
          return;
        }
      }
      if (code === 38 && current.parentNode.id === "shortcuts") {
        el("camera-card").focus();
        return;
      }
      if (current.id === "camera-card" && (code === 38 || code === 40)) {
        if (code === 38) el("games-card").focus();
        else firstTile();
        return;
      }
      if (current.id === "games-card" && (code === 38 || code === 40)) {
        if (code === 38) selectedNav().focus();
        else el("camera-card").focus();
        return;
      }
    }
    nearby(code);
  });
  function clock() {
    var now = new Date(),
      days = ["dom.", "seg.", "ter.", "qua.", "qui.", "sex.", "sáb."];
    el("time").textContent =
      ("0" + now.getHours()).slice(-2) +
      ":" +
      ("0" + now.getMinutes()).slice(-2);
    el("date").textContent =
      days[now.getDay()] +
      " " +
      ("0" + now.getDate()).slice(-2) +
      "/" +
      ("0" + (now.getMonth() + 1)).slice(-2) +
      "/" +
      now.getFullYear();
  }
  function resize() {
    var scale = Math.min(window.innerWidth / 1920, window.innerHeight / 1080);
    el("stage").style.transform = "scale(" + scale + ")";
    el("stage").style.left =
      Math.max(0, (window.innerWidth - 1920 * scale) / 2) + "px";
    el("stage").style.top =
      Math.max(0, (window.innerHeight - 1080 * scale) / 2) + "px";
  }
  window.addEventListener("resize", resize);
  document.addEventListener("webOSRelaunch", function () {
    show("home");
  });
  document.addEventListener("visibilitychange", function () {
    if (!document.hidden) {
      clock();
      if (page === "home") {
        var b = el("shortcuts").querySelector('[data-id="' + lastTile + '"]');
        if (b) b.focus();
      }
    }
  });
  window.OwnTVContent.init({ show: show, toast: toast });
  apply();
  resize();
  clock();
  setInterval(clock, 30000);
  setInterval(function () {
    if (state.rotate && !document.hidden) {
      photoIndex = (photoIndex + 1) % window.OwnTVWallpapers.length;
      photo();
    }
  }, 60000);
  show("home");
  refreshApps(false);
  setTimeout(function () {
    el("splash").classList.add("fade");
    setTimeout(function () {
      el("splash").hidden = true;
    }, 300);
  }, 650);
})();
