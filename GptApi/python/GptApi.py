import openai


commit_diff = """
diff --git a/utils.py b/utils.py
index e69de29..b6fc4c6 100644
--- a/utils.py
+++ b/utils.py
@@ def calculate():
-    return a + b
+    try:
+        return a + b
+    except Exception as e:
+        print(e)
+        return None
"""

response = openai.chat.completions.create(
    model="gpt-4",
    messages=[
        {"role": "system", "content": "너는 소스 코드 변경을 분석해서 요약해주는 AI야."},
        {"role": "user", "content": f"아래 Git diff를 요약해줘:\n\n{commit_diff}\n\n한 줄 요약으로 부탁해."}
    ],
    temperature=0.2
)

print(response.choices[0].message.content)
