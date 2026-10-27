const assert = require("node:assert/strict");
const m = require("../app/model.js");
const empty = m.normalize();
assert.equal(empty.brand, "ep");
assert.equal(empty.colored, true);
assert.equal(empty.rotate, true);
assert.equal(m.normalize({ rotate: false }).rotate, false);
assert.deepEqual(
  m.normalize({ order: ["bogus", empty.order[2], empty.order[2]] }).order,
  [empty.order[2], empty.order[0], empty.order[1], empty.order[3]]
);
assert.deepEqual(m.move(empty, empty.order[0], -1), empty);
const moved = m.move(empty, empty.order[2], -1);
assert.equal(moved.order[1], empty.order[2]);
assert.notDeepEqual(moved.order, empty.order);
let data = "corrupt";
const storage = {
  getItem: () => data,
  setItem: (key, value) => {
    data = value;
  }
};
assert.deepEqual(m.read(storage), empty);
m.write(storage, { brand: "owntv", colored: false, order: moved.order });
assert.equal(m.read(storage).colored, false);
assert.deepEqual(m.read(storage).order, moved.order);
console.log(
  "Preferences: defaults, corrupt state, ordering bounds, deduplication and persistence passed."
);

const library = m.updateApps(m.normalize(), [
  {
    id: "netflix",
    title: "Netflix",
    icon: "http://192.168.1.20:3000/resources/icon.png"
  },
  { id: "amazon", title: "Prime Video" },
  { id: "netflix", title: "Duplicate" },
  { id: 'bad\"selector', title: "Invalid" },
  { id: "io.elizandropacheco.owntv.webos", title: "Self" },
  { id: "hidden.app", visible: false },
  { id: "unsafe.icon", icon: "javascript:alert(1)" }
]);
assert.equal(library.apps.length, 3);
assert.equal(library.apps[0].nativeIcon, true);
assert.equal(library.apps[2].icon, "");
let selection = m.toggle(library, "netflix");
assert.equal(selection.order.at(-1), "netflix");
selection = m.move(selection, "netflix", -4);
assert.equal(selection.order[0], "netflix");
m.write(storage, selection);
assert.deepEqual(m.read(storage), selection);
selection.order.slice().forEach((id) => (selection = m.toggle(selection, id)));
assert.deepEqual(m.normalize(selection).order, []);
m.write(storage, selection);
assert.deepEqual(m.read(storage).order, []);
assert.deepEqual(m.toggle(selection, "unknown").order, []);
selection = m.toggle(selection, "amazon");
assert.deepEqual(
  m.updateApps(selection, [{ id: "netflix", title: "Netflix" }]).order,
  []
);
assert.equal(library.order.includes("netflix"), false); // Discovery must not choose for the user.
console.log(
  "Installed-app discovery, filtering, add/remove, empty selection, migration and reload persistence passed."
);
