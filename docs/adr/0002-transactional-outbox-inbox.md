# ADR-0002: Transactional outbox e inbox

**Estado:** Aceptado

## Contexto

No existe transacción atómica entre PostgreSQL y RabbitMQ. Publicar directamente después de cambiar el estado crea ventanas de pérdida o duplicación.

## Decisión

Persistir el evento en outbox dentro de la transacción del agregado; publicar con confirms; consumir con inbox, submission única e idempotencia bancaria.

## Consecuencias

- Entrega al menos una vez, no exactamente una vez.
- Duplicados seguros y observables.
- Requiere limpieza y monitoreo de outbox/inbox.
