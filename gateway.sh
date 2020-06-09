#!/bin/sh
case $@ in
  start)
    docker-compose -f docker-compose-rabbitmq.yml down
        python ../infrastructure/rabbitmq/rabbitmqsetup.py ./rabbitmq/gateway_rabbitmq.json ./rabbitmq/definitions.json
        docker-compose -f docker-compose-rabbitmq.yml up --build -d
        docker-compose -f docker-compose-backend.yml -p backend start
