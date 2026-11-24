# Categoría "Préstamos": entradas del catálogo (2026-11-24)

Sprint 6 de "Préstamos y financiaciones" (spec `docs/specs/2026-11-24-prestamos-fase0.md`, D6-D8 y §8).
Categoría nueva **Préstamos** (V18, clave `prestamos`, 🏦, `#1e3a8a`). Todas las filas van con `price = null`
(cada usuario escribe su cuota) y ciclo MONTHLY. **Sin microcréditos** (Vivus, Moneyman, Wandoo…), por decisión del usuario.

## Cómo se verificó cada logo

1. Se descargó `https://icon.horse/icon/{dominio}` y `https://www.google.com/s2/favicons?domain={dominio}&sz=128` de cada dominio.
2. Se compusieron en una hoja de contacto y se **miraron una a una**. icon.horse devuelve 200 con una letra gris genérica sobre
   fondo claro (256×256) cuando no conoce el dominio, así que el código HTTP no sirve. El favicon de Google devuelve
   404 y un globo de 16×16 si no tiene icono.
3. Si icon.horse da la letra genérica o un logo claramente peor, la fila lleva `iconUrl` con el favicon de Google
   (mismo patrón que Blissim o Midjourney; la CSP de la web ya lo permite).
4. La web de cada entidad se abrió con `curl -L` para confirmar que existe. Las que dan 403 (BBVA, Bankinter, Bankinter
   Consumer Finance, El Corte Inglés, PayPal) son protecciones antibots; la web existe.

## Filas añadidas (35)

### Bancos (14)

| Nombre | Dominio | Logo | Notas |
|---|---|---|---|
| Banco Santander | bancosantander.es | Google s2 128 px (`iconUrl`) | icon.horse solo da un favicon pixelado de 48 px. En la app (suscripciones guardadas) se usa `santander.com`, que icon.horse sí tiene a 256 px. |
| BBVA | bbva.es | icon.horse 256 px | |
| CaixaBank | caixabank.es | icon.horse 36 px | |
| Banco Sabadell | bancsabadell.com | icon.horse 192 px | |
| Bankinter | bankinter.com | icon.horse 32 px | Incluye a los antiguos clientes de EVO Banco (ver descartes). |
| Unicaja | unicajabanco.es | icon.horse 64 px | |
| Abanca | abanca.com | icon.horse 57 px | |
| Kutxabank | kutxabank.es | icon.horse 180 px | Google no tiene icono. |
| Ibercaja | ibercaja.es | icon.horse 57 px | Google no tiene icono. |
| ING | ing.es | icon.horse 180 px | |
| Openbank | openbank.es | icon.horse 32 px | |
| Cajamar | cajamar.es | icon.horse 57 px | Google no tiene icono. |
| Caja Rural | ruralvia.com | icon.horse 32 px | ruralvia.com redirige a grupocajarural.es (enlace de la fila), pero ese dominio da la letra genérica: el logo se saca de ruralvia.com. |
| Laboral Kutxa | laboralkutxa.com | icon.horse 16 px | **Dudoso (calidad)**: logo real pero de 16 px en las dos fuentes. |

### Financieras de consumo (11)

| Nombre | Dominio | Logo | Notas |
|---|---|---|---|
| Cetelem | cetelem.es | Google s2 de `cetelem.fr` (`iconUrl`) | **Dudoso (calidad)**: cetelem.es y cetelem.com dan la letra genérica; cetelem.fr tiene la "C" verde real, pero a 16 px. |
| Cofidis | cofidis.es | icon.horse 180 px | |
| Santander Consumer Finance | santanderconsumer.es | icon.horse 32 px | |
| Sabadell Consumer Finance | sabadellconsumer.com | icon.horse 192 px | La web redirige a `/colaboradores/`. |
| Bankinter Consumer Finance | bankinterconsumerfinance.com | icon.horse 194 px | Logo "bk card". |
| CaixaBank Payments & Consumer | caixabankpc.com | icon.horse 32 px | |
| Oney | oney.es | Google s2 de `oney.com` (`iconUrl`) | oney.es da la letra genérica; oney.com (grupo) tiene el aro verde a 128 px. |
| WiZink | wizink.es | icon.horse 16 px | **Dudoso (calidad)**: logo real "WZ" de 16 px; wizink.com da la letra genérica. |
| Younited | younited.com | icon.horse 192 px | younited-credit.com redirige a younited.com/es; mismo logo. |
| Financiera El Corte Inglés | elcorteingles.es | icon.horse 256 px | Triángulo verde de la marca. |
| Volkswagen Financial Services | vwfs.es | icon.horse 256 px | |

### Pago a plazos (4)

| Nombre | Dominio | Logo | Notas |
|---|---|---|---|
| Klarna | klarna.com | Google s2 128 px (`iconUrl`) | icon.horse da la letra genérica en klarna.com, www.klarna.com y klarna.es. **En la app, una suscripción ya guardada con este nombre mostrará la inicial** (la app solo usa el dominio para las guardadas). |
| PayPal Paga en 3 plazos | paypal.com | icon.horse 32 px | |
| seQura | sequra.com | icon.horse 256 px | |
| Aplazame | aplazame.com | icon.horse 256 px | La marca se escribe sin tilde. |

### Clínicas (1)

| Nombre | Dominio | Logo | Notas |
|---|---|---|---|
| Vitaldent (financiación) | vitaldent.com | icon.horse 16 px | **Dudoso (calidad)**: la "V" real, de 16 px. Google no tiene icono (ni en vitaldent.es). |

### Genéricos sin marca (5)

Sin dominio, como "Comisión de mantenimiento bancaria": la web y la app muestran la inicial.
Hipoteca, Préstamo personal, Préstamo de coche, Financiación de compra a plazos y Tarjeta revolving.

## Descartados

| Entidad | Motivo |
|---|---|
| EVO Banco | Bankinter la absorbió en 2025 (fusión inscrita y migración tecnológica prevista para julio de 2025, según las notas de prensa de Bankinter). evobanco.com ya sirve el favicon de Bankinter y responde 403. Sus clientes buscan "Bankinter"; en la app, un gasto guardado como "EVO Banco" toma el logo de Bankinter. |
| Microcréditos (Vivus, Moneyman, Wandoo, Dineo…) | Decisión del usuario (§8, P5). |

## Otros cambios del sprint

- `price` admite null en el backend. La web muestra "Importe variable" en la tarjeta y el botón "Añadir" lleva a
  `/subscriptions/new?catalogo={nombre}` con nombre, descripción, moneda, periodicidad y categoría puestos y el importe vacío.
- El escaneo de Gmail ignora la categoría `prestamos` (los correos de un banco casarían con todo).
- Duplicados (D8): la vista "Todos" de la web ya no repite las 17 filas de "prueba" y el contador cuenta servicios distintos.
  "Google AI Pro 5 TB" (cloud) se quita: es el mismo plan que "Google AI Pro" (ia). La web oficial de Google España
  (one.google.com/about/plans, 24-11-2026) lo anuncia como "Google AI Pro (5 TB)" a 21,99 €/mes, con Gemini y 5 TB.
  Se queda la fila de IA con ese precio, y "Google AI Pro 5 TB" y "Google One 5 TB" pasan a ser alias de "Google AI Pro".
  El precio anual (219,99 €) no sale en la página ("Ahorra hasta un 16 %"); se mantiene el que ya había.
- Otros parecidos revisados y **no tocados** (son productos distintos): Xbox Game Pass Premium / PC Game Pass,
  GitHub Pro / Team, ProtonMail / ProtonDrive / Proton Pass Plus, Nintendo Switch Online / Online+.
  Sigue pendiente "Movistar+" (9,99 €) frente a "Movistar Plus+ Cine y Series" (4,99 €): la misma marca escrita de dos formas.
