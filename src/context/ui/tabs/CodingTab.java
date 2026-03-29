package context.ui.tabs;

import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Timer;
import context.ui.AdvancedTextArea;
import context.ui.BetterIdeDialog;
import context.ui.dialogs.FileSyncTypeDialog;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

public class CodingTab extends BasicTab {

    public ImageButton fileSyncButton;
    public Table main = new Table();
    public Table lineNumbers = new Table();
    public AdvancedTextArea codingArea;
    public boolean syncs = true;

    private Fi synchronizedFile = null;
    private Cons<Fi> onSynchronize = fi -> {
    };
    private long lastFileTime = 0;
    private Timer.Task verifyFile = null;

    private String lineNumberCache = "";

    public CodingTab(String name) {
        this(name, true);
    }

    public CodingTab(String name, boolean syncs) {
        super(name);
        this.syncs = syncs;
        if (syncs) {
            tab.clearChildren();
            fileSyncButton = new ImageButton(Icon.fileTextFillSmall, new ImageButton.ImageButtonStyle(Styles.clearNonei));
            fileSyncButton.clicked(() -> {
                if (synchronizedFile != null) {
                    synchronizeFile();
                    return;
                }
                Vars.platform.showFileChooser(true, "js", fi -> {
                    if (!fi.exists()) {
                        try {
                            fi.writeString("");
                        } catch (Exception e) {
                            Log.err("Failed to create file: " + e.getMessage());
                            return;
                        }
                    }
                    String content = fi.readString();
                    if (this.codingArea.getText().isEmpty() || content.equals(this.codingArea.getText().replaceAll("\\r", ""))) {
                        this.synchronizeFile(fi, false);
                        return;
                    }
                    new FileSyncTypeDialog(true, true, type -> {
                        if (type == FileSyncTypeDialog.SyncType.CANCEL) return;
                        if (type == FileSyncTypeDialog.SyncType.DELETE) codingArea.setText("");

                        synchronizeFile(fi, type != FileSyncTypeDialog.SyncType.DOWNLOAD);

                    });
                });
            });
            fileSyncButton.addListener(new Tooltip(tab -> {
                tab.setBackground(Styles.black8);
                tab.add(new Label(() -> synchronizedFile == null ? "Synchronize file" : "Synchronized to: " + synchronizedFile.path()))
                  .get().setStyle(Styles.outlineLabel);
            }));
            tab.add(fileSyncButton).size(20).pad(20).left();
            tabLabel = tab.add(name).color(Color.lightGray).growX().get();
        }

        Table contentTable = new Table();

        codingArea = new AdvancedTextArea("");
        lineNumbers.add(new Label(() -> lineNumberCache))
                .style(Styles.monoLabel)
                .padTop(0)
                .top()
                .right();

        codingArea.onUpdateDisplay = (str) -> lineNumberCache = codingArea.lineNumbers();

        codingArea.setStyle(new TextField.TextFieldStyle() {{
            font = Fonts.monospace;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            selection = Tex.selection;
            background = Tex.clear;
            cursor = Tex.cursor;
            messageFont = Fonts.def;
            messageFontColor = Color.gray;
        }});
        contentTable.add(lineNumbers)
                .padTop(0)
                .width(50)
                .right().top();
        contentTable.add(codingArea)
                .padTop(5)
                .grow();

        ScrollPane scroll = new ScrollPane(contentTable);
        codingArea.parentScroll = scroll;

        scroll.setFlingTime(0.1f);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        scroll.setScrollingDisabled(false, false);
//        scroll.clearListeners();

        main.add(scroll).grow();
    }

    @Override
    public Element MainArea() {
        return main;
    }

    public void setText(String text) {
        codingArea.setText(text);
    }
    public String getText() {
        return codingArea.getText();
    }

    @Override
    public void UpdateStyle(BetterIdeDialog.BetterIdeDialogStyle style, boolean selected) {
        super.UpdateStyle(style, selected);
        if (!syncs) return;
        ImageButton.ImageButtonStyle defstyle = fileSyncButton.getStyle();
        if (synchronizedFile != null && verifyFile == null) {
            if (selected) {
                defstyle.imageUpColor = style.tabSelectedFileSyncIconColor;
            } else {
                defstyle.imageUpColor = style.tabUnselectedFileSyncIconColor;
            }
        } else {
            if (selected) {
                defstyle.imageUpColor = style.tabSelectedFileUnsyncIconColor;
            } else {
                defstyle.imageUpColor = style.tabUnselectedFileUnsyncIconColor;
            }
        }
        fileSyncButton.setStyle(defstyle);
    }

    @Override
    public int totalExportedBytes() {
        return codingArea.getText().length() + 2;
    }

    public void synchronizeFile() {
        this.synchronizedFile = null;
        this.lastFileTime = 0;
        if (verifyFile != null) verifyFile.cancel();
        verifyFile = null;
    }

    public void synchronizeFile(Fi file, boolean replaceFile) {
        onSynchronize.get(file);
        this.synchronizedFile = file;
        if (file == null) {
            this.synchronizeFile();
            return;
        }
        fileSyncIO(replaceFile);
        verifyFile = Timer.schedule(() -> {
            if (synchronizedFile == null) return;
            if (!synchronizedFile.exists()) {
                synchronizeFile();
                return;
            }
            if (synchronizedFile.lastModified() != lastFileTime) {
                fileSyncIO(false);
            }
        }, 1, 1);
    }

    public void setOnSynchronize(Cons<Fi> onSynchronize) {
        this.onSynchronize = onSynchronize;
    }

    public void fileSyncIO(boolean write) {
        if (synchronizedFile == null) return;
        lastFileTime = synchronizedFile.lastModified();
        if (write) synchronizedFile.writeString(codingArea.getText());
        else codingArea.setText(synchronizedFile.readString());
//        ide.blinkArea(Color.green, 1f);
    }

}
