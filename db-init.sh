#!/bin/sh

database="gateway"
db_host="localhost"
db_user="postgres"
db_password="password"

export PGPASSWORD=$db_password

psql -h $db_host -U $db_user $database

psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.consent_manager (name, url, cm_id, suffix, active, date_created,
 date_modified, blocklisted, license, licensing_authority)
  VALUES ('', 'http://consent-manager:8000', 'ncg', 'ncg', 'true', now(), now(), 'false', '', '')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.consent_manager (name, url, cm_id, suffix, active, date_created,
 date_modified, blocklisted, license, licensing_authority)
  VALUES ('', 'http://consent-manager:8000', 'nhs', 'nhs', 'true', now(), now(), 'false', '', '')"

psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge (name, url, bridge_id, active, date_created, date_modified, blocklisted)
  VALUES ('', 'http://hip:80', '10000005', 'true', now(), now(), 'false')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge (name, url, bridge_id, active, date_created, date_modified, blocklisted)
  VALUES ('', 'http://172.16.2.27:8001', '10000004', 'true', now(), now(), 'false')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge (name, url, bridge_id, active, date_created, date_modified, blocklisted)
  VALUES ('', 'http://ncg.tmc.gov.in/hip-service', '10000002', 'true', now(), now(), 'false')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge (name, url, bridge_id, active, date_created, date_modified, blocklisted)
  VALUES ('', 'https://ncg.amritatech.com', '10000003', 'true', now(), now(), 'false')"

psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge_service (bridge_id, type, register_time,
 date_created, date_modified, active) VALUES ('10000005', 'HIP', now(), now(), now(), 'true')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge_service (bridge_id, type, register_time,
 date_created, date_modified, active) VALUES ('10000004', 'HIP', now(), now(), now(), 'true')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge_service (bridge_id, type, register_time,
 date_created, date_modified, active) VALUES ('10000002', 'HIP', now(), now(), now(), 'true')"
psql -d $database -h $db_host -U $db_user -c "INSERT INTO public.bridge_service (bridge_id, type, register_time,
 date_created, date_modified, active) VALUES ('10000003', 'HIP', now(), now(), now(), 'true')"
