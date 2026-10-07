"""Paso 1: extraer el escudo (sin baldosa ni texto) del logo original de 512 px.

Uso: python extract_shield.py <logo_512.png> <out_dir>
Genera en out_dir:
  shield.png       escudo recortado con alfa (RGBA)
  mask.png         máscara binaria del escudo (L)
  keyhole.png      máscara binaria de la cerradura luminosa (L)
  debug_sheet.png  hoja de contacto para inspección visual
"""
import sys, os
import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage as ndi

src, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)

im = Image.open(src).convert("RGBA")
a = np.asarray(im).astype(np.float32)
rgb, alpha = a[..., :3], a[..., 3]
lum = 0.299 * rgb[..., 0] + 0.587 * rgb[..., 1] + 0.114 * rgb[..., 2]
H, W = lum.shape

# Fondo de la baldosa = degradado suave. Dos criterios y nos quedamos con la unión:
#  (a) píxeles de gradiente bajo conectados a la semilla (el escudo tiene bordes fuertes, claros arriba y oscuros abajo)
#  (b) píxeles que se parecen a un modelo polinómico 2D del degradado, ajustado sobre un anillo exterior al escudo
seed = (30, W // 2)  # fila, columna: degradado de la baldosa, encima del escudo
def flood(fillable):
    lab, _ = ndi.label(fillable)
    assert lab[seed] != 0, "la semilla no es rellenable"
    return lab == lab[seed]
sm = np.stack([ndi.gaussian_filter(rgb[..., c], 1.0) for c in range(3)], -1)
gx = np.stack([ndi.sobel(sm[..., c], 1) for c in range(3)], -1)
gy = np.stack([ndi.sobel(sm[..., c], 0) for c in range(3)], -1)
gm = np.sqrt((gx ** 2 + gy ** 2).sum(-1))
bg_a = flood((alpha > 0) & (gm < 40))
yy, xx = np.mgrid[0:H, 0:W]
rr = np.hypot(yy - 250, xx - 256)
samp = (alpha > 0) & (rr > 205) & (rr < 232) & (yy < 390)
X = np.stack([np.ones(H * W), xx.ravel(), yy.ravel(), xx.ravel() ** 2, yy.ravel() ** 2, (xx * yy).ravel()], 1) / [1, 512, 512, 512 ** 2, 512 ** 2, 512 ** 2]
fit = np.zeros_like(rgb)
for c in range(3):
    coef, *_ = np.linalg.lstsq(X[samp.ravel()], rgb[..., c].ravel()[samp.ravel()], rcond=None)
    fit[..., c] = (X @ coef).reshape(H, W)
res = np.abs(rgb - fit).max(-1)
bg_b = flood((alpha > 0) & (res < 40))
background = bg_a | bg_b

# Comprobación de fuga: el interior del escudo NO debe pertenecer al fondo.
interior_probe = (120, W // 2)
print("fuga al interior?", background[interior_probe])

non_bg = ~background & (alpha > 0)
lab2, n2 = ndi.label(non_bg)
shield_label = lab2[interior_probe]
assert shield_label != 0
shield = lab2 == shield_label
shield = ndi.binary_fill_holes(shield)
# Limpieza: apertura con disco (quita flecos de sombra finos), cierre suave, relleno y mayor componente
def disk(r):
    y, x = np.ogrid[-r:r + 1, -r:r + 1]
    return (x * x + y * y) <= r * r
shield = ndi.binary_opening(shield, structure=disk(7))
shield = ndi.binary_closing(shield, structure=disk(4))
shield = ndi.binary_fill_holes(shield)
lab4, n4 = ndi.label(shield)
sizes4 = ndi.sum(shield, lab4, range(1, n4 + 1))
shield = lab4 == (1 + int(np.argmax(sizes4)))

ys, xs = np.where(shield)
y0, y1, x0, x1 = ys.min(), ys.max(), xs.min(), xs.max()
print("bbox escudo:", (x0, y0, x1, y1), "tamaño", (x1 - x0 + 1, y1 - y0 + 1))

# Cerradura luminosa: píxeles casi blancos dentro del escudo, en la zona central.
hole = shield & (lum > 236)
cy, cx = (y0 + y1) / 2, (x0 + x1) / 2
yy, xx = np.mgrid[0:H, 0:W]
hole &= (np.hypot(yy - cy, xx - cx) < 0.22 * (y1 - y0))
lab3, n3 = ndi.label(hole)
if n3:
    sizes = ndi.sum(hole, lab3, range(1, n3 + 1))
    hole = lab3 == (1 + int(np.argmax(sizes)))
    hole = ndi.binary_closing(hole, iterations=3)
    hole = ndi.binary_fill_holes(hole)
hy, hx = np.where(hole)
print("cerradura bbox:", (hx.min(), hy.min(), hx.max(), hy.max()) if len(hx) else None)

# Alfa suavizado (antialias de 1 px) y recorte
mask_img = Image.fromarray((shield * 255).astype(np.uint8), "L")
soft = mask_img.filter(ImageFilter.GaussianBlur(0.8))
soft_np = np.asarray(soft).astype(np.float32) / 255.0
out_rgba = a.copy()
out_rgba[..., 3] = np.minimum(alpha, soft_np * 255.0)
pad = 2
crop = (max(x0 - pad, 0), max(y0 - pad, 0), min(x1 + pad + 1, W), min(y1 + pad + 1, H))
shield_img = Image.fromarray(out_rgba.astype(np.uint8), "RGBA").crop(crop)
shield_img.save(os.path.join(out, "shield.png"))
mask_img.crop(crop).save(os.path.join(out, "mask.png"))
Image.fromarray((hole * 255).astype(np.uint8), "L").crop(crop).save(os.path.join(out, "keyhole.png"))
with open(os.path.join(out, "crop.txt"), "w") as f:
    f.write(",".join(map(str, crop)))

# Hoja de contacto: original | fondo detectado | escudo sobre tablero
sheet = Image.new("RGB", (W * 3, H), (255, 255, 255))
sheet.paste(im.convert("RGB"), (0, 0))
sheet.paste(Image.fromarray((background * 255).astype(np.uint8), "L").convert("RGB"), (W, 0))
checker = Image.new("RGB", (W, H), (200, 200, 200))
for yb in range(0, H, 32):
    for xb in range(0, W, 32):
        if (xb // 32 + yb // 32) % 2 == 0:
            checker.paste((240, 240, 240), (xb, yb, xb + 32, yb + 32))
full = Image.fromarray(out_rgba.astype(np.uint8), "RGBA")
checker.paste(full, (0, 0), full)
sheet.paste(checker, (W * 2, 0))
sheet.save(os.path.join(out, "debug_sheet.png"))
print("ok")
