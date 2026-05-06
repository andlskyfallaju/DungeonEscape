import zipfile
import xml.etree.ElementTree as ET
import os
import sys

# Ensure output is UTF-8
if sys.stdout.encoding != 'utf-8':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def extract_text_from_pptx(path, output_file):
    if not os.path.exists(path):
        print(f"File not found: {path}")
        return

    try:
        with zipfile.ZipFile(path, 'r') as zip_ref:
            slide_files = sorted([f for f in zip_ref.namelist() if f.startswith('ppt/slides/slide') and f.endswith('.xml')], 
                                 key=lambda x: int(x.replace('ppt/slides/slide', '').replace('.xml', '')))
            
            with open(output_file, 'w', encoding='utf-8') as f:
                for slide_file in slide_files:
                    slide_content = zip_ref.read(slide_file)
                    tree = ET.fromstring(slide_content)
                    
                    namespaces = {'a': 'http://schemas.openxmlformats.org/drawingml/2006/main',
                                  'p': 'http://schemas.openxmlformats.org/presentationml/2006/main'}
                    
                    title = ""
                    for sp in tree.findall('.//p:sp', namespaces):
                        ph = sp.find('.//p:ph', namespaces)
                        if ph is not None and ph.get('type') in ['title', 'ctrTitle']:
                            title_parts = [t.text for t in sp.findall('.//a:t', namespaces) if t.text]
                            title = " ".join(title_parts)
                            break
                    
                    all_text = [t.text for t in tree.findall('.//a:t', namespaces) if t.text]
                    
                    slide_num = slide_file.replace('ppt/slides/slide', '').replace('.xml', '')
                    f.write(f"=== Slide {slide_num}: {title} ===\n")
                    f.write(" ".join(all_text) + "\n")
                    f.write("-" * 40 + "\n")
            
            print(f"Extraction complete. Results saved to {output_file}")
            
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    extract_pptx_path = "Dungeon_Escape_Presentation_v2.pptx"
    output_path = "scratch/slides_text.txt"
    extract_text_from_pptx(extract_pptx_path, output_path)
