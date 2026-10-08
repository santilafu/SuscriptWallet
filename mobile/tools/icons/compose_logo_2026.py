"""Recursos de icono a partir del logo 2026 ("O · M con movimiento", página 15 del PDF del usuario).

Uso: python -I compose_logo_2026.py <logo_fuente.png> <res_dir> <static_dir> <preview_dir>

El logo es una ilustración cuadrada a sangre (degradado + tarjetas + monedas), sin capas.
  - mipmap-*/ic_launcher_foreground.png  108 dp opaco: el logo a 70 dp centrado y el degradado
                                          prolongado hasta el borde (borde replicado + desenfoque),
                                          para que ninguna máscara (círculo, squircle) muestre un corte.
  - mipmap-*/ic_launcher.png             48 dp legacy (logo con esquinas redondeadas)
  - drawable-nodpi/logo_app.png          logo de la pantalla de login (esquinas redondeadas)
  - drawable/ic_launcher_monochrome.xml  silueta de la tarjeta con la S recortada (Android 13+)
  - drawable/ic_stat_renovacion.xml      la misma silueta a 24 dp para notificaciones
  - static/favicon*.png, apple-touch-icon.png, favicon.ico  para la web
"""
import os, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy import ndimage as ndi
from skimage import measure

src_path, res, static, prev = sys.argv[1:5]
src = Image.open(src_path).convert("RGB")
N = src.size[0]
assert src.size == (N, N)

LOGO_DP = 70.0  # el logo ocupa 70 de los 108 dp: las monedas quedan dentro del círculo visible de 72 dp
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def foreground(px_per_dp):
    size = int(round(108 * px_per_dp))
    logo_px = int(round(LOGO_DP * px_per_dp))
    pad = (size - logo_px) // 2
    logo = np.asarray(src.resize((logo_px, logo_px), Image.LANCZOS)).astype(float)
    # Fondo: borde replicado y muy desenfocado = el degradado continúa sin vetas.
    ext = np.pad(logo, ((pad, size - logo_px - pad), (pad, size - logo_px - pad), (0, 0)), mode="edge")
    blur = np.stack([ndi.gaussian_filter(ext[..., c], sigma=8 * px_per_dp) for c in range(3)], -1)
    # Mezcla con borde suave de 4 dp para que no se note la costura.
    m = np.zeros((size, size))
    f = 4 * px_per_dp
    m[pad:pad + logo_px, pad:pad + logo_px] = 1
    m = ndi.distance_transform_edt(m)
    m = np.clip(m / f, 0, 1)[..., None]
    out = blur * (1 - m) + ext * m
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)).convert("RGBA")


def rounded(img, size, radius_frac):
    im = img.resize((size, size), Image.LANCZOS).convert("RGBA")
    mk = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mk).rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=int(size * 4 * radius_frac), fill=255)
    mk = mk.resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(im, (0, 0), mk)
    return out


for d, k in DENSITIES.items():
    folder = os.path.join(res, f"mipmap-{d}")
    os.makedirs(folder, exist_ok=True)
    foreground(k).save(os.path.join(folder, "ic_launcher_foreground.png"), optimize=True)
    rounded(src, int(round(48 * k)), 0.22).save(os.path.join(folder, "ic_launcher.png"), optimize=True)

os.makedirs(os.path.join(res, "drawable-nodpi"), exist_ok=True)
rounded(src, 288, 0.22).save(os.path.join(res, "drawable-nodpi", "logo_app.png"), optimize=True)

# ---- Silueta: tarjeta blanca (relleno) con la S recortada ----
a = np.asarray(src).astype(int)
blanco = (a.min(axis=2) > 215) & (a.max(axis=2) - a.min(axis=2) < 40)
lbl, n = ndi.label(blanco)
tarjeta = lbl == (np.argmax(np.bincount(lbl.ravel())[1:]) + 1)  # la mancha blanca más grande
tarjeta = ndi.binary_opening(tarjeta, iterations=2)
llena = ndi.binary_fill_holes(tarjeta)
ese = llena & ~tarjeta
ese = ndi.binary_opening(ese, iterations=2)
# suavizado leve de ambas formas
llena = ndi.gaussian_filter(llena.astype(float), N / 512) > 0.5
ese = ndi.gaussian_filter(ndi.binary_closing(ese, iterations=4).astype(float), N / 512 * 3) > 0.5


def contorno(binary, tol):
    c = max(measure.find_contours(np.pad(binary.astype(float), 1), 0.5), key=len) - 1.0
    c = measure.approximate_polygon(c, tolerance=tol)
    return [(float(x), float(y)) for y, x in c]


tol = N / 512 * 0.8
out_pts, s_pts = contorno(llena, tol), contorno(ese, tol)
ys, xs = np.where(llena)
cx, cy = (xs.min() + xs.max()) / 2, (ys.min() + ys.max()) / 2
r_src = np.hypot(ys - cy, xs - cx).max()


def pd(pts, scale, vp):
    d = "M" + "L".join(f"{(x - cx) * scale + vp / 2:.2f},{(y - cy) * scale + vp / 2:.2f}" for x, y in pts)
    return d + "Z"


def vector(vp, radio_dp, comentario):
    s = radio_dp / r_src
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- {comentario}
     Silueta trazada del logo 2026 (tarjeta con la S recortada) con mobile/tools/icons/compose_logo_2026.py.
     Relleno even-odd: la S queda hueca. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{vp}dp"
    android:height="{vp}dp"
    android:viewportWidth="{vp}"
    android:viewportHeight="{vp}">
    <path
        android:fillColor="#FFFFFFFF"
        android:fillType="evenOdd"
        android:pathData="{pd(out_pts, s, vp)}{pd(s_pts, s, vp)}" />
</vector>
"""


open(os.path.join(res, "drawable", "ic_launcher_monochrome.xml"), "w", encoding="utf-8").write(
    vector(108, 30.0, "Capa monochrome del icono adaptativo (iconos temáticos, Android 13+)."))
open(os.path.join(res, "drawable", "ic_stat_renovacion.xml"), "w", encoding="utf-8").write(
    vector(24, 11.0, "Icono de estado (barra de notificaciones): blanco sobre transparente, 24 dp."))

# ---- Web ----
for nombre, tam in [("favicon-16x16.png", 16), ("favicon-32x32.png", 32), ("favicon-192x192.png", 192)]:
    rounded(src, tam, 0.22).save(os.path.join(static, nombre), optimize=True)
src.resize((180, 180), Image.LANCZOS).save(os.path.join(static, "apple-touch-icon.png"), optimize=True)  # iOS redondea solo
rounded(src, 48, 0.22).save(os.path.join(static, "favicon.ico"), sizes=[(16, 16), (32, 32), (48, 48)])

# ---- Previsualización: máscaras círculo / squircle / cuadrado redondeado + silueta ----
px, size = 4, 432
fg = foreground(px)
sheet = Image.new("RGB", (size * 5, size), (235, 235, 235))
for i, forma in enumerate(["circle", "squircle", "rounded"]):
    mk = Image.new("L", (size, size), 0)
    dr = ImageDraw.Draw(mk)
    v = 18 * px  # solo se ven los 72 dp centrales
    if forma == "circle":
        dr.ellipse((v, v, size - v, size - v), fill=255)
    elif forma == "squircle":
        dr.rounded_rectangle((v, v, size - v, size - v), radius=int(0.42 * (size - 2 * v) / 2 * 2 * 0.5 * 1.6), fill=255)
    else:
        dr.rounded_rectangle((v, v, size - v, size - v), radius=int((size - 2 * v) * 0.16), fill=255)
    sheet.paste(fg.convert("RGB"), (size * i, 0), mk)
mono = Image.new("RGB", (size, size), (40, 30, 70))
s = 30.0 / r_src * px
dm = ImageDraw.Draw(mono)
dm.polygon([((x - cx) * s + size / 2, (y - cy) * s + size / 2) for x, y in out_pts], fill="white")
dm.polygon([((x - cx) * s + size / 2, (y - cy) * s + size / 2) for x, y in s_pts], fill=(40, 30, 70))
sheet.paste(mono, (size * 3, 0))
sheet.paste(fg.convert("RGB").resize((size, size)), (size * 4, 0))
sheet.save(os.path.join(prev, "preview_logo_2026.png"))
print("ok", N, "px; silueta", len(out_pts), "+", len(s_pts), "puntos")
