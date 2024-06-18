PODMAN = podman
GRADLE = ./gradlew

COMPOSE_FILE ?= containers/compose.dev.yml

GRADLE_PROJECT = app
GRADLE_TASK_DEVELOPMENT = :$(GRADLE_PROJECT):runDevelopment
GRADLE_TASK_NORMAL = :$(GRADLE_PROJECT):run

.PHONY: compose-up
compose-up:
	@echo "Starting containers..."
	$(PODMAN) compose -f $(COMPOSE_FILE) up -d

.PHONY: compose-down
compose-down:
	@echo "Stopping and removing containers..."
	$(PODMAN) compose -f $(COMPOSE_FILE) down

.PHONY: dev
dev: compose-up
	@echo "Starting development environment..."
	$(GRADLE) $(GRADLE_TASK_DEVELOPMENT)

.PHONY: run
run: compose-up
	@echo "Starting normal environment..."
	$(GRADLE) $(GRADLE_TASK_NORMAL)

.PHONY: clean
clean: compose-down
	@echo "Cleaning up environment..."
	$(PODMAN) volume prune -f
