DEV_COMPOSE_FILE=docker-compose.dev.yml
PROD_COMPOSE_FILE=docker-compose.yml
ENV_FILE=stack.env


help:
	@echo " ____  _____ __ __ _____ _____ _____ _____ _____     "
	@echo "|    \|  _  |  |  |     |  |  |   __|   __|_   _|    "
	@echo "|  |  |     |_   _|  |  |  |  |   __|__   | | |      "
	@echo "|____/|__|__| |_| |__  _|_____|_____|_____| |_|      "
	@echo "                     |__|               by AgentP    "
	@echo ""
	@echo "Makefile for managing Docker Compose"
	@echo ""
	@echo "Usage:"
	@echo "  make dev              # Run Docker Compose in development mode"
	@echo "  make prod             # Run Docker Compose in production mode"
	@echo "  make down             # Stop and remove containers"
	@echo "  make build            # Build the containers"
	@echo "  make logs             # Tail logs of the containers"
	@echo "  make test             # Run all unit tests"
	@echo "  make test-unit        # Run unit tests only"
	@echo "  make test-integration # Run integration tests only"
	@echo "  make test-coverage    # Run tests with coverage report"
	@echo "  make test-service SERVICE=<service-name>  # Run tests for specific service"

dev:
	@echo " ____  _____ __ __ _____ _____ _____ _____ _____     "
	@echo "|    \|  _  |  |  |     |  |  |   __|   __|_   _|    "
	@echo "|  |  |     |_   _|  |  |  |  |   __|__   | | |      "
	@echo "|____/|__|__| |_| |__  _|_____|_____|_____| |_|      "
	@echo "                     |__|               by AgentP    "
	@echo ""
	@echo "Starting Docker Compose in development mode..."
	docker-compose --env-file $(ENV_FILE) -f $(DEV_COMPOSE_FILE) up --build

prod:
	@echo " ____  _____ __ __ _____ _____ _____ _____ _____     "
	@echo "|    \|  _  |  |  |     |  |  |   __|   __|_   _|    "
	@echo "|  |  |     |_   _|  |  |  |  |   __|__   | | |      "
	@echo "|____/|__|__| |_| |__  _|_____|_____|_____| |_|      "
	@echo "                     |__|               by AgentP    "
	@echo ""
	@echo "Starting Docker Compose in production mode..."
	docker-compose --env-file $(ENV_FILE) -f $(PROD_COMPOSE_FILE) up --build

down:
	@echo "Stopping and removing containers..."
	docker-compose --env-file $(ENV_FILE) -f $(DEV_COMPOSE_FILE) down
	docker-compose --env-file $(ENV_FILE) -f $(PROD_COMPOSE_FILE) down

build:
	@echo " ____  _____ __ __ _____ _____ _____ _____ _____     "
	@echo "|    \|  _  |  |  |     |  |  |   __|   __|_   _|    "
	@echo "|  |  |     |_   _|  |  |  |  |   __|__   | | |      "
	@echo "|____/|__|__| |_| |__  _|_____|_____|_____| |_|      "
	@echo "                     |__|               by AgentP    "
	@echo ""
	@echo "Building Docker images..."
	docker-compose --env-file $(ENV_FILE) -f $(DEV_COMPOSE_FILE) build

logs:
	@echo "Tailing logs of containers..."
	docker-compose --env-file $(ENV_FILE) -f $(DEV_COMPOSE_FILE) logs -f

test:
	@echo " ____  _____ __ __ _____ _____ _____ _____ _____     "
	@echo "|    \|  _  |  |  |     |  |  |   __|   __|_   _|    "
	@echo "|  |  |     |_   _|  |  |  |  |   __|__   | | |      "
	@echo "|____/|__|__| |_| |__  _|_____|_____|_____| |_|      "
	@echo "                     |__|               by AgentP    "
	@echo ""
	@echo "Running unit tests for all modules..."
	.\mvnw.cmd test --batch-mode -DfailIfNoTests=false

test-unit:
	@echo "Running unit tests only..."
	.\mvnw.cmd test -Dtest="**/*Test" --batch-mode

test-integration:
	@echo "Running integration tests only..."
	.\mvnw.cmd test -Dtest="**/*IT" --batch-mode

test-coverage:
	@echo "Running tests with coverage report..."
	.\mvnw.cmd test jacoco:report --batch-mode

test-service:
	@echo "Running tests for specific service: $(SERVICE)"
	.\mvnw.cmd test -pl $(SERVICE) -am --batch-mode

clean:
	@echo "Cleaning up Docker resources..."
	docker-compose --env-file $(ENV_FILE) -f $(DEV_COMPOSE_FILE) down -v --rmi local
	docker system prune -f

reset-ratelimit:
	@echo "Resetting all rate limits in Redis..."
	docker exec redis_container redis-cli KEYS "ratelimit:*" | xargs -I {} docker exec redis_container redis-cli DEL {}
	@echo "Rate limits cleared!"

redis-cli:
	@echo "Opening Redis CLI..."
	docker exec -it redis_container redis-cli

.PHONY: help dev prod down build logs test test-unit test-integration test-coverage test-service clean reset-ratelimit redis-cli
