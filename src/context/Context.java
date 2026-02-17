package context;

import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Log;
import context.content.world.blocks.*;
import context.ui.BetterIdeDialog;
import context.ui.dialogs.ReloadContents;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import mindustry.mod.Mods.*;
import mindustry.mod.Scripts;
import rhino.*;

import java.lang.reflect.Field;

import static mindustry.Vars.platform;

@SuppressWarnings("unused")
public class Context extends Mod {
    public static Log.LogHandler onLog;
    public static boolean logging = false;

    @Override
    public void loadContent() {
        Log.LogHandler log = Log.logger;
        if(!logging) {
            logging = true;
            Log.logger = (level, text) -> {
                if(onLog != null) onLog.log(level, text);
                log.log(level, text);
            };
        }

        Scripts scripts = Vars.mods.getScripts();
        scripts.scope.put("BetterIdeDialog", scripts.scope,  BetterIdeDialog.class);
        scripts.scope.put("ContextMod", scripts.scope, this);
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
     * The intention of this command for now is to use in console `ContextMod.reloadContents()`.
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

    /**
     * Loads the classes of a certain java mod onto the console, so that they are accessible like vanilla.
		 */
    public void loadToConsole(LoadedMod mod, String moduleName) {
        if (!mod.isJava()) {
            Log.err("The mod @(@) is not a java mod", mod.meta.displayName, mod.name);
            return;
        }

        Fi root = mod.root.child(moduleName);

        if (!root.exists()) {
            Log.err("Root of @(@) was not found", mod.meta.displayName, mod.name);
            return;
        }

        Seq<Fi> possiblePackages = new Seq<>();
        Seq<Fi> temp = Seq.with(root);

        while (!temp.isEmpty()) {
            Fi cur = temp.pop();
            possiblePackages.addUnique(cur);
            for (Fi file : cur.list()) {
                if (file.isDirectory()) {
                    temp.add(file);
                }
            }
        }

        possiblePackages.each(file -> {
            StringBuilder packageName = new StringBuilder(file.path().replaceAll("/", "."));
            packageName.deleteCharAt(packageName.length() - 1);

            Vars.mods.getScripts().runConsole("importPackage(ContextMod.getPackage(\"" + packageName + "\"))");
        });
    }
    public NativeJavaPackage getPackage(String name) {
        NativeJavaPackage loadingPackage = new NativeJavaPackage(name, Vars.mods.mainLoader());
        loadingPackage.setParentScope(Vars.mods.getScripts().scope);
        return loadingPackage;
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

    public BetterIdeDialog VSCodeWindow() {
        return new BetterIdeDialog();
    }
}
