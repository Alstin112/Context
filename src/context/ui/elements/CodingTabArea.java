package context.ui.elements;

import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Timer;
import context.ui.ColoredTextArea;
import context.ui.TabArea;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class CodingTabArea extends TabArea {
    private final ColoredTextArea codeArea = new ColoredTextArea("");
    private final Label lineLabel = new Label("") {
        @Override
        public float getPrefHeight() {
            return 0f;
        }
    };

    /**
     * Table that is behind the codeArea, used to set the backColor of the codeArea
     */
    private final Table behindTable = new Table();

    private TextButton buttonSync;
    private Fi synchronizedFile = null;
    private Cons<Fi> onSynchronize = fi -> {};
    private long lastFileTime = 0;
    private Timer.Task verifyFile = null;


    public CodingTabArea() {
        super();
        //line numbers
        Label.LabelStyle labelStyle = new Label.LabelStyle(Styles.outlineLabel);
        labelStyle.background = Styles.black8;

        lineLabel.setText(() -> {
            int firstLine = codeArea.getFirstLineShowing();
            StringBuilder sb = new StringBuilder();

            int cursorLine = codeArea.getCursorLine();

            for (int i = 0; i < codeArea.getLinesShowing(); i++) {
                if (i + firstLine == cursorLine) sb.append("[yellow]");

                sb.append(firstLine + i + 1).append("\n");

                if (i + firstLine == cursorLine) sb.append("[]");
            }
            return sb.toString();
        });
        lineLabel.setStyle(labelStyle);
        lineLabel.setAlignment(Align.topRight);

        // Code backColor
        behindTable.setBackground(Styles.grayPanel);

        // Code Area
        TextField.TextFieldStyle codeAreaStyle = new TextField.TextFieldStyle(Styles.defaultField);
        codeAreaStyle.background = null;
        codeArea.setStyle(codeAreaStyle);
        codeArea.changed(() -> setCode(codeArea.getText()));

        behindTable.add(codeArea).grow();
    }

    @Override
    public void generateBase(Table table) {
        super.generateBase(table);
        table.add(lineLabel).left().top().padRight(5);
        table.add(behindTable).grow();
    }

    @Override
    public Table addButtons(Table table) {

        buttonSync = new TextButton("@context.sync");
        buttonSync.clicked(() -> {
            if (synchronizedFile != null) {
                setSync();
                return;
            }
            Vars.platform.showFileChooser(false, "js", fi -> {
                String content = fi.readString();
                if (content.equals(this.getCode())) {
                    this.setSync(fi, false);
                }
                CodingTabArea cta = this;
                this.syncTypeAsk(true, new ExpImpListener() {
                    @Override
                    public void upload() {
                        cta.setSync(fi, true);
                    }

                    @Override
                    public void download() {
                        cta.setSync(fi, false);
                    }

                    @Override
                    public void delete() {
                        cta.setCode("");
                        cta.setSync(fi, true);
                    }
                });
            });
        });
        Button searchTerm = new TextButton("@context.search-term");
        searchTerm.clicked(() -> {
            BaseDialog bd = new BaseDialog("Search Function");
            bd.cont.add(SearchFunction.cont).grow();
            bd.cont.setBackground(Tex.button);
            bd.buttons.button("@exit", bd::hide).size(120f, 60f);
            bd.closeOnBack();
            bd.show();
        });
        table.add(buttonSync).size(120f, 60f).padRight(4f);
        table.add(searchTerm).size(120f, 60f).padRight(4f);
        return table;
    }

    public void setCode(String code) {
        this.codeArea.setText(code);
    }

    public void setSync() {
        this.synchronizedFile = null;
        this.buttonSync.setText("@context.sync");
        this.lastFileTime = 0;
        if (verifyFile != null) verifyFile.cancel();
        verifyFile = null;
    }

    public void setSync(Fi file, boolean replaceFile) {
        getOnSynchronize().get(file);
        this.synchronizedFile = file;
        if (file == null) {
            this.setSync();
            return;
        }
        this.buttonSync.setText("@context.unsync");
        readFromFile(replaceFile);
        verifyFile = Timer.schedule(() -> {
            if (synchronizedFile == null) return;
            if (synchronizedFile.lastModified() != lastFileTime) {
                readFromFile(false);
            }
        }, 1, 1);
    }

    public void syncTypeAsk(boolean resetOption, ExpImpListener cb) {
        BaseDialog d = new BaseDialog("@choose");
        d.cont.label(() -> "@context.code-ide.difference");
        d.closeOnBack();
        if (resetOption) {
            d.buttons.button("@context.delete", Icon.trash, () -> {
                cb.delete();
                d.hide();
            }).size(230f, 60f);
        }
        d.buttons.button("@context.upload", Icon.upload, () -> {
            cb.upload();
            d.hide();
        }).size(230f, 60f);
        d.buttons.button("@context.download", Icon.download, () -> {
            cb.download();
            d.hide();
        }).size(230f, 60f);
        d.buttons.button("@context.cancel", Icon.none, () -> {
            cb.cancel();
            d.hide();
        }).size(230f, 60f);
        d.show();
    }

    public void readFromFile(boolean replaceFile) {
        if (synchronizedFile == null) return;
        lastFileTime = synchronizedFile.lastModified();
        if (replaceFile) synchronizedFile.writeString(this.getCode());
        else this.setCode(synchronizedFile.readString());
        ide.blinkArea(Color.green, 1f);
    }

    @Override
    public int TotalExportedBytes() {
        return this.codeArea.getText().length() + 2;
    }

    @Override
    public void close() {
        super.close();
        if (verifyFile != null) verifyFile.cancel();
        verifyFile = null;
    }

    public String getCode() {
        return this.codeArea.getText();
    }

    public Cons<Fi> getOnSynchronize() {
        return onSynchronize;
    }

    public void setOnSynchronize(Cons<Fi> onSynchronize) {
        this.onSynchronize = onSynchronize;
    }

    public abstract static class ExpImpListener {
        public void upload() {}
        public void download() {}
        public void cancel() {}
        public void delete() {}
    }

}
