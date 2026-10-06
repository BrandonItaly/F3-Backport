package io.github.brandonitaly.f3backport.debug;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class PanelRenderer {
    private PanelRenderer() {}

    public static void render(GuiGraphicsExtractor graphics, Font font, PanelLayout.Placement placement, int screenWidth) {
        DebugPanel panel = placement.panel();
        int nameWidth = panel.facts.stream().mapToInt(f -> font.width(f.name())).max().orElse(0);
        int width = Math.max(panel.title.isEmpty() ? 0 : font.width(panel.title) + 3,
                panel.lines.stream().mapToInt(font::width).max().orElse(0));
        for (DebugPanel.Fact fact : panel.facts) width = Math.max(width, nameWidth + 5 + font.width(fact.value()));
        boolean leftSide = placement.side() == DebugPanel.Side.LEFT;
        int left = leftSide ? 3 : screenWidth - 3 - width;
        int top = placement.top();
        int y = top;
        if (!panel.title.isEmpty()) {
            graphics.fill(left - 1, y - 1, left + width + 1, y + 8, 0x90303030);
            graphics.text(font, panel.title, left + 3, y, 0xffffffff, false);
            y += 9;
        }
        graphics.fill(left - 1, y - 1, left + width + 1, top + panel.height() + 1, 0x90505050);
        for (DebugPanel.Fact fact : panel.facts) {
            graphics.text(font, fact.name() + ":", left + nameWidth - font.width(fact.name()), y, 0xffe0e0e0, false);
            graphics.text(font, fact.value(), left + nameWidth + 5, y, 0xffd0d0d0, false);
            y += 9;
        }
        if (!panel.facts.isEmpty() && !panel.lines.isEmpty()) y += 2;
        for (String line : panel.lines) {
            graphics.text(font, line, left, y, 0xffe0e0e0, false);
            y += 9;
        }
        if (panel.accent != null) {
            int x = leftSide ? 0 : screenWidth - 1;
            graphics.fill(x, top - 1, x + 1, top + panel.height() + 1, 0xff000000 | panel.accent);
        }
    }
}
