import base64
import html
import urllib.parse
import uuid
import zlib
from datetime import datetime, timezone
from pathlib import Path
import xml.etree.ElementTree as ET


OUT_DIR = Path(r"D:\code\aaaaljt\docs\diagrams\flows")

NODE_STYLES = {
    "start": (
        "ellipse;whiteSpace=wrap;html=1;fillColor=#dcfce7;strokeColor=#166534;"
        "fontColor=#111827;fontFamily=Microsoft YaHei;fontSize=14;"
    ),
    "end": (
        "ellipse;whiteSpace=wrap;html=1;fillColor=#fee2e2;strokeColor=#991b1b;"
        "fontColor=#111827;fontFamily=Microsoft YaHei;fontSize=14;"
    ),
    "process": (
        "rounded=1;whiteSpace=wrap;html=1;fillColor=#eef2ff;strokeColor=#1f2937;"
        "fontColor=#111827;fontFamily=Microsoft YaHei;fontSize=14;arcSize=12;"
    ),
    "decision": (
        "rhombus;whiteSpace=wrap;html=1;fillColor=#fef3c7;strokeColor=#92400e;"
        "fontColor=#111827;fontFamily=Microsoft YaHei;fontSize=14;"
    ),
}

EDGE_STYLE = (
    "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;"
    "strokeColor=#475569;fontColor=#334155;fontFamily=Microsoft YaHei;fontSize=12;"
    "endArrow=block;endFill=1;"
)


def compress_xml(xml_bytes):
    payload = urllib.parse.quote(xml_bytes.decode("utf-8"), safe="").encode("utf-8")
    compressor = zlib.compressobj(level=9, wbits=-15)
    raw = compressor.compress(payload) + compressor.flush()
    return base64.b64encode(raw).decode("ascii")


def build_drawio(title, nodes, edges):
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
            "pageWidth": "1800",
            "pageHeight": "1600",
            "math": "0",
            "shadow": "0",
            "background": "#ffffff",
        },
    )
    root = ET.SubElement(model, "root")
    ET.SubElement(root, "mxCell", {"id": "0"})
    ET.SubElement(root, "mxCell", {"id": "1", "parent": "0"})

    next_id = 2
    ids = {}

    for node in nodes:
        node_id = str(next_id)
        next_id += 1
        ids[node["id"]] = node_id
        cell = ET.SubElement(
            root,
            "mxCell",
            {
                "id": node_id,
                "value": html.escape(node["label"]),
                "style": NODE_STYLES[node["kind"]],
                "vertex": "1",
                "parent": "1",
            },
        )
        ET.SubElement(
            cell,
            "mxGeometry",
            {
                "x": str(node["x"]),
                "y": str(node["y"]),
                "width": str(node["w"]),
                "height": str(node["h"]),
                "as": "geometry",
            },
        )

    for edge in edges:
        cell = ET.SubElement(
            root,
            "mxCell",
            {
                "id": str(next_id),
                "value": edge.get("label", ""),
                "style": EDGE_STYLE,
                "edge": "1",
                "parent": "1",
                "source": ids[edge["source"]],
                "target": ids[edge["target"]],
            },
        )
        next_id += 1
        ET.SubElement(cell, "mxGeometry", {"relative": "1", "as": "geometry"})

    mxfile = ET.Element(
        "mxfile",
        {
            "host": "app.diagrams.net",
            "modified": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
            "agent": "Codex",
            "version": "26.0.11",
            "type": "device",
        },
    )
    diagram = ET.SubElement(
        mxfile,
        "diagram",
        {
            "id": str(uuid.uuid4())[:8],
            "name": title,
        },
    )
    diagram.text = compress_xml(ET.tostring(model, encoding="utf-8"))
    return ET.tostring(mxfile, encoding="utf-8", xml_declaration=True)


DIAGRAMS = [
    {
        "stem": "flow-login-register",
        "title": "登录与短信验证码流程图",
        "mermaid": """flowchart TD
    A((开始)) --> B{用户选择}
    B -->|注册| C[输入手机号]
    C --> D[请求发送验证码]
    D --> E{60秒内已发送?}
    E -->|是| F[提示稍后重试]
    F --> Z((结束))
    E -->|否| G[生成并发送验证码]
    G --> H[输入验证码]
    H --> I{验证码正确?}
    I -->|否| J[提示验证码错误]
    J --> Z
    I -->|是| K[输入用户名和密码]
    K --> L{用户名已存在?}
    L -->|是| M[提示用户名已存在]
    M --> Z
    L -->|否| N[创建账号]
    N --> O[返回注册成功]
    O --> Z
    B -->|登录| P[输入用户名和密码]
    P --> Q[校验账号密码]
    Q --> R{校验通过?}
    R -->|否| S[提示账号或密码错误]
    S --> Z
    R -->|是| T[查询用户ID并签发Token]
    T --> U[进入系统首页]
    U --> Z""",
        "nodes": [
            {"id": "A", "kind": "start", "label": "开始", "x": 470, "y": 30, "w": 110, "h": 50},
            {"id": "B", "kind": "decision", "label": "用户选择", "x": 435, "y": 120, "w": 180, "h": 100},
            {"id": "C", "kind": "process", "label": "输入手机号", "x": 110, "y": 280, "w": 200, "h": 60},
            {"id": "D", "kind": "process", "label": "请求发送验证码", "x": 110, "y": 380, "w": 200, "h": 60},
            {"id": "E", "kind": "decision", "label": "60秒内已发送?", "x": 130, "y": 500, "w": 160, "h": 100},
            {"id": "F", "kind": "process", "label": "提示稍后重试", "x": 10, "y": 650, "w": 180, "h": 60},
            {"id": "G", "kind": "process", "label": "生成并发送验证码", "x": 250, "y": 650, "w": 200, "h": 60},
            {"id": "H", "kind": "process", "label": "输入验证码", "x": 250, "y": 760, "w": 200, "h": 60},
            {"id": "I", "kind": "decision", "label": "验证码正确?", "x": 270, "y": 880, "w": 160, "h": 100},
            {"id": "J", "kind": "process", "label": "提示验证码错误", "x": 20, "y": 1030, "w": 180, "h": 60},
            {"id": "K", "kind": "process", "label": "输入用户名和密码", "x": 250, "y": 1030, "w": 200, "h": 60},
            {"id": "L", "kind": "decision", "label": "用户名已存在?", "x": 270, "y": 1150, "w": 160, "h": 100},
            {"id": "M", "kind": "process", "label": "提示用户名已存在", "x": 20, "y": 1300, "w": 180, "h": 60},
            {"id": "N", "kind": "process", "label": "创建账号", "x": 250, "y": 1300, "w": 200, "h": 60},
            {"id": "O", "kind": "process", "label": "返回注册成功", "x": 250, "y": 1400, "w": 200, "h": 60},
            {"id": "P", "kind": "process", "label": "输入用户名和密码", "x": 730, "y": 300, "w": 200, "h": 60},
            {"id": "Q", "kind": "process", "label": "校验账号密码", "x": 730, "y": 400, "w": 200, "h": 60},
            {"id": "R", "kind": "decision", "label": "校验通过?", "x": 750, "y": 520, "w": 160, "h": 100},
            {"id": "S", "kind": "process", "label": "提示账号或密码错误", "x": 620, "y": 680, "w": 200, "h": 60},
            {"id": "T", "kind": "process", "label": "查询用户ID并签发Token", "x": 860, "y": 680, "w": 220, "h": 60},
            {"id": "U", "kind": "process", "label": "进入系统首页", "x": 860, "y": 790, "w": 220, "h": 60},
            {"id": "Z", "kind": "end", "label": "结束", "x": 470, "y": 1510, "w": 110, "h": 50},
        ],
        "edges": [
            {"source": "A", "target": "B"},
            {"source": "B", "target": "C", "label": "注册"},
            {"source": "C", "target": "D"},
            {"source": "D", "target": "E"},
            {"source": "E", "target": "F", "label": "是"},
            {"source": "F", "target": "Z"},
            {"source": "E", "target": "G", "label": "否"},
            {"source": "G", "target": "H"},
            {"source": "H", "target": "I"},
            {"source": "I", "target": "J", "label": "否"},
            {"source": "J", "target": "Z"},
            {"source": "I", "target": "K", "label": "是"},
            {"source": "K", "target": "L"},
            {"source": "L", "target": "M", "label": "是"},
            {"source": "M", "target": "Z"},
            {"source": "L", "target": "N", "label": "否"},
            {"source": "N", "target": "O"},
            {"source": "O", "target": "Z"},
            {"source": "B", "target": "P", "label": "登录"},
            {"source": "P", "target": "Q"},
            {"source": "Q", "target": "R"},
            {"source": "R", "target": "S", "label": "否"},
            {"source": "S", "target": "Z"},
            {"source": "R", "target": "T", "label": "是"},
            {"source": "T", "target": "U"},
            {"source": "U", "target": "Z"},
        ],
    },
    {
        "stem": "flow-market-order",
        "title": "社区集市下单流程图",
        "mermaid": """flowchart TD
    A((开始)) --> B[浏览或搜索商品]
    B --> C[查看商品详情]
    C --> D{操作方式}
    D -->|加入购物车| E[加入购物车]
    E --> F[查看购物车并勾选商品]
    F --> G[提交订单]
    D -->|立即购买| G
    G --> H{库存充足?}
    H -->|否| I[提示库存不足]
    I --> Z((结束))
    H -->|是| J[扣减库存并创建订单]
    J --> K{支付成功?}
    K -->|否| L[提示下单失败或待支付]
    L --> Z
    K -->|是| M[返回订单成功]
    M --> Z""",
        "nodes": [
            {"id": "A", "kind": "start", "label": "开始", "x": 440, "y": 30, "w": 110, "h": 50},
            {"id": "B", "kind": "process", "label": "浏览或搜索商品", "x": 390, "y": 120, "w": 220, "h": 60},
            {"id": "C", "kind": "process", "label": "查看商品详情", "x": 390, "y": 220, "w": 220, "h": 60},
            {"id": "D", "kind": "decision", "label": "操作方式", "x": 420, "y": 340, "w": 160, "h": 100},
            {"id": "E", "kind": "process", "label": "加入购物车", "x": 130, "y": 520, "w": 200, "h": 60},
            {"id": "F", "kind": "process", "label": "查看购物车并勾选商品", "x": 120, "y": 630, "w": 220, "h": 60},
            {"id": "G", "kind": "process", "label": "提交订单", "x": 390, "y": 740, "w": 220, "h": 60},
            {"id": "H", "kind": "decision", "label": "库存充足?", "x": 420, "y": 860, "w": 160, "h": 100},
            {"id": "I", "kind": "process", "label": "提示库存不足", "x": 160, "y": 1020, "w": 200, "h": 60},
            {"id": "J", "kind": "process", "label": "扣减库存并创建订单", "x": 390, "y": 1020, "w": 220, "h": 60},
            {"id": "K", "kind": "decision", "label": "支付成功?", "x": 420, "y": 1140, "w": 160, "h": 100},
            {"id": "L", "kind": "process", "label": "提示下单失败或待支付", "x": 160, "y": 1300, "w": 220, "h": 60},
            {"id": "M", "kind": "process", "label": "返回订单成功", "x": 420, "y": 1300, "w": 160, "h": 60},
            {"id": "N", "kind": "process", "label": "立即购买", "x": 670, "y": 520, "w": 180, "h": 60},
            {"id": "Z", "kind": "end", "label": "结束", "x": 440, "y": 1430, "w": 110, "h": 50},
        ],
        "edges": [
            {"source": "A", "target": "B"},
            {"source": "B", "target": "C"},
            {"source": "C", "target": "D"},
            {"source": "D", "target": "E", "label": "加入购物车"},
            {"source": "E", "target": "F"},
            {"source": "F", "target": "G"},
            {"source": "D", "target": "N", "label": "立即购买"},
            {"source": "N", "target": "G"},
            {"source": "G", "target": "H"},
            {"source": "H", "target": "I", "label": "否"},
            {"source": "I", "target": "Z"},
            {"source": "H", "target": "J", "label": "是"},
            {"source": "J", "target": "K"},
            {"source": "K", "target": "L", "label": "否"},
            {"source": "L", "target": "Z"},
            {"source": "K", "target": "M", "label": "是"},
            {"source": "M", "target": "Z"},
        ],
    },
    {
        "stem": "flow-activity-review",
        "title": "本地活动发布审核流程图",
        "mermaid": """flowchart TD
    A((开始)) --> B[发起人填写活动信息]
    B --> C[提交活动申请]
    C --> D{表单校验通过?}
    D -->|否| E[提示补全信息]
    E --> Z((结束))
    D -->|是| F[保存为待审核]
    F --> G[管理员查看审核列表]
    G --> H{审核通过?}
    H -->|是| I[发布活动]
    I --> J[通知发起人审核通过]
    J --> Z
    H -->|否| K[退回并填写驳回备注]
    K --> L[通知发起人修改后重提]
    L --> Z""",
        "nodes": [
            {"id": "A", "kind": "start", "label": "开始", "x": 430, "y": 30, "w": 110, "h": 50},
            {"id": "B", "kind": "process", "label": "发起人填写活动信息", "x": 375, "y": 120, "w": 220, "h": 60},
            {"id": "C", "kind": "process", "label": "提交活动申请", "x": 375, "y": 220, "w": 220, "h": 60},
            {"id": "D", "kind": "decision", "label": "表单校验通过?", "x": 405, "y": 340, "w": 160, "h": 100},
            {"id": "E", "kind": "process", "label": "提示补全信息", "x": 140, "y": 500, "w": 180, "h": 60},
            {"id": "F", "kind": "process", "label": "保存为待审核", "x": 375, "y": 500, "w": 220, "h": 60},
            {"id": "G", "kind": "process", "label": "管理员查看审核列表", "x": 375, "y": 610, "w": 220, "h": 60},
            {"id": "H", "kind": "decision", "label": "审核通过?", "x": 405, "y": 730, "w": 160, "h": 100},
            {"id": "I", "kind": "process", "label": "发布活动", "x": 140, "y": 890, "w": 180, "h": 60},
            {"id": "J", "kind": "process", "label": "通知发起人审核通过", "x": 110, "y": 1000, "w": 240, "h": 60},
            {"id": "K", "kind": "process", "label": "退回并填写驳回备注", "x": 640, "y": 890, "w": 220, "h": 60},
            {"id": "L", "kind": "process", "label": "通知发起人修改后重提", "x": 620, "y": 1000, "w": 260, "h": 60},
            {"id": "Z", "kind": "end", "label": "结束", "x": 430, "y": 1130, "w": 110, "h": 50},
        ],
        "edges": [
            {"source": "A", "target": "B"},
            {"source": "B", "target": "C"},
            {"source": "C", "target": "D"},
            {"source": "D", "target": "E", "label": "否"},
            {"source": "E", "target": "Z"},
            {"source": "D", "target": "F", "label": "是"},
            {"source": "F", "target": "G"},
            {"source": "G", "target": "H"},
            {"source": "H", "target": "I", "label": "是"},
            {"source": "I", "target": "J"},
            {"source": "J", "target": "Z"},
            {"source": "H", "target": "K", "label": "否"},
            {"source": "K", "target": "L"},
            {"source": "L", "target": "Z"},
        ],
    },
    {
        "stem": "flow-notification-read",
        "title": "通知查看与已读处理流程图",
        "mermaid": """flowchart TD
    A((开始)) --> B[管理员编写通知内容]
    B --> C[发布通知]
    C --> D[系统生成通知记录]
    D --> E[用户进入通知中心]
    E --> F[加载通知列表和未读数]
    F --> G{用户操作}
    G -->|查看单条| H[打开通知详情]
    H --> I[标记单条已读]
    I --> J[刷新未读数]
    J --> Z((结束))
    G -->|全部已读| K[执行全部已读]
    K --> J
    G -->|无操作| Z""",
        "nodes": [
            {"id": "A", "kind": "start", "label": "开始", "x": 430, "y": 30, "w": 110, "h": 50},
            {"id": "B", "kind": "process", "label": "管理员编写通知内容", "x": 375, "y": 120, "w": 220, "h": 60},
            {"id": "C", "kind": "process", "label": "发布通知", "x": 375, "y": 220, "w": 220, "h": 60},
            {"id": "D", "kind": "process", "label": "系统生成通知记录", "x": 375, "y": 320, "w": 220, "h": 60},
            {"id": "E", "kind": "process", "label": "用户进入通知中心", "x": 375, "y": 420, "w": 220, "h": 60},
            {"id": "F", "kind": "process", "label": "加载通知列表和未读数", "x": 355, "y": 520, "w": 260, "h": 60},
            {"id": "G", "kind": "decision", "label": "用户操作", "x": 405, "y": 640, "w": 160, "h": 100},
            {"id": "H", "kind": "process", "label": "打开通知详情", "x": 120, "y": 820, "w": 180, "h": 60},
            {"id": "I", "kind": "process", "label": "标记单条已读", "x": 120, "y": 920, "w": 180, "h": 60},
            {"id": "K", "kind": "process", "label": "执行全部已读", "x": 650, "y": 820, "w": 180, "h": 60},
            {"id": "J", "kind": "process", "label": "刷新未读数", "x": 375, "y": 1040, "w": 220, "h": 60},
            {"id": "Z", "kind": "end", "label": "结束", "x": 430, "y": 1170, "w": 110, "h": 50},
        ],
        "edges": [
            {"source": "A", "target": "B"},
            {"source": "B", "target": "C"},
            {"source": "C", "target": "D"},
            {"source": "D", "target": "E"},
            {"source": "E", "target": "F"},
            {"source": "F", "target": "G"},
            {"source": "G", "target": "H", "label": "查看单条"},
            {"source": "H", "target": "I"},
            {"source": "I", "target": "J"},
            {"source": "G", "target": "K", "label": "全部已读"},
            {"source": "K", "target": "J"},
            {"source": "G", "target": "Z", "label": "无操作"},
            {"source": "J", "target": "Z"},
        ],
    },
    {
        "stem": "flow-agent-toolcalling-search",
        "title": "Agent 智能检索 Tool Calling 流程图",
        "mermaid": """flowchart TD
    A((开始)) --> B[用户输入检索问题]
    B --> C[创建Agent请求]
    C --> D[解析意图与参数]
    D --> E{需要调用工具?}
    E -->|否| F[直接生成回答]
    F --> Z((结束))
    E -->|是| G[选择结构化搜索工具]
    G --> H[检索候选结果]
    H --> I{需要知识补充?}
    I -->|是| J[调用知识库检索]
    I -->|否| L{需要实时确认?}
    J --> L
    L -->|是| M[调用实时查询工具]
    L -->|否| N[汇总检索结果]
    M --> N
    N --> O[生成答案和推荐卡片]
    O --> P[返回前端展示]
    P --> Z""",
        "nodes": [
            {"id": "A", "kind": "start", "label": "开始", "x": 430, "y": 30, "w": 110, "h": 50},
            {"id": "B", "kind": "process", "label": "用户输入检索问题", "x": 375, "y": 120, "w": 220, "h": 60},
            {"id": "C", "kind": "process", "label": "创建Agent请求", "x": 375, "y": 220, "w": 220, "h": 60},
            {"id": "D", "kind": "process", "label": "解析意图与参数", "x": 375, "y": 320, "w": 220, "h": 60},
            {"id": "E", "kind": "decision", "label": "需要调用工具?", "x": 405, "y": 440, "w": 160, "h": 100},
            {"id": "F", "kind": "process", "label": "直接生成回答", "x": 110, "y": 600, "w": 180, "h": 60},
            {"id": "G", "kind": "process", "label": "选择结构化搜索工具", "x": 410, "y": 600, "w": 220, "h": 60},
            {"id": "H", "kind": "process", "label": "检索候选结果", "x": 410, "y": 700, "w": 220, "h": 60},
            {"id": "I", "kind": "decision", "label": "需要知识补充?", "x": 440, "y": 820, "w": 160, "h": 100},
            {"id": "J", "kind": "process", "label": "调用知识库检索", "x": 150, "y": 980, "w": 200, "h": 60},
            {"id": "L", "kind": "decision", "label": "需要实时确认?", "x": 440, "y": 980, "w": 160, "h": 100},
            {"id": "M", "kind": "process", "label": "调用实时查询工具", "x": 720, "y": 1140, "w": 200, "h": 60},
            {"id": "N", "kind": "process", "label": "汇总检索结果", "x": 440, "y": 1140, "w": 160, "h": 60},
            {"id": "O", "kind": "process", "label": "生成答案和推荐卡片", "x": 390, "y": 1260, "w": 260, "h": 60},
            {"id": "P", "kind": "process", "label": "返回前端展示", "x": 410, "y": 1360, "w": 220, "h": 60},
            {"id": "Z", "kind": "end", "label": "结束", "x": 430, "y": 1480, "w": 110, "h": 50},
        ],
        "edges": [
            {"source": "A", "target": "B"},
            {"source": "B", "target": "C"},
            {"source": "C", "target": "D"},
            {"source": "D", "target": "E"},
            {"source": "E", "target": "F", "label": "否"},
            {"source": "F", "target": "Z"},
            {"source": "E", "target": "G", "label": "是"},
            {"source": "G", "target": "H"},
            {"source": "H", "target": "I"},
            {"source": "I", "target": "J", "label": "是"},
            {"source": "J", "target": "L"},
            {"source": "I", "target": "L", "label": "否"},
            {"source": "L", "target": "M", "label": "是"},
            {"source": "M", "target": "N"},
            {"source": "L", "target": "N", "label": "否"},
            {"source": "N", "target": "O"},
            {"source": "O", "target": "P"},
            {"source": "P", "target": "Z"},
        ],
    },
]


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for diagram in DIAGRAMS:
        (OUT_DIR / f"{diagram['stem']}.mmd").write_text(diagram["mermaid"], encoding="utf-8")
        drawio_bytes = build_drawio(diagram["title"], diagram["nodes"], diagram["edges"])
        (OUT_DIR / f"{diagram['stem']}.drawio").write_bytes(drawio_bytes)
        print(OUT_DIR / f"{diagram['stem']}.drawio")


if __name__ == "__main__":
    main()
