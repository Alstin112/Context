package context.ui.elements;

import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.Button;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Timer;
import context.ui.AdvancedTextArea;
import context.ui.TabArea;
import context.ui.dialogs.FileSyncTypeDialog;
import context.ui.dialogs.SearchFunction;
import mindustry.Vars;
import mindustry.ui.Styles;

public class CodingTabArea extends TabArea {
    private final AdvancedTextArea codeArea = new AdvancedTextArea("");
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

    // region Search Terms
    private Object objThis = null;

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
        CodingTabArea tab = this;
        codeArea.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode) {
                if(keycode == KeyCode.f1) {
                    int start = codeArea.getSelectionStart();
                    int cursor = codeArea.getCursorPosition();
                    codeArea.setSelection(Math.min(start, cursor), Math.max(start, cursor));
                    String selectedText = codeArea.getSelection();
                    SearchFunction.setText(selectedText);
                    tab.showSearch();
                }
                return true;
            }
        });

        behindTable.add(codeArea).grow();
    }

    @Override
    public void generateBase(Table table) {
        super.generateBase(table);
        table.add(lineLabel).left().top().padRight(5);
        table.add(behindTable).grow();
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
                new FileSyncTypeDialog(true,true, type -> {
                    if(type == FileSyncTypeDialog.SyncType.CANCEL) return;
                    if(type == FileSyncTypeDialog.SyncType.DELETE) cta.setCode("");

                    cta.setSync(fi, type != FileSyncTypeDialog.SyncType.DOWNLOAD);
                });
            });
        });
    }

    @Override
    public void addButtons(Table table) {
        Button searchTerm = new TextButton("@context.search-function.button-name");
        searchTerm.clicked(this::showSearch);
        table.add(buttonSync).size(120f, 60f).padRight(4f);
        table.add(searchTerm).size(120f, 60f).padRight(4f);
    }

    private void showSearch() {
        SearchFunction.setObjThis(objThis);
        SearchFunction.setOnUpload(str -> {
            codeArea.getScene().setKeyboardFocus(codeArea);
            String text = codeArea.getText();
            int cursor = codeArea.getCursorPosition();
            if(cursor == 0) {
                codeArea.setText(str+text);
                codeArea.setCursorPosition(str.length());
                codeArea.setSelection(0, str.length());
                return;
            }
            char c = text.charAt(cursor-1);
            for (int i = str.length()-1; i >= 0; i--) {
                if(c == str.charAt(i) && i < cursor && text.substring(cursor-i-1, cursor).equals(str.substring(0, i+1))) {
                    int finalCursor = cursor - i - 1 + str.length();
                    codeArea.setText(
                            text.substring(0, cursor-i-1) +
                                    str +
                                    text.substring(cursor)
                    );
                    codeArea.setCursorPosition(finalCursor);
                    codeArea.setSelection(cursor-i-1, finalCursor);
                    return;
                }
            }
            codeArea.setText(text.substring(0, cursor) + str + text.substring(cursor));
            codeArea.setCursorPosition(cursor + str.length());
            codeArea.setSelection(cursor, cursor + str.length());
        });
        SearchFunction.show();
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

    public void readFromFile(boolean replaceFile) {
        if (synchronizedFile == null) return;
        lastFileTime = synchronizedFile.lastModified();
        if (replaceFile) synchronizedFile.writeString(this.getCode());
        else this.setCode(synchronizedFile.readString());
        ide.blinkArea(Color.green, 1f);
    }

    @Override
    public int totalExportedBytes() {
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

    public Object getObjThis() {
        return objThis;
    }
    public void setObjThis(Object objThis) {
        this.objThis = objThis;
    }
}
