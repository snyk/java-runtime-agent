test: java-goof test-reload test-shutdown test-tomcat

java-goof:
	git clone --depth 1 --branch java9 https://github.com/snyk/java-goof

test-%:
	python3 e2e/test-$*.py

.PHONY: test test-%
