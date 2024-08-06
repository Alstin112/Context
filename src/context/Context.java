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

    public void reloadContents() {
        ReloadContents.show();
    }

}
