plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.147" apply false
}

stonecutter active "26.2-fabric" /* [SC] DO NOT EDIT */

apply(from = "gradle/universal.gradle")

stonecutter parameters {
    constants.match(node.project.property("loom.platform").toString(), "fabric", "neoforge")
    replacements {
        string(eval(current.version, "<26.1")) {
            replace("GuiGraphicsExtractor", "GuiGraphics")
            replace(".text(", ".drawString(")
            replace("extractRenderState", "render")
            replace("extractLines", "renderLines")
        }
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds every supported Minecraft and loader combination."
    dependsOn(rootProject.childProjects.values.map { "${it.path}:build" })
}
