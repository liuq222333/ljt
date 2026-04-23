import base64
import urllib.parse
import zlib
from datetime import datetime, timezone
from pathlib import Path
import xml.etree.ElementTree as ET


OUT_DIR = Path(r"D:\code\aaaaljt\docs\diagrams\flows")


def add_vertex(root, cell_id, value, style, x, y, w, h):
    cell = ET.SubElement(
        root,
        "mxCell",
        {
            "id": str(cell_id),
            "value": value,
            "style": style,
            "vertex": "1",
            "parent": "1",
        },
    )
    ET.SubElement(
        cell,
        "mxGeometry",
        {"x": str(x), "y": str(y), "width": str(w), "height": str(h), "as": "geometry"},
    )


def add_edge(root, cell_id, source, target, label="", dashed=False, points=None):
    style = (
        "edgeStyle=orthogonalEdgeStyle;rounded=1;jettySize=auto;html=1;"
        "strokeColor=#5b8fd0;fontColor=#475569;fontFamily=Microsoft YaHei;fontSize=12;"
        "endArrow=block;endFill=1;"
    )
    if dashed:
        style = (
            "edgeStyle=orthogonalEdgeStyle;rounded=1;jettySize=auto;html=1;dashed=1;"
            "strokeColor=#94a3b8;fontColor=#64748b;fontFamily=Microsoft YaHei;fontSize=12;"
            "endArrow=block;endFill=1;"
        )
    cell = ET.SubElement(
        root,
        "mxCell",
        {
            "id": str(cell_id),
            "value": label,
            "style": style,
            "edge": "1",
            "parent": "1",
            "source": str(source),
            "target": str(target),
        },
    )
    geom = ET.SubElement(cell, "mxGeometry", {"relative": "1", "as": "geometry"})
    if points:
        arr = ET.SubElement(geom, "Array", {"as": "points"})
        for x, y in points:
            ET.SubElement(arr, "mxPoint", {"x": str(x), "y": str(y)})


def build_mxgraph():
    model = ET.Element(
        "mxGraphModel",
        {
            "dx": "1600",
            "dy": "1200",
            "grid": "1",
            "gridSize": "10",
            "guides": "1",
            "tooltips": "1",
            "connect": "1",
            "arrows": "1",
            "fold": "1",
            "page": "1",
            "pageScale": "1",
            "pageWidth": "1600",
            "pageHeight": "1200",
            "math": "0",
            "shadow": "0",
            "background": "#ffffff",
            "adaptiveColors": "auto",
        },
    )
    root = ET.SubElement(model, "root")
    ET.SubElement(root, "mxCell", {"id": "0"})
    ET.SubElement(root, "mxCell", {"id": "1", "parent": "0"})

    lane = (
        "rounded=1;whiteSpace=wrap;html=1;strokeWidth=1;"
        "fontFamily=Microsoft YaHei;fontSize=18;fontStyle=1;align=center;verticalAlign=top;"
        "spacingTop=10;"
    )
    box = (
        "rounded=1;whiteSpace=wrap;html=1;strokeWidth=2;"
        "fontFamily=Microsoft YaHei;fontSize=16;fontColor=#111827;shadow=1;"
    )
    inner = (
        "rounded=1;whiteSpace=wrap;html=1;dashed=1;dashPattern=6 6;"
        "fontFamily=Microsoft YaHei;fontSize=14;fontColor=#64748b;align=center;verticalAlign=middle;"
    )

    add_vertex(
        root,
        2,
        "<b>backend_api 工具调用分支流程图</b>",
        "text;html=1;align=center;verticalAlign=middle;fontSize=24;fontStyle=1;fontFamily=Microsoft YaHei;fontColor=#0f172a;",
        300,
        24,
        900,
        40,
    )

    add_vertex(root, 10, "上层输入", lane + "fillColor=#eef2ff;strokeColor=#c7d2fe;fontColor=#375a9e;", 120, 90, 1260, 140)
    add_vertex(root, 11, "模型输出", lane + "fillColor=#fefce8;strokeColor=#d9cf8f;fontColor=#556b2f;", 120, 280, 1260, 120)
    add_vertex(root, 12, "后端分支", lane + "fillColor=#ecfdf5;strokeColor=#bbf7d0;fontColor=#166534;", 120, 440, 1260, 180)
    add_vertex(root, 13, "后端分支", lane + "fillColor=#eff6ff;strokeColor=#bfdbfe;fontColor=#1d4ed8;", 120, 660, 1260, 110)
    add_vertex(root, 14, "结果返回", lane + "fillColor=#f8fafc;strokeColor=#cbd5e1;fontColor=#334155;", 420, 820, 660, 100)

    add_vertex(
        root,
        20,
        "<b>backend_api 工具入参</b><br/><font style=\"color:#475569;\">{resource, action, pathVariables, params, payload, authorization}</font>",
        box + "fillColor=#ffffff;strokeColor=#9db5df;",
        230,
        130,
        1040,
        78,
    )
    add_vertex(
        root,
        21,
        "<b>模型输出：text 或 tool_calls</b>",
        box + "fillColor=#ffffff;strokeColor=#b7c98a;",
        320,
        320,
        860,
        64,
    )

    add_vertex(root, 22, "", inner + "strokeColor=#cbd5e1;fillColor=none;", 160, 500, 540, 90)
    add_vertex(root, 23, "", inner + "strokeColor=#d9e3b0;fillColor=none;", 760, 500, 500, 90)

    add_vertex(
        root,
        24,
        "<b>路由解析</b><br/><font style=\"color:#475569;\">resource + action<br/>路由匹配</font>",
        box + "fillColor=#f8fbff;strokeColor=#5ca3d6;",
        200,
        510,
        420,
        80,
    )
    add_vertex(
        root,
        25,
        "<b>参数解析</b><br/><font style=\"color:#475569;\">params / payload / pathVariables<br/>结构化参数</font>",
        box + "fillColor=#fcfff7;strokeColor=#99b672;",
        800,
        510,
        420,
        80,
    )

    add_vertex(
        root,
        26,
        "<b>tool_calls → ToolHandler</b>",
        box + "fillColor=#5ca3b4;strokeColor=#4b93a7;fontColor=#ffffff;",
        160,
        695,
        500,
        48,
    )
    add_vertex(
        root,
        27,
        "<b>text → 直接返回</b>",
        box + "fillColor=#fffef4;strokeColor=#d8d3a4;",
        710,
        695,
        500,
        48,
    )

    add_vertex(
        root,
        28,
        "<b>结果返回</b><br/><font style=\"color:#64748b;\">统一响应结果</font>",
        box + "fillColor=#ffffff;strokeColor=#cbd5e1;",
        500,
        855,
        500,
        56,
    )
    add_vertex(
        root,
        29,
        "✓",
        "ellipse;whiteSpace=wrap;html=1;fillColor=#f0fdf4;strokeColor=#84cc16;fontColor=#65a30d;fontSize=26;fontStyle=1;fontFamily=Microsoft YaHei;",
        720,
        930,
        60,
        60,
    )

    add_edge(root, 100, 20, 21)
    add_edge(root, 101, 21, 24, points=[(490, 420), (410, 420), (410, 510)])
    add_edge(root, 102, 21, 25, points=[(1010, 420), (1010, 510)])
    add_edge(root, 103, 24, 25, dashed=True, label="协同")
    add_edge(root, 104, 25, 24, dashed=True, label="回填", points=[(1010, 600), (1010, 620), (410, 620), (410, 590)])
    add_edge(root, 105, 24, 26, points=[(410, 590), (410, 695)])
    add_edge(root, 106, 25, 27, points=[(1010, 590), (1010, 695)])
    add_edge(root, 107, 26, 28, points=[(660, 719), (750, 719), (750, 855)])
    add_edge(root, 108, 27, 28, points=[(960, 743), (960, 855)])
    add_edge(root, 109, 28, 29)

    return ET.tostring(model, encoding="utf-8").decode("utf-8")


def compress_diagram(xml_text):
    payload = urllib.parse.quote(xml_text, safe="").encode("utf-8")
    compressor = zlib.compressobj(level=9, wbits=-15)
    raw = compressor.compress(payload) + compressor.flush()
    return base64.b64encode(raw).decode("ascii")


def build_drawio(mxgraph_xml):
    modified = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%fZ")
    compressed = compress_diagram(mxgraph_xml)
    return (
        "<?xml version='1.0' encoding='utf-8'?>\n"
        f"<mxfile host=\"app.diagrams.net\" modified=\"{modified}\" agent=\"Codex\" "
        "version=\"26.0.11\" type=\"device\">"
        f"<diagram id=\"backend-api-model-branch\" name=\"backend_api 工具调用分支流程图\">{compressed}</diagram>"
        "</mxfile>"
    )


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    mxgraph_xml = build_mxgraph()
    xml_path = OUT_DIR / "backend-api-model-branch-flow.mxgraph.xml"
    drawio_path = OUT_DIR / "backend-api-model-branch-flow.drawio"
    xml_path.write_text(mxgraph_xml, encoding="utf-8")
    drawio_path.write_text(build_drawio(mxgraph_xml), encoding="utf-8")
    print(xml_path)
    print(drawio_path)


if __name__ == "__main__":
    main()
