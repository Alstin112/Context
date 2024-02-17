package context.ui.dialogs;

import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.game.EventType;
import mindustry.mod.Mod;
import mindustry.ui.dialogs.BaseDialog;

public class ReloadContents {

    private static BaseDialog dialog;

    static {
        dialog = new BaseDialog("Reload all blocks contents");
        dialog.cont.add("Are you sure you want to reload all blocks contents?");
        dialog.cont.row();
        dialog.cont.add("You can reload only the blocks in your category");
        dialog.buttons.button("@reload-category", () -> {
            reload();
            dialog.hide();
        });
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
        Events.fire(new EventType.UnlockEvent(Vars.content.block("context-icon-dictionary")));
    }
}
