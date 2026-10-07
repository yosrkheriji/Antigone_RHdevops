import os
import sys

sys.stdout.reconfigure(encoding='utf-8')

resources_dir = r'src\main\resources'
pdfs = [f for f in os.listdir(resources_dir) if f.lower().endswith('.pdf')]

if not pdfs:
    print('No PDF found!')
    sys.exit(1)

pdf_path = os.path.join(resources_dir, pdfs[0])
print(f'Reading: {pdf_path}')

try:
    from pdfminer.high_level import extract_text
    text = extract_text(pdf_path)
    out_path = os.path.join(resources_dir, 'reglement_interieur.txt')
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(text)
    print(f'SUCCESS: extracted {len(text)} characters')
    print('--- PREVIEW (first 800 chars) ---')
    print(text[:800])
except Exception as e:
    print(f'ERROR: {e}')
    import traceback
    traceback.print_exc()
