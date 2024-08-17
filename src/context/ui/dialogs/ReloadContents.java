package context.ui.dialogs;

import arc.Core;
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
        dialog = new BaseDialog("@context.reload-contents.title");
        dialog.cont.add("@context.reload-contents.warning");
        dialog.buttons.button("@context.reload-contents.reload-category", () -> {
            reload();
            dialog.hide();
        }).size(210f, 64f);
        dialog.addCloseButton();
    }

    private  ReloadContents() {}

    public static void show() {
        dialog.show();
    }

    @SuppressWarnings("java:S3011")
    public static void reload() {
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();
        Vars.content.loadColors();

        Vars.mods.eachEnabled(mod -> {
            if (!mod.isJava()) return;
            ClassLoader loader;
            try {
                loader = Vars.platform.loadJar(mod.file, Vars.mods.mainLoader());
                Class<?> main = Class.forName(mod.meta.main, true, loader);
                Mod instance = (Mod) main.getDeclaredConstructor().newInstance();
                Vars.content.setCurrentMod(mod);
                instance.loadContent();
            } catch (Exception e) {
                Log.err(Core.bundle.format("context.reload-contents.error-load-content", mod.meta.name));
            }
        });
        Vars.content.setCurrentMod(null);

        Vars.content.init();
        Vars.content.load();

        // Refresh the catalog
        if(Vars.state.isPlaying()) {
            try {
                PlacementFragment bFrag = Vars.ui.hudfrag.blockfrag;
                Method rebuild = bFrag.getClass().getDeclaredMethod("rebuild");
                rebuild.setAccessible(true);
                rebuild.invoke(bFrag);
            } catch (Exception e) {
                Log.err(Core.bundle.get("context.reload-contents.error-reload-category"));
            }

            if(Vars.control.input.block != null) {
                String name = Vars.control.input.block.name;
                Vars.control.input.block = Vars.content.block(name);
            }
        }
    }
}
