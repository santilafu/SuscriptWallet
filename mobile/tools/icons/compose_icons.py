"""Paso 2: generar los recursos del launcher a partir de out/shield.png (escudo con alfa).

Uso: python compose_icons.py <out_dir_de_extract> <res_dir> <bg_hex> [--preview-only]
  - mipmap-*/ic_launcher_foreground.png   108 dp (escudo centrado dentro del círculo seguro de 66 dp)
  - mipmap-*/ic_launcher.png              48 dp legacy (fondo sólido + escudo, esquinas redondeadas)
  - drawable/ic_launcher_monochrome.xml   silueta vectorial trazada de la máscara (capa monochrome, Android 13+)
  - drawable/ic_stat_renovacion.xml       la misma silueta a 24 dp para notificaciones
  - <out>/preview_*.png                   hojas de contacto para inspección visual
"""
import os, sys
import numpy as np
from PIL import Image, ImageDraw
from skimage import measure

out, res, bg_hex = sys.argv[1], sys.argv[2], sys.argv[3]
preview_only = "--preview-only" in sys.argv
bg_rgb = tuple(int(bg_hex.lstrip("#")[i:i + 2], 16) for i in (0, 2, 4))

shield = Image.open(os.path.join(out, "shield.png")).convert("RGBA")
mask = np.asarray(Image.open(os.path.join(out, "mask.png")).convert("L")) > 127
hole = np.asarray(Image.open(os.path.join(out, "keyhole.png")).convert("L")) > 127
sh_w, sh_h = shield.size
assert mask.shape == (sh_h, sh_w)

# Zona segura del icono adaptativo: círculo de 66 dp de diámetro centrado en el lienzo de 108 dp.
# Escalamos para que TODO píxel opaco quede a <= SAFE_R dp del centro (1 dp de margen extra).
SAFE_R = 32.0
ys, xs = np.where(mask)
cy_src, cx_src = (ys.min() + ys.max()) / 2.0, (xs.min() + xs.max()) / 2.0
r_src = np.hypot(ys - cy_src, xs - cx_src).max()
scale_dp = SAFE_R / r_src  # dp por píxel fuente
print(f"escudo fuente {sh_w}x{sh_h}px, radio max {r_src:.1f}px -> {sh_w*scale_dp:.1f}x{sh_h*scale_dp:.1f} dp en 108 dp")

DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def foreground(px_per_dp):
    size = int(round(108 * px_per_dp))
    s = scale_dp * px_per_dp
    fg = shield.resize((max(1, int(round(sh_w * s))), max(1, int(round(sh_h * s)))), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # centramos el centro de la bbox del escudo en el centro del lienzo
    ox = int(round(size / 2 - cx_src * s))
    oy = int(round(size / 2 - cy_src * s))
    canvas.paste(fg, (ox, oy), fg)
    return canvas


def circle_mask(size, radius_dp, px_per_dp):
    m = Image.new("L", (size, size), 0)
    r = radius_dp * px_per_dp
    ImageDraw.Draw(m).ellipse((size / 2 - r, size / 2 - r, size / 2 + r, size / 2 + r), fill=255)
    return m


def legacy(px_per_dp):
    """48 dp: recorte central de 72 dp del lienzo adaptativo, esquinas redondeadas."""
    full = int(round(108 * px_per_dp))
    fg = foreground(px_per_dp)
    comp = Image.new("RGBA", (full, full), bg_rgb + (255,))
    comp.alpha_composite(fg)
    m = int(round(18 * px_per_dp))
    inner = comp.crop((m, m, full - m, full - m))
    size = int(round(48 * px_per_dp))
    inner = inner.resize((size, size), Image.LANCZOS)
    rm = Image.new("L", (size, size), 0)
    ImageDraw.Draw(rm).rounded_rectangle((0, 0, size - 1, size - 1), radius=int(size * 0.18), fill=255)
    outp = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    outp.paste(inner, (0, 0), rm)
    return outp


# ---- Trazado vectorial de la silueta (para monochrome e icono de notificación) ----
def trace(binary, tol):
    padded = np.pad(binary.astype(float), 1)
    contours = measure.find_contours(padded, 0.5)
    contours.sort(key=lambda c: -len(c))
    c = contours[0] - 1.0  # quitar el padding
    c = douglas_peucker(c, tol)
    return [(float(x), float(y)) for y, x in c]  # (col, fila) -> (x, y)


def douglas_peucker(pts, tol):
    """Simplificación iterativa (sin recursión) de una polilínea cerrada."""
    pts = np.asarray(pts, dtype=float)
    n = len(pts)
    keep = np.zeros(n, dtype=bool)
    keep[0] = keep[-1] = True
    stack = [(0, n - 1)]
    while stack:
        i, j = stack.pop()
        if j <= i + 1:
            continue
        seg = pts[j] - pts[i]
        L = np.hypot(*seg)
        if L == 0:
            d = np.hypot(*(pts[i + 1:j] - pts[i]).T)
        else:
            v = pts[i + 1:j] - pts[i]
            d = np.abs(seg[0] * v[:, 1] - seg[1] * v[:, 0]) / L
        k = int(np.argmax(d))
        if d[k] > tol:
            m = i + 1 + k
            keep[m] = True
            stack.append((i, m))
            stack.append((m, j))
    return pts[keep]


def path_data(points, fx, fy, fs):
    d = f"M{(points[0][0]-fx)*fs:.2f},{(points[0][1]-fy)*fs:.2f}"
    for x, y in points[1:]:
        d += f"L{(x-fx)*fs:.2f},{(y-fy)*fs:.2f}"
    return d + "Z"


# Silueta para el vector: simetrizamos (el escudo es simétrico; elimina muescas de sombra) y suavizamos.
from scipy import ndimage as ndi
def disk(r):
    y, x = np.ogrid[-r:r + 1, -r:r + 1]
    return (x * x + y * y) <= r * r
_shift = int(round(2 * cx_src)) - (sh_w - 1)
_mirror = np.roll(mask[:, ::-1], _shift, axis=1)
sil = mask | _mirror
sil = ndi.binary_closing(sil, structure=disk(8))
sil = ndi.gaussian_filter(sil.astype(float), 4.0) > 0.5
sil = ndi.binary_fill_holes(sil)
outline = trace(sil, tol=1.5)
# Cerradura geométrica (círculo + trapecio) en la posición/tamaño de la cerradura luminosa detectada.
_hy, _hx = np.where(hole)
_kx, _ky0, _ky1 = (_hx.min() + _hx.max()) / 2.0, float(_hy.min()), float(_hy.max())
_kr = (_hx.max() - _hx.min()) * 0.42
_kcy = _ky0 + _kr
# Arco desde abajo-derecha (x=+0.42r) por arriba hasta abajo-izquierda (x=-0.42r), luego trapecio que se ensancha.
_a = np.arccos(0.42)
_ang = np.linspace(-_a, np.pi + _a, 40)
keyhole = [(_kx + _kr * np.cos(t), _kcy - _kr * np.sin(t)) for t in _ang]
keyhole += [(_kx - 0.62 * _kr, _ky1), (_kx + 0.62 * _kr, _ky1)]


def vector_xml(viewport, glyph_scale, color, name_comment):
    """Escala dp-por-píxel-fuente = glyph_scale; centra el escudo en viewport x viewport."""
    fx = cx_src - (viewport / 2) / glyph_scale
    fy = cy_src - (viewport / 2) / glyph_scale
    d_out = path_data(outline, fx, fy, glyph_scale)
    d_hole = path_data(keyhole, fx, fy, glyph_scale)
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- {name_comment}
     Silueta trazada automáticamente de la ilustración original del escudo (ic_app_logo.png)
     con scratchpad/compose_icons.py. Relleno even-odd: la cerradura queda recortada. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{viewport}dp"
    android:height="{viewport}dp"
    android:viewportWidth="{viewport}"
    android:viewportHeight="{viewport}">
    <path
        android:fillColor="{color}"
        android:fillType="evenOdd"
        android:pathData="{d_out}{d_hole}" />
</vector>
"""


mono_xml = vector_xml(108, scale_dp, "#FFFFFFFF", "Capa monochrome del icono adaptativo (iconos temáticos, Android 13+).")
# Notificación: 24 dp con el escudo de 20 dp de alto (2 dp de margen).
stat_scale = 20.0 / sh_h
stat_xml = vector_xml(24, stat_scale, "#FFFFFFFF", "Icono de estado (barra de notificaciones): blanco sobre transparente, 24 dp.")


# ---- Previsualización ----
def render_vector_preview(points_out, points_hole, fx, fy, fs, size, bg):
    img = Image.new("RGB", (size, size), bg)
    d = ImageDraw.Draw(img)
    d.polygon([((x - fx) * fs, (y - fy) * fs) for x, y in points_out], fill=(255, 255, 255))
    d.polygon([((x - fx) * fs, (y - fy) * fs) for x, y in points_hole], fill=bg)
    return img


px = 4  # xxxhdpi
size = 432
fg = foreground(px)
sheet = Image.new("RGB", (size * 5, size), (255, 255, 255))
for i, (label, bg) in enumerate([("bg", bg_rgb), ("indigo", (0x4F, 0x46, 0xE5)), ("teal", (0x0F, 0x4C, 0x5C))]):
    comp = Image.new("RGBA", (size, size), bg + (255,))
    comp.alpha_composite(fg)
    circ = circle_mask(size, 36, px)  # máscara circular (72 dp visibles)
    outp = Image.new("RGB", (size, size), (240, 240, 240))
    outp.paste(comp.convert("RGB"), (0, 0), circ)
    # guía: círculo de zona segura (66 dp)
    dd = ImageDraw.Draw(outp)
    r = 33 * px
    dd.ellipse((size / 2 - r, size / 2 - r, size / 2 + r, size / 2 + r), outline=(255, 0, 0), width=2)
    sheet.paste(outp, (size * i, 0))
# monochrome preview (108 viewport a 4x)
fx = cx_src - 54 / scale_dp
fy = cy_src - 54 / scale_dp
mono_prev = render_vector_preview(outline, keyhole, fx, fy, scale_dp * px, size, (0x0B, 0x3B, 0x4C))
sheet.paste(mono_prev, (size * 3, 0))
# ic_stat preview a 24dp*18
fx2 = cx_src - 12 / stat_scale
fy2 = cy_src - 12 / stat_scale
stat_prev = render_vector_preview(outline, keyhole, fx2, fy2, stat_scale * 18, 432, (60, 60, 60))
sheet.paste(stat_prev, (size * 4, 0))
sheet.save(os.path.join(out, "preview_icons.png"))

# comprobación numérica: ningún píxel opaco fuera de la zona segura ni tocando el borde
a = np.asarray(fg)[..., 3]
yy, xx = np.where(a > 8)
rmax = np.hypot(yy - (size - 1) / 2, xx - (size - 1) / 2).max() / px
print(f"radio máximo opaco en foreground xxxhdpi: {rmax:.1f} dp (zona segura 33 dp); bbox px {xx.min()},{yy.min()}-{xx.max()},{yy.max()} de {size}")

if preview_only:
    sys.exit(0)

# ---- Escritura de recursos ----
for name, d in DENSITIES.items():
    folder = os.path.join(res, f"mipmap-{name}")
    os.makedirs(folder, exist_ok=True)
    foreground(d).save(os.path.join(folder, "ic_launcher_foreground.png"), optimize=True)
    legacy(d).save(os.path.join(folder, "ic_launcher.png"), optimize=True)
    bgp = os.path.join(folder, "ic_launcher_background.png")
    if os.path.exists(bgp):
        os.remove(bgp)
with open(os.path.join(res, "drawable", "ic_launcher_monochrome.xml"), "w", encoding="utf-8") as f:
    f.write(mono_xml)
with open(os.path.join(res, "drawable", "ic_stat_renovacion.xml"), "w", encoding="utf-8") as f:
    f.write(stat_xml)
adaptive = """<?xml version="1.0" encoding="utf-8"?>
<!-- Icono adaptativo: fondo sólido + ilustración original del escudo (sin texto) centrada en la
     zona segura de 66 dp + silueta monocroma para iconos temáticos (Android 13+).
     Los PNG se regeneran con scratchpad/extract_shield.py + compose_icons.py a partir de
     drawable/ic_app_logo.png (ver nota en engram "SubIA / iconos launcher"). -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""
for n in ("ic_launcher.xml", "ic_launcher_round.xml"):
    with open(os.path.join(res, "mipmap-anydpi-v26", n), "w", encoding="utf-8") as f:
        f.write(adaptive)
print("recursos escritos")
