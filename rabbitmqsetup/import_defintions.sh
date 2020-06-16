rabbitmq_container_id=$(docker ps -aqf "name=rabbitmq")
docker cp ./rabbitmq_defns_with_gw.json $rabbitmq_container_id:/tmp
docker exec -it $rabbitmq_container_id /bin/bash -c "rabbitmqctl import_definitions /tmp/rabbitmq_defns_with_gw.json"
