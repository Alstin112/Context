package context;

import arc.util.Log;
import context.content.world.blocks.DrawTester;
import context.content.world.blocks.EffectTester;
import context.content.world.blocks.IconDictionary;
import context.content.world.blocks.JsTester;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.mod.Mod;
import mindustry.mod.Scripts;

@SuppressWarnings("unused")
public class Context extends Mod {

    @Override
    public void loadContent() {
        new DrawTester("draw-tester");
        new JsTester("js-tester");
        new EffectTester("effect-tester");
        new IconDictionary("icon-dictionary");
    }

    public void reloadContents() {
        //Vars.mods.locateMod("context").main.reloadContents()
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();
        Vars.content.loadColors();

        Vars.mods.eachEnabled(mod -> {
            if (!mod.isJava()) return;
            ClassLoader loader = null;
            try {
                loader = Vars.platform.loadJar(mod.file, Vars.mods.mainLoader());
                Class<?> main = Class.forName(mod.meta.main, true, loader);
                Mod instance = (Mod) main.getDeclaredConstructor().newInstance();
                Vars.content.setCurrentMod(mod);
                instance.loadContent();
            } catch (Exception e) {
                Log.err("Failed to load mod content: @", mod.meta.name);
            }
        });
        Vars.content.setCurrentMod(null);

        Vars.content.init();
        Vars.content.load();
    }

}
