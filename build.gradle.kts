plugins {
    java
    id("software.amazon.smithy.gradle.smithy-jar") version "1.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("software.amazon.smithy:smithy-model:1.68.0")
    implementation("software.amazon.smithy:smithy-openapi:1.68.0")
    implementation("software.amazon.smithy:smithy-aws-traits:1.68.0")
}

java.sourceSets["main"].java {
    srcDirs("model", "src/main/smithy")
}

java.sourceSets["main"].java {
    srcDirs("model", "src/main/smithy")
}
