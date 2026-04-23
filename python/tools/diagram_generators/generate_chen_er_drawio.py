import base64
import html
import urllib.parse
import uuid
import zlib
from datetime import datetime, timezone
from pathlib import Path
import xml.etree.ElementTree as ET


OUT_DIR = Path(r"D:\code\aaaaljt\docs\diagrams\er\chen")

NODE_STYLES = {
    "entity": (
        "whiteSpace=wrap;html=1;rounded=0;fillColor=#dae8fc;strokeColor=#6c8ebf;"
        "fontColor=#111827;fontFamily=Microsoft YaHei;fontSize=14;"
    ),
    "attribute": (
        "ellipse;whiteSpace=wrap;html=1;fillColor=#fff2cc;strokeColor=#d6b656;"
        "fontColor=#111827;fontFamily=Microsoft YaHei;fontSize=13;"
    ),
    "relationship": (
        "rhombus;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;"
        "fontColor=#111827;fontFamily=Microsoft YaHei;fontSize=14;"
    ),
    "note": (
        "rounded=1;whiteSpace=wrap;html=1;fillColor=#f8fafc;strokeColor=#94a3b8;"
        "fontColor=#334155;fontFamily=Microsoft YaHei;fontSize=12;dashed=1;"
    ),
}

EDGE_STYLE = (
    "edgeStyle=none;rounded=0;orthogonalLoop=0;jettySize=auto;html=1;"
    "strokeColor=#475569;fontColor=#334155;fontFamily=Microsoft YaHei;fontSize=12;"
    "endArrow=none;startArrow=none;"
)


LABEL_TRANSLATIONS = {
    "&lt;u&gt;user_id&lt;/u&gt;": "&lt;u&gt;用户编号&lt;/u&gt;",
    "&lt;u&gt;id&lt;/u&gt;": "&lt;u&gt;编号&lt;/u&gt;",
    "主键 user_id": "主键 用户编号",
    "主键 id": "主键 编号",
    "userName": "用户名",
    "email": "邮箱",
    "phone": "手机号",
    "address": "地址",
    "avatarKey": "头像键",
    "latitude": "纬度",
    "longitude": "经度",
    "msgId": "消息编号",
    "userId": "用户编号",
    "title": "标题",
    "type": "类型",
    "content": "内容",
    "priority": "优先级",
    "readStatus": "已读状态",
    "createdAt": "创建时间",
    "readAt": "阅读时间",
    "images": "图片集合",
    "visibility": "可见范围",
    "locationText": "位置描述",
    "likesCount": "点赞数",
    "commentsCount": "评论数",
    "created_at": "创建时间",
    "feedId": "动态编号",
    "status": "状态",
    "organizer_user_id": "组织者用户编号",
    "category_code": "分类编码",
    "location_text": "位置描述",
    "start_at": "开始时间",
    "end_at": "结束时间",
    "review_note": "审核备注",
    "activity_id": "活动编号",
    "waitlist_rank": "候补序号",
    "checkin_code": "签到码",
    "author_user_id": "作者用户编号",
    "summary": "摘要",
    "likes": "点赞数",
    "owner_user_id": "模板所属用户编号",
    "weekday": "星期",
    "start_time": "开始时刻",
    "end_time": "结束时刻",
    "tag": "标签",
    "requester_user_id": "发起人用户编号",
    "assignee_user_id": "承接人用户编号",
    "volunteer_slots": "志愿名额",
    "filled_slots": "已报人数",
    "task_id": "任务编号",
    "joined_at": "参与时间",
    "completed_at": "完成时间",
    "category": "分类",
    "doc_type": "文档类型",
    "entity_type": "实体类型",
    "entity_id": "实体编号",
    "knowledge_id": "知识编号",
    "vector_data": "向量数据",
    "session_id": "会话编号",
    "user_question": "用户问题",
    "ai_answer": "智能回答",
    "knowledge_ids": "知识编号集",
    "is_helpful": "是否有帮助",
    "resource": "资源名",
    "action": "动作名",
    "http_method": "请求方式",
    "path_template": "路径模板",
    "path_params": "路径参数",
    "operation_type": "操作类型",
    "enabled": "启用状态",
    "store_key": "存储键",
    "payload_json": "状态内容",
    "reminder_minutes": "提醒分钟数",
    "capacity": "人数上限",
}


def translate_text(text):
    if not isinstance(text, str):
        return text
    translated = text
    for old, new in sorted(LABEL_TRANSLATIONS.items(), key=lambda item: len(item[0]), reverse=True):
        translated = translated.replace(old, new)
    return translated


def compress_xml(xml_bytes):
    payload = urllib.parse.quote(xml_bytes.decode("utf-8"), safe="").encode("utf-8")
    compressor = zlib.compressobj(level=9, wbits=-15)
    raw = compressor.compress(payload) + compressor.flush()
    return base64.b64encode(raw).decode("ascii")


def build_drawio(title, nodes, edges):
    model = ET.Element(
        "mxGraphModel",
        {
            "dx": "1800",
            "dy": "1400",
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
            "pageHeight": "1400",
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
                "value": translate_text(node["label"]),
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
                "value": translate_text(edge.get("label", "")),
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
        "stem": "chen-er-user-notification-feed",
        "title": "用户、通知与社区互动ER图（Chen）",
        "mermaid": """flowchart LR
    subgraph G1["用户、通知与社区互动 ER 图（Chen）"]
        U[用户]
        N[通知]
        F[社区动态]
        C[动态评论]
        R1{"接收"}
        R2{"发布"}
        R3{"发表评论"}
        R4{"属于"}
        R5{"点赞"}
        U_ID((主键 user_id))
        U_NAME((userName))
        U_PHONE((phone))
        U_AVATAR((avatarKey))
        N_ID((主键 id))
        N_TITLE((title))
        N_TYPE((type))
        N_READ((readStatus))
        F_ID((主键 id))
        F_CONTENT((content))
        F_VIS((visibility))
        F_TIME((created_at))
        C_ID((主键 id))
        C_CONTENT((content))
        C_TIME((created_at))
        RL_TIME((created_at))
        U ---|1| R1
        R1 ---|N| N
        U ---|1| R2
        R2 ---|N| F
        U ---|1| R3
        R3 ---|N| C
        F ---|1| R4
        R4 ---|N| C
        U ---|N| R5
        R5 ---|N| F
        R5 --- RL_TIME
        U --- U_ID
        U --- U_NAME
        U --- U_PHONE
        U --- U_AVATAR
        N --- N_ID
        N --- N_TITLE
        N --- N_TYPE
        N --- N_READ
        F --- F_ID
        F --- F_CONTENT
        F --- F_VIS
        F --- F_TIME
        C --- C_ID
        C --- C_CONTENT
        C --- C_TIME
    end""",
        "nodes": [
            {"id": "U", "kind": "entity", "label": "用户", "x": 60, "y": 270, "w": 140, "h": 60},
            {"id": "N", "kind": "entity", "label": "通知", "x": 520, "y": 40, "w": 150, "h": 60},
            {"id": "F", "kind": "entity", "label": "社区动态", "x": 520, "y": 270, "w": 170, "h": 60},
            {"id": "C", "kind": "entity", "label": "动态评论", "x": 900, "y": 270, "w": 170, "h": 60},
            {"id": "R1", "kind": "relationship", "label": "接收", "x": 260, "y": 90, "w": 90, "h": 60},
            {"id": "R2", "kind": "relationship", "label": "发布", "x": 260, "y": 270, "w": 90, "h": 60},
            {"id": "R3", "kind": "relationship", "label": "发表评论", "x": 260, "y": 430, "w": 110, "h": 70},
            {"id": "R4", "kind": "relationship", "label": "属于", "x": 720, "y": 270, "w": 90, "h": 60},
            {"id": "R5", "kind": "relationship", "label": "点赞", "x": 260, "y": 610, "w": 90, "h": 60},
            {"id": "U1", "kind": "attribute", "label": "&lt;u&gt;user_id&lt;/u&gt;", "x": 10, "y": 170, "w": 120, "h": 46},
            {"id": "U2", "kind": "attribute", "label": "userName", "x": 0, "y": 240, "w": 120, "h": 46},
            {"id": "U3", "kind": "attribute", "label": "email", "x": -10, "y": 320, "w": 120, "h": 46},
            {"id": "U4", "kind": "attribute", "label": "phone", "x": 40, "y": 390, "w": 120, "h": 46},
            {"id": "U5", "kind": "attribute", "label": "address", "x": 150, "y": 430, "w": 120, "h": 46},
            {"id": "U6", "kind": "attribute", "label": "avatarKey", "x": 150, "y": 160, "w": 130, "h": 46},
            {"id": "U7", "kind": "attribute", "label": "latitude", "x": 150, "y": 230, "w": 120, "h": 46},
            {"id": "U8", "kind": "attribute", "label": "longitude", "x": 150, "y": 310, "w": 120, "h": 46},
            {"id": "N1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 540, "y": -10, "w": 110, "h": 42},
            {"id": "N2", "kind": "attribute", "label": "msgId", "x": 410, "y": -10, "w": 110, "h": 42},
            {"id": "N3", "kind": "attribute", "label": "userId", "x": 400, "y": 50, "w": 110, "h": 42},
            {"id": "N4", "kind": "attribute", "label": "title", "x": 700, "y": 20, "w": 110, "h": 42},
            {"id": "N5", "kind": "attribute", "label": "type", "x": 700, "y": 80, "w": 110, "h": 42},
            {"id": "N6", "kind": "attribute", "label": "content", "x": 710, "y": 140, "w": 120, "h": 42},
            {"id": "N7", "kind": "attribute", "label": "priority", "x": 540, "y": 130, "w": 120, "h": 42},
            {"id": "N8", "kind": "attribute", "label": "readStatus", "x": 390, "y": 120, "w": 130, "h": 42},
            {"id": "N9", "kind": "attribute", "label": "createdAt", "x": 520, "y": 190, "w": 120, "h": 42},
            {"id": "N10", "kind": "attribute", "label": "readAt", "x": 670, "y": 200, "w": 110, "h": 42},
            {"id": "F1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 540, "y": 220, "w": 100, "h": 40},
            {"id": "F2", "kind": "attribute", "label": "userId", "x": 390, "y": 220, "w": 110, "h": 42},
            {"id": "F3", "kind": "attribute", "label": "content", "x": 690, "y": 230, "w": 110, "h": 42},
            {"id": "F4", "kind": "attribute", "label": "images", "x": 690, "y": 180, "w": 110, "h": 42},
            {"id": "F5", "kind": "attribute", "label": "visibility", "x": 690, "y": 310, "w": 120, "h": 42},
            {"id": "F6", "kind": "attribute", "label": "locationText", "x": 520, "y": 360, "w": 130, "h": 42},
            {"id": "F7", "kind": "attribute", "label": "likesCount", "x": 380, "y": 300, "w": 120, "h": 42},
            {"id": "F8", "kind": "attribute", "label": "commentsCount", "x": 340, "y": 250, "w": 140, "h": 42},
            {"id": "F9", "kind": "attribute", "label": "status", "x": 840, "y": 240, "w": 100, "h": 42},
            {"id": "F10", "kind": "attribute", "label": "created_at", "x": 840, "y": 320, "w": 130, "h": 42},
            {"id": "C1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 930, "y": 220, "w": 100, "h": 40},
            {"id": "C2", "kind": "attribute", "label": "feedId", "x": 850, "y": 170, "w": 100, "h": 42},
            {"id": "C3", "kind": "attribute", "label": "userId", "x": 1020, "y": 170, "w": 100, "h": 42},
            {"id": "C4", "kind": "attribute", "label": "content", "x": 1080, "y": 250, "w": 110, "h": 42},
            {"id": "C5", "kind": "attribute", "label": "status", "x": 1080, "y": 320, "w": 100, "h": 42},
            {"id": "C6", "kind": "attribute", "label": "created_at", "x": 930, "y": 350, "w": 130, "h": 42},
            {"id": "R5A", "kind": "attribute", "label": "created_at", "x": 220, "y": 700, "w": 130, "h": 42},
            {"id": "NOTE", "kind": "note", "label": "商品相关实体按要求暂不纳入本组 ER 图。", "x": 870, "y": 40, "w": 220, "h": 50},
        ],
        "edges": [
            {"source": "U", "target": "R1", "label": "1"},
            {"source": "R1", "target": "N", "label": "N"},
            {"source": "U", "target": "R2", "label": "1"},
            {"source": "R2", "target": "F", "label": "N"},
            {"source": "U", "target": "R3", "label": "1"},
            {"source": "R3", "target": "C", "label": "N"},
            {"source": "F", "target": "R4", "label": "1"},
            {"source": "R4", "target": "C", "label": "N"},
            {"source": "U", "target": "R5", "label": "N"},
            {"source": "R5", "target": "F", "label": "N"},
            {"source": "R5", "target": "R5A"},
            {"source": "U", "target": "U1"},
            {"source": "U", "target": "U2"},
            {"source": "U", "target": "U3"},
            {"source": "U", "target": "U4"},
            {"source": "U", "target": "U5"},
            {"source": "U", "target": "U6"},
            {"source": "U", "target": "U7"},
            {"source": "U", "target": "U8"},
            {"source": "N", "target": "N1"},
            {"source": "N", "target": "N2"},
            {"source": "N", "target": "N3"},
            {"source": "N", "target": "N4"},
            {"source": "N", "target": "N5"},
            {"source": "N", "target": "N6"},
            {"source": "N", "target": "N7"},
            {"source": "N", "target": "N8"},
            {"source": "N", "target": "N9"},
            {"source": "N", "target": "N10"},
            {"source": "F", "target": "F1"},
            {"source": "F", "target": "F2"},
            {"source": "F", "target": "F3"},
            {"source": "F", "target": "F4"},
            {"source": "F", "target": "F5"},
            {"source": "F", "target": "F6"},
            {"source": "F", "target": "F7"},
            {"source": "F", "target": "F8"},
            {"source": "F", "target": "F9"},
            {"source": "F", "target": "F10"},
            {"source": "C", "target": "C1"},
            {"source": "C", "target": "C2"},
            {"source": "C", "target": "C3"},
            {"source": "C", "target": "C4"},
            {"source": "C", "target": "C5"},
            {"source": "C", "target": "C6"},
        ],
    },
    {
        "stem": "chen-er-local-activity",
        "title": "本地活动ER图（Chen）",
        "mermaid": """flowchart LR
    subgraph G2["本地活动 ER 图（Chen）"]
        U[用户]
        AA[活动审核表]
        A[本地活动]
        E[活动报名]
        S[活动故事]
        T[日程模板]
        TG[活动标签]
        R1{"提交审核"}
        R2{"审核通过"}
        R3{"组织"}
        R4{"报名"}
        R5{"对应"}
        R6{"撰写"}
        R7{"关联"}
        R8{"创建模板"}
        R9{"包含"}
        U ---|1| R1
        R1 ---|N| AA
        AA ---|1| R2
        R2 ---|1| A
        U ---|1| R3
        R3 ---|N| A
        U ---|1| R4
        R4 ---|N| E
        A ---|1| R5
        R5 ---|N| E
        U ---|1| R6
        R6 ---|N| S
        A ---|1| R7
        R7 ---|N| S
        U ---|1| R8
        R8 ---|N| T
        A ---|1| R9
        R9 ---|N| TG
    end""",
        "nodes": [
            {"id": "U", "kind": "entity", "label": "用户", "x": 50, "y": 340, "w": 140, "h": 60},
            {"id": "AA", "kind": "entity", "label": "活动审核表", "x": 420, "y": 60, "w": 170, "h": 60},
            {"id": "A", "kind": "entity", "label": "本地活动", "x": 780, "y": 60, "w": 160, "h": 60},
            {"id": "E", "kind": "entity", "label": "活动报名", "x": 1080, "y": 300, "w": 160, "h": 60},
            {"id": "S", "kind": "entity", "label": "活动故事", "x": 780, "y": 560, "w": 160, "h": 60},
            {"id": "T", "kind": "entity", "label": "日程模板", "x": 420, "y": 560, "w": 160, "h": 60},
            {"id": "TG", "kind": "entity", "label": "活动标签", "x": 1080, "y": 60, "w": 160, "h": 60},
            {"id": "R1", "kind": "relationship", "label": "提交审核", "x": 220, "y": 120, "w": 100, "h": 60},
            {"id": "R2", "kind": "relationship", "label": "审核通过", "x": 620, "y": 60, "w": 100, "h": 60},
            {"id": "R3", "kind": "relationship", "label": "组织", "x": 320, "y": 250, "w": 90, "h": 60},
            {"id": "R4", "kind": "relationship", "label": "报名", "x": 250, "y": 340, "w": 90, "h": 60},
            {"id": "R5", "kind": "relationship", "label": "对应", "x": 930, "y": 300, "w": 90, "h": 60},
            {"id": "R6", "kind": "relationship", "label": "撰写", "x": 250, "y": 570, "w": 90, "h": 60},
            {"id": "R7", "kind": "relationship", "label": "关联", "x": 800, "y": 410, "w": 90, "h": 60},
            {"id": "R8", "kind": "relationship", "label": "创建模板", "x": 220, "y": 660, "w": 100, "h": 60},
            {"id": "R9", "kind": "relationship", "label": "包含", "x": 970, "y": 60, "w": 90, "h": 60},
            {"id": "U1", "kind": "attribute", "label": "&lt;u&gt;user_id&lt;/u&gt;", "x": 0, "y": 250, "w": 120, "h": 44},
            {"id": "U2", "kind": "attribute", "label": "userName", "x": 0, "y": 420, "w": 120, "h": 44},
            {"id": "U3", "kind": "attribute", "label": "phone", "x": 140, "y": 420, "w": 110, "h": 44},
            {"id": "AA1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 450, "y": 10, "w": 100, "h": 40},
            {"id": "AA2", "kind": "attribute", "label": "organizer_user_id", "x": 290, "y": 0, "w": 150, "h": 40},
            {"id": "AA3", "kind": "attribute", "label": "title", "x": 350, "y": 130, "w": 100, "h": 40},
            {"id": "AA4", "kind": "attribute", "label": "category_code", "x": 520, "y": 140, "w": 130, "h": 40},
            {"id": "AA5", "kind": "attribute", "label": "location_text", "x": 610, "y": 0, "w": 130, "h": 40},
            {"id": "AA6", "kind": "attribute", "label": "start_at", "x": 350, "y": 180, "w": 100, "h": 40},
            {"id": "AA7", "kind": "attribute", "label": "end_at", "x": 470, "y": 190, "w": 100, "h": 40},
            {"id": "AA8", "kind": "attribute", "label": "status", "x": 590, "y": 190, "w": 100, "h": 40},
            {"id": "AA9", "kind": "attribute", "label": "review_note", "x": 700, "y": 120, "w": 120, "h": 40},
            {"id": "AA10", "kind": "attribute", "label": "created_at", "x": 710, "y": 180, "w": 120, "h": 40},
            {"id": "A1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 810, "y": 10, "w": 100, "h": 40},
            {"id": "A2", "kind": "attribute", "label": "organizer_user_id", "x": 940, "y": 0, "w": 150, "h": 40},
            {"id": "A3", "kind": "attribute", "label": "title", "x": 700, "y": 130, "w": 100, "h": 40},
            {"id": "A4", "kind": "attribute", "label": "category_code", "x": 850, "y": 140, "w": 130, "h": 40},
            {"id": "A5", "kind": "attribute", "label": "location_text", "x": 700, "y": 180, "w": 130, "h": 40},
            {"id": "A6", "kind": "attribute", "label": "start_at", "x": 840, "y": 200, "w": 100, "h": 40},
            {"id": "A7", "kind": "attribute", "label": "end_at", "x": 960, "y": 190, "w": 100, "h": 40},
            {"id": "A8", "kind": "attribute", "label": "capacity", "x": 1070, "y": 130, "w": 100, "h": 40},
            {"id": "A9", "kind": "attribute", "label": "status", "x": 1080, "y": 70, "w": 100, "h": 40},
            {"id": "A10", "kind": "attribute", "label": "reminder_minutes", "x": 1030, "y": 200, "w": 140, "h": 40},
            {"id": "E1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 1110, "y": 250, "w": 100, "h": 40},
            {"id": "E2", "kind": "attribute", "label": "activity_id", "x": 1230, "y": 250, "w": 110, "h": 40},
            {"id": "E3", "kind": "attribute", "label": "user_id", "x": 1230, "y": 300, "w": 100, "h": 40},
            {"id": "E4", "kind": "attribute", "label": "status", "x": 1260, "y": 350, "w": 100, "h": 40},
            {"id": "E5", "kind": "attribute", "label": "waitlist_rank", "x": 1100, "y": 380, "w": 130, "h": 40},
            {"id": "E6", "kind": "attribute", "label": "checkin_code", "x": 1010, "y": 250, "w": 120, "h": 40},
            {"id": "E7", "kind": "attribute", "label": "created_at", "x": 1000, "y": 350, "w": 120, "h": 40},
            {"id": "S1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 810, "y": 640, "w": 100, "h": 40},
            {"id": "S2", "kind": "attribute", "label": "activity_id", "x": 960, "y": 520, "w": 110, "h": 40},
            {"id": "S3", "kind": "attribute", "label": "author_user_id", "x": 960, "y": 570, "w": 130, "h": 40},
            {"id": "S4", "kind": "attribute", "label": "title", "x": 690, "y": 520, "w": 100, "h": 40},
            {"id": "S5", "kind": "attribute", "label": "summary", "x": 690, "y": 580, "w": 110, "h": 40},
            {"id": "S6", "kind": "attribute", "label": "visibility", "x": 690, "y": 640, "w": 120, "h": 40},
            {"id": "S7", "kind": "attribute", "label": "likes", "x": 840, "y": 700, "w": 100, "h": 40},
            {"id": "S8", "kind": "attribute", "label": "created_at", "x": 960, "y": 640, "w": 120, "h": 40},
            {"id": "T1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 450, "y": 640, "w": 100, "h": 40},
            {"id": "T2", "kind": "attribute", "label": "owner_user_id", "x": 270, "y": 610, "w": 130, "h": 40},
            {"id": "T3", "kind": "attribute", "label": "title", "x": 350, "y": 520, "w": 100, "h": 40},
            {"id": "T4", "kind": "attribute", "label": "weekday", "x": 310, "y": 570, "w": 110, "h": 40},
            {"id": "T5", "kind": "attribute", "label": "start_time", "x": 520, "y": 520, "w": 120, "h": 40},
            {"id": "T6", "kind": "attribute", "label": "end_time", "x": 560, "y": 580, "w": 110, "h": 40},
            {"id": "T7", "kind": "attribute", "label": "location_text", "x": 520, "y": 640, "w": 130, "h": 40},
            {"id": "T8", "kind": "attribute", "label": "status", "x": 360, "y": 700, "w": 100, "h": 40},
            {"id": "TG1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 1110, "y": 10, "w": 100, "h": 40},
            {"id": "TG2", "kind": "attribute", "label": "activity_id", "x": 1240, "y": 20, "w": 110, "h": 40},
            {"id": "TG3", "kind": "attribute", "label": "tag", "x": 1260, "y": 80, "w": 100, "h": 40},
        ],
        "edges": [
            {"source": "U", "target": "R1", "label": "1"},
            {"source": "R1", "target": "AA", "label": "N"},
            {"source": "AA", "target": "R2", "label": "1"},
            {"source": "R2", "target": "A", "label": "1"},
            {"source": "U", "target": "R3", "label": "1"},
            {"source": "R3", "target": "A", "label": "N"},
            {"source": "U", "target": "R4", "label": "1"},
            {"source": "R4", "target": "E", "label": "N"},
            {"source": "A", "target": "R5", "label": "1"},
            {"source": "R5", "target": "E", "label": "N"},
            {"source": "U", "target": "R6", "label": "1"},
            {"source": "R6", "target": "S", "label": "N"},
            {"source": "A", "target": "R7", "label": "1"},
            {"source": "R7", "target": "S", "label": "N"},
            {"source": "U", "target": "R8", "label": "1"},
            {"source": "R8", "target": "T", "label": "N"},
            {"source": "A", "target": "R9", "label": "1"},
            {"source": "R9", "target": "TG", "label": "N"},
            {"source": "U", "target": "U1"},
            {"source": "U", "target": "U2"},
            {"source": "U", "target": "U3"},
            {"source": "AA", "target": "AA1"},
            {"source": "AA", "target": "AA2"},
            {"source": "AA", "target": "AA3"},
            {"source": "AA", "target": "AA4"},
            {"source": "AA", "target": "AA5"},
            {"source": "AA", "target": "AA6"},
            {"source": "AA", "target": "AA7"},
            {"source": "AA", "target": "AA8"},
            {"source": "AA", "target": "AA9"},
            {"source": "AA", "target": "AA10"},
            {"source": "A", "target": "A1"},
            {"source": "A", "target": "A2"},
            {"source": "A", "target": "A3"},
            {"source": "A", "target": "A4"},
            {"source": "A", "target": "A5"},
            {"source": "A", "target": "A6"},
            {"source": "A", "target": "A7"},
            {"source": "A", "target": "A8"},
            {"source": "A", "target": "A9"},
            {"source": "A", "target": "A10"},
            {"source": "E", "target": "E1"},
            {"source": "E", "target": "E2"},
            {"source": "E", "target": "E3"},
            {"source": "E", "target": "E4"},
            {"source": "E", "target": "E5"},
            {"source": "E", "target": "E6"},
            {"source": "E", "target": "E7"},
            {"source": "S", "target": "S1"},
            {"source": "S", "target": "S2"},
            {"source": "S", "target": "S3"},
            {"source": "S", "target": "S4"},
            {"source": "S", "target": "S5"},
            {"source": "S", "target": "S6"},
            {"source": "S", "target": "S7"},
            {"source": "S", "target": "S8"},
            {"source": "T", "target": "T1"},
            {"source": "T", "target": "T2"},
            {"source": "T", "target": "T3"},
            {"source": "T", "target": "T4"},
            {"source": "T", "target": "T5"},
            {"source": "T", "target": "T6"},
            {"source": "T", "target": "T7"},
            {"source": "T", "target": "T8"},
            {"source": "TG", "target": "TG1"},
            {"source": "TG", "target": "TG2"},
            {"source": "TG", "target": "TG3"},
        ],
    },
    {
        "stem": "chen-er-neighbor-support",
        "title": "邻里互助ER图（Chen）",
        "mermaid": """flowchart LR
    subgraph G3["邻里互助 ER 图（Chen）"]
        U[用户]
        TA[互助任务审核表]
        T[邻里互助任务]
        AS[互助报名]
        R1{"提交审核"}
        R2{"审核发布"}
        R3{"发起"}
        R4{"参与"}
        R5{"对应"}
        R6{"承接"}
        U ---|1| R1
        R1 ---|N| TA
        TA ---|1| R2
        R2 ---|1| T
        U ---|1| R3
        R3 ---|N| T
        U ---|1| R4
        R4 ---|N| AS
        T ---|1| R5
        R5 ---|N| AS
        U ---|1| R6
        R6 ---|N| T
    end""",
        "nodes": [
            {"id": "U", "kind": "entity", "label": "用户", "x": 60, "y": 260, "w": 140, "h": 60},
            {"id": "TA", "kind": "entity", "label": "互助任务审核表", "x": 470, "y": 60, "w": 180, "h": 60},
            {"id": "T", "kind": "entity", "label": "邻里互助任务", "x": 470, "y": 280, "w": 180, "h": 60},
            {"id": "AS", "kind": "entity", "label": "互助报名", "x": 910, "y": 500, "w": 160, "h": 60},
            {"id": "R1", "kind": "relationship", "label": "提交审核", "x": 240, "y": 80, "w": 100, "h": 60},
            {"id": "R2", "kind": "relationship", "label": "审核发布", "x": 500, "y": 170, "w": 100, "h": 60},
            {"id": "R3", "kind": "relationship", "label": "发起", "x": 240, "y": 280, "w": 90, "h": 60},
            {"id": "R4", "kind": "relationship", "label": "参与", "x": 240, "y": 520, "w": 90, "h": 60},
            {"id": "R5", "kind": "relationship", "label": "对应", "x": 700, "y": 500, "w": 90, "h": 60},
            {"id": "R6", "kind": "relationship", "label": "承接", "x": 240, "y": 400, "w": 90, "h": 60},
            {"id": "U1", "kind": "attribute", "label": "&lt;u&gt;user_id&lt;/u&gt;", "x": 0, "y": 180, "w": 120, "h": 44},
            {"id": "U2", "kind": "attribute", "label": "userName", "x": 0, "y": 340, "w": 120, "h": 44},
            {"id": "U3", "kind": "attribute", "label": "phone", "x": 130, "y": 350, "w": 110, "h": 44},
            {"id": "TA1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 510, "y": 10, "w": 100, "h": 40},
            {"id": "TA2", "kind": "attribute", "label": "requester_user_id", "x": 300, "y": 10, "w": 150, "h": 40},
            {"id": "TA3", "kind": "attribute", "label": "assignee_user_id", "x": 650, "y": 10, "w": 140, "h": 40},
            {"id": "TA4", "kind": "attribute", "label": "title", "x": 380, "y": 130, "w": 100, "h": 40},
            {"id": "TA5", "kind": "attribute", "label": "category_code", "x": 650, "y": 130, "w": 130, "h": 40},
            {"id": "TA6", "kind": "attribute", "label": "location_text", "x": 420, "y": 180, "w": 130, "h": 40},
            {"id": "TA7", "kind": "attribute", "label": "priority", "x": 580, "y": 190, "w": 100, "h": 40},
            {"id": "TA8", "kind": "attribute", "label": "status", "x": 700, "y": 190, "w": 100, "h": 40},
            {"id": "TA9", "kind": "attribute", "label": "created_at", "x": 810, "y": 120, "w": 120, "h": 40},
            {"id": "T1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 510, "y": 360, "w": 100, "h": 40},
            {"id": "T2", "kind": "attribute", "label": "requester_user_id", "x": 300, "y": 240, "w": 150, "h": 40},
            {"id": "T3", "kind": "attribute", "label": "assignee_user_id", "x": 660, "y": 240, "w": 140, "h": 40},
            {"id": "T4", "kind": "attribute", "label": "title", "x": 380, "y": 320, "w": 100, "h": 40},
            {"id": "T5", "kind": "attribute", "label": "category_code", "x": 660, "y": 320, "w": 130, "h": 40},
            {"id": "T6", "kind": "attribute", "label": "volunteer_slots", "x": 660, "y": 390, "w": 130, "h": 40},
            {"id": "T7", "kind": "attribute", "label": "filled_slots", "x": 500, "y": 440, "w": 120, "h": 40},
            {"id": "T8", "kind": "attribute", "label": "priority", "x": 360, "y": 390, "w": 100, "h": 40},
            {"id": "T9", "kind": "attribute", "label": "status", "x": 500, "y": 220, "w": 100, "h": 40},
            {"id": "T10", "kind": "attribute", "label": "created_at", "x": 820, "y": 320, "w": 120, "h": 40},
            {"id": "AS1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 940, "y": 450, "w": 100, "h": 40},
            {"id": "AS2", "kind": "attribute", "label": "task_id", "x": 1080, "y": 450, "w": 100, "h": 40},
            {"id": "AS3", "kind": "attribute", "label": "user_id", "x": 1080, "y": 500, "w": 100, "h": 40},
            {"id": "AS4", "kind": "attribute", "label": "status", "x": 1100, "y": 550, "w": 100, "h": 40},
            {"id": "AS5", "kind": "attribute", "label": "joined_at", "x": 920, "y": 600, "w": 110, "h": 40},
            {"id": "AS6", "kind": "attribute", "label": "completed_at", "x": 1070, "y": 610, "w": 130, "h": 40},
        ],
        "edges": [
            {"source": "U", "target": "R1", "label": "1"},
            {"source": "R1", "target": "TA", "label": "N"},
            {"source": "TA", "target": "R2", "label": "1"},
            {"source": "R2", "target": "T", "label": "1"},
            {"source": "U", "target": "R3", "label": "1"},
            {"source": "R3", "target": "T", "label": "N"},
            {"source": "U", "target": "R4", "label": "1"},
            {"source": "R4", "target": "AS", "label": "N"},
            {"source": "T", "target": "R5", "label": "1"},
            {"source": "R5", "target": "AS", "label": "N"},
            {"source": "U", "target": "R6", "label": "1"},
            {"source": "R6", "target": "T", "label": "N"},
            {"source": "U", "target": "U1"},
            {"source": "U", "target": "U2"},
            {"source": "U", "target": "U3"},
            {"source": "TA", "target": "TA1"},
            {"source": "TA", "target": "TA2"},
            {"source": "TA", "target": "TA3"},
            {"source": "TA", "target": "TA4"},
            {"source": "TA", "target": "TA5"},
            {"source": "TA", "target": "TA6"},
            {"source": "TA", "target": "TA7"},
            {"source": "TA", "target": "TA8"},
            {"source": "TA", "target": "TA9"},
            {"source": "T", "target": "T1"},
            {"source": "T", "target": "T2"},
            {"source": "T", "target": "T3"},
            {"source": "T", "target": "T4"},
            {"source": "T", "target": "T5"},
            {"source": "T", "target": "T6"},
            {"source": "T", "target": "T7"},
            {"source": "T", "target": "T8"},
            {"source": "T", "target": "T9"},
            {"source": "T", "target": "T10"},
            {"source": "AS", "target": "AS1"},
            {"source": "AS", "target": "AS2"},
            {"source": "AS", "target": "AS3"},
            {"source": "AS", "target": "AS4"},
            {"source": "AS", "target": "AS5"},
            {"source": "AS", "target": "AS6"},
        ],
    },
    {
        "stem": "chen-er-agent-governance",
        "title": "智能检索与治理辅助ER图（Chen）",
        "mermaid": """flowchart LR
    subgraph G4["智能检索与治理辅助 ER 图（Chen）"]
        KB[知识库]
        KV[向量数据]
        CF[对话反馈]
        AR[接口路由]
        GS[治理状态]
        R1{"向量化"}
        R2{"反馈引用"}
        KB ---|1| R1
        R1 ---|N| KV
        KB ---|N| R2
        R2 ---|N| CF
    end""",
        "nodes": [
            {"id": "KB", "kind": "entity", "label": "知识库", "x": 410, "y": 120, "w": 150, "h": 60},
            {"id": "KV", "kind": "entity", "label": "向量数据", "x": 860, "y": 120, "w": 150, "h": 60},
            {"id": "CF", "kind": "entity", "label": "对话反馈", "x": 860, "y": 470, "w": 150, "h": 60},
            {"id": "AR", "kind": "entity", "label": "接口路由", "x": 70, "y": 120, "w": 150, "h": 60},
            {"id": "GS", "kind": "entity", "label": "治理状态", "x": 70, "y": 470, "w": 150, "h": 60},
            {"id": "R1", "kind": "relationship", "label": "向量化", "x": 640, "y": 120, "w": 90, "h": 60},
            {"id": "R2", "kind": "relationship", "label": "反馈引用", "x": 640, "y": 470, "w": 100, "h": 60},
            {"id": "KB1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 440, "y": 60, "w": 100, "h": 40},
            {"id": "KB2", "kind": "attribute", "label": "category", "x": 300, "y": 70, "w": 110, "h": 40},
            {"id": "KB3", "kind": "attribute", "label": "doc_type", "x": 300, "y": 130, "w": 110, "h": 40},
            {"id": "KB4", "kind": "attribute", "label": "title", "x": 450, "y": 200, "w": 100, "h": 40},
            {"id": "KB5", "kind": "attribute", "label": "summary", "x": 580, "y": 200, "w": 110, "h": 40},
            {"id": "KB6", "kind": "attribute", "label": "entity_type", "x": 580, "y": 70, "w": 120, "h": 40},
            {"id": "KB7", "kind": "attribute", "label": "entity_id", "x": 580, "y": 130, "w": 110, "h": 40},
            {"id": "KB8", "kind": "attribute", "label": "priority", "x": 410, "y": 10, "w": 100, "h": 40},
            {"id": "KB9", "kind": "attribute", "label": "status", "x": 520, "y": 10, "w": 100, "h": 40},
            {"id": "KB10", "kind": "attribute", "label": "created_at", "x": 700, "y": 200, "w": 120, "h": 40},
            {"id": "KV1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 890, "y": 60, "w": 100, "h": 40},
            {"id": "KV2", "kind": "attribute", "label": "knowledge_id", "x": 1020, "y": 100, "w": 120, "h": 40},
            {"id": "KV3", "kind": "attribute", "label": "vector_data", "x": 1020, "y": 160, "w": 120, "h": 40},
            {"id": "KV4", "kind": "attribute", "label": "created_at", "x": 860, "y": 210, "w": 120, "h": 40},
            {"id": "CF1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 890, "y": 540, "w": 100, "h": 40},
            {"id": "CF2", "kind": "attribute", "label": "session_id", "x": 1020, "y": 430, "w": 110, "h": 40},
            {"id": "CF3", "kind": "attribute", "label": "user_question", "x": 1020, "y": 490, "w": 120, "h": 40},
            {"id": "CF4", "kind": "attribute", "label": "ai_answer", "x": 1020, "y": 550, "w": 110, "h": 40},
            {"id": "CF5", "kind": "attribute", "label": "knowledge_ids", "x": 860, "y": 610, "w": 120, "h": 40},
            {"id": "CF6", "kind": "attribute", "label": "is_helpful", "x": 1110, "y": 610, "w": 110, "h": 40},
            {"id": "CF7", "kind": "attribute", "label": "created_at", "x": 860, "y": 430, "w": 120, "h": 40},
            {"id": "AR1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 100, "y": 60, "w": 100, "h": 40},
            {"id": "AR2", "kind": "attribute", "label": "resource", "x": 0, "y": 130, "w": 110, "h": 40},
            {"id": "AR3", "kind": "attribute", "label": "action", "x": 210, "y": 130, "w": 100, "h": 40},
            {"id": "AR4", "kind": "attribute", "label": "http_method", "x": 0, "y": 190, "w": 120, "h": 40},
            {"id": "AR5", "kind": "attribute", "label": "path_template", "x": 210, "y": 190, "w": 130, "h": 40},
            {"id": "AR6", "kind": "attribute", "label": "path_params", "x": 0, "y": 250, "w": 120, "h": 40},
            {"id": "AR7", "kind": "attribute", "label": "operation_type", "x": 210, "y": 250, "w": 130, "h": 40},
            {"id": "AR8", "kind": "attribute", "label": "enabled", "x": 90, "y": 310, "w": 100, "h": 40},
            {"id": "AR9", "kind": "attribute", "label": "created_at", "x": 210, "y": 310, "w": 120, "h": 40},
            {"id": "GS1", "kind": "attribute", "label": "&lt;u&gt;id&lt;/u&gt;", "x": 100, "y": 540, "w": 100, "h": 40},
            {"id": "GS2", "kind": "attribute", "label": "store_key", "x": 0, "y": 430, "w": 110, "h": 40},
            {"id": "GS3", "kind": "attribute", "label": "payload_json", "x": 210, "y": 430, "w": 120, "h": 40},
            {"id": "GS4", "kind": "attribute", "label": "created_at", "x": 0, "y": 610, "w": 120, "h": 40},
            {"id": "GS5", "kind": "attribute", "label": "updated_at", "x": 210, "y": 610, "w": 120, "h": 40},
            {"id": "NOTE", "kind": "note", "label": "api_routes 与 governance_admin_states 在当前项目中主要作为独立治理配置实体存在。", "x": 30, "y": 300, "w": 310, "h": 60},
        ],
        "edges": [
            {"source": "KB", "target": "R1", "label": "1"},
            {"source": "R1", "target": "KV", "label": "N"},
            {"source": "KB", "target": "R2", "label": "N"},
            {"source": "R2", "target": "CF", "label": "N"},
            {"source": "KB", "target": "KB1"},
            {"source": "KB", "target": "KB2"},
            {"source": "KB", "target": "KB3"},
            {"source": "KB", "target": "KB4"},
            {"source": "KB", "target": "KB5"},
            {"source": "KB", "target": "KB6"},
            {"source": "KB", "target": "KB7"},
            {"source": "KB", "target": "KB8"},
            {"source": "KB", "target": "KB9"},
            {"source": "KB", "target": "KB10"},
            {"source": "KV", "target": "KV1"},
            {"source": "KV", "target": "KV2"},
            {"source": "KV", "target": "KV3"},
            {"source": "KV", "target": "KV4"},
            {"source": "CF", "target": "CF1"},
            {"source": "CF", "target": "CF2"},
            {"source": "CF", "target": "CF3"},
            {"source": "CF", "target": "CF4"},
            {"source": "CF", "target": "CF5"},
            {"source": "CF", "target": "CF6"},
            {"source": "CF", "target": "CF7"},
            {"source": "AR", "target": "AR1"},
            {"source": "AR", "target": "AR2"},
            {"source": "AR", "target": "AR3"},
            {"source": "AR", "target": "AR4"},
            {"source": "AR", "target": "AR5"},
            {"source": "AR", "target": "AR6"},
            {"source": "AR", "target": "AR7"},
            {"source": "AR", "target": "AR8"},
            {"source": "AR", "target": "AR9"},
            {"source": "GS", "target": "GS1"},
            {"source": "GS", "target": "GS2"},
            {"source": "GS", "target": "GS3"},
            {"source": "GS", "target": "GS4"},
            {"source": "GS", "target": "GS5"},
        ],
    },
    {
        "stem": "chen-er-agent-toolcalling-simple",
        "title": "简化 Agent 搜索与工具调用ER图（Chen）",
        "mermaid": """flowchart LR
    subgraph G5["简化 Agent 搜索与工具调用 ER 图（Chen）"]
        M[用户消息]
        R[接口路由]
        C[工具调用记录]
        O[调用结果]
        P1{"匹配接口"}
        P2{"产生结果"}
        M ---|N| P1
        P1 ---|1| R
        M ---|1| C
        R ---|1| C
        C ---|1| P2
        P2 ---|1| O
        M_ID((主键 编号))
        M_CONTENT((消息内容))
        M_INTENT((识别意图))
        M_TIME((创建时间))
        R_ID((主键 编号))
        R_RES((资源名))
        R_ACT((动作名))
        R_DESC((接口描述))
        R_EN((启用状态))
        C_ID((主键 编号))
        C_MSG((消息编号))
        C_ROUTE((路由编号))
        C_REASON((匹配依据))
        C_PARAM((调用参数))
        C_STATUS((调用状态))
        O_ID((主键 编号))
        O_CALL((调用记录编号))
        O_SUM((结果摘要))
        O_DATA((结果数据))
        O_TIME((创建时间))
        M --- M_ID
        M --- M_CONTENT
        M --- M_INTENT
        M --- M_TIME
        R --- R_ID
        R --- R_RES
        R --- R_ACT
        R --- R_DESC
        R --- R_EN
        C --- C_ID
        C --- C_MSG
        C --- C_ROUTE
        C --- C_REASON
        C --- C_PARAM
        C --- C_STATUS
        O --- O_ID
        O --- O_CALL
        O --- O_SUM
        O --- O_DATA
        O --- O_TIME
    end""",
        "nodes": [
            {"id": "M", "kind": "entity", "label": "用户消息", "x": 60, "y": 280, "w": 150, "h": 60},
            {"id": "R", "kind": "entity", "label": "接口路由", "x": 520, "y": 80, "w": 150, "h": 60},
            {"id": "C", "kind": "entity", "label": "工具调用记录", "x": 520, "y": 420, "w": 170, "h": 60},
            {"id": "O", "kind": "entity", "label": "调用结果", "x": 940, "y": 420, "w": 150, "h": 60},
            {"id": "P1", "kind": "relationship", "label": "匹配接口", "x": 280, "y": 120, "w": 100, "h": 60},
            {"id": "P2", "kind": "relationship", "label": "产生结果", "x": 740, "y": 420, "w": 100, "h": 60},
            {"id": "M1", "kind": "attribute", "label": "&lt;u&gt;编号&lt;/u&gt;", "x": 40, "y": 210, "w": 110, "h": 42},
            {"id": "M2", "kind": "attribute", "label": "消息内容", "x": 20, "y": 350, "w": 120, "h": 42},
            {"id": "M3", "kind": "attribute", "label": "识别意图", "x": 150, "y": 360, "w": 120, "h": 42},
            {"id": "M4", "kind": "attribute", "label": "创建时间", "x": 140, "y": 220, "w": 120, "h": 42},
            {"id": "R1", "kind": "attribute", "label": "&lt;u&gt;编号&lt;/u&gt;", "x": 540, "y": 20, "w": 110, "h": 42},
            {"id": "R2", "kind": "attribute", "label": "资源名", "x": 430, "y": 90, "w": 100, "h": 42},
            {"id": "R3", "kind": "attribute", "label": "动作名", "x": 690, "y": 90, "w": 100, "h": 42},
            {"id": "R4", "kind": "attribute", "label": "接口描述", "x": 520, "y": 160, "w": 120, "h": 42},
            {"id": "R5", "kind": "attribute", "label": "启用状态", "x": 670, "y": 160, "w": 120, "h": 42},
            {"id": "C1", "kind": "attribute", "label": "&lt;u&gt;编号&lt;/u&gt;", "x": 540, "y": 360, "w": 110, "h": 42},
            {"id": "C2", "kind": "attribute", "label": "消息编号", "x": 410, "y": 430, "w": 100, "h": 42},
            {"id": "C3", "kind": "attribute", "label": "路由编号", "x": 690, "y": 430, "w": 100, "h": 42},
            {"id": "C4", "kind": "attribute", "label": "匹配依据", "x": 410, "y": 500, "w": 110, "h": 42},
            {"id": "C5", "kind": "attribute", "label": "调用参数", "x": 540, "y": 510, "w": 110, "h": 42},
            {"id": "C6", "kind": "attribute", "label": "调用状态", "x": 680, "y": 500, "w": 110, "h": 42},
            {"id": "O1", "kind": "attribute", "label": "&lt;u&gt;编号&lt;/u&gt;", "x": 960, "y": 360, "w": 110, "h": 42},
            {"id": "O2", "kind": "attribute", "label": "调用记录编号", "x": 1100, "y": 420, "w": 130, "h": 42},
            {"id": "O3", "kind": "attribute", "label": "结果摘要", "x": 1100, "y": 480, "w": 110, "h": 42},
            {"id": "O4", "kind": "attribute", "label": "结果数据", "x": 960, "y": 520, "w": 110, "h": 42},
            {"id": "O5", "kind": "attribute", "label": "创建时间", "x": 820, "y": 420, "w": 110, "h": 42},
        ],
        "edges": [
            {"source": "M", "target": "P1", "label": "N"},
            {"source": "P1", "target": "R", "label": "1"},
            {"source": "M", "target": "C", "label": "1"},
            {"source": "R", "target": "C", "label": "1"},
            {"source": "C", "target": "P2", "label": "1"},
            {"source": "P2", "target": "O", "label": "1"},
            {"source": "M", "target": "M1"},
            {"source": "M", "target": "M2"},
            {"source": "M", "target": "M3"},
            {"source": "M", "target": "M4"},
            {"source": "R", "target": "R1"},
            {"source": "R", "target": "R2"},
            {"source": "R", "target": "R3"},
            {"source": "R", "target": "R4"},
            {"source": "R", "target": "R5"},
            {"source": "C", "target": "C1"},
            {"source": "C", "target": "C2"},
            {"source": "C", "target": "C3"},
            {"source": "C", "target": "C4"},
            {"source": "C", "target": "C5"},
            {"source": "C", "target": "C6"},
            {"source": "O", "target": "O1"},
            {"source": "O", "target": "O2"},
            {"source": "O", "target": "O3"},
            {"source": "O", "target": "O4"},
            {"source": "O", "target": "O5"},
        ],
    },
]


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for diagram in DIAGRAMS:
        drawio_bytes = build_drawio(diagram["title"], diagram["nodes"], diagram["edges"])
        (OUT_DIR / f'{diagram["stem"]}.drawio').write_bytes(drawio_bytes)
        (OUT_DIR / f'{diagram["stem"]}.mmd').write_text(translate_text(diagram["mermaid"]), encoding="utf-8")
    print(f"Generated {len(DIAGRAMS)} Chen ER diagrams in {OUT_DIR}")


if __name__ == "__main__":
    main()
