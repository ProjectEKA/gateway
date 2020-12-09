#!/bin/sh

database="gateway"
db_host="localhost"
db_user="postgres"
db_password="password"

export PGPASSWORD=$db_password

psql -d $database -h $db_host -U $db_user -c "CREATE EXTENSION tablefunc"

psql -d $database -h $db_host -U $db_user -c "INSERT INTO bridge_service(service_id, bridge_id, active, register_time, date_created, date_modified, name, endpoints, is_hip, is_hiu, is_health_locker)(SELECT split_part(combined_id, '-', 1) as service_id, split_part(combined_id, '-', 2) as bridge_id,
active, register_time, date_created, date_modified, name, endpoints, is_hip, is_hiu, is_health_locker
FROM CROSSTAB('SELECT CONCAT(service_id,''-'', bridge_id), active, register_time, date_created, date_modified, name, endpoints, type,
                CASE WHEN type IS NOT NULL AND active = true THEN true ELSE false END
            FROM bridge_service_old ORDER BY 1,2','VALUES (''HIP''), (''HIU''), (''HEALTH_LOCKER'')')
AS flattened(combined_id VARCHAR(100), active BOOLEAN, register_time TIMESTAMP, date_created TIMESTAMP, date_modified TIMESTAMP, name VARCHAR(50), endpoints JSON, is_hip BOOLEAN, is_hiu BOOLEAN, is_health_locker BOOLEAN))"

psql -d $database -h $db_host -U $db_user -c "UPDATE bridge_service SET active = true"