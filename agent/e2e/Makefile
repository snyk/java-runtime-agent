all: build-verbose
	cp ../build/libs/agent.jar snyk-agent.jar
	docker run -i $(shell docker build -q .)

build-verbose:
	(cd ../.. && ./gradlew shadow)
	docker build .

.PHONY: build-verbose
