all: build-verbose
	docker run -i $(shell docker build -q .)

build-verbose:
	(cd ../.. && ./gradlew shadow)
	cp ../build/libs/agent.jar snyk-agent.jar
	docker build .

.PHONY: build-verbose
