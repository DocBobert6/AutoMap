#!/usr/bin/env python3
"""osmium geojsonseq on stdin -> gzipped `lat<TAB>lon<TAB>kind[<TAB>bearing]` TSV for RoadFeatures.

kind: S traffic signal, T stop sign, R level crossing, H speed hump/bump/table/cushion,
C fixed speed camera. Anything else on stdin is skipped. Prints the row count.

BEARING: the orientation of the ROAD the node sits on, 0-179 degrees (undirected, so north-south is
0 and east-west is 90), or empty when it could not be worked out. It is what tells a stop sign that
holds YOU from the one that holds the side street entering your road: the app keeps a sign whose
road runs the way you are traveling and drops the one across it. Pass the highway ways as a second
geojsonseq file with --ways; without it the column is empty.

Only drivable ways count. A stop line's node is often shared with a crosswalk, which runs across
the side street and so along the main road. A stop sign on a junction node holds the most minor
road through it. Empty for a sign tagged `stop=all`, and for one whose most minor roads cross (two
residential streets meeting at the node): the app then judges it on distance alone.

    osmium export filtered.osm.pbf -f geojsonseq -o - | python3 scripts/road_features_tsv.py out.bin --ways ways.geojsonseq
"""
import gzip
import json
import math
import sys


def kind_of(p):
    hw = p.get("highway")
    if hw == "traffic_signals":
        return "S"
    if hw == "stop":
        return "T"
    if hw == "speed_camera":
        return "C"
    if p.get("railway") == "level_crossing":
        return "R"
    if p.get("traffic_calming") in ("bump", "hump", "table", "cushion"):
        return "H"
    return None


# Drivable highway classes, ranked major (0) to minor. A `*_link` takes its parent's rank.
ROAD_RANK = {
    "motorway": 0, "trunk": 1, "primary": 2, "secondary": 3, "tertiary": 4,
    "unclassified": 5, "road": 5, "residential": 6, "living_street": 7, "service": 8, "track": 9,
}

# Two orientations closer than this are the same road. The app's alignment tolerance.
AGREE_DEG = 40


def road_rank(highway):
    if not highway:
        return None
    if highway.endswith("_link"):
        highway = highway[:-len("_link")]
    return ROAD_RANK.get(highway)


def axis_gap(a, b):
    """Degrees between two undirected orientations (0-179), 0 to 90."""
    d = abs(a - b) % 180
    return min(d, 180 - d)


def candidates_from_ways(path, wanted):
    """(rank, orientation 0-179) of every drivable way through each wanted (lat, lon) key."""
    out = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip().lstrip("\x1e")
            if not line:
                continue
            try:
                ft = json.loads(line)
            except ValueError:
                continue
            rank = road_rank((ft.get("properties") or {}).get("highway"))
            if rank is None:
                continue
            g = ft.get("geometry") or {}
            if g.get("type") == "LineString":
                lines = [g["coordinates"]]
            elif g.get("type") == "MultiLineString":
                lines = g["coordinates"]
            else:
                continue
            for coords in lines:
                for i, c in enumerate(coords):
                    key = (round(c[1], 7), round(c[0], 7))
                    if key not in wanted:
                        continue
                    a = coords[i - 1] if i > 0 else c
                    b = coords[i + 1] if i + 1 < len(coords) else c
                    if a is b or (a[0] == b[0] and a[1] == b[1]):
                        continue
                    dx = (b[0] - a[0]) * math.cos(math.radians(c[1]))
                    dy = b[1] - a[1]
                    out.setdefault(key, []).append((rank, int(round(math.degrees(math.atan2(dx, dy)))) % 180))
    return out


def resolve(kind, all_way, cands):
    """The orientation written for one node, or None for an empty column."""
    if not cands:
        return None
    if kind != "T":
        return min(cands)[1]  # nothing reads it; the most major road, so the value is stable
    if all_way:
        return None  # holds every road through the junction
    minor = max(r for r, _ in cands)
    bs = [b for r, b in cands if r == minor]
    if any(axis_gap(x, y) > AGREE_DEG for i, x in enumerate(bs) for y in bs[i + 1:]):
        return None
    return bs[0]


def main():
    out = sys.argv[1]
    ways = None
    if "--ways" in sys.argv:
        ways = sys.argv[sys.argv.index("--ways") + 1]
    n = 0
    kinds = {}
    rows = []
    for line in sys.stdin:
        line = line.strip().lstrip("\x1e")  # geojsonseq record separator
        if not line:
            continue
        try:
            ft = json.loads(line)
        except ValueError:
            continue
        g = ft.get("geometry") or {}
        if g.get("type") != "Point":
            continue
        lon, lat = g["coordinates"][:2]
        p = ft.get("properties") or {}
        k = kind_of(p)
        if k is None:
            continue
        rows.append((lat, lon, k, p.get("stop") == "all"))
        kinds[k] = kinds.get(k, 0) + 1

    cands = {}
    if ways:
        wanted = {(round(la, 7), round(lo, 7)) for la, lo, _, _ in rows}
        cands = candidates_from_ways(ways, wanted)

    nb = 0
    with gzip.open(out, "wt", encoding="utf-8", compresslevel=9) as f:
        for la, lo, k, all_way in rows:
            b = resolve(k, all_way, cands.get((round(la, 7), round(lo, 7))))
            f.write(f"{la:.6f}\t{lo:.6f}\t{k}\t{b if b is not None else ''}\n")
            n += 1
            if b is not None:
                nb += 1
    print(f"{n} rows {kinds} bearings={nb}", file=sys.stderr)


if __name__ == "__main__":
    main()
