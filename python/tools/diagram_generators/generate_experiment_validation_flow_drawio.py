from pathlib import Path

from generate_flow_drawio import build_drawio


OUT_DIR = Path(r"D:\code\aaaaljt\docs\diagrams\flows")
STEM = "flow-experiment-validation"
TITLE = "实验验证流程图"

MERMAID = """flowchart TD
    A((开始)) --> B[搭建实验环境]
    B --> C[启动后端服务与基础组件]
    C --> D[选择验证对象]

    D --> D1[通知模块]
    D --> D2[社区集市模块]
    D --> D3[本地活动与互助模块]
    D --> D4[智能检索模块]

    D1 --> E[执行业务操作]
    D2 --> E
    D3 --> E
    D4 --> E

    E --> F[采集运行结果]
    F --> F1[功能是否正确]
    F --> F2[状态是否更新]
    F --> F3[通知是否触达]
    F --> F4[调用是否受控]

    F1 --> G{是否满足预期}
    F2 --> G
    F3 --> G
    F4 --> G

    G -->|是| H[记录验证结果]
    G -->|否| I[定位问题并调整系统]
    I --> E

    H --> J[分析关键链路]
    J --> J1[通知触达链路]
    J --> J2[附近检索链路]
    J --> J3[受控调用链路]

    J1 --> K[形成实验结论]
    J2 --> K
    J3 --> K
    K --> L((结束))
"""

NODES = [
    {"id": "A", "kind": "start", "label": "开始", "x": 500, "y": 30, "w": 100, "h": 50},
    {"id": "B", "kind": "process", "label": "搭建实验环境", "x": 440, "y": 110, "w": 220, "h": 60},
    {"id": "C", "kind": "process", "label": "启动后端服务与基础组件", "x": 400, "y": 210, "w": 300, "h": 60},
    {"id": "D", "kind": "process", "label": "选择验证对象", "x": 450, "y": 310, "w": 200, "h": 60},
    {"id": "D1", "kind": "process", "label": "通知模块", "x": 40, "y": 430, "w": 180, "h": 60},
    {"id": "D2", "kind": "process", "label": "社区集市模块", "x": 290, "y": 430, "w": 180, "h": 60},
    {"id": "D3", "kind": "process", "label": "本地活动与互助模块", "x": 540, "y": 430, "w": 220, "h": 60},
    {"id": "D4", "kind": "process", "label": "智能检索模块", "x": 840, "y": 430, "w": 180, "h": 60},
    {"id": "E", "kind": "process", "label": "执行业务操作", "x": 440, "y": 560, "w": 220, "h": 60},
    {"id": "F", "kind": "process", "label": "采集运行结果", "x": 440, "y": 660, "w": 220, "h": 60},
    {"id": "F1", "kind": "process", "label": "功能是否正确", "x": 30, "y": 770, "w": 180, "h": 60},
    {"id": "F2", "kind": "process", "label": "状态是否更新", "x": 290, "y": 770, "w": 180, "h": 60},
    {"id": "F3", "kind": "process", "label": "通知是否触达", "x": 550, "y": 770, "w": 180, "h": 60},
    {"id": "F4", "kind": "process", "label": "调用是否受控", "x": 810, "y": 770, "w": 180, "h": 60},
    {"id": "G", "kind": "decision", "label": "是否满足预期", "x": 460, "y": 900, "w": 180, "h": 100},
    {"id": "H", "kind": "process", "label": "记录验证结果", "x": 700, "y": 1030, "w": 200, "h": 60},
    {"id": "I", "kind": "process", "label": "定位问题并调整系统", "x": 180, "y": 1030, "w": 240, "h": 60},
    {"id": "J", "kind": "process", "label": "分析关键链路", "x": 460, "y": 1140, "w": 180, "h": 60},
    {"id": "J1", "kind": "process", "label": "通知触达链路", "x": 120, "y": 1260, "w": 180, "h": 60},
    {"id": "J2", "kind": "process", "label": "附近检索链路", "x": 430, "y": 1260, "w": 180, "h": 60},
    {"id": "J3", "kind": "process", "label": "受控调用链路", "x": 740, "y": 1260, "w": 180, "h": 60},
    {"id": "K", "kind": "process", "label": "形成实验结论", "x": 440, "y": 1390, "w": 220, "h": 60},
    {"id": "L", "kind": "end", "label": "结束", "x": 500, "y": 1500, "w": 100, "h": 50},
]

EDGES = [
    {"source": "A", "target": "B"},
    {"source": "B", "target": "C"},
    {"source": "C", "target": "D"},
    {"source": "D", "target": "D1"},
    {"source": "D", "target": "D2"},
    {"source": "D", "target": "D3"},
    {"source": "D", "target": "D4"},
    {"source": "D1", "target": "E"},
    {"source": "D2", "target": "E"},
    {"source": "D3", "target": "E"},
    {"source": "D4", "target": "E"},
    {"source": "E", "target": "F"},
    {"source": "F", "target": "F1"},
    {"source": "F", "target": "F2"},
    {"source": "F", "target": "F3"},
    {"source": "F", "target": "F4"},
    {"source": "F1", "target": "G"},
    {"source": "F2", "target": "G"},
    {"source": "F3", "target": "G"},
    {"source": "F4", "target": "G"},
    {"source": "G", "target": "H", "label": "是"},
    {"source": "G", "target": "I", "label": "否"},
    {"source": "I", "target": "E"},
    {"source": "H", "target": "J"},
    {"source": "J", "target": "J1"},
    {"source": "J", "target": "J2"},
    {"source": "J", "target": "J3"},
    {"source": "J1", "target": "K"},
    {"source": "J2", "target": "K"},
    {"source": "J3", "target": "K"},
    {"source": "K", "target": "L"},
]


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / f"{STEM}.mmd").write_text(MERMAID, encoding="utf-8")
    drawio_bytes = build_drawio(TITLE, NODES, EDGES)
    (OUT_DIR / f"{STEM}.drawio").write_bytes(drawio_bytes)
    print(OUT_DIR / f"{STEM}.mmd")
    print(OUT_DIR / f"{STEM}.drawio")


if __name__ == "__main__":
    main()
