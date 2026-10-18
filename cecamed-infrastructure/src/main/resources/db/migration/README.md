# Migraciones del backend

Flyway administra el esquema `cecamed`. La secuencia actual es:

- `V1__identity_and_audit.sql`: identidad, tokens y auditoría.
- `V2__patient.sql`: pacientes.
- `V3__create_consultations_table.sql`: consultas.
- `V4__create_medical_records_table.sql`: expedientes.
- `V5__stabilize_clinical_data.sql`: identificación opcional, IDs generados de consultas,
  CIE-10 y expedientes iniciales de pacientes existentes. Conserva los IDs anteriores
  y coloca la secuencia después del máximo existente.

El script de expedientes tenía la versión duplicada `V2`; se renombró a `V4`
para permitir la migración desde una base vacía y mantener las versiones de los
otros scripts. No se modificó su SQL. Si una instalación llegó a aplicar el
antiguo `V2` de expedientes de forma aislada, revisar su historial antes de
actualizar; no ejecutar `repair` ni borrar datos automáticamente.

Las siguientes migraciones deben aumentar la versión. No editar migraciones ya aplicadas.

Los nombres y checksums se validan al arrancar. `clean` y el baseline automático
están deshabilitados; no usar `schema.sql`, `data.sql` ni `ddl-auto=update`.
La tarea `:cecamed-api:postgresTest` comprueba el esquema y el historial contra
una base PostgreSQL desechable. Ver las instrucciones en el README raíz.

La base del backend es independiente de la aplicación de escritorio. No copiar
automáticamente sus migraciones ni apuntar este proyecto a la base existente.
