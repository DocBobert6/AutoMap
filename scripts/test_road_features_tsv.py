#!/usr/bin/env python3
"""Tests for road_features_tsv.py: which road a stop sign's orientation is taken from.

    python3 -m unittest discover -s scripts -p 'test_*.py'

Fixture: a primary street running east-west through Davis, CA, and a residential street meeting
it from the north.
"""
import gzip
import json
import os
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import road_features_tsv as rf  # noqa: E402

LAT = 38.5600
JCT = (-121.7650, LAT)            # the junction node, (lon, lat) as GeoJSON has it
STOP = (-121.7650, LAT + 0.00005)  # the side street's stop line, 5.5 m north of the junction
WEST = (-121.7700, LAT)
EAST = (-121.7600, LAT)
NORTH = (-121.7650, LAT + 0.0020)
SOUTH = (-121.7650, LAT - 0.0020)


def way(highway, coords, **tags):
    return {"type": "Feature", "properties": dict(highway=highway, **tags),
            "geometry": {"type": "LineString", "coordinates": [list(c) for c in coords]}}


def key(c):
    return (round(c[1], 7), round(c[0], 7))


class RoadFeaturesTsvTest(unittest.TestCase):

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)

    def write(self, name, features):
        path = os.path.join(self.dir.name, name)
        with open(path, "w", encoding="utf-8") as f:
            for ft in features:
                f.write("\x1e" + json.dumps(ft) + "\n")
        return path

    def bearing(self, node, ways, kind="T", all_way=False):
        path = self.write("ways.geojsonseq", ways)
        return rf.resolve(kind, all_way, rf.candidates_from_ways(path, {key(node)}).get(key(node)))

    def test_a_crosswalk_through_the_stop_line_does_not_set_its_road(self):
        crosswalk = way("footway", [(-121.7652, STOP[1]), STOP, (-121.7648, STOP[1])], footway="crossing")
        side = way("residential", [JCT, STOP, NORTH])
        # The crosswalk comes first in the file, as a lower way id would put it.
        self.assertEqual(0, self.bearing(STOP, [crosswalk, side]))

    def test_a_sign_on_the_junction_node_holds_the_minor_road(self):
        main = way("primary", [WEST, JCT, EAST])
        side = way("residential", [JCT, NORTH])
        self.assertEqual(0, self.bearing(JCT, [main, side]))
        self.assertEqual(0, self.bearing(JCT, [side, main]))

    def test_a_link_ranks_with_its_parent(self):
        main = way("primary", [WEST, JCT, EAST])
        ramp = way("primary_link", [JCT, NORTH])
        self.assertIsNone(self.bearing(JCT, [main, ramp]))

    def test_an_all_way_stop_has_no_single_road(self):
        main = way("primary", [WEST, JCT, EAST])
        side = way("residential", [JCT, NORTH])
        self.assertIsNone(self.bearing(JCT, [main, side], all_way=True))

    def test_two_equal_streets_crossing_at_the_sign_are_ambiguous(self):
        a = way("residential", [WEST, JCT, EAST])
        b = way("residential", [SOUTH, JCT, NORTH])
        self.assertIsNone(self.bearing(JCT, [a, b]))

    def test_one_street_split_into_two_ways_at_the_sign_agrees(self):
        a = way("residential", [SOUTH, JCT])
        b = way("residential", [JCT, NORTH])
        self.assertEqual(0, self.bearing(JCT, [a, b]))

    def test_a_sign_at_the_end_of_a_way_takes_its_one_side(self):
        self.assertEqual(0, self.bearing(JCT, [way("residential", [JCT, NORTH])]))

    def test_a_sign_on_no_drivable_way_has_no_bearing(self):
        self.assertIsNone(self.bearing(STOP, [way("footway", [(-121.7652, STOP[1]), STOP, (-121.7648, STOP[1])])]))

    def test_a_signal_takes_the_major_road(self):
        main = way("primary", [WEST, JCT, EAST])
        side = way("residential", [JCT, NORTH])
        self.assertEqual(90, self.bearing(JCT, [side, main], kind="S"))

    def test_the_whole_script_writes_the_column(self):
        ways = self.write("ways.geojsonseq", [
            way("footway", [(-121.7652, STOP[1]), STOP, (-121.7648, STOP[1])], footway="crossing"),
            way("primary", [WEST, JCT, EAST]),
            way("residential", [JCT, STOP, NORTH]),
        ])
        nodes = "".join("\x1e" + json.dumps({
            "type": "Feature", "properties": props,
            "geometry": {"type": "Point", "coordinates": list(c)},
        }) + "\n" for c, props in [
            (STOP, {"highway": "stop"}),
            (JCT, {"highway": "stop", "stop": "all"}),
            (EAST, {"highway": "traffic_signals"}),
        ])
        out = os.path.join(self.dir.name, "out.bin")
        script = os.path.join(os.path.dirname(os.path.abspath(__file__)), "road_features_tsv.py")
        subprocess.run([sys.executable, "-I", script, out, "--ways", ways],
                       input=nodes, text=True, check=True, capture_output=True)
        with gzip.open(out, "rt", encoding="utf-8") as f:
            cols = [line.rstrip("\n").split("\t") for line in f]
        self.assertEqual([["T", "0"], ["T", ""], ["S", "90"]], [c[2:] for c in cols])


if __name__ == "__main__":
    unittest.main()
