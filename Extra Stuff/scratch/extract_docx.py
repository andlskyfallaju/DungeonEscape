import docx
import os

docx_file = r"C:\Users\andim\OneDrive\Documentos\Uni Work 2025\Year 2\Semester 1\Programming in Java\DungeonEscape\Dungeon_Escape_Technical_Writeup_Extended.docx"

doc = docx.Document(docx_file)
full_text = []
for para in doc.paragraphs:
    full_text.append(para.text)

with open("extracted_writeup.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(full_text))

print("Extracted to extracted_writeup.txt")
