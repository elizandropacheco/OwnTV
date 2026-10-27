const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");
const methods = {},
  files = {},
  writes = [];
const context = {
  require(name) {
    if (name === "webos-service")
      return function () {
        this.register = (name, fn) => (methods[name] = fn);
      };
    if (name === "./content") return require("../service/content");
    if (name === "./package.json") return require("../service/package.json");
    if (name === "fs")
      return {
        existsSync: (name) => name in files,
        mkdirSync: (name, mode) => {
          writes.push(mode);
          files[name] = null;
        },
        writeFileSync: (name, data, options) => {
          files[name] = data;
          writes.push(options.mode);
        },
        readFileSync: (name) => {
          if (!(name in files)) throw Error("missing");
          return files[name];
        },
        chmodSync: (name, mode) => writes.push(mode),
        unlinkSync: (name) => delete files[name]
      };
    return require(name);
  },
  __dirname: "/private-service",
  process,
  Buffer,
  setTimeout,
  clearTimeout
};
vm.runInNewContext(
  fs.readFileSync(require.resolve("../service/service"), "utf8"),
  context
);
const invoke = (
  method,
  payload,
  sender = "io.elizandropacheco.owntv.webos"
) => {
  let response;
  methods[method]({ sender, payload, respond: (value) => (response = value) });
  return response;
};
const secret = "a".repeat(40);
assert.equal(
  invoke("setBridge", { url: "http://192.168.1.2:8788", token: secret })
    .returnValue,
  true
);
assert.ok(writes.includes(0o700));
assert.ok(writes.includes(0o600));
assert.equal(invoke("bridge", {}).configured, true);
for (const method of ["bridge", "setBridge", "clearBridge", "news", "videos"]) {
  const result = invoke(method, {}, "unrelated.app");
  assert.equal(result.returnValue, false);
  assert.equal(JSON.stringify(result).includes(secret), false);
}
assert.equal(
  invoke("setBridge", { url: "http://evil.invalid", token: secret })
    .returnValue,
  false
);
assert.equal(
  invoke("setBridge", { url: "http://192.168.1.2:8788", token: "short" })
    .returnValue,
  false
);
assert.equal(invoke("bridge", {}).token, secret);
assert.equal(invoke("clearBridge", {}).returnValue, true);
assert.equal(invoke("bridge", {}).configured, false);
console.log(
  "Service caller isolation, private file permissions, validation and disconnect passed."
);
assert.equal(
  invoke("setAppsKey", { key: "valid-pairing-key" }).returnValue,
  true
);
assert.equal(invoke("appsConnection", {}).key, "valid-pairing-key");
for (const method of ["appsConnection", "setAppsKey"]) {
  const result = invoke(method, { key: "attacker-key" }, "unrelated.app");
  assert.equal(result.returnValue, false);
  assert.equal(JSON.stringify(result).includes("valid-pairing-key"), false);
}
assert.equal(invoke("setAppsKey", { key: "../../bad" }).returnValue, false);
assert.equal(invoke("appsConnection", {}).key, "valid-pairing-key");
console.log("TV-local pairing validation and caller isolation passed.");
