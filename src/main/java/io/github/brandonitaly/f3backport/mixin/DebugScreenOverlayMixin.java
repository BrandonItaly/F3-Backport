package io.github.brandonitaly.f3backport.mixin;

import io.github.brandonitaly.f3backport.debug.DebugFrame;
import io.github.brandonitaly.f3backport.debug.DebugPanel;
import io.github.brandonitaly.f3backport.debug.EntryCollector;
import io.github.brandonitaly.f3backport.debug.PanelLayout;
import io.github.brandonitaly.f3backport.debug.PanelRenderer;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Unique private final DebugFrame f3backport$frame = new DebugFrame();
    @Unique private final PanelLayout f3backport$layout = new PanelLayout();
    @Unique private List<Identifier> f3backport$previousEntries = List.of();
    @Unique private String f3backport$entryId = "";

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void f3backport$beginFrame(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        f3backport$frame.clear();
        List<Identifier> enabled = List.copyOf(minecraft.debugEntries.getCurrentlyEnabled());
        if (!enabled.equals(f3backport$previousEntries)) f3backport$layout.clear();
        f3backport$previousEntries = enabled;
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/components/debug/DebugScreenEntries;getEntry(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/gui/components/debug/DebugScreenEntry;"))
    private DebugScreenEntry f3backport$identifyEntry(Identifier id) {
        f3backport$entryId = id.toString();
        return DebugScreenEntries.getEntry(id);
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/components/debug/DebugScreenEntry;display(Lnet/minecraft/client/gui/components/debug/DebugScreenDisplayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/chunk/LevelChunk;)V"))
    private void f3backport$collectEntry(DebugScreenEntry entry, DebugScreenDisplayer vanilla,
                                       Level level, LevelChunk clientChunk, LevelChunk serverChunk) {
        EntryCollector collector = new EntryCollector(vanilla);
        entry.display(collector, level, clientChunk, serverChunk);
        f3backport$frame.accept(f3backport$entryId, collector.output);
        if (f3backport$entryId.equals("minecraft:looking_at_entity") && !collector.output.isEmpty()) {
            Entity entity = minecraft.crosshairPickEntity;
            if (entity != null) {
                f3backport$frame.panel("looking_at_entity").facts.add(0, new DebugPanel.Fact("Coordinates",
                        DebugFrame.white(String.format(Locale.ROOT, "%.2f, %.2f, %.2f", entity.getX(), entity.getY(), entity.getZ()))));
            }
        }
    }

    @Inject(method = "extractLines", at = @At("HEAD"), cancellable = true)
    private void f3backport$renderPanels(GuiGraphicsExtractor graphics, List<String> lines, boolean alignLeft,
    //? if >=26.3 {
    /*int scaledScreenWidth,
    *///? }
    CallbackInfo ci) {
        if (alignLeft) {
            //? if <26.3 {
            int scaledScreenWidth = graphics.guiWidth();
            //? }
            int scaledScreenHeight = graphics.guiHeight();
            if (scaledScreenWidth != graphics.guiWidth()) {
                int scale = Math.max(1, minecraft.getWindow().getWidth() / scaledScreenWidth);
                scaledScreenHeight = minecraft.getWindow().getHeight() / scale;
            }
            f3backport$frame.help(lines);
            for (PanelLayout.Placement placement : f3backport$layout.arrange(f3backport$frame.panels(), scaledScreenHeight)) {
                PanelRenderer.render(graphics, minecraft.font, placement, scaledScreenWidth);
            }
        }
        ci.cancel();
    }
}
