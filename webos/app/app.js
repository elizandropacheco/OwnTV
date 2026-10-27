(function () {
  "use strict";
  var model = window.OwnTVModel,
    state = model.read(localStorage),
    page = "home",
    ordering = false,
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
        "Não foi possível salvar. Suas escolhas anteriores foram mantidas.",
      );
    }
  }
  function item(id) {
    return model.catalog.filter(function (a) {
      return a.id === id;
    })[0];
  }
  function firstTile() {
    var b = el("shortcuts").querySelector("button");
    if (b) b.focus();
  }
  function selectedNav() {
    return document.querySelector("nav .selected") || el("home-nav");
  }
  function show(next) {
    page = next;
    ordering = false;
    el("order-editor").hidden = true;
    each(document.querySelectorAll(".page"), function (p) {
      p.hidden = true;
    });
    each(document.querySelectorAll("nav button"), function (b) {
      b.classList.toggle("selected", b.getAttribute("data-page") === next);
    });
    if (next === "home") {
      el("home").hidden = false;
      firstTile();
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
      "Muito escuro",
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
    renderTiles(el("shortcuts"), state.order);
  }
  function renderTiles(host, ids) {
    host.textContent = "";
    ids.forEach(function (id) {
      var a = item(id),
        b = document.createElement("button"),
        icon = document.createElement("span"),
        text = document.createElement("span"),
        name = document.createElement("strong"),
        desc = document.createElement("small");
      b.className = "tile";
      b.setAttribute("data-id", id);
      b.setAttribute("aria-label", a.name);
      icon.className = "tile-icon";
      if (a.icon) {
        var image = document.createElement("img");
        image.src = a.icon;
        image.alt = "";
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
        launch(a.id, a.name);
      });
      host.appendChild(b);
    });
  }
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
        "Este atalho abre " + name + " quando o OwnTV está instalado na LG.",
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
            ". Confira se o aplicativo está disponível nesta TV.",
        );
      },
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
        apps: "Todos os apps",
      },
      ids = [],
      note = "";
    el("category-title").textContent = titles[which] || "Aplicativos";
    el("category-description").textContent = "Atalhos desta versão piloto.";
    if (which === "movies")
      ids = ["youtube.leanback.v4", "com.webos.app.livetv"];
    if (which === "games") ids = ["com.webos.app.inputpicker"];
    if (which === "apps") ids = state.order;
    if (which === "music")
      note =
        "Os aplicativos de música ainda não foram integrados neste piloto.";
    if (which === "house")
      note =
        "A página de câmeras está em preparação para webOS. Nenhuma configuração das suas câmeras foi importada para este aplicativo.";
    if (which === "apps")
      note =
        "Esta é a seleção de atalhos do piloto. A descoberta automática dos demais aplicativos ainda não está disponível.";
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
      { id: "com.webos.app.homeconnect", name: "Central de entradas" },
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
  function renderOrder(focusId) {
    var host = el("order-items");
    host.textContent = "";
    state.order.forEach(function (id) {
      var b = document.createElement("button");
      b.textContent = item(id).name;
      b.setAttribute("data-order", id);
      b.addEventListener("click", finishOrder);
      host.appendChild(b);
    });
    var target =
      host.querySelector('[data-order="' + focusId + '"]') || host.firstChild;
    if (target) target.focus();
  }
  function finishOrder() {
    ordering = false;
    el("order-editor").hidden = true;
    el("order-option").focus();
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
          state.hdmi ? "HDMI " + state.hdmi : "Entradas",
        );
        return;
      }
      if (action === "games-settings") {
        show("settings");
        document.querySelector('[data-action="hdmi"]').focus();
        return;
      }
      if (action === "order") {
        ordering = true;
        el("order-editor").hidden = false;
        renderOrder(state.order[0]);
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
    each(document.querySelectorAll("button"), function (b) {
      if (b === current || !b.offsetWidth || !b.offsetHeight) return;
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
    var code = e.keyCode,
      current = document.activeElement;
    if (code === 461 || code === 27) {
      e.preventDefault();
      if (ordering) finishOrder();
      else if (page !== "home") show("home");
      else toast("Use Home no controle para voltar à tela da LG.");
      return;
    }
    if (code < 37 || code > 40) return;
    e.preventDefault();
    if (ordering) {
      var id = current.getAttribute("data-order");
      if (id && (code === 37 || code === 39)) {
        save(model.move(state, id, code === 37 ? -1 : 1));
        renderOrder(id);
      } else if (code === 38) finishOrder();
      return;
    }
    if (page === "home") {
      if (code === 40 && current.parentNode.tagName === "NAV") {
        firstTile();
        return;
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
  setTimeout(function () {
    el("splash").classList.add("fade");
    setTimeout(function () {
      el("splash").hidden = true;
    }, 300);
  }, 650);
})();
