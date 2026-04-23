import base64
import html
import math
import re
import urllib.parse
import uuid
import zlib
from datetime import datetime, timezone
from pathlib import Path
import xml.etree.ElementTree as ET

BASE = Path(r"D:\code\aaaaljt\docs\diagrams\classes")
CLASS_STYLE = (
    "rounded=0;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#1f2937;"
    "fontColor=#111827;fontFamily=Microsoft YaHei;fontSize=13;align=left;"
    "verticalAlign=top;spacing=6;spacingLeft=10;spacingTop=10;shadow=0;"
)
EDGE_STYLES = {
    "assoc": "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#475569;fontColor=#334155;fontFamily=Microsoft YaHei;fontSize=12;endArrow=block;endFill=1;",
    "dep": "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;dashed=1;strokeColor=#64748b;fontColor=#334155;fontFamily=Microsoft YaHei;fontSize=12;endArrow=open;endFill=0;",
    "comp": "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#475569;fontColor=#334155;fontFamily=Microsoft YaHei;fontSize=12;startArrow=diamondThin;startFill=1;endArrow=none;startSize=14;",
}


def split_body(body):
    attrs, methods = [], []
    for raw in body.splitlines():
        line = raw.strip()
        if not line:
            continue
        if "(" in line and ")" in line:
            methods.append(line)
        else:
            attrs.append(line)
    return attrs, methods


def parse_mermaid(text):
    labels = {m.group(1): m.group(2) for m in re.finditer(r'class\s+(\w+)\["([^"]+)"\](?!\s*\{)', text)}
    classes = {}
    order = []
    for m in re.finditer(r'class\s+(\w+)(?:\["([^"]+)"\])?\s*\{(.*?)\}', text, re.S):
        name = m.group(1)
        label = m.group(2) or labels.get(name, name)
        attrs, methods = split_body(m.group(3))
        classes[name] = {"label": label, "attrs": attrs, "methods": methods}
        order.append(name)

    edges = []
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("%%") or line.startswith("classDiagram") or line.startswith("direction") or line.startswith("class "):
            continue
        line = re.sub(r'"[^"]*"', "", line)
        line = re.sub(r"\s+", " ", line).strip()
        m = re.match(r"^(\w+)\s+<--\s+(\w+)\s*:\s*(.+)$", line)
        if m:
            edges.append({"source": m.group(2), "target": m.group(1), "kind": "assoc", "label": m.group(3)})
            continue
        m = re.match(r"^(\w+)\s+-->\s+(\w+)\s*:\s*(.+)$", line)
        if m:
            edges.append({"source": m.group(1), "target": m.group(2), "kind": "assoc", "label": m.group(3)})
            continue
        m = re.match(r"^(\w+)\s+\*--\s+(\w+)\s*:\s*(.+)$", line)
        if m:
            edges.append({"source": m.group(1), "target": m.group(2), "kind": "comp", "label": m.group(3)})
            continue
        m = re.match(r"^(\w+)\s+o--\s+(\w+)\s*:\s*(.+)$", line)
        if m:
            edges.append({"source": m.group(1), "target": m.group(2), "kind": "assoc", "label": m.group(3)})
            continue
        m = re.match(r"^(\w+)\s+\.\.>\s+(\w+)\s*:\s*(.+)$", line)
        if m:
            edges.append({"source": m.group(1), "target": m.group(2), "kind": "dep", "label": m.group(3)})
    return classes, order, edges


def label_html(title, attrs, methods):
    def render(lines):
        if not lines:
            return "&nbsp;"
        return "<br/>".join(html.escape(x) for x in lines)
    return f"<b>{html.escape(title)}</b><hr size=\"1\"/>{render(attrs)}<hr size=\"1\"/>{render(methods)}"


def class_height(attrs, methods):
    return max(130, 42 + (1 + max(1, len(attrs)) + max(1, len(methods))) * 22)


def compress_xml(xml_bytes):
    payload = urllib.parse.quote(xml_bytes.decode("utf-8"), safe="").encode("utf-8")
    co = zlib.compressobj(level=9, wbits=-15)
    raw = co.compress(payload) + co.flush()
    return base64.b64encode(raw).decode("ascii")


def build_drawio(title, classes, order, edges):
    model = ET.Element("mxGraphModel", {
        "dx": "1600", "dy": "1200", "grid": "1", "gridSize": "10", "guides": "1",
        "tooltips": "1", "connect": "1", "arrows": "1", "fold": "1", "page": "1",
        "pageScale": "1", "pageWidth": "1800", "pageHeight": "1400", "math": "0",
        "shadow": "0", "background": "#ffffff"
    })
    root = ET.SubElement(model, "root")
    ET.SubElement(root, "mxCell", {"id": "0"})
    ET.SubElement(root, "mxCell", {"id": "1", "parent": "0"})

    ids = {}
    next_id = 2
    col_count = min(5, max(3, math.ceil(len(order) / 3)))
    per_col = math.ceil(len(order) / col_count)
    x_positions = [40, 350, 660, 970, 1280]
    columns = [order[i:i + per_col] for i in range(0, len(order), per_col)]

    for col_index, col in enumerate(columns):
        x = x_positions[col_index]
        y = 60
        for key in col:
            cls = classes[key]
            h = class_height(cls["attrs"], cls["methods"])
            ids[key] = str(next_id)
            cell = ET.SubElement(root, "mxCell", {
                "id": ids[key],
                "value": label_html(cls["label"], cls["attrs"], cls["methods"]),
                "style": CLASS_STYLE,
                "vertex": "1",
                "parent": "1",
            })
            ET.SubElement(cell, "mxGeometry", {
                "x": str(x), "y": str(y), "width": "260", "height": str(h), "as": "geometry"
            })
            next_id += 1
            y += h + 40

    for edge in edges:
        if edge["source"] not in ids or edge["target"] not in ids:
            continue
        cell = ET.SubElement(root, "mxCell", {
            "id": str(next_id),
            "value": edge["label"],
            "style": EDGE_STYLES[edge["kind"]],
            "edge": "1",
            "parent": "1",
            "source": ids[edge["source"]],
            "target": ids[edge["target"]],
        })
        ET.SubElement(cell, "mxGeometry", {"relative": "1", "as": "geometry"})
        next_id += 1

    mxfile = ET.Element("mxfile", {
        "host": "app.diagrams.net",
        "modified": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
        "agent": "Codex",
        "version": "26.0.11",
        "type": "device",
    })
    diagram = ET.SubElement(mxfile, "diagram", {"id": str(uuid.uuid4())[:8], "name": title})
    diagram.text = compress_xml(ET.tostring(model, encoding="utf-8"))
    return ET.tostring(mxfile, encoding="utf-8", xml_declaration=True)


def title_from_name(name):
    mapping = {
        "class-overview": "系统核心类图总览",
        "class-auth-user": "用户与认证模块类图",
        "class-market": "社区集市模块类图",
        "class-local-activity": "本地活动与互助模块类图",
        "class-community-notification": "社区互动与通知模块类图",
        "class-agent-governance": "Agent 与后台治理模块类图",
    }
    return mapping.get(name, name)


def main():
    for mmd in BASE.glob("*.mmd"):
        text = mmd.read_text(encoding="utf-8")
        classes, order, edges = parse_mermaid(text)
        out = mmd.with_suffix(".drawio")
        out.write_bytes(build_drawio(title_from_name(mmd.stem), classes, order, edges))
        print(out)


if __name__ == "__main__":
    main()
