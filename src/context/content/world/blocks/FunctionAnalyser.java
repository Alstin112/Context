package context.content.world.blocks;

import arc.scene.ui.layout.Table;
import context.ui.dialogs.FunctionAnalyserDialog;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

public class FunctionAnalyser extends BaseContextBlock {
    public FunctionAnalyser(String name) {
        super(name);
    }

    @SuppressWarnings("unused")
    public class FunctionAnalyserBuild extends BaseContextBuild {
        @Override
        public void buildConfiguration(Table table) {
            table.button(Icon.book, Styles.cleari, FunctionAnalyserDialog::show).size(40f);
        }
    }
}
