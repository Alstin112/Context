package context.ui;

import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;

public abstract class TabArea {
    public Table parent;
    public Button tabButton;
    protected CodeIde ide;
    public TabArea() {

    }

    public void generateBase(Table table) {
        parent = table;
    };
    public abstract Table addButtons(Table table);
    public void removeBase() {
        if (parent != null) parent.reset();
    };
    public void close() {

    }

    public int TotalExportedBytes() {
        return 0;
    }

    public void setIde(CodeIde ide) {
        this.ide = ide;
    }
}
