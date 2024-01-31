package context;

import context.content.world.blocks.DrawTester;
import context.content.world.blocks.EffectTester;
import context.content.world.blocks.IconDictionary;
import context.content.world.blocks.JsTester;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.mod.Mod;
import mindustry.mod.ModClassLoader;
import mindustry.mod.Mods;

import static mindustry.Vars.platform;

public class Context extends Mod{

    public static DrawTester drawTester;
    public static JsTester jsTester;
    public static EffectTester effectTester;
    public static IconDictionary iconDictionary;


    public Context(){
    }

    @Override
    public void loadContent(){
        drawTester = new DrawTester("draw-tester");
        jsTester = new JsTester("js-tester");
        effectTester = new EffectTester("effect-tester");
        iconDictionary = new IconDictionary("icon-dictionary");
    }

    public void reloadContents(){
        //Vars.mods.locateMod("context").main.reloadContents()
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();
        Vars.content.loadColors();

        Vars.mods.eachEnabled(mod -> {
            if(!mod.isJava()) return;
            try {
                ClassLoader loader = Vars.platform.loadJar(mod.file, Vars.mods.mainLoader());
                Class<?> main = Class.forName(mod.meta.main, true, loader);
                Mod instance = (Mod) main.getDeclaredConstructor().newInstance();
                Vars.content.setCurrentMod(mod);
                instance.loadContent();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Vars.content.setCurrentMod(null);

        Vars.content.init();
        Vars.content.load();
    }

}
