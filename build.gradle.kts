plugins {
    application
}

group = "com.emu"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:6.7.0")
    implementation("org.slf4j:slf4j-simple:2.0.17")
}

tasks.withType<JavaCompile> {
    options.release = 25
}

application {
    mainClass = "com.emu.codemodeval.App"
}
