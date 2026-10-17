# Migraciones del backend

Flyway administra el esquema `cecamed`. Esta etapa no crea tablas de negocio.
La primera funcionalidad incorporará `V1__create_patients.sql`; las siguientes
migraciones deberán aumentar la versión. No editar migraciones ya aplicadas.

Los nombres y checksums se validan al arrancar. `clean` y el baseline automático
están deshabilitados; no usar `schema.sql`, `data.sql` ni `ddl-auto=update`.
La tarea `:cecamed-api:postgresTest` comprueba el esquema y el historial contra
una base PostgreSQL desechable. Ver las instrucciones en el README raíz.

La base del backend es independiente de la aplicación de escritorio. No copiar
automáticamente sus migraciones ni apuntar este proyecto a la base existente.
