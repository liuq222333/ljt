import base64
import urllib.parse
import zlib
from datetime import datetime, timezone
from pathlib import Path
import xml.etree.ElementTree as ET


OUT_DIR = Path(r"D:\code\aaaaljt\docs\diagrams\architecture")


LANE_STYLE = (
    "rounded=1;whiteSpace=wrap;html=1;dashed=1;dashPattern=8 8;"
    "fontFamily=Microsoft YaHei;fontSize=18;fontStyle=1;align=left;verticalAlign=top;"
    "spacingTop=10;spacingLeft=14;"
)
BOX_STYLE = (
    "rounded=1;whiteSpace=wrap;html=1;strokeWidth=2;"
    "fontFamily=Microsoft YaHei;fontSize=16;fontColor=#111827;"
)
NOTE_STYLE = (
    "rounded=1;whiteSpace=wrap;html=1;strokeWidth=1;"
    "fontFamily=Microsoft YaHei;fontSize=15;fontColor=#4c1d95;"
)
CYL_STYLE = (
    "shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;strokeWidth=2;"
    "fontFamily=Microsoft YaHei;fontSize=16;fontColor=#111827;"
)
EDGE_STYLE = (
    "edgeStyle=orthogonalEdgeStyle;rounded=1;jettySize=auto;html=1;"
    "strokeColor=#475569;fontColor=#334155;fontFamily=Microsoft YaHei;fontSize=12;"
    "endArrow=block;endFill=1;"
)
DASHED_EDGE_STYLE = (
    "edgeStyle=orthogonalEdgeStyle;rounded=1;jettySize=auto;html=1;dashed=1;"
    "strokeColor=#64748b;fontColor=#475569;fontFamily=Microsoft YaHei;fontSize=12;"
    "endArrow=block;endFill=1;"
)


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
    cell = ET.SubElement(
        root,
        "mxCell",
        {
            "id": str(cell_id),
            "value": label,
            "style": DASHED_EDGE_STYLE if dashed else EDGE_STYLE,
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
            "pageHeight": "1300",
            "math": "0",
            "shadow": "0",
            "background": "#ffffff",
            "adaptiveColors": "auto",
        },
    )
    root = ET.SubElement(model, "root")
    ET.SubElement(root, "mxCell", {"id": "0"})
    ET.SubElement(root, "mxCell", {"id": "1", "parent": "0"})

    add_vertex(
        root,
        2,
        "<b>接入-业务-通知分层架构</b><br/><font style=\"font-size:15px;color:#64748b;\">Access / Business / Notification Architecture</font>",
        "text;html=1;align=center;verticalAlign=middle;fontSize=24;fontStyle=1;fontFamily=Microsoft YaHei;fontColor=#0f172a;",
        270,
        18,
        900,
        48,
    )

    add_vertex(root, 10, "接入层 / Access Layer", LANE_STYLE + "fillColor=#dbeafe;strokeColor=#60a5fa;fontColor=#1d4ed8;", 120, 90, 1260, 170)
    add_vertex(root, 11, "业务服务层 / Business Service Layer", LANE_STYLE + "fillColor=#dcfce7;strokeColor=#22c55e;fontColor=#166534;", 120, 290, 1260, 210)
    add_vertex(root, 12, "消息通知层 / Notification Layer", LANE_STYLE + "fillColor=#ffedd5;strokeColor=#f59e0b;fontColor=#9a3412;", 120, 530, 1260, 210)
    add_vertex(root, 13, "基础设施层 / Infrastructure Layer", LANE_STYLE + "fillColor=#fee2e2;strokeColor=#ef4444;fontColor=#991b1b;", 120, 770, 1260, 180)
    add_vertex(root, 14, "治理层 / Governance Layer", LANE_STYLE + "fillColor=#f1f5f9;strokeColor=#94a3b8;fontColor=#334155;", 120, 980, 1260, 120)

    add_vertex(
        root,
        20,
        "请求类型：<br/>• 对话/工具调用<br/>• 业务请求<br/>• 触达请求",
        NOTE_STYLE + "fillColor=#ede9fe;strokeColor=#a78bfa;",
        260,
        140,
        180,
        92,
    )
    add_vertex(
        root,
        21,
        "<b>REST API / WebSocket</b>",
        BOX_STYLE + "fillColor=#ffffff;strokeColor=#93c5fd;",
        560,
        140,
        220,
        72,
    )
    add_vertex(
        root,
        22,
        "<b>API Gateway</b>",
        BOX_STYLE + "fillColor=#ffffff;strokeColor=#93c5fd;",
        960,
        140,
        220,
        72,
    )

    add_vertex(
        root,
        30,
        "<b>Agent 模块</b><br/><font style=\"color:#64748b;\">接入入口</font>",
        BOX_STYLE + "fillColor=#ffffff;strokeColor=#86efac;",
        300,
        360,
        200,
        80,
    )
    add_vertex(
        root,
        31,
        "<b>业务模块</b><br/><font style=\"color:#64748b;\">动态 / 集市 / 活动 / 互助</font>",
        BOX_STYLE + "fillColor=#ffffff;strokeColor=#86efac;",
        590,
        360,
        270,
        80,
    )
    add_vertex(
        root,
        32,
        "<b>通知模块</b><br/><font style=\"color:#64748b;\">Sender / Listener</font>",
        BOX_STYLE + "fillColor=#ffffff;strokeColor=#86efac;",
        930,
        360,
        220,
        80,
    )
    add_vertex(
        root,
        33,
        "<b>关键动作</b><br/>变更执行<br/>通知事件",
        BOX_STYLE + "fillColor=#fef3c7;strokeColor=#f59e0b;fontColor=#92400e;",
        1210,
        360,
        130,
        80,
    )

    add_vertex(
        root,
        40,
        "<b>事件总线</b><br/><font style=\"color:#64748b;\">RabbitMQ</font>",
        CYL_STYLE + "fillColor=#ffffff;strokeColor=#f59e0b;fontColor=#9a3412;",
        420,
        600,
        180,
        96,
    )
    add_vertex(
        root,
        41,
        "<b>异步处理服务</b>",
        BOX_STYLE + "fillColor=#ffffff;strokeColor=#fdba74;",
        700,
        608,
        210,
        72,
    )
    add_vertex(
        root,
        42,
        "<b>事件分发器</b>",
        BOX_STYLE + "fillColor=#ffffff;strokeColor=#fdba74;",
        1010,
        608,
        190,
        72,
    )

    add_vertex(
        root,
        50,
        "<b>MySQL</b><br/><font style=\"color:#64748b;\">关系型数据库</font>",
        CYL_STYLE + "fillColor=#ffffff;strokeColor=#f87171;",
        210,
        830,
        170,
        90,
    )
    add_vertex(
        root,
        51,
        "<b>Redis</b><br/><font style=\"color:#64748b;\">缓存</font>",
        CYL_STYLE + "fillColor=#ffffff;strokeColor=#f87171;",
        470,
        830,
        170,
        90,
    )
    add_vertex(
        root,
        52,
        "<b>RabbitMQ</b><br/><font style=\"color:#64748b;\">消息队列</font>",
        CYL_STYLE + "fillColor=#ffffff;strokeColor=#f87171;",
        730,
        830,
        170,
        90,
    )
    add_vertex(
        root,
        53,
        "<b>MinIO</b><br/><font style=\"color:#64748b;\">对象存储</font>",
        CYL_STYLE + "fillColor=#ffffff;strokeColor=#f87171;",
        990,
        830,
        170,
        90,
    )

    add_vertex(
        root,
        60,
        "API 路由白名单",
        BOX_STYLE + "fillColor=#ffffff;strokeColor=#cbd5e1;fontColor=#334155;",
        230,
        1025,
        180,
        56,
    )
    add_vertex(
        root,
        61,
        "参数校验",
        BOX_STYLE + "fillColor=#ffffff;strokeColor=#cbd5e1;fontColor=#334155;",
        470,
        1025,
        180,
        56,
    )
    add_vertex(
        root,
        62,
        "审计日志",
        BOX_STYLE + "fillColor=#ffffff;strokeColor=#cbd5e1;fontColor=#334155;",
        710,
        1025,
        180,
        56,
    )
    add_vertex(
        root,
        63,
        "安全管控",
        BOX_STYLE + "fillColor=#ffffff;strokeColor=#cbd5e1;fontColor=#334155;",
        950,
        1025,
        180,
        56,
    )

    add_edge(root, 100, 20, 21, "请求进入")
    add_edge(root, 101, 21, 30, "统一入口", points=[(670, 260), (400, 260), (400, 360)])
    add_edge(root, 102, 21, 31, "业务请求", points=[(670, 260), (725, 260), (725, 360)])
    add_edge(root, 103, 22, 31, "路由转发", points=[(1070, 260), (1070, 320), (725, 320), (725, 360)])
    add_edge(root, 104, 22, 32, "触达请求", points=[(1070, 260), (1070, 360)])
    add_edge(root, 105, 30, 32, "事件触发")
    add_edge(root, 106, 31, 32, "构建通知事件")
    add_edge(root, 107, 32, 33, "关键动作")
    add_edge(root, 108, 32, 40, "异步投递", points=[(1040, 480), (1040, 560), (510, 560), (510, 600)])
    add_edge(root, 109, 40, 41, "消息消费")
    add_edge(root, 110, 41, 42, "处理结果")
    add_edge(root, 111, 41, 50, "持久化", points=[(805, 700), (805, 790), (295, 790), (295, 830)])
    add_edge(root, 112, 41, 51, "缓存更新", points=[(805, 700), (805, 790), (555, 790), (555, 830)])
    add_edge(root, 113, 40, 52, "队列支撑")
    add_edge(root, 114, 42, 53, "附件存储", points=[(1105, 700), (1105, 830)])
    add_edge(root, 115, 22, 60, "路由治理", dashed=True, points=[(1070, 260), (1070, 960), (320, 960), (320, 1025)])
    add_edge(root, 116, 21, 61, "参数校验", dashed=True, points=[(670, 260), (670, 1025), (560, 1025)])
    add_edge(root, 117, 41, 62, "审计记录", dashed=True, points=[(805, 700), (805, 1025)])
    add_edge(root, 118, 32, 63, "权限/策略", dashed=True, points=[(1040, 480), (1040, 1025)])

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
        f"<diagram id=\"access-notification-arch\" name=\"接入-业务-通知分层架构\">{compressed}</diagram>"
        "</mxfile>"
    )


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    mxgraph_xml = build_mxgraph()
    xml_path = OUT_DIR / "access-business-notification-architecture.mxgraph.xml"
    drawio_path = OUT_DIR / "access-business-notification-architecture.drawio"
    xml_path.write_text(mxgraph_xml, encoding="utf-8")
    drawio_path.write_text(build_drawio(mxgraph_xml), encoding="utf-8")
    print(xml_path)
    print(drawio_path)


if __name__ == "__main__":
    main()
