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
    public String code = "";

    private Label lineLabel;
    private TextArea codeArea;
    private Slider slider;


    private TextButton buttonSync;
    private Fi synchronizedFile = null;
    public Cons<Fi> onSynchronize = (Fi) -> {
    };
    public long lastFileTime = 0;
    public Timer.Task verifyFile = null;


    public CodingTabArea() {
        super();
    }

    private void updateLineHeight() {
        StringBuilder txt = new StringBuilder();
        int minimum = codeArea.getFirstLineShowing();
        int maximum = codeArea.getFirstLineShowing() + codeArea.getLinesShowing();
        for (int i = minimum; i < maximum - 1; i++) {
            String sendString = String.valueOf(i + 1);
            if (sendString.length() < String.valueOf(maximum).length()) txt.append(" ");
            txt.append(sendString).append("\n");
        }
        lineLabel.invalidate();
        lineLabel.setText(txt.toString());

        if (codeArea.getLines() > codeArea.getLinesShowing()) {
            slider.setRange(0, codeArea.getLines() - codeArea.getLinesShowing());
            slider.setValue(codeArea.getFirstLineShowing());
        }
    }

    @Override
    public void generateBase(Table table) {
        super.generateBase(table);

        //line numbers
        Label.LabelStyle labelStyle = new Label.LabelStyle(Styles.outlineLabel);
        labelStyle.background = Styles.black8;
        lineLabel = new Label("1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12\n13\n14\n15\n16\n17\n18\n19\n20\n21\n22\n23\n24\n25\n26\n27\n28\n29\n30\n31\n32\n33\n34\n35\n36\n37");
        lineLabel.setStyle(labelStyle);
        lineLabel.setAlignment(Align.topRight);

        // Code backColor
        Table behindTable = new Table();
        behindTable.setBackground(Styles.grayPanel);

        // Code Area
        TextField.TextFieldStyle codeAreaStyle = new TextField.TextFieldStyle(Styles.defaultField);
        codeAreaStyle.background = null;
        codeArea = new ColoredTextArea(code);
        codeArea.setStyle(codeAreaStyle);
        codeArea.setTextFieldListener((textField, c) -> {
            code = textField.getText();
            updateLineHeight();
        });

        // ScrollBar
        slider = new Slider(0, 1, 1f, false);
        slider.visible(() -> this.codeArea != null && this.codeArea.getLinesShowing() < this.codeArea.getLines());

        // Adding to Table
        table.add(lineLabel).left().top().padRight(5);
        table.add(behindTable).grow();
        behindTable.add(codeArea).grow();
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
                if(content.equals(this.code)) {
                    this.setSync(fi,false);
                }
                CodingTabArea cta = this;
                this.syncTypeAsk(true, new ExpImpListener(){
                    @Override
                    public void upload() { cta.setSync(fi, true);}

                    @Override
                    public void download() { cta.setSync(fi, false);}

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
            SearchFunction sf = new SearchFunction();
            BaseDialog bd = new BaseDialog("Search Function");
            bd.cont.add(sf.cont).grow();
            bd.cont.setBackground(Tex.button);
            bd.buttons.button("@exit", bd::hide).size(120f,60f);
            bd.closeOnBack();
            bd.show();
        });
        table.add(buttonSync).size(120f, 60f).padRight(4f);
        table.add(searchTerm).size(120f, 60f).padRight(4f);
        return table;
    }

    public void setCode(String code) {
        this.codeArea.setText(code);
        this.code = code;
    }

    public void setSync() {
        this.synchronizedFile = null;
        this.buttonSync.setText("@context.sync");
        this.lastFileTime = 0;
        if (verifyFile != null) verifyFile.cancel();
        verifyFile = null;
    }
    public void setSync(Fi file, boolean replaceFile) {
        onSynchronize.get(file);
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
        d.buttons.button("@context.delete", Icon.trash, () -> {
            cb.delete();
            d.hide();
        }).size(230f,60f);
        d.buttons.button("@context.upload", Icon.upload, () -> {
            cb.upload();
            d.hide();
        }).size(230f,60f);
        d.buttons.button("@context.download", Icon.download, () -> {
            cb.download();
            d.hide();
        }).size(230f,60f);
        d.buttons.button("@context.cancel", Icon.none, () -> {
            cb.cancel();
            d.hide();
        }).size(230f,60f);
        d.show();
    }

    public void readFromFile(boolean replaceFile) {
        if (synchronizedFile == null) return;
        lastFileTime = synchronizedFile.lastModified();
        if(replaceFile) synchronizedFile.writeString(this.code);
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

    public static class ExpImpListener {

        public ExpImpListener() {}

        public void upload(){}
        public void download(){}
        public void cancel(){}
        public void delete(){}
    }

}
