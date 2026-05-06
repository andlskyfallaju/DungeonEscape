import json
import os

log_file = r"C:\Users\andim\.gemini\antigravity\brain\4045911d-6d90-4012-98ea-f48dbe62b242\.system_generated\logs\overview.txt"
step_index = 323

with open(log_file, "r", encoding="utf-8") as f:
    for line in f:
        try:
            data = json.loads(line)
            if data.get("step_index") == step_index:
                with open("recovered_gamepanel.txt", "w", encoding="utf-8") as out:
                    out.write(data.get("content"))
                print("Extracted to recovered_gamepanel.txt")
        except:
            pass
