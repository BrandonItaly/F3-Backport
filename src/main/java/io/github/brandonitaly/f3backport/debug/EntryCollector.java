package io.github.brandonitaly.f3backport.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.resources.Identifier;

public final class EntryCollector implements DebugScreenDisplayer {
    private final DebugScreenDisplayer vanilla;
    public final List<DebugFrame.Line> output = new ArrayList<>();

    public EntryCollector(DebugScreenDisplayer vanilla) { this.vanilla = vanilla; }

    @Override
    public void addPriorityLine(String line) {
        vanilla.addPriorityLine(line);
        output.add(new DebugFrame.Line(null, line, true));
    }

    @Override
    public void addLine(String line) {
        vanilla.addLine(line);
        output.add(new DebugFrame.Line(null, line, false));
    }

    @Override
    public void addToGroup(Identifier group, Collection<String> lines) {
        vanilla.addToGroup(group, lines);
        for (String line : lines) output.add(new DebugFrame.Line(group.toString(), line, false));
    }

    @Override
    public void addToGroup(Identifier group, String line) {
        vanilla.addToGroup(group, line);
        output.add(new DebugFrame.Line(group.toString(), line, false));
    }
}
