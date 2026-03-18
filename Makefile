.PHONY: build test publish-local clean release publish-bundle \
       setup demo-ios demo-android demo-all clean-example

build:
	./gradlew build

test:
	./gradlew :encore-kmp:allTests

publish-local:
	./gradlew :encore-kmp:publishToMavenLocal

clean:
	./gradlew clean

release:
	./scripts/release/publish-release.sh

publish-bundle:
	./scripts/release/publish-release.sh --bundle-only

setup:
	./scripts/demo/setup-example.sh

demo-ios:
	./scripts/demo/demo-ios.sh

demo-android:
	./scripts/demo/demo-android.sh

demo-all: demo-ios demo-android

clean-example:
	./scripts/demo/clean-example.sh
