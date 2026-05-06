import json
import os

log_file = r"C:\Users\andim\.gemini\antigravity\brain\4045911d-6d90-4012-98ea-f48dbe62b242\.system_generated\logs\overview.txt"

with open(log_file, "r", encoding="utf-8") as f:
    for line in f:
        try:
            data = json.loads(line)
            if data.get("type") == "VIEW_FILE" and "GamePanel.java" in data.get("content", ""):
                print(f"Step {data.get('step_index')}: {data.get('content')[:200]}...")
        except:
            pass
