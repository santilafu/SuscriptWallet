"""Capturas de la ficha de Play (1080x1920) a partir de pantallazos del emulador (1080x2400).

Uso: python componer_capturas.py <dir_pantallazos> <dir_salida> <fuente.ttf>

Fondo con el degradado del logo/banner (rosa, violeta, azul sobre azul noche), titular y
subtítulo arriba, y el pantallazo dentro de un marco de móvil que sale por abajo.
"""
import os, sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

raw, out, fuente = sys.argv[1:4]
os.makedirs(out, exist_ok=True)
W, H = 1080, 1920

CAPTURAS = [
    ("dash1", "Todos tus gastos fijos,\nde un vistazo", "Suscripciones y recibos sumados al mes y al año"),
    ("dash2", "Sabe qué te viene\ncada mes", "Los cobros de los próximos 12 meses, sin sorpresas"),
    ("lista", "Todo en un\nsolo sitio", "Netflix, la luz, el gimnasio, el móvil…"),
    ("dash3", "Descubre en qué\nse te va el dinero", "Tu gasto mensual por categoría"),
    ("cat_streaming", "Más de 400 servicios\ncon su precio real", "Elige y se rellena solo: precio, ciclo y logo"),
    ("cat_seguros", "Seguros, luz, gas\ny telefonía también", "Los recibos recurrentes son suscripciones"),
    ("resumen", "Tu año en\nsuscripciones", "Descubre cuánto suman y compártelo"),
    ("dash_dark", "Te avisamos antes\nde cada cobro", "Y de las pruebas gratis antes de que caduquen"),
]


def fuente_peso(tam, peso):
    f = ImageFont.truetype(fuente, tam)
    try:
        f.set_variation_by_axes([peso])
    except Exception:
        pass
    return f


def fondo():
    img = Image.new("RGB", (W, H), (13, 11, 30))
    glow = Image.new("RGB", (W, H), (0, 0, 0))
    d = ImageDraw.Draw(glow)
    for (cx, cy, r, col) in [(-80, 120, 620, (236, 72, 153)), (1160, 260, 640, (59, 130, 246)),
                             (540, 1500, 760, (124, 58, 237)), (1100, 1900, 420, (249, 115, 22))]:
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=col)
    glow = glow.filter(ImageFilter.GaussianBlur(220))
    return Image.blend(img, glow, 0.55)


def redondear(im, r):
    m = Image.new("L", im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, im.size[0] - 1, im.size[1] - 1), radius=r, fill=255)
    o = Image.new("RGBA", im.size, (0, 0, 0, 0))
    o.paste(im, (0, 0), m)
    return o


def componer(nombre, titulo, sub):
    lienzo = fondo().convert("RGBA")
    d = ImageDraw.Draw(lienzo)
    ft, fs = fuente_peso(78, 800), fuente_peso(38, 500)
    y = 118
    for linea in titulo.split("\n"):
        w = d.textlength(linea, font=ft)
        d.text(((W - w) / 2, y), linea, font=ft, fill=(255, 255, 255))
        y += 92
    w = d.textlength(sub, font=fs)
    d.text(((W - w) / 2, y + 18), sub, font=fs, fill=(214, 208, 240))

    # Pantallazo sin barra de estado, dentro de un marco de móvil que sale por abajo.
    # En la de avisos la notificación tapa la barra de estado: se recorta menos para que se vea entera.
    arriba = 40 if nombre == "dash_dark" else 110
    shot = Image.open(os.path.join(raw, nombre + ".png")).convert("RGB").crop((0, arriba, 1080, 2400))
    ancho = 820
    shot = shot.resize((ancho, int(shot.height * ancho / 1080)), Image.LANCZOS)
    bisel = 18
    marco = Image.new("RGBA", (ancho + 2 * bisel, shot.height + 2 * bisel), (24, 21, 38, 255))
    marco = redondear(marco, 78)
    marco.alpha_composite(redondear(shot.convert("RGBA"), 62), (bisel, bisel))
    x0, y0 = (W - marco.width) // 2, 470
    sombra = Image.new("RGBA", lienzo.size, (0, 0, 0, 0))
    ImageDraw.Draw(sombra).rounded_rectangle((x0 + 10, y0 + 30, x0 + marco.width - 10, y0 + marco.height), radius=78, fill=(0, 0, 0, 150))
    lienzo.alpha_composite(sombra.filter(ImageFilter.GaussianBlur(40)))
    lienzo.alpha_composite(marco, (x0, y0))
    return lienzo.convert("RGB")


for i, (nombre, titulo, sub) in enumerate(CAPTURAS, 1):
    destino = os.path.join(out, f"{i:02d}_{nombre}.png")
    componer(nombre, titulo, sub).save(destino, optimize=True)
    print(destino)
