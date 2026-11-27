"""Auxiliar de captura: copia la última captura del navegador integrado y guarda los rectángulos medidos.
Uso: python docs/guia/add.py nombre '{"clave":[x,y,w,h],...}' [archivo-de-origen]"""
import glob, json, os, shutil, sys
nombre, rects = sys.argv[1], json.loads(sys.argv[2])
origen = sys.argv[3] if len(sys.argv) > 3 else max(glob.glob(os.path.expanduser(
    '~/.claude/projects/C--Users-luisn-Desktop-Trabajos-7mo-MIGAL-SmartOps/56406efa-b850-451c-afd7-020d5c09bd7e/tool-results/mcp-Claude_Browser-blob-*')), key=os.path.getmtime)
base = os.path.dirname(os.path.abspath(__file__))
ext = os.path.splitext(origen)[1]
shutil.copy(origen, os.path.join(base, 'img', nombre + ext))
json.dump({'img': nombre + ext, 'rects': rects}, open(os.path.join(base, 'data', nombre + '.json'), 'w'), ensure_ascii=False)
print('ok', nombre + ext)
