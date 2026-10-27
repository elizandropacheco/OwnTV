/* OwnTV webOS pilot. GPL-3.0; no device data is bundled. */
(function (root) {
  "use strict";
  var catalog = [
    {
      id: "youtube.leanback.v4",
      name: "YouTube",
      detail: "Vídeos e canais",
      icon: "assets/youtube.svg",
      glyph: "▶"
    },
    {
      id: "com.webos.app.livetv",
      name: "TV ao vivo",
      detail: "Seus canais",
      icon: "assets/tv.svg",
      glyph: "TV"
    },
    {
      id: "com.webos.app.inputpicker",
      name: "Entradas",
      detail: "HDMI e dispositivos",
      icon: "assets/input.svg",
      glyph: "HDMI"
    },
    {
      id: "settings",
      name: "Personalizar",
      detail: "Deixe do seu jeito",
      icon: "assets/settings.svg",
      glyph: "EP"
    }
  ];
  function cleanApps(rows) {
    var result = [],
      seen = {};
    (Array.isArray(rows) ? rows : []).slice(0, 200).forEach(function (a) {
      if (
        !a ||
        !/^[A-Za-z0-9][A-Za-z0-9._-]{0,159}$/.test(a.id) ||
        seen[a.id] ||
        a.id === "io.elizandropacheco.owntv.webos" ||
        a.visible === false
      )
        return;
      seen[a.id] = true;
      var name = String(a.title || a.name || a.id).slice(0, 100);
      var icon = String(a.icon || "");
      if (
        !/^http:\/\/\d+\.\d+\.\d+\.\d+:3000\/resources\/[A-Za-z0-9_./%-]+$/.test(
          icon
        )
      )
        icon = "";
      result.push({
        id: a.id,
        name: name,
        detail: "Aplicativo instalado",
        icon: icon,
        glyph: name.slice(0, 2).toUpperCase(),
        nativeIcon: !!icon
      });
    });
    return result;
  }
  function appsFor(raw) {
    var apps = cleanApps(raw && raw.apps),
      ids = apps.map(function (a) {
        return a.id;
      });
    catalog.forEach(function (a) {
      if (ids.indexOf(a.id) < 0) apps.push(a);
    });
    return apps;
  }
  function normalize(raw) {
    raw = raw || {};
    var ids = appsFor(raw).map(function (a) {
      return a.id;
    });
    var order = [];
    (Array.isArray(raw.order) ? raw.order : []).forEach(function (id) {
      if (ids.indexOf(id) >= 0 && order.indexOf(id) < 0) order.push(id);
    });
    // Legacy releases always had four shortcuts. v2 allows any subset, including empty.
    if (raw.schema !== 2)
      catalog.forEach(function (a) {
        if (order.indexOf(a.id) < 0) order.push(a.id);
      });
    return {
      schema: 2,
      apps: cleanApps(raw.apps),
      brand: raw.brand === "owntv" ? "owntv" : "ep",
      colored: raw.colored !== false,
      order: order,
      wallpaper:
        typeof raw.wallpaper === "number" &&
        raw.wallpaper >= -1 &&
        raw.wallpaper < 15
          ? Math.floor(raw.wallpaper)
          : 0,
      dim: [0, 1, 2, 3, 4].indexOf(raw.dim) >= 0 ? raw.dim : 2,
      rotate: raw.rotate !== false,
      effects: raw.effects !== false,
      hdmi: [0, 1, 2, 3].indexOf(raw.hdmi) >= 0 ? raw.hdmi : 0
    };
  }
  function move(state, id, delta) {
    var copy = normalize(state),
      i = copy.order.indexOf(id),
      j = i + delta;
    if (i >= 0 && j >= 0 && j < copy.order.length) {
      copy.order.splice(i, 1);
      copy.order.splice(j, 0, id);
    }
    return copy;
  }
  function toggle(state, id) {
    var copy = normalize(state),
      index = copy.order.indexOf(id);
    if (
      appsFor(copy).every(function (a) {
        return a.id !== id;
      })
    )
      return copy;
    if (index >= 0) copy.order.splice(index, 1);
    else copy.order.push(id);
    return copy;
  }
  function updateApps(state, rows) {
    var copy = normalize(state);
    copy.apps = cleanApps(rows);
    return normalize(copy);
  }
  function read(storage) {
    try {
      return normalize(JSON.parse(storage.getItem("owntv.webos.preferences")));
    } catch (e) {
      return normalize();
    }
  }
  function write(storage, state) {
    var clean = normalize(state);
    storage.setItem("owntv.webos.preferences", JSON.stringify(clean));
    return clean;
  }
  var api = {
    catalog: catalog,
    normalize: normalize,
    appsFor: appsFor,
    cleanApps: cleanApps,
    toggle: toggle,
    updateApps: updateApps,
    move: move,
    read: read,
    write: write
  };
  if (typeof module === "object" && module.exports) module.exports = api;
  else root.OwnTVModel = api;
})(this);
