"""Icono de la pantalla de arranque (splash de Android 12+ y core-splashscreen) con el logo 2026.

Uso: python compose_splash_2026.py <logo_fuente.png> <res_dir>

Por qué un recurso aparte y no @mipmap/ic_launcher: el splash recorta el icono a un círculo de
192 dp dentro de un lienzo de 288 dp. Con el icono adaptativo, ese círculo deja ver solo los 72 dp
centrales del primer plano, así que el logo (cuadrado, 70 dp) salía con las esquinas cortadas y
el degradado desenfocado alrededor. Aquí se dibuja el logo entero con esquinas redondeadas sobre
transparente, dimensionado para que quepa completo dentro del círculo visible.

Genera drawable-<densidad>/splash_logo.png (288 dp) para mdpi..xxxhdpi.
"""
import os
import sys

from PIL import Image, ImageDraw

src_path, res = sys.argv[1:3]
src = Image.open(src_path).convert("RGB")
if src.size[0] != src.size[1]:
    sys.exit(f"el logo fuente debe ser cuadrado, es {src.size}")

CANVAS_DP = 288.0
RADIUS_FRAC = 0.22  # mismo redondeo que logo_app.png y ic_launcher.png legacy
# Lado del logo: el punto más lejano del centro de un cuadrado redondeado de lado s y radio r=0,22·s
# está a (s/2 − r)·√2 + r ≈ 0,616·s. Para no salirse del círculo de 96 dp de radio: s ≤ 155 dp.
# 148 dp deja algo de margen visual.
LOGO_DP = 148.0
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def splash(px_per_dp):
    canvas = int(round(CANVAS_DP * px_per_dp))
    size = int(round(LOGO_DP * px_per_dp))
    logo = src.resize((size, size), Image.LANCZOS).convert("RGBA")
    # Máscara a 4x y reducida = bordes antialiasados.
    mk = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mk).rounded_rectangle(
        (0, 0, size * 4 - 1, size * 4 - 1), radius=int(size * 4 * RADIUS_FRAC), fill=255)
    mk = mk.resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    off = (canvas - size) // 2
    out.paste(logo, (off, off), mk)
    return out


for d, k in DENSITIES.items():
    folder = os.path.join(res, f"drawable-{d}")
    os.makedirs(folder, exist_ok=True)
    splash(k).save(os.path.join(folder, "splash_logo.png"), optimize=True)
print("ok: splash_logo.png en", len(DENSITIES), "densidades")
