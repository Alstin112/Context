package context;

import context.content.world.blocks.DrawTester;
import context.content.world.blocks.EffectTester;
import context.content.world.blocks.IconDictionary;
import context.content.world.blocks.JsTester;
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
    }

    public void reloadContents() {
        ReloadContents.show();
    }

}
