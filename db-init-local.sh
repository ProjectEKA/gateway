#!/bin/sh

database="gateway"
db_host="localhost"
db_user="postgres"
db_password="password"

export PGPASSWORD=$db_password

psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.consent_manager (name, url, cm_id, suffix, active, date_created,
 date_modified, blocklisted, license, licensing_authority)
  VALUES ('', 'http://localhost:8081', 'ncg', 'ncg', 'true', now(), now(), 'false', '', '')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.consent_manager (name, url, cm_id, suffix, active, date_created,
 date_modified, blocklisted, license, licensing_authority)
  VALUES ('', 'http://localhost:8002', 'nhs', 'nhs', 'true', now(), now(), 'false', '', '')"

psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge (name, url, bridge_id, active, date_created, date_modified, blocklisted)
  VALUES ('', 'http://localhost:9052', '10000005', 'true', now(), now(), 'false')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge (name, url, bridge_id, active, date_created, date_modified, blocklisted)
  VALUES ('', 'http://localhost:8001', '10000004', 'true', now(), now(), 'false')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge (name, url, bridge_id, active, date_created, date_modified, blocklisted)
  VALUES ('', 'http://localhost:8003', '10000002', 'true', now(), now(), 'false')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge (name, url, bridge_id, active, date_created, date_modified, blocklisted)
  VALUES ('', 'http://localhost:8005', '10000010', 'true', now(), now(), 'false')"

psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge_service (bridge_id, service_id, is_hip, active, register_time, date_created, date_modified)
 VALUES ('10000005', '10000005', 'true', 'true', now(), now(), now())"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge_service (bridge_id, service_id, is_hip, active, register_time, date_created, date_modified)
 VALUES ('10000004', '10000004', 'true', 'true', now(), now(), now())"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge_service (bridge_id, service_id, is_hiu, active, register_time, date_created, date_modified)
 VALUES ('10000002', '10000002', 'true', 'true', now(), now(), now())"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge_service (bridge_id, service_id, is_hip, is_hiu, is_health_locker, active,
 register_time, date_created, date_modified) VALUES ('10000010', '10000010', 'true', 'true', 'true', 'true', now(), now(), now())"
