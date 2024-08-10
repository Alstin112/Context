package context.ui;

import arc.scene.ui.layout.Table;

public abstract class TabArea {
    protected CodeIde ide;
    protected TabArea() {

    }

    public void generateBase(Table table) {
    }

    public abstract void addButtons(Table table);

    public void close() {

    }

    public int totalExportedBytes() {
        return 0;
    }

    public void setIde(CodeIde ide) {
        this.ide = ide;
    }
}
