from pathlib import Path

from generate_flow_drawio import build_drawio


OUT_DIR = Path(r"D:\code\aaaaljt\docs\diagrams\flows")

MERMAID = """flowchart TD
    A[backend_api 工具入参\\n{resource, action, pathVariables, params, payload, authorization}] --> B

    subgraph L1[输入层]
      A
    end

    subgraph L2[解析层]
      direction LR
      B[路由解析\\nresource + action\\n路由匹配] <--> C[参数解析\\nparams / payload / pathVariables\\n结构化参数]
    end

    subgraph L3[执行层]
      D[查找路由配置\\napi_routes] --> E[状态校验\\nenabled]
      E --> F[参数校验]
      F --> G[URL 拼装]
      G --> H[接口转发]
      H --> I[结果返回]
    end

    B --> D
    C --> F
"""

NODES = [
    {"id": "A", "kind": "process", "label": "backend_api 工具入参\n{resource, action, pathVariables,\nparams, payload, authorization}", "x": 300, "y": 110, "w": 520, "h": 90},
    {"id": "L1", "kind": "process", "label": "输入层", "x": 120, "y": 60, "w": 880, "h": 40},
    {"id": "L2", "kind": "process", "label": "解析层", "x": 120, "y": 250, "w": 880, "h": 40},
    {"id": "B", "kind": "process", "label": "路由解析\nresource + action\n路由匹配", "x": 170, "y": 330, "w": 290, "h": 90},
    {"id": "C", "kind": "process", "label": "参数解析\nparams / payload / pathVariables\n结构化参数", "x": 610, "y": 330, "w": 300, "h": 90},
    {"id": "L3", "kind": "process", "label": "执行层", "x": 120, "y": 480, "w": 880, "h": 40},
    {"id": "D", "kind": "process", "label": "查找路由配置\napi_routes", "x": 180, "y": 560, "w": 250, "h": 70},
    {"id": "E", "kind": "process", "label": "状态校验\nenabled", "x": 180, "y": 660, "w": 250, "h": 70},
    {"id": "F", "kind": "process", "label": "参数校验", "x": 610, "y": 560, "w": 260, "h": 60},
    {"id": "G", "kind": "process", "label": "URL 拼装", "x": 610, "y": 650, "w": 260, "h": 60},
    {"id": "H", "kind": "process", "label": "接口转发", "x": 610, "y": 740, "w": 260, "h": 60},
    {"id": "I", "kind": "end", "label": "结果返回", "x": 650, "y": 840, "w": 180, "h": 60},
]

EDGES = [
    {"source": "A", "target": "B", "label": "输入"},
    {"source": "B", "target": "C", "label": "协同解析"},
    {"source": "C", "target": "B", "label": "参数反馈"},
    {"source": "B", "target": "D", "label": "进入执行"},
    {"source": "D", "target": "E"},
    {"source": "E", "target": "F", "label": "通过"},
    {"source": "C", "target": "F", "label": "结构化参数"},
    {"source": "F", "target": "G"},
    {"source": "G", "target": "H"},
    {"source": "H", "target": "I"},
]


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / "backend-api-tool-call-flow.mmd").write_text(MERMAID, encoding="utf-8")
    drawio = build_drawio("backend_api 工具调用流程图", NODES, EDGES)
    (OUT_DIR / "backend-api-tool-call-flow.drawio").write_bytes(drawio)
    print(OUT_DIR / "backend-api-tool-call-flow.drawio")


if __name__ == "__main__":
    main()
