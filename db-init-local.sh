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


psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge_service (bridge_id, type, register_time,
 date_created, date_modified, active, service_id) VALUES ('10000005', 'HIP', now(), now(), now(), 'true', '10000005')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge_service (bridge_id, type, register_time,
 date_created, date_modified, active, service_id) VALUES ('10000004', 'HIP', now(), now(), now(), 'true', '10000004')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge_service (bridge_id, type, register_time,
 date_created, date_modified, active, service_id) VALUES ('10000002', 'HIU', now(), now(), now(), 'true', '10000002')"
