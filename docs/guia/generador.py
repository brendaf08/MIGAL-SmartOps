"""Generador de la guía de uso: lee las capturas (img/) y los rectángulos medidos (data/), dibuja las flechas numeradas
sobre cada captura como SVG y produce un único archivo HTML autocontenido (guia-de-uso.html).

Uso: python docs/guia/generador.py
"""
import base64
import html
import json
import math
import os

from PIL import Image

BASE = os.path.dirname(os.path.abspath(__file__))
ANCHO = 1000  # unidades del SVG; la altura sale de la proporción de cada captura
COLOR = '#e8590c'


def cargar(nombre):
    datos = json.load(open(os.path.join(BASE, 'data', nombre + '.json'), encoding='utf-8'))
    ruta = os.path.join(BASE, 'img', datos['img'])
    w, h = Image.open(ruta).size
    return datos, ruta, w, h


def punto_en_borde(cx, cy, rect, tx, ty):
    """Punto donde el segmento desde (tx, ty) hacia el centro del rectángulo cruza su borde (con holgura)."""
    x0, y0, x1, y1 = rect
    x0, y0, x1, y1 = x0 - 3, y0 - 3, x1 + 3, y1 + 3
    dx, dy = cx - tx, cy - ty
    if dx == 0 and dy == 0:
        return tx, ty
    mejor = None
    for t in ((x0 - tx) / dx if dx else None, (x1 - tx) / dx if dx else None,
              (y0 - ty) / dy if dy else None, (y1 - ty) / dy if dy else None):
        if t is None or t < 0 or t > 1:
            continue
        px, py = tx + dx * t, ty + dy * t
        if x0 - 0.5 <= px <= x1 + 0.5 and y0 - 0.5 <= py <= y1 + 0.5:
            if mejor is None or t < mejor[0]:
                mejor = (t, px, py)
    return (mejor[1], mejor[2]) if mejor else (cx, cy)


def _inter_seg_rect(ax, ay, bx, by, r):
    """True si el segmento (a→b) atraviesa el interior del rectángulo r (muestreo)."""
    x0, y0, x1, y1 = r
    for k in range(1, 20):
        t = k / 20
        x, y = ax + (bx - ax) * t, ay + (by - ay) * t
        if x0 + 2 < x < x1 - 2 and y0 + 2 < y < y1 - 2:
            return True
    return False


def colocar(rects_px, areas, h_svg, preferencias):
    """Elige la posición de cada insignia minimizando choques con otros marcos, entre insignias y flechas largas."""
    dirs = {'t': (0, -1), 'b': (0, 1), 'l': (-1, 0), 'r': (1, 0), 'tl': (-.7, -.7), 'tr': (.7, -.7), 'bl': (-.7, .7), 'br': (.7, .7)}
    puestos = []
    for idx, (r, pref) in enumerate(zip(rects_px, preferencias)):
        x0, y0, x1, y1 = r
        cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
        mejor = None
        for orden, nombre in enumerate([pref] + [k for k in dirs if k != pref]):
            ux, uy = dirs[nombre]
            for d in (52, 74, 100, 130):
                # El punto de anclaje recorre el borde: centro, y desplazamientos a lo largo del lado mayor.
                for frac in (0.5, 0.15, 0.85):
                    if uy != 0 and ux == 0:
                        ax_, ay_ = x0 + (x1 - x0) * frac, (y0 if uy < 0 else y1)
                    elif ux != 0 and uy == 0:
                        ax_, ay_ = (x0 if ux < 0 else x1), y0 + (y1 - y0) * frac
                    else:
                        ax_, ay_ = (x0 if ux < 0 else x1), (y0 if uy < 0 else y1)
                        if frac != 0.5:
                            continue
                    bx, by = ax_ + ux * d, ay_ + uy * d
                    if not (24 <= bx <= ANCHO - 24 and 24 <= by <= h_svg - 24):
                        continue
                    pen = d * 0.04 + orden * 1.5 + (0 if frac == 0.5 else 1)
                    for k, (r2, area) in enumerate(zip(rects_px, areas)):
                        if k == idx:
                            continue
                        a0, b0, a1, b1 = r2
                        dentro = a0 - 20 < bx < a1 + 20 and b0 - 20 < by < b1 + 20
                        if dentro:
                            pen += 4 if area > 0.12 else 40
                        if _inter_seg_rect(bx, by, ax_, ay_, r2) and area <= 0.12:
                            pen += 12
                    for (px, py) in puestos:
                        if math.hypot(bx - px, by - py) < 46:
                            pen += 60
                    if mejor is None or pen < mejor[0]:
                        mejor = (pen, bx, by)
        puestos.append((mejor[1], mejor[2]) if mejor else (cx, y0 - 50))
    return puestos


def svg_figura(rects, llamadas, h_svg, nombre=''):
    px = []
    for clave, _t, _p in llamadas:
        x, y, w, h = rects[clave]
        px.append((x * ANCHO, y * h_svg, (x + w) * ANCHO, (y + h) * h_svg))
    areas = [(r[2] - r[0]) * (r[3] - r[1]) / (ANCHO * h_svg) for r in px]
    puestos = colocar(px, areas, h_svg, [pos if pos != 'auto' else 't' for _c, _t, pos in llamadas])
    partes = [f'<svg class="flechas" viewBox="0 0 {ANCHO} {h_svg:.1f}" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">',
              f'<defs><marker id="p" markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto">'
              f'<path d="M0,0 L9,4.5 L0,9 z" fill="{COLOR}"/></marker></defs>']
    for n, ((x0, y0, x1, y1), (bx, by)) in enumerate(zip(px, puestos), 1):
        ox, oy = min(max(bx, x0), x1), min(max(by, y0), y1)
        ex, ey = punto_en_borde(ox, oy, (x0, y0, x1, y1), bx, by)
        largo = math.hypot(ex - bx, ey - by)
        partes.append(f'<rect x="{x0 - 3:.1f}" y="{y0 - 3:.1f}" width="{x1 - x0 + 6:.1f}" height="{y1 - y0 + 6:.1f}" rx="7" class="marco"/>')
        if largo > 24:
            ux, uy = (ex - bx) / largo, (ey - by) / largo
            sx, sy = bx + ux * 19, by + uy * 19
            partes.append(f'<line x1="{sx:.1f}" y1="{sy:.1f}" x2="{ex:.1f}" y2="{ey:.1f}" class="halo"/>')
            partes.append(f'<line x1="{sx:.1f}" y1="{sy:.1f}" x2="{ex:.1f}" y2="{ey:.1f}" class="linea" marker-end="url(#p)"/>')
        partes.append(f'<circle cx="{bx:.1f}" cy="{by:.1f}" r="17" class="insignia"/>')
        partes.append(f'<text x="{bx:.1f}" y="{by + 6.5:.1f}" class="num">{n}</text>')
    partes.append('</svg>')
    return ''.join(partes)


def figura(nombre, llamadas, pie=''):
    datos, ruta, w, h = cargar(nombre)
    h_svg = ANCHO * h / w
    b64 = base64.b64encode(open(ruta, 'rb').read()).decode()
    mime = 'image/png' if ruta.endswith('.png') else 'image/jpeg'
    leyenda = ''.join(f'<li><span class="n">{i}</span><span>{texto}</span></li>'
                      for i, (_c, texto, _p) in enumerate(llamadas, 1))
    pie_html = f'<figcaption>{pie}</figcaption>' if pie else ''
    return (f'<figure class="fig"><div class="lienzo"><img src="data:{mime};base64,{b64}" alt="{html.escape(pie or nombre)}">'
            f'{svg_figura(datos["rects"], llamadas, h_svg, nombre)}</div>{pie_html}<ol class="leyenda">{leyenda}</ol></figure>')


# --- Bloques de texto ---------------------------------------------------------------------------------------------
def p(t):
    return f'<p>{t}</p>'


def pasos(*items):
    return '<ol class="pasos">' + ''.join(f'<li>{i}</li>' for i in items) + '</ol>'


def lista(*items):
    return '<ul>' + ''.join(f'<li>{i}</li>' for i in items) + '</ul>'


def consejo(t):
    return f'<div class="aviso consejo"><strong>Consejo</strong><span>{t}</span></div>'


def ojo(t):
    return f'<div class="aviso ojo"><strong>Ojo</strong><span>{t}</span></div>'


def tabla(cabeza, filas, clase=''):
    h = ''.join(f'<th>{c}</th>' for c in cabeza)
    f = ''.join('<tr>' + ''.join(f'<td>{c}</td>' for c in fila) + '</tr>' for fila in filas)
    return f'<div class="tabla-wrap"><table class="{clase}"><thead><tr>{h}</tr></thead><tbody>{f}</tbody></table></div>'


def flujo(*etapas):
    partes = []
    for i, e in enumerate(etapas):
        if i:
            partes.append('<span class="sig">→</span>')
        partes.append(f'<span class="etapa">{e}</span>')
    return '<div class="flujo">' + ''.join(partes) + '</div>'


def seccion(id_, titulo, *contenido, nivel=2):
    return f'<section id="{id_}"><h{nivel}>{titulo}</h{nivel}>{"".join(contenido)}</section>'


CSS = """
:root{--bg:#f4f6fb;--tx:#16213b;--mut:#5a6785;--card:#fff;--bd:#dfe4ef;--ac:#0b3a9a;--ac2:#e8f0ff;--ok:#157347;--warn:#b45309;--warnbg:#fff6e5;--tipbg:#e8f4ff;--code:#eef1f8}
@media (prefers-color-scheme:dark){:root:not([data-tema=claro]){--bg:#10162a;--tx:#e8ecf7;--mut:#a3aecb;--card:#19213a;--bd:#2c3657;--ac:#8fb0ff;--ac2:#1f2b50;--ok:#6fd49b;--warn:#f5b45a;--warnbg:#2f2614;--tipbg:#14263f;--code:#222c4a}}
:root[data-tema=oscuro]{--bg:#10162a;--tx:#e8ecf7;--mut:#a3aecb;--card:#19213a;--bd:#2c3657;--ac:#8fb0ff;--ac2:#1f2b50;--ok:#6fd49b;--warn:#f5b45a;--warnbg:#2f2614;--tipbg:#14263f;--code:#222c4a}
*{box-sizing:border-box}html{scroll-behavior:smooth}
body{margin:0;background:var(--bg);color:var(--tx);font:16px/1.6 system-ui,-apple-system,Segoe UI,Roboto,sans-serif}
.cuerpo{display:grid;grid-template-columns:270px minmax(0,1fr);gap:0;max-width:1400px;margin:0 auto}
nav.toc{position:sticky;top:0;align-self:start;height:100vh;overflow:auto;padding:24px 18px;border-right:1px solid var(--bd);font-size:14px}
nav.toc h2{font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:var(--mut);margin:0 0 10px}
nav.toc a{display:block;color:var(--tx);text-decoration:none;padding:4px 8px;border-radius:6px}
nav.toc a:hover{background:var(--ac2)}nav.toc a.h3{padding-left:22px;color:var(--mut);font-size:13px}
nav.toc a.h2{font-weight:600;margin-top:8px}
main{padding:32px clamp(16px,4vw,56px) 80px;min-width:0}
header.portada{background:linear-gradient(135deg,#0b3a9a,#1456d6);color:#fff;border-radius:18px;padding:40px clamp(20px,4vw,48px);margin-bottom:32px}
header.portada h1{margin:0 0 8px;font-size:clamp(28px,4vw,42px);line-height:1.15}header.portada p{margin:4px 0;opacity:.92;max-width:62ch}
.tema{float:right;background:rgba(255,255,255,.18);color:#fff;border:1px solid rgba(255,255,255,.4);border-radius:8px;padding:6px 12px;cursor:pointer;font:inherit;font-size:14px}
h2{font-size:28px;margin:56px 0 8px;padding-top:12px;border-top:3px solid var(--ac);scroll-margin-top:12px}
h3{font-size:21px;margin:34px 0 6px;scroll-margin-top:12px}h4{font-size:17px;margin:22px 0 4px}
p,li{max-width:78ch}
.fig{margin:18px 0 26px;background:var(--card);border:1px solid var(--bd);border-radius:14px;padding:14px;max-width:920px}
.lienzo{position:relative;line-height:0;border-radius:8px;overflow:hidden;border:1px solid var(--bd)}
.lienzo img{width:100%;height:auto;display:block}
.flechas{position:absolute;inset:0;width:100%;height:100%}
.marco{fill:rgba(232,89,12,.08);stroke:#e8590c;stroke-width:2.6;stroke-dasharray:none}
.halo{stroke:#fff;stroke-width:7;stroke-linecap:round;opacity:.9}.linea{stroke:#e8590c;stroke-width:3.4;stroke-linecap:round}
.insignia{fill:#e8590c;stroke:#fff;stroke-width:3}.num{fill:#fff;font:700 19px system-ui,sans-serif;text-anchor:middle}
figcaption{font-size:14px;color:var(--mut);margin:10px 4px 2px;font-style:italic}
.leyenda{list-style:none;margin:12px 0 0;padding:0;display:grid;gap:8px}
.leyenda li{display:flex;gap:10px;align-items:flex-start;line-height:1.45}
.leyenda .n,.pasos li::before{flex:none;width:24px;height:24px;border-radius:50%;background:#e8590c;color:#fff;font-weight:700;font-size:13px;display:inline-grid;place-items:center;margin-top:1px}
.pasos{list-style:none;counter-reset:p;padding:0;margin:12px 0}.pasos li{counter-increment:p;display:flex;gap:12px;margin:8px 0}
.pasos li::before{content:counter(p);background:var(--ac);color:var(--bg)}
.aviso{display:flex;gap:12px;border-radius:10px;padding:12px 14px;margin:14px 0;max-width:78ch;align-items:flex-start;line-height:1.5}
.aviso strong{flex:none;text-transform:uppercase;font-size:12px;letter-spacing:.06em;padding-top:3px}
.consejo{background:var(--tipbg);border-left:5px solid var(--ac)}.consejo strong{color:var(--ac)}
.ojo{background:var(--warnbg);border-left:5px solid var(--warn)}.ojo strong{color:var(--warn)}
.tabla-wrap{overflow-x:auto;margin:14px 0}table{border-collapse:collapse;background:var(--card);border:1px solid var(--bd);border-radius:10px;font-size:14.5px}
th,td{padding:8px 12px;border-bottom:1px solid var(--bd);text-align:left;vertical-align:top}th{background:var(--ac2);font-weight:600}
td.si{color:var(--ok);font-weight:700;text-align:center}td.no{color:var(--mut);text-align:center}
.flujo{display:flex;flex-wrap:wrap;align-items:center;gap:8px;margin:14px 0}
.etapa{background:var(--card);border:2px solid var(--ac);border-radius:10px;padding:7px 12px;font-weight:600;font-size:14.5px}.sig{color:var(--ac);font-weight:700;font-size:20px}
code,kbd{background:var(--code);border-radius:5px;padding:1px 6px;font-size:.92em}
.insig{display:inline-grid;place-items:center;width:22px;height:22px;border-radius:50%;background:#e8590c;color:#fff;font-weight:700;font-size:12px;vertical-align:middle}
details{background:var(--card);border:1px solid var(--bd);border-radius:10px;padding:10px 14px;margin:10px 0;max-width:78ch}summary{cursor:pointer;font-weight:600}
footer{margin-top:60px;color:var(--mut);font-size:14px}
@media (max-width:900px){.cuerpo{grid-template-columns:1fr}nav.toc{position:static;height:auto;border-right:0;border-bottom:1px solid var(--bd)}}
@media print{nav.toc,.tema{display:none}.cuerpo{display:block}body{background:#fff;color:#000;font-size:11pt}.fig{break-inside:avoid;border:1px solid #ccc}h2{break-before:page;margin-top:0}header.portada{break-after:page}}
"""

JS = """
const t=document.querySelector('.tema');
if(t){const o=getComputedStyle(document.documentElement).getPropertyValue('--bg').trim()==='#10162a';t.textContent=o?'Modo claro':'Modo oscuro';}
t&&t.addEventListener('click',()=>{const r=document.documentElement;const oscuro=getComputedStyle(r).getPropertyValue('--bg').trim()==='#10162a';
r.setAttribute('data-tema',oscuro?'claro':'oscuro');t.textContent=oscuro?'Modo oscuro':'Modo claro';});
"""


def construir(secciones, indice, titulo, subtitulo):
    toc = ''.join(f'<a class="{n}" href="#{i}">{t}</a>' for n, i, t in indice)
    return (f'<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
            f'<title>{titulo}</title><style>{CSS}</style></head><body><div class="cuerpo"><nav class="toc"><h2>Contenido</h2>{toc}</nav>'
            f'<main><header class="portada"><button class="tema" type="button">Modo oscuro</button><h1>{titulo}</h1><p>{subtitulo}</p></header>'
            f'{"".join(secciones)}<footer>Guía generada a partir del sistema en funcionamiento (capturas reales). Las flechas numeradas '
            f'se explican debajo de cada imagen.</footer></main></div><script>{JS}</script></body></html>')
