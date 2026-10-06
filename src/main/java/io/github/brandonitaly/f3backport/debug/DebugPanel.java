package io.github.brandonitaly.f3backport.debug;

import java.util.ArrayList;
import java.util.List;

public final class DebugPanel {
    public record Fact(String name, String value) {}
    public enum Side { LEFT, RIGHT }

    public final String id;
    public final String title;
    public final Integer accent;
    public final Side preferredSide;
    public final List<Fact> facts = new ArrayList<>();
    public final List<String> lines = new ArrayList<>();

    public DebugPanel(String id, String title, Integer accent, Side preferredSide) {
        this.id = id;
        this.title = title;
        this.accent = accent;
        this.preferredSide = preferredSide;
    }

    public void fact(String name, String value) { facts.add(new Fact(name, value)); }
    public boolean isEmpty() { return facts.isEmpty() && lines.isEmpty(); }
    public int height() {
        return 9 * (facts.size() + lines.size() + (title.isEmpty() ? 0 : 1))
                + (!facts.isEmpty() && !lines.isEmpty() ? 2 : 0);
    }
}
