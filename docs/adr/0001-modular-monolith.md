# ADR-0001: Monolito modular con core financiero hexagonal

**Estado:** Aceptado

## Contexto

El sistema requiere transacciones fuertes entre nómina, auditoría y outbox. Separarlo prematuramente en microservicios introduciría transacciones distribuidas sin aportar una frontera de negocio estable.

## Decisión

Mantener un despliegue único, módulos por capability y dominio financiero independiente de frameworks. Los contratos bancarios y mensajes versionados serán las fronteras de extracción futura.

## Consecuencias

- Menor complejidad operativa y de consistencia.
- Escalado conjunto en esta fase.
- Necesidad de reglas arquitectónicas para evitar acoplamiento accidental.
