package io.github.brandonitaly.f3backport.debug;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts the pre-26.4 entry output without changing which entries vanilla enables. */
public final class DebugFrame {
    public record Line(String group, String text, boolean priority) {}
    private static final Pattern FORMATTING = Pattern.compile("\u00a7[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEIGHT = Pattern.compile("(SW|OW|ML|S|O|M): ([^ ]+)");
    private static final Pattern COUNTER = Pattern.compile("([A-Za-z]+):\\s*([^, ]+)");
    private static final Map<String, String> HEIGHT_NAMES = Map.of(
            "SW", "(WG) Surface", "S", "Surface", "OW", "(WG) Ocean Floor",
            "O", "Ocean Floor", "M", "Motion", "ML", "Motion (w/o Leaves)");
    private static final Map<String, String> SPAWN_NAMES = Map.of(
            "M", "Monster", "C", "Creature", "A", "Ambient", "Axo", "Axolotls",
            "U", "Underground Water Creature", "W", "Water Creature", "WA", "Water Ambient", "Mi", "Misc");
    private final Map<String, DebugPanel> panels = new LinkedHashMap<>();
    private int priorityCount;

    public void clear() { panels.clear(); priorityCount = 0; }
    public DebugPanel panel(String id) {
        DebugPanel definition = DebugPanels.create(id);
        return panels.computeIfAbsent(definition.id, ignored -> definition);
    }
    public Map<String, DebugPanel> panels() {
        // Miscellaneous data follows all explicitly grouped entries, as in the snapshot.
        DebugPanel misc = panels.remove("misc");
        if (misc != null) panels.put("misc", misc);
        return panels;
    }

    public void accept(String entry, List<Line> output) {
        if (entry.equals("minecraft:heightmap")) {
            heightmaps(output);
            return;
        }
        boolean serverLight = output.stream().anyMatch(line -> plain(line.text).startsWith("Server Light:"));
        boolean serverBiome = output.stream().anyMatch(line -> plain(line.text).startsWith("Server Biome:"));
        for (Line line : output) {
            if (line.text == null || line.text.isEmpty()) continue;
            if (line.priority) {
                panel(priorityCount++ % 2 == 0 ? "priority_left" : "priority_right").lines.add(line.text);
                continue;
            }
            // Unknown mod entries keep their text and group; never parse it as vanilla data.
            if (!entry.startsWith("minecraft:")) {
                panel(line.group == null ? "misc" : line.group).lines.add(line.text);
                continue;
            }
            String id = entry.substring("minecraft:".length());
            String text = plain(line.text);
            switch (id) {
                case "player_position", "player_section_position", "player_speed", "biome" -> {
                    if (text.contains(" FC: ")) {
                        String[] parts = text.split(" FC: ", 2);
                        fact("position", "Dimension", white(parts[0]));
                        if (!parts[1].equals("0")) fact("position", "Forced Chunks", white(parts[1]));
                    } else {
                        labelled("position", line.text, serverBiome && text.startsWith("Biome:") ? "Client Biome" : null);
                    }
                }
                case "memory", "detailed_memory" -> labelled("memory", line.text, switch (label(text)) {
                    case "Mem" -> "Used";
                    case "Allocation rate" -> "Alloc rate";
                    case "Memory (heap)" -> "Heap";
                    case "Memory (non-heap)" -> "Non-heap";
                    default -> null;
                });
                case "system_specs" -> labelled("system", line.text, null);
                case "light_levels" -> labelled(serverLight ? "light" : "misc", line.text,
                        text.startsWith("Server Light:") ? "Server" : serverLight ? "Client" : "Light");
                case "looking_at_block_state", "looking_at_fluid_state", "looking_at_entity" -> target(id, line.text);
                case "looking_at_block_tags", "looking_at_fluid_tags", "looking_at_entity_tags" ->
                        panel(targetGroup(id)).lines.add(line.text);
                case "simple_performance_impactors" -> performance(line.text);
                case "particle_render_stats" -> labelled("performance", line.text, "Particles");
                case "chunk_source_stats" -> chunkSource(text);
                case "chunk_render_stats" -> chunkRendering(text);
                case "chunk_generation_stats" -> generation(line.text);
                case "entity_render_stats" -> entityRendering(text);
                case "entity_spawn_counts" -> spawnCounts(text);
                case "tps" -> server(text);
                case "day_count" -> fact("misc", "Day", white(text.replaceFirst("^Day #", "")));
                case "sound_mood" -> sound(text);
                case "sound_cache" -> labelled("misc", line.text, "Sound Cache");
                case "post_effects" -> labelled("misc", line.text, "Post Effects");
                case "gpu_utilization" -> labelled("misc", line.text, "GPU Utilization");
                default -> labelled(line.group == null ? "misc" : vanillaGroup(line.group), line.text, null);
            }
        }
    }

    public void help(List<String> vanillaLeft) {
        for (int i = 0; i < vanillaLeft.size(); i++) {
            if (vanillaLeft.get(i).startsWith("Debug charts:")) {
                for (int j = i; j < vanillaLeft.size(); j++) {
                    if (!vanillaLeft.get(j).isEmpty()) panel("help").lines.add(vanillaLeft.get(j));
                }
                return;
            }
        }
    }

    private void target(String entry, String raw) {
        String group = targetGroup(entry);
        String text = plain(raw);
        if (text.startsWith("Targeted Block:") || text.startsWith("Targeted Fluid:")) {
            fact(group, "Coordinates", white(value(text)));
        } else if (text.equals("Targeted Entity")) {
            // The section header replaces the old underlined heading.
        } else if (text.contains(": ")) {
            labelled(group, raw, null);
        } else {
            fact(group, "Type", white(raw));
        }
    }

    private static String targetGroup(String entry) {
        return entry.contains("block") ? "looking_at_block" : entry.contains("fluid") ? "looking_at_fluid" : "looking_at_entity";
    }

    private void performance(String text) {
        if (text.contains("B: ")) {
            fact("performance", "OIT", white(text.contains("improved-transparency") ? "On" : "Off"));
            fact("performance", "Clouds", white(text.contains("fast-clouds") ? "Fast" : text.contains("fancy-clouds") ? "Fancy" : "Off"));
            fact("performance", "Biome Blend", white(text.substring(text.indexOf("B: ") + 3)));
        } else if (text.startsWith("Terrain Rendering:")) {
            fact("performance", "Multi-draw", white(text.contains("multidrawindirect") ? "On" : "Off"));
        } else labelled("performance", text, null);
    }

    private void server(String text) {
        int packets = text.lastIndexOf(", ", text.lastIndexOf(", ") - 1);
        if (packets < 0) { panel("misc").lines.add(text); return; }
        fact("misc", "Server", white(text.substring(0, packets).replace("Integrated server", "Integrated").replace(" server", "")));
        fact("misc", "Packets", numbers(text.substring(packets + 2)));
    }

    private void sound(String text) {
        int mood = text.lastIndexOf(" (Mood ");
        if (mood < 0) { fact("misc", "Sounds", numbers(text)); return; }
        fact("misc", "Sounds", numbers(text.substring(0, mood).replaceFirst("^Sounds: ", "")));
        fact("misc", "Mood", white(text.substring(mood + 7, text.length() - 1)));
    }

    private void entityRendering(String text) {
        Matcher matcher = Pattern.compile("E: ([^,]+), SD: (.+)").matcher(text);
        if (matcher.matches()) {
            fact("misc", "Entities", numbers(matcher.group(1)));
            fact("misc", "Simulation Distance", white(matcher.group(2)));
        } else panel("misc").lines.add(text);
    }

    private void chunkSource(String text) {
        Matcher matcher = Pattern.compile("Chunks\\[([CS])] W: (.+) E: (.+)").matcher(text);
        if (!matcher.matches()) { panel("performance").lines.add(text); return; }
        boolean client = matcher.group(1).equals("C");
        String chunks = matcher.group(2);
        String entities = matcher.group(3);
        String[] counts = chunks.split(", ");
        if (client && counts.length == 2) chunks = counts[1] + " loaded (" + counts[0] + " max)";
        else if (!client) chunks += " loaded";
        String[] entityCounts = entities.split(",\\s*");
        if (client && entityCounts.length == 3) entities = entityCounts[0] + " in " + entityCounts[1] + " sections (" + entityCounts[2] + " ticking chunks)";
        fact("performance", client ? "Client Chunks" : "Server Chunks", numbers(chunks));
        fact("performance", client ? "Client Entities" : "Server Entities", numbers(entities));
    }

    private void chunkRendering(String text) {
        Matcher matcher = Pattern.compile("C: ([^ ]+)(?: ([^ ]+))? D: ([^,]+), (.+)").matcher(text);
        if (!matcher.matches()) { panel("chunk_rendering").lines.add(text); return; }
        fact("chunk_rendering", "Chunk Sections", numbers(matcher.group(1).replace("/", " / ")));
        if (matcher.group(2) == null) fact("chunk_rendering", "Culling", white("off"));
        fact("chunk_rendering", "View Distance", white(matcher.group(3)));
        Matcher counters = COUNTER.matcher(matcher.group(4));
        while (counters.find()) fact("chunk_rendering", switch (counters.group(1)) {
            case "pC" -> "Queued";
            case "pU" -> "Pending Upload";
            case "aB" -> "Avail Buffers";
            default -> counters.group(1);
        }, white(counters.group(2)));
    }

    private void generation(String text) {
        // Noise/biome sources used compressed multi-value rows before 26.4.
        Matcher matcher = Pattern.compile("([A-Za-z_]+):\\s*(.*?)(?= [A-Za-z_]+:|$)").matcher(text);
        boolean matched = false;
        while (matcher.find()) {
            matched = true;
            fact("chunk_generation", matcher.group(1), white(matcher.group(2)));
        }
        if (!matched) panel("chunk_generation").lines.add(text);
    }

    private void spawnCounts(String text) {
        Matcher matcher = COUNTER.matcher(text);
        boolean matched = false;
        while (matcher.find()) {
            matched = true;
            String name = matcher.group(1);
            fact("spawn_counts", name.equals("SC") ? "Chunks" : SPAWN_NAMES.getOrDefault(name, name), white(matcher.group(2)));
        }
        if (!matched) panel("spawn_counts").lines.add(text);
    }

    private void heightmaps(List<Line> output) {
        if (output.isEmpty()) return;
        Map<String, String> client = new LinkedHashMap<>();
        Map<String, String> server = new LinkedHashMap<>();
        for (Line line : output) {
            Matcher matcher = HEIGHT.matcher(line.text);
            Map<String, String> target = line.text.startsWith("CH") ? client : server;
            while (matcher.find()) target.put(matcher.group(1), matcher.group(2));
        }
        for (String key : List.of("SW", "S", "OW", "O", "M", "ML")) {
            String c = client.get(key), s = server.get(key);
            String result = c != null ? c : s;
            if (result == null || result.equals("??")) result = "";
            if (c != null && s != null && !s.equals("??") && !c.equals(s)) result = c + " (client), " + s + " (server)";
            fact("heightmaps", HEIGHT_NAMES.get(key), numbers(result));
        }
    }

    private void labelled(String group, String raw, String rename) {
        int colon = raw.indexOf(": ");
        if (colon < 0) { panel(group).lines.add(raw); return; }
        String name = plain(raw.substring(0, colon));
        String val = raw.substring(colon + 2);
        fact(group, rename != null ? rename : name, val.indexOf('\u00a7') >= 0 ? val : white(val));
    }

    public void fact(String group, String name, String value) { panel(group).fact(name, value); }
    public static String plain(String text) { return FORMATTING.matcher(text).replaceAll(""); }
    public static String white(String value) { return "\u00a7f" + value; }
    private static String numbers(String value) {
        return value.replaceAll("(-?\\d+(?:\\.\\d+)?)", "\u00a7f$1\u00a7r");
    }
    private static String label(String text) { int colon = text.indexOf(": "); return colon < 0 ? text : text.substring(0, colon); }
    private static String value(String text) { return text.substring(text.indexOf(": ") + 2); }
    private static String vanillaGroup(String group) { return group.startsWith("minecraft:") ? group.substring(10) : group; }
}
