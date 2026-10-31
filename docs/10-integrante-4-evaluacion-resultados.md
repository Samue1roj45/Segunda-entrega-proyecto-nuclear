# Integrante 4 — Evaluación y resultados

Módulo de seguimiento de participaciones, calificación, reportes y notificaciones por correo Django.

## Decisiones

- Intentos independientes (`Participacion` N:1 con `AutorizacionEstudiante` + `numero_intento`).
- Fórmula: `nota = (peso_obtenido / peso_total) * rubrica.escala_maxima` (escala 100 si no hay rúbrica).
- RN17: no se inicia un intento nuevo después del punto medio entre `fecha_inicio` y `fecha_fin` de la práctica. Se puede reanudar un intento `EN_CURSO`.
- Notificaciones solo por `django.core.mail`.
- Ausencia: se crea `Participacion` `NO_INICIADA` con `Resultado` 0 para trazabilidad, sin duplicar al reejecutar.

## Patrones

| Patrón | Dónde | Responsabilidad |
|---|---|---|
| State | `apps/participaciones/state.py` | Transiciones NO_INICIADA → EN_CURSO → FINALIZADA/INCOMPLETA |
| Strategy | `apps/resultados/strategies.py` | Completa, incompleta, ausencia |
| Facade | `participaciones/facade.py`, `resultados/facade.py`, `reportes/facade.py`, `resultados/notificaciones.py` | Coordinar inicio/cierre, cálculo, consulta, reportes y correo |
| Proxy | `apps/resultados/proxy.py` | Estudiante solo lo suyo; docente solo sus prácticas |
| Observer | `apps/resultados/events.py` + `notificaciones.py` + `practicas/signals.py` | Convocatoria, cambios, calificación, retroalimentación |

## Migraciones

- `participaciones/0002_intentos_independientes.py`
- `resultados/0002_feedback_y_notificaciones.py`

No aplicar sobre una base compartida sin autorización del equipo.

## Pruebas

```powershell
cd backend
python manage.py test apps.participaciones apps.resultados apps.reportes --settings=config.test_settings
```

SQLite en memoria se usa **solo** en `test_settings`. El runtime del proyecto sigue en PostgreSQL.
