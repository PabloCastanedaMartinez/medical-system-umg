# Migraciones del backend

Flyway administra el esquema `cecamed`. Esta etapa no crea tablas de negocio.
La primera funcionalidad incorporará `V1__create_patients.sql`; las siguientes
migraciones deberán aumentar la versión. No editar migraciones ya aplicadas.

La base del backend es independiente de la aplicación de escritorio. No copiar
automáticamente sus migraciones ni apuntar este proyecto a la base existente.
