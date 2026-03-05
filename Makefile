OPENAPI_JSON := build/smithyprojections/smithy-poc/openapi/openapi/MyService.openapi.json

.PHONY: all clean smithy generate generate-proto docs

all: generate generate-proto docs

smithy:
	gradle build

generate: smithy
	cd gen && go run github.com/oapi-codegen/oapi-codegen/v2/cmd/oapi-codegen \
		--generate types \
		--package api \
		-o api/types.gen.go \
		../$(OPENAPI_JSON)

generate-proto: smithy
	buf generate

docs: smithy
	./scripts/generate-docs.sh

clean:
	gradle clean
	rm -f gen/api/types.gen.go gen/proto/*.go docs/index.html
