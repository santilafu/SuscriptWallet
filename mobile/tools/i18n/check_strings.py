"""Comprueba que todas las traducciones de strings.xml están completas y son coherentes.

Uso (desde mobile/):  python tools/i18n/check_strings.py

Compara values/strings.xml (español, idioma base) con cada values-XX/strings.xml:
- mismas claves <string> y <plurals> (ni faltan ni sobran);
- en cada <plurals>, las cantidades "one" y "other" presentes;
- mismos placeholders (%1$s, %d, %2$d, %%...) en cada texto, para que un String.format
  no reviente en tiempo de ejecución en un idioma concreto;
- apóstrofos sin escapar (aapt los rechaza o se comen el texto).

Sale con código 1 si encuentra cualquier problema.
"""
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

RES = Path(__file__).resolve().parents[2] / "androidApp" / "src" / "androidMain" / "res"
BASE = "values"
IDIOMAS = ["values-en", "values-fr", "values-pt"]

# %1$s, %2$d, %d, %s, %.2f ... y el literal %% (cuenta como placeholder para no perderlo).
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[sdfxXc]|%%")


def cargar(carpeta: str) -> dict[str, str]:
    """Devuelve {clave: texto} con los plurals aplanados como 'clave#cantidad'."""
    ruta = RES / carpeta / "strings.xml"
    raiz = ET.parse(ruta).getroot()
    textos: dict[str, str] = {}
    for nodo in raiz:
        if nodo.get("translatable") == "false":
            continue
        nombre = nodo.get("name")
        if nodo.tag == "string":
            textos[nombre] = "".join(nodo.itertext())
        elif nodo.tag == "plurals":
            for item in nodo.findall("item"):
                textos[f"{nombre}#{item.get('quantity')}"] = "".join(item.itertext())
    return textos


def placeholders(texto: str) -> Counter:
    return Counter(PLACEHOLDER.findall(texto))


def main() -> int:
    base = cargar(BASE)
    errores: list[str] = []
    print(f"{BASE}: {len(base)} entradas (strings + items de plurals)")
    for carpeta in IDIOMAS:
        traduccion = cargar(carpeta)
        faltan = sorted(set(base) - set(traduccion))
        sobran = sorted(set(traduccion) - set(base))
        for clave in faltan:
            errores.append(f"{carpeta}: falta '{clave}'")
        for clave in sobran:
            errores.append(f"{carpeta}: sobra '{clave}' (no está en {BASE})")
        for clave in sorted(set(base) & set(traduccion)):
            ph_base, ph_trad = placeholders(base[clave]), placeholders(traduccion[clave])
            # En plurals "one" es habitual omitir el número ("1 día" -> "un día"); solo se
            # exige igualdad estricta en strings y en la cantidad "other".
            if clave.endswith("#one"):
                if not set(ph_trad) <= set(placeholders(base[clave.replace("#one", "#other")])):
                    errores.append(f"{carpeta}: '{clave}' placeholders {dict(ph_trad)} desconocidos")
            elif ph_base != ph_trad:
                errores.append(f"{carpeta}: '{clave}' placeholders {dict(ph_trad)} != {dict(ph_base)}")
        texto_crudo = (RES / carpeta / "strings.xml").read_text(encoding="utf-8")
        for linea_n, linea in enumerate(texto_crudo.splitlines(), 1):
            contenido = re.sub(r"<!--.*?-->", "", linea)
            if re.search(r"(?<!\\)'", re.sub(r'"[^"]*"', "", contenido)):
                errores.append(f"{carpeta}:{linea_n}: apóstrofo sin escapar")
        print(f"{carpeta}: {len(traduccion)} entradas, {len(faltan)} faltan, {len(sobran)} sobran")

    if errores:
        print("\nPROBLEMAS:")
        for e in errores:
            print("  -", e)
        return 1
    print("\nOK: mismas claves y mismos placeholders en todos los idiomas.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
