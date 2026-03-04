package net.imishinist.smithy.extensions;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ValidationRulesPlugin implements SmithyBuildPlugin {

    private static final ShapeId COMPLEX_VALIDATION = ShapeId.from("net.imishinist.traits#complexValidation");

    @Override
    public String getName() {
        return "validation-rules";
    }

    @Override
    public void execute(PluginContext context) {
        Model model = context.getModel();
        List<Node> allRules = new ArrayList<>();

        for (StructureShape struct : model.getStructureShapes()) {
            var traitOpt = struct.findTrait(COMPLEX_VALIDATION);
            if (traitOpt.isEmpty()) continue;

            ArrayNode rules = traitOpt.get().toNode().expectArrayNode();
            for (Node rule : rules.getElements()) {
                ObjectNode ruleObj = rule.expectObjectNode();
                // Add the source structure name to each rule
                allRules.add(ruleObj.withMember("structure", Node.from(struct.getId().getName())));
            }
        }

        if (allRules.isEmpty()) return;

        ObjectNode output = ObjectNode.builder()
                .withMember("rules", ArrayNode.fromNodes(allRules))
                .build();

        Path outputFile = context.getFileManifest().getBaseDir().resolve("validation-rules.json");
        try {
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, Node.prettyPrintJson(output));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write validation rules", e);
        }
        context.getFileManifest().addFile(outputFile);
    }
}
