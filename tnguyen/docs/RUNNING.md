# Running Guide

## Starting the Services

```bash
# Ensure you are in the project root
cd /home/nguyen/Documents/chat-server-microservices

# Bring up all services in detached mode
docker compose up -d
```

The `docker-compose.yml` defines the following containers:
- `chat-rabbitmq` – RabbitMQ broker with management UI (http://localhost:15672, user: guest, pass: guest)
- `chat-log-service` – Log service (exposed on port **8084**)
- `chat-gateway-service` – API Gateway (exposed on port **8080**)
- `chat-presence-service` – Presence service (exposed on port **8083**)

## Verifying the Setup

- Open the RabbitMQ management UI at `http://localhost:15672`.
- Access the gateway API, e.g., `http://localhost:8080/actuator/health`.
- Check the logs directory (`log-service/logs`) on the host for generated log files.

## Stopping the Services

```bash
docker compose down
```

Feel free to modify the docker‑compose file for additional services (auth, messaging, …) as the project evolves.
