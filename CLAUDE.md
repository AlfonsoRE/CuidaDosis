@AGENTS.md

## Notas específicas de Claude Code

- Al iniciar una fase, usa modo plan: lee lo necesario, propón el plan y espera aprobación antes de escribir código.
- Usa AskUserQuestion para las decisiones de diseño importantes en lugar de suponer.
- Al cerrar una fase, termina tu respuesta con: qué se hizo, cómo probarlo, qué no se pudo probar y el hash del commit.
