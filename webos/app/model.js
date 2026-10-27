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
  function normalize(raw) {
    raw = raw || {};
    var ids = catalog.map(function (a) {
      return a.id;
    });
    var order = [];
    (Array.isArray(raw.order) ? raw.order : []).forEach(function (id) {
      if (ids.indexOf(id) >= 0 && order.indexOf(id) < 0) order.push(id);
    });
    ids.forEach(function (id) {
      if (order.indexOf(id) < 0) order.push(id);
    });
    return {
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
    move: move,
    read: read,
    write: write
  };
  if (typeof module === "object" && module.exports) module.exports = api;
  else root.OwnTVModel = api;
})(this);
