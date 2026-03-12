.PHONY: build test publish-local clean

build:
	./gradlew build

test:
	./gradlew :shared:allTests

publish-local:
	./gradlew publishToMavenLocal

clean:
	./gradlew clean
