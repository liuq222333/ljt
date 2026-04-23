import base64
import urllib.parse
import zlib
from datetime import datetime, timezone
from pathlib import Path
import xml.etree.ElementTree as ET


OUT_DIR = Path(r"D:\code\aaaaljt\docs\diagrams\architecture")


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
        "strokeColor=#64748b;fontColor=#475569;fontFamily=Microsoft YaHei;fontSize=12;"
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

    lane_style = (
        "rounded=1;whiteSpace=wrap;html=1;dashed=1;dashPattern=8 8;"
        "fontFamily=Microsoft YaHei;fontSize=18;fontStyle=1;align=left;verticalAlign=top;"
        "spacingTop=10;spacingLeft=14;"
    )
    box_style = (
        "rounded=1;whiteSpace=wrap;html=1;strokeWidth=2;"
        "fontFamily=Microsoft YaHei;fontSize=16;fontColor=#111827;"
    )
    cyl_style = (
        "shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;strokeWidth=2;"
        "fontFamily=Microsoft YaHei;fontSize=16;fontColor=#111827;"
    )

    add_vertex(
        root,
        2,
        "<b>前后端通信与基础设施架构</b><br/><font style=\"font-size:15px;color:#64748b;\">Frontend / Backend / Infrastructure Architecture</font>",
        "text;html=1;align=center;verticalAlign=middle;fontSize=24;fontStyle=1;fontFamily=Microsoft YaHei;fontColor=#0f172a;",
        280,
        18,
        900,
        48,
    )

    add_vertex(root, 10, "前端层 / Frontend Layer", lane_style + "fillColor=#dbeafe;strokeColor=#60a5fa;fontColor=#1d4ed8;", 120, 90, 1260, 170)
    add_vertex(root, 11, "后端服务层 / Backend Service Layer", lane_style + "fillColor=#dcfce7;strokeColor=#22c55e;fontColor=#166534;", 120, 290, 1260, 250)
    add_vertex(root, 12, "基础设施层 / Infrastructure Layer", lane_style + "fillColor=#fee2e2;strokeColor=#ef4444;fontColor=#991b1b;", 120, 570, 1260, 220)
    add_vertex(root, 13, "技术说明", "rounded=1;whiteSpace=wrap;html=1;fillColor=#f8fafc;strokeColor=#cbd5e1;fontColor=#334155;fontFamily=Microsoft YaHei;fontSize=14;align=left;spacingLeft=12;spacingTop=10;", 120, 820, 1260, 80)

    add_vertex(root, 20, "<b>HTTP 协议</b><br/><font style=\"color:#7c2d12;\">RESTful API</font>", box_style + "fillColor=#ffedd5;strokeColor=#fdba74;", 200, 145, 180, 86)
    add_vertex(root, 21, "<b>Web / H5 前端应用</b><br/><font style=\"color:#64748b;\">用户交互界面 / 页面展示</font>", box_style + "fillColor=#ffffff;strokeColor=#93c5fd;", 560, 135, 300, 106)
    add_vertex(root, 22, "<b>WebSocket 协议</b><br/><font style=\"color:#7c2d12;\">实时通信</font>", box_style + "fillColor=#ffedd5;strokeColor=#fdba74;", 1040, 145, 190, 86)

    add_vertex(root, 30, "<b>REST API 服务</b><br/><font style=\"color:#64748b;\">标准 HTTP 接口<br/>RESTful 风格</font>", box_style + "fillColor=#ffffff;strokeColor=#86efac;", 180, 375, 220, 110)
    add_vertex(root, 31, "<b>Spring Boot 后端服务</b><br/><font style=\"color:#64748b;\">核心业务逻辑<br/>API 接口服务</font>", box_style + "fillColor=#ffffff;strokeColor=#86efac;", 530, 360, 360, 120)
    add_vertex(root, 32, "<b>STOMP 服务</b><br/><font style=\"color:#64748b;\">WebSocket 消息协议<br/>实时双向通信</font>", box_style + "fillColor=#ffffff;strokeColor=#86efac;", 1030, 375, 220, 110)

    add_vertex(root, 40, "<b>MySQL</b><br/><font style=\"color:#64748b;\">关系型数据库</font><br/><font style=\"font-size:13px;color:#92400e;\">持久化存储 / 事务管理</font>", cyl_style + "fillColor=#ffffff;strokeColor=#f87171;", 170, 640, 180, 110)
    add_vertex(root, 41, "<b>Redis</b><br/><font style=\"color:#64748b;\">缓存数据库</font><br/><font style=\"font-size:13px;color:#92400e;\">高速缓存 / 会话管理</font>", cyl_style + "fillColor=#ffffff;strokeColor=#f87171;", 450, 640, 180, 110)
    add_vertex(root, 42, "<b>RabbitMQ</b><br/><font style=\"color:#64748b;\">消息队列</font><br/><font style=\"font-size:13px;color:#92400e;\">异步消息 / 解耦处理</font>", cyl_style + "fillColor=#ffffff;strokeColor=#f87171;", 730, 640, 180, 110)
    add_vertex(root, 43, "<b>MinIO</b><br/><font style=\"color:#64748b;\">对象存储</font><br/><font style=\"font-size:13px;color:#92400e;\">文件存储 / 图片媒体</font>", cyl_style + "fillColor=#ffffff;strokeColor=#f87171;", 1010, 640, 180, 110)

    add_vertex(
        root,
        50,
        "前端：Web/H5 | 后端：Spring Boot | 通信：HTTP + WebSocket | 存储：MySQL + Redis + RabbitMQ + MinIO",
        "text;html=1;align=left;verticalAlign=middle;fontSize=15;fontFamily=Microsoft YaHei;fontColor=#334155;",
        150,
        846,
        1180,
        30,
    )

    add_edge(root, 100, 21, 20, "HTTP 调用", points=[(500, 188), (420, 188), (420, 188), (380, 188)])
    add_edge(root, 101, 20, 30, "HTTP 请求", points=[(290, 231), (290, 375)])
    add_edge(root, 102, 21, 31, "业务调用", points=[(710, 241), (710, 360)])
    add_edge(root, 103, 21, 22, "实时连接", dashed=True, points=[(860, 188), (950, 188), (950, 188), (1040, 188)])
    add_edge(root, 104, 22, 32, "WebSocket 连接", points=[(1135, 231), (1135, 375)])
    add_edge(root, 105, 30, 31, "接口转发", dashed=True, points=[(400, 430), (470, 430), (470, 420), (530, 420)])
    add_edge(root, 106, 31, 32, "消息转发", dashed=True, points=[(890, 420), (960, 420), (960, 430), (1030, 430)])
    add_edge(root, 107, 30, 40, "持久化", points=[(290, 485), (290, 640)])
    add_edge(root, 108, 31, 41, "状态缓存", points=[(620, 480), (540, 480), (540, 640)])
    add_edge(root, 109, 31, 42, "异步消息", points=[(800, 480), (820, 480), (820, 640)])
    add_edge(root, 110, 31, 43, "文件存储", points=[(860, 480), (1100, 480), (1100, 640)])
    add_edge(root, 111, 32, 42, "消息订阅", dashed=True, points=[(1140, 485), (1140, 600), (820, 600), (820, 640)])

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
        f"<diagram id=\"frontend-backend-infra\" name=\"前后端通信与基础设施架构\">{compressed}</diagram>"
        "</mxfile>"
    )


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    mxgraph_xml = build_mxgraph()
    xml_path = OUT_DIR / "frontend-backend-infrastructure-architecture.mxgraph.xml"
    drawio_path = OUT_DIR / "frontend-backend-infrastructure-architecture.drawio"
    xml_path.write_text(mxgraph_xml, encoding="utf-8")
    drawio_path.write_text(build_drawio(mxgraph_xml), encoding="utf-8")
    print(xml_path)
    print(drawio_path)


if __name__ == "__main__":
    main()
