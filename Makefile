OPENAPI_JSON := build/smithyprojections/smithy-poc/openapi/openapi/MyService.openapi.json

.PHONY: all clean smithy generate docs

all: generate docs

smithy:
	gradle build

generate: smithy
	cd gen && go run github.com/oapi-codegen/oapi-codegen/v2/cmd/oapi-codegen \
		--generate types \
		--package api \
		-o api/types.gen.go \
		../$(OPENAPI_JSON)

docs: smithy
	./scripts/generate-docs.sh

clean:
	gradle clean
	rm -f gen/api/types.gen.go docs/index.html
