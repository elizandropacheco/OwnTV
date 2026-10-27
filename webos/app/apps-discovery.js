/* TV-local SSAP app inventory. Credentials stay in the private service. ES5. */
(function (root) {
  "use strict";
  var pending = [],
    busy = false;
  function call(method, data, done) {
    var Bridge = root.PalmServiceBridge || root.WebOSServiceBridge;
    if (!Bridge) {
      done({ returnValue: false });
      return;
    }
    var bridge = new Bridge(),
      finished = false;
    pending.push(bridge);
    var timer = setTimeout(function () {
      finish({ returnValue: false });
    }, 10000);
    function finish(value) {
      if (finished) return;
      finished = true;
      clearTimeout(timer);
      pending.splice(pending.indexOf(bridge), 1);
      done(value);
    }
    bridge.onservicecallback = function (raw) {
      try {
        finish(JSON.parse(raw));
      } catch (_) {
        finish({ returnValue: false });
      }
    };
    bridge.call(
      "luna://io.elizandropacheco.owntv.webos.content/" + method,
      JSON.stringify(data)
    );
  }
  function discover(interactive, status, done) {
    if (busy) return;
    busy = true;
    var socket,
      timer,
      finished = false;
    function finish(error, apps) {
      if (finished) return;
      finished = true;
      busy = false;
      clearTimeout(timer);
      if (socket) socket.close();
      done(error, apps);
    }
    call("appsConnection", {}, function (config) {
      if (!config.returnValue || !config.hosts || !config.hosts.length) {
        finish("Não foi possível obter o endereço desta TV.");
        return;
      }
      if (!config.key && !interactive) {
        finish("Escolha Atualizar apps e permita a conexão na TV.");
        return;
      }
      var index = 0;
      function connect() {
        if (index >= config.hosts.length) {
          finish(
            "Não foi possível conectar à TV. Confira a rede e tente Atualizar apps."
          );
          return;
        }
        var host = config.hosts[index++];
        if (!/^\d+\.\d+\.\d+\.\d+$/.test(host)) {
          connect();
          return;
        }
        var current;
        try {
          current = new WebSocket("ws://" + host + ":3000");
        } catch (_) {
          connect();
          return;
        }
        socket = current;
        clearTimeout(timer);
        timer = setTimeout(
          function () {
            finish(
              "A conexão não foi autorizada a tempo. Tente Atualizar apps novamente."
            );
          },
          interactive ? 180000 : 15000
        );
        current.onopen = function () {
          var payload = {
            forcePairing: false,
            pairingType: "PROMPT",
            manifest: {
              manifestVersion: 1,
              appVersion: "0.2.1",
              permissions: [
                "READ_INSTALLED_APPS",
                "READ_RUNNING_APPS",
                "READ_APP_STATUS",
                "LAUNCH"
              ]
            }
          };
          if (config.key) payload["client-key"] = config.key;
          current.send(
            JSON.stringify({
              type: "register",
              id: "register",
              payload: payload
            })
          );
        };
        current.onmessage = function (e) {
          if (finished || socket !== current) return;
          if (e.data.length > 1048576) {
            finish("Resposta de aplicativos inválida.");
            return;
          }
          var m;
          try {
            m = JSON.parse(e.data);
          } catch (_) {
            finish("Resposta inválida da TV.");
            return;
          }
          if (m.type === "registered") {
            if (
              !m.payload ||
              typeof m.payload["client-key"] !== "string" ||
              !/^[A-Za-z0-9_-]{8,256}$/.test(m.payload["client-key"])
            ) {
              finish("Pareamento inválido recebido da TV.");
              return;
            }
            call(
              "setAppsKey",
              { key: m.payload["client-key"] },
              function (saved) {
                if (finished) return;
                if (!saved.returnValue) {
                  finish("Não foi possível salvar o pareamento.");
                  return;
                }
                current.send(
                  JSON.stringify({
                    type: "request",
                    id: "apps",
                    uri: "ssap://com.webos.applicationManager/listLaunchPoints",
                    payload: {}
                  })
                );
              }
            );
          } else if (m.payload && m.payload.pairingType) {
            if (!interactive) {
              finish("Pareamento necessário. Escolha Atualizar apps.");
              return;
            }
            status("Permita a conexão de controle remoto no aviso da TV.");
          } else if (m.type === "error")
            finish(
              "A LG recusou a consulta. Tente Atualizar apps e permitir novamente."
            );
          else if (m.id === "apps") {
            if (
              !m.payload ||
              !Array.isArray(m.payload.launchPoints) ||
              m.payload.returnValue === false
            )
              finish("A LG não forneceu a lista de aplicativos.");
            else finish(null, m.payload.launchPoints);
          }
        };
        current.onerror = function () {
          if (!finished && socket === current) {
            socket = null;
            current.close();
            connect();
          }
        };
        current.onclose = function () {
          if (!finished && socket === current)
            finish(
              "A conexão com a TV foi encerrada. Seus favoritos foram mantidos."
            );
        };
      }
      connect();
    });
  }
  root.OwnTVApps = { discover: discover };
})(this);
