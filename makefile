PODMAN = podman
GRADLE = ./gradlew

COMPOSE_FILE ?= containers/compose.dev.yml

GRADLE_PROJECT = app
GRADLE_TASK_DEVELOPMENT = :$(GRADLE_PROJECT):runDevelopment
GRADLE_TASK_NORMAL = :$(GRADLE_PROJECT):run

.PHONY: dev
dev:
	@echo "Starting development environment..."
	$(PODMAN) compose -f $(COMPOSE_FILE) up -d
	$(GRADLE) $(GRADLE_TASK_DEVELOPMENT)

.PHONY: run
run:
	@echo "Starting normal environment..."
	$(PODMAN) compose -f $(COMPOSE_FILE) up -d
	$(GRADLE) $(GRADLE_TASK_NORMAL)

.PHONY: clean
clean:
	@echo "Cleaning up environment..."
	$(PODMAN) compose -f $(COMPOSE_FILE) down
	$(PODMAN) volume prune -f
