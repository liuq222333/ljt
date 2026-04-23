# Python Sidecar Runbook

## Start

```powershell
Set-Location python
.\.venv\Scripts\python.exe -m uvicorn query_parser_langchain.api:app --host 127.0.0.1 --port 9001
```

## Test

```powershell
Set-Location python
.\.venv\Scripts\python.exe -m unittest discover -s tests -p "test_*.py" -v
```

## Contract
- Python only handles orchestration and response composition.
- Java remains the authority for search, realtime, knowledge retrieval, and write execution.
- Java falls back to local implementations when the sidecar is unavailable.
