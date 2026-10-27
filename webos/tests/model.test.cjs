const assert = require("node:assert/strict");
const m = require("../app/model.js");
const empty = m.normalize();
assert.equal(empty.brand, "ep");
assert.equal(empty.colored, true);
assert.equal(empty.rotate, true);
assert.equal(m.normalize({rotate:false}).rotate, false);
assert.deepEqual(
  m.normalize({ order: ["bogus", empty.order[2], empty.order[2]] }).order,
  [empty.order[2], empty.order[0], empty.order[1], empty.order[3]],
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
  },
};
assert.deepEqual(m.read(storage), empty);
m.write(storage, { brand: "owntv", colored: false, order: moved.order });
assert.equal(m.read(storage).colored, false);
assert.deepEqual(m.read(storage).order, moved.order);
console.log(
  "Preferences: defaults, corrupt state, ordering bounds, deduplication and persistence passed.",
);
