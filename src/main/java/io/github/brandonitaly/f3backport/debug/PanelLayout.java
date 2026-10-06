package io.github.brandonitaly.f3backport.debug;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Remembers panel placement between frames so changing counters cannot swap columns. */
public final class PanelLayout {
    public record Placement(DebugPanel panel, DebugPanel.Side side, int top) {}
    private final List<String> previousLeft = new ArrayList<>();
    private final List<String> previousRight = new ArrayList<>();

    public void clear() { previousLeft.clear(); previousRight.clear(); }

    public List<Placement> arrange(Map<String, DebugPanel> panels, int screenHeight) {
        Map<String, DebugPanel> remaining = new LinkedHashMap<>(panels);
        List<Placement> placements = new ArrayList<>();
        int[] heights = {2, 2};
        add(remaining.remove("priority_left"), DebugPanel.Side.LEFT, heights, placements);
        add(remaining.remove("priority_right"), DebugPanel.Side.RIGHT, heights, placements);
        restore(previousLeft, DebugPanel.Side.LEFT, remaining, heights, placements, screenHeight);
        restore(previousRight, DebugPanel.Side.RIGHT, remaining, heights, placements, screenHeight);
        for (DebugPanel panel : List.copyOf(remaining.values())) {
            if (panel.preferredSide != null && heights[panel.preferredSide.ordinal()] < screenHeight) {
                add(panel, panel.preferredSide, heights, placements);
                remaining.remove(panel.id);
            }
        }
        for (DebugPanel panel : remaining.values()) {
            DebugPanel.Side side = heights[0] < heights[1] && heights[0] < screenHeight
                    ? DebugPanel.Side.LEFT : DebugPanel.Side.RIGHT;
            if (heights[side.ordinal()] < screenHeight) add(panel, side, heights, placements);
        }
        clear();
        for (Placement placement : placements) {
            if (!placement.panel.id.startsWith("priority_")) {
                (placement.side == DebugPanel.Side.LEFT ? previousLeft : previousRight).add(placement.panel.id);
            }
        }
        return placements;
    }

    private static void restore(List<String> previous, DebugPanel.Side side,
                                Map<String, DebugPanel> remaining, int[] heights,
                                List<Placement> placements, int screenHeight) {
        for (String id : previous) {
            if (heights[side.ordinal()] < screenHeight) add(remaining.remove(id), side, heights, placements);
        }
    }

    private static void add(DebugPanel panel, DebugPanel.Side side, int[] heights, List<Placement> placements) {
        if (panel == null || panel.isEmpty()) return;
        placements.add(new Placement(panel, side, heights[side.ordinal()]));
        heights[side.ordinal()] += panel.height() + 9;
    }
}
