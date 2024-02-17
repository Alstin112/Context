package context.ui.dialogs;

import arc.util.Log;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.mod.Mod;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.fragments.PlacementFragment;

import java.lang.reflect.Method;

public class ReloadContents {

    private static final BaseDialog dialog;

    static {
        dialog = new BaseDialog("Reload all blocks contents");
        dialog.cont.add("Are you sure you want to reload all blocks contents?\n" +
                "You can reload only the blocks in your category");
        dialog.buttons.button("@reload-category", () -> {
            reload();
            dialog.hide();
        }).size(210f);
        dialog.addCloseButton();
    }

    private  ReloadContents() {}

    public static void show() {
        dialog.show();
    }
    private static void reload() {
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

        // Refresh the catalog
        if(Vars.state.isPlaying()) {
            try {
                PlacementFragment bfrag = Vars.ui.hudfrag.blockfrag;
                Method rebuild = bfrag.getClass().getDeclaredMethod("rebuild");
                rebuild.setAccessible(true);
                rebuild.invoke(bfrag);
            } catch (Exception e) {
                Log.err("Failed to reload the categories, please reload your save.");
            }

            if(Vars.control.input.block != null) {
                String name = Vars.control.input.block.name;
                Vars.control.input.block = Vars.content.block(name);
            }
        }
    }
}
