#!/usr/bin/env python3
"""Build map.bin: country outlines for the globe quiz.

Source: Natural Earth 10m admin_0_map_units (public domain), matched to our ISO codes.

Simplification is topology-preserving (see tools/topology.py). Simplifying each
country on its own pulls shared borders apart, which shows up as gaps and overlaps
between neighbours; shared arcs are therefore simplified once and reused by both
sides. Two levels of detail are produced: accurate outlines for hit-testing and
coarse ones for drawing a globe that has to stay smooth while it spins.

Output is binary, not JSON: ~100k points parse far faster this way on a phone.
"""
import json, math, os, struct, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import topology

QUANT = 100           # stored at 1/100 degree
GRID_HIT = 0.004      # snap grid for the hit-test level, ~440 m
GRID_DRAW = 0.01      # snap grid for the draw level, ~1.1 km
EPS_HIT = 0.02
EPS_DRAW = 0.08


def ring_area(r):
    s = 0.0
    for i in range(len(r)):
        x1, y1 = r[i]
        x2, y2 = r[(i + 1) % len(r)]
        s += x1 * y2 - x2 * y1
    return abs(s) / 2


def iso_code(p):
    for k in ("ISO_A2_EH", "ISO_A2", "ISO_A2_US"):
        v = p.get(k)
        if v and v != "-99":
            return v
    return None


def quantize(ring):
    out, px, py = [], 0, 0
    for x, y in ring:
        qx, qy = round(x * QUANT), round(y * QUANT)
        out.append(qx - px)
        out.append(qy - py)
        px, py = qx, qy
    return out


def write_binary(entries, path):
    """int magic 'MSPT', int version, int quant, int count; then per country:
    2 bytes ISO code, 4 shorts bbox, 2 shorts centre, float area, byte dot flag,
    then hit rings and draw rings (byte count + int point count + 2 shorts each,
    with -32768 escaping to a following int for the one Antarctic wrap)."""
    buf = bytearray()
    buf += struct.pack(">4sIII", b"MSPT", 1, QUANT, len(entries))
    for e in entries:
        buf += e["a2"].encode("ascii")
        buf += struct.pack(">4h", *[round(v * QUANT) for v in e["bbox"]])
        buf += struct.pack(">2h", *[round(v * QUANT) for v in e["c"]])
        buf += struct.pack(">f", e["area"])
        buf += struct.pack(">B", 1 if e["dot"] else 0)
        for key in ("rings", "draw"):
            rings = e[key]
            buf += struct.pack(">B", len(rings))
            for r in rings:
                buf += struct.pack(">I", len(r) // 2)
                for v in r:
                    if -32767 <= v <= 32767:
                        buf += struct.pack(">h", v)
                    else:
                        buf += struct.pack(">hi", -32768, v)
    open(path, "wb").write(buf)


# Map units that carry no ISO code but lie inside a country ISO 3166-1 lists them under.
# Without this they were holes: northern Somalia showed as open water, Cyprus had its
# north missing, a strip ran across Korea. Genuinely disputed or unclaimed areas
# (Siachen, Bir Tawil, the Southern Patagonian Ice Field, the reefs and banks) and the
# UK Sovereign Base Areas on Cyprus stay out, as before.
ASSIGN_BY_GU_A3 = {
    "SOL": "SO",  # Somaliland
    "CYN": "CY",  # Northern Cyprus
    "CNM": "CY",  # Cyprus UN buffer zone
    "KNX": "KR",  # Korean DMZ, south half
    "KNZ": "KP",  # Korean DMZ, north half
    "USG": "CU",  # Guantanamo Bay (leased; Cuban territory per ISO)
}


def main(src, countries_path, out):
    from shapely.geometry import shape
    from shapely.ops import unary_union

    feats = json.load(open(src, encoding="utf-8"))["features"]
    parts = {}
    for f in feats:
        p = f["properties"]
        code = iso_code(p) or ASSIGN_BY_GU_A3.get(p.get("GU_A3"))
        if not code:
            continue
        parts.setdefault(code, []).append(shape(f["geometry"]))

    # Natural Earth's map units split some countries (Bosnia into the Federation,
    # Republika Srpska and Brcko; Serbia into Serbia and Vojvodina; the UK, Belgium,
    # Portugal, the US, ...). Dissolve each country into one shape first, otherwise the
    # internal lines are drawn as if they were borders and small units such as Brcko
    # fall under the 2 % cut below. The tiny buffer closes hairline gaps between units.
    eps = 1e-6
    raw = {}
    for code, geoms in parts.items():
        merged = unary_union([g.buffer(eps) for g in geoms]).buffer(-eps)
        polys = list(merged.geoms) if merged.geom_type == "MultiPolygon" else [merged]
        for poly in polys:
            if poly.is_empty:
                continue
            raw.setdefault(code, []).append([(x, y) for x, y in poly.exterior.coords])

    # main landmass plus anything down to 2% of it, at most 12 rings
    for code, rs in list(raw.items()):
        rs.sort(key=ring_area, reverse=True)
        biggest = ring_area(rs[0])
        raw[code] = [r for r in rs[:12] if ring_area(r) >= biggest * 0.02]

    hit = topology.build(raw, grid=GRID_HIT, eps_levels=[EPS_HIT])[EPS_HIT]
    draw = topology.build(raw, grid=GRID_DRAW, eps_levels=[EPS_DRAW])[EPS_DRAW]

    # The Vatican is ~700 m across, so it collapses on any snap grid. Fall back to
    # the unsnapped source outline for anything that disappears.
    for code, rs in raw.items():
        if code not in hit:
            hit[code] = [[(round(x, 4), round(y, 4)) for x, y in r] for r in rs]

    countries = json.load(open(countries_path, encoding="utf-8"))
    entries, dots, missing = [], 0, []
    for c in countries:
        code = c["alpha2"]
        if code not in hit:
            missing.append(code)
            continue
        rings = hit[code]
        xs = [x for r in rings for x, _ in r]
        ys = [y for r in rings for _, y in r]
        # Circular mean for longitude: Kiribati, Fiji and Russia straddle the
        # antimeridian, and a plain average puts their centre on the far side of
        # the planet (Kiribati ended up in Africa).
        sx = sum(math.sin(math.radians(x)) for x in xs) / len(xs)
        cxv = sum(math.cos(math.radians(x)) for x in xs) / len(xs)
        cx = math.degrees(math.atan2(sx, cxv))
        cy = sum(ys) / len(ys)
        total = sum(ring_area(r) for r in rings)
        biggest = max(ring_area(r) for r in rings)
        tiny = biggest < 1.0
        entries.append({
            "a2": code,
            "bbox": [round(min(xs), 2), round(min(ys), 2), round(max(xs), 2), round(max(ys), 2)],
            "c": [round(cx, 2), round(cy, 2)],
            "area": round(total, 3),
            "dot": tiny,
            "rings": [quantize(r) for r in rings],
            "draw": [quantize(r) for r in draw.get(code, rings)],
        })
        dots += tiny

    write_binary(entries, out)

    # Record each country's mapped area back into countries.json. The outline quiz
    # needs it to skip shapes too small to recognise from their silhouette.
    by_code = {e["a2"]: e for e in entries}
    for c in countries:
        e = by_code.get(c["alpha2"])
        c["map_available"] = e is not None
        c["map_area"] = round(e["area"], 3) if e else 0.0
    json.dump(countries, open(countries_path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    pts = sum(len(r) // 2 for e in entries for r in e["rings"])
    dpts = sum(len(r) // 2 for e in entries for r in e["draw"])
    print(f"entries={len(entries)} dots={dots} hit_points={pts} draw_points={dpts} "
          f"bytes={os.path.getsize(out)} missing={missing}")


if __name__ == "__main__":
    main(*sys.argv[1:4])
