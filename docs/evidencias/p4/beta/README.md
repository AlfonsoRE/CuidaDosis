# Evidencia del grupo beta

- `CuidaDosis_P4_beta_anonimizado.csv`: una fila por archivo recibido (34), con códigos B01 a B34 asignados por el equipo de análisis (no son los códigos que escribieron los participantes). Incluye: formato del archivo, marca del teléfono (agrupada) y versión de Android, rol y experiencia, resultado y dificultad de las 8 tareas, aviso previo y alarma, los 10 ítems de la escala SUS con su puntaje (0 a 100) y las 7 preguntas sobre la app.
- La columna `incluido` indica si el archivo entra en el análisis (33 de 34; se excluye una respuesta simulada). `duplicado_exacto_de` indica el código del archivo con respuestas idénticas.
- Cálculo del SUS: ítems impares, respuesta menos 1; ítems pares, 5 menos la respuesta; suma por 2.5. Solo se calcula con los 10 ítems completos (27 archivos).
- El CSV no contiene nombres, correos, teléfonos ni comentarios libres.
