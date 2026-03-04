package net.imishinist.smithy.openapi;

import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.fromsmithy.Smithy2OpenApiExtension;

import java.util.List;

public class UnionDiscriminatorExtension implements Smithy2OpenApiExtension {
    @Override
    public List<OpenApiMapper> getOpenApiMappers() {
        return List.of(new UnionDiscriminatorMapper());
    }
}
