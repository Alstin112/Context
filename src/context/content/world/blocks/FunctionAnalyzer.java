package context.content.world.blocks;

import arc.scene.ui.layout.Table;
import context.ui.dialogs.FunctionAnalyzerDialog;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

public class FunctionAnalyzer extends BaseContextBlock {
    public FunctionAnalyzer(String name) {
        super(name);
    }

    @SuppressWarnings("unused")
    public class FunctionAnalyserBuild extends BaseContextBuild {
        @Override
        public void buildConfiguration(Table table) {
            table.button(Icon.book, Styles.cleari, FunctionAnalyzerDialog::show).size(40f);
        }
    }
}
