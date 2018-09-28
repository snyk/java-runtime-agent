all: build-verbose
	(cd ../.. && ./gradlew shadow)
	cp ../build/libs/agent.jar snyk-agent.jar
	docker run -i $(shell docker build -q .)

build-verbose:
	docker build .