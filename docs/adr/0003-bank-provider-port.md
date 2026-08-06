# ADR-0003: Puerto bancario multi-adaptador

**Estado:** Aceptado

## Contexto

Cada banco expone protocolos, estados, credenciales y capacidades distintos. Acoplarlos a REST controllers impide evolución, pruebas y homologación.

## Decisión

Usar `BankPaymentProvider`, perfiles persistidos por empresa/banco y un resolver por `providerKey`. No crear adaptadores de marca sin contrato real.

## Consecuencias

- Dominio estable frente a APIs propietarias.
- Cada adapter debe homologarse y pasar un contrato común.
- Los estados externos siempre se normalizan antes de afectar la nómina.
