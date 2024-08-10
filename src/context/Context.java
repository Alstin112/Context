package context;

import context.content.world.blocks.*;
import context.ui.dialogs.ReloadContents;
import mindustry.mod.Mod;

@SuppressWarnings("unused")
public class Context extends Mod {
    @Override
    public void loadContent() {
        new DrawTester("draw-tester");
        new JsTester("js-tester");
        new EffectTester("effect-tester");
        new IconDictionary("icon-dictionary");
        new FunctionAnalyzer("function-analyzer");
    }

    /**
     * WIP
     * Reloads the contents of the all the mods. This can crash your game, but to reload the content you will need restart the game anyway
     * soo it's not a big deal.
     * The intention of this command for now is to use in console `Vars.mods.getMod("context").main.reloadContents()`.
     * can be used inside the world, but the blocks will be needed to be replaced with new from your inventory.
     */
    public void reloadContents() {
        ReloadContents.show();
    }

}
