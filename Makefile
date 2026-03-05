OPENAPI_JSON := build/smithyprojections/smithy-poc/openapi/openapi/MyService.openapi.json
VALIDATION_RULES := build/smithyprojections/smithy-poc/validation/validation-rules/validation-rules.json
VALGEN := cd cmd/valgen &&

.PHONY: all clean smithy generate generate-proto generate-validation docs

all: generate generate-proto generate-validation docs

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

generate-validation: generate generate-proto
	$(VALGEN) go run . \
		-rules ../../$(VALIDATION_RULES) \
		-src ../../gen/api/types.gen.go \
		-pkg openapi \
		-import "github.com/imishinist/smithy-poc/gen/api" \
		-src-pkg api \
		-o ../../gen/validation/openapi/validate.gen.go
	$(VALGEN) go run . \
		-rules ../../$(VALIDATION_RULES) \
		-src ../../gen/proto/model.pb.go \
		-pkg proto \
		-import "github.com/imishinist/smithy-poc/gen/proto" \
		-src-pkg proto \
		-o ../../gen/validation/proto/validate.gen.go

docs: smithy
	./scripts/generate-docs.sh

clean:
	gradle clean
	rm -f gen/api/types.gen.go gen/proto/*.go docs/index.html
	rm -f gen/validation/openapi/*.go gen/validation/proto/*.go
