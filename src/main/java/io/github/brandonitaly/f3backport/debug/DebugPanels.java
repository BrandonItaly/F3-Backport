package io.github.brandonitaly.f3backport.debug;

import static io.github.brandonitaly.f3backport.debug.DebugPanel.Side.*;

public final class DebugPanels {
    private DebugPanels() {}

    public static DebugPanel create(String id) {
        return switch (id) {
            case "position", "biome" -> new DebugPanel("position", "Position", 0xffffff, LEFT);
            case "memory" -> new DebugPanel(id, "Memory", 0xff9b00, RIGHT);
            case "system" -> new DebugPanel(id, "System Specs", 0xff0000, RIGHT);
            case "performance" -> new DebugPanel(id, "Performance Impactors", 0x00ff00, RIGHT);
            case "chunk_rendering" -> new DebugPanel(id, "Chunk Rendering", 0xf0b0a0, null);
            case "chunk_generation" -> new DebugPanel(id, "Chunk Generation", 0x99ffaa, null);
            case "heightmaps" -> new DebugPanel(id, "Heightmap", 0x00aaff, null);
            case "spawn_counts" -> new DebugPanel(id, "Entity Spawn Counts", 0xff4444, null);
            case "light" -> new DebugPanel(id, "Light", 0xffff00, null);
            case "looking_at_block" -> new DebugPanel(id, "Looking At Block", 0xcc00ff, null);
            case "looking_at_fluid" -> new DebugPanel(id, "Looking At Fluid", 0xffcc00, null);
            case "looking_at_entity" -> new DebugPanel(id, "Looking At Entity", 0x00ffcc, null);
            case "help" -> new DebugPanel(id, "Help", null, null);
            case "misc", "priority_left", "priority_right" -> new DebugPanel(id, "", null, null);
            default -> new DebugPanel(id, id, null, null);
        };
    }
}
