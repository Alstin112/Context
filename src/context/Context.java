package context;

import arc.util.Log;
import context.content.world.blocks.*;
import context.ui.dialogs.ReloadContents;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods;

import java.lang.reflect.Field;

import static mindustry.Vars.platform;

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
        if (Vars.state.isMenu()) {
            try {
                ReloadContents.reload();
            } catch (NoClassDefFoundError e) {
                if (forceLoadMod("context")) {
                    ReloadContents.reload();
                    Log.info("Mods reloaded!");
                } else {
                    Log.err("Error while reloading the mods");
                }

            }
        } else ReloadContents.show();
    }

    @SuppressWarnings("java:S3011")
    public boolean forceLoadMod(String modName) {
        Mods.LoadedMod mod = Vars.mods.getMod(modName);
        if (mod == null) return false;
        ClassLoader loader;
        try {
            loader = platform.loadJar(mod.file, Vars.mods.mainLoader());
            Class<?> main = Class.forName(mod.meta.main, true, loader);
            Vars.content.setCurrentMod(mod);
            Mod mainMod = (Mod) main.getDeclaredConstructor().newInstance();
            Vars.content.setCurrentMod(null);
            Field fieldMain = mod.getClass().getDeclaredField("main");
            fieldMain.setAccessible(true);
            fieldMain.set(mod, mainMod);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
