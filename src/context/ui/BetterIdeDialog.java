package context.ui;

import arc.Core;
import arc.Graphics;
import arc.func.Prov;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.event.DragListener;
import arc.scene.event.InputEvent;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import arc.util.Align;
import context.Context;
import context.ui.dialogs.SearchFunction;
import context.ui.tabs.BasicTab;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.util.ArrayList;
import java.util.List;

public class BetterIdeDialog extends Dialog {

    public Table sidebar;
    public Table tabsTable;
    public Table mainArea;
    public Seq<BasicTab> tabs = new Seq<>();
    public Table tabArea;
    public Table footer;
    public Table header;
    public int selectedTab = 0;
    public Label footerLabel;

    public Table consoleTable;
    public Table resizeHandle;
    public Cell<?> consoleCell;

    public Table consoleContainer;

    public boolean isConsoleOpen = false;
    public float consoleHeight = 150f;
    public Label consoleLabel;

    public int MaxByteOutput = -1;
    public Runnable onSave;


    public String ConsoleDisplayText = "";
    public List<String> ConsoleText = new ArrayList<>();
    public boolean ConsoleDisplayReRender = false;

    // region
    public static BetterIdeDialogStyle style = new BetterIdeDialogStyle();
    // endregion

    public BetterIdeDialog() {
        super("");
        children.remove(this.titleTable);

        setFillParent(true);
        setResizable(false);
        setMovable(false);
        addCloseButton();
        closeOnBack();

        cont.margin(0);

        Table root = new Table();
        root.background(style.mainBackground);
        createSideBar(root);

        mainArea = new Table();

        header = new Table();
        tabsTable = new Table();
        tabsTable.background(style.mainBackground);
        tabsTable.left();

        header.add(tabsTable).growX().height(35);
        header.button(Icon.cancel, Styles.cleari, this::close).size(35);
        mainArea.add(header).growX().height(35).padTop(3).row();

        tabArea = new Table();
        tabArea.background(style.selectedTabBackground);
        mainArea.add(tabArea).grow().row();

        consoleContainer = new Table();
        mainArea.add(consoleContainer).growX().row();
        createConsole();

        footer = new Table();
        footer.background(style.selectedTabBackground);
        footerLabel = new Label("Context IDE - Console output");
        footer.left().add(footerLabel).color(Color.gray).padLeft(5);
        mainArea.add(footer).growX().height(25);

        Context.onLog = (level, text) -> {
            ConsoleDisplayReRender = true;
            ConsoleText.add(text);
            if (ConsoleText.size() > 200) {
                ConsoleText.remove(0);
            }
        };
        root.add(mainArea).grow();
        cont.add(root).grow();
    }

    public void selectTab(int index) {
        if (index < 0 || index >= tabs.size) return;
        selectedTab = index;
        tabArea.clear();
        tabArea.add(tabs.get(index).MainArea()).grow();

        // Atualiza o estilo dos botões das abas
        for (int i = 0; i < tabs.size; i++) {
            tabs.get(i).UpdateStyle(style, i == selectedTab);
        }
    }

    public int getTotalExportedBytes() {
        int total = 0;
        for (BasicTab tab : tabs) {
            total += tab.totalExportedBytes();
        }
        return total;
    }

    public void createTab(BasicTab tab) {
        int index = tabs.size;
        tabs.add(tab);
        Button tabButton = tab.TabButton();
        tabButton.clicked(() -> selectTab(index));
        tabsTable.add(tabButton).width(200).height(35).pad(3).get();
        if (selectedTab == index) selectTab(index);
        else tab.UpdateStyle(style, false);
    }

    public boolean trySave() {
        int totalBytes = getTotalExportedBytes();
        if (MaxByteOutput > 0 && totalBytes > MaxByteOutput) {
            showError(Core.bundle.format("context.error.byte-limit", totalBytes, MaxByteOutput));
            return false;
        }
        if (onSave != null) onSave.run();
        return true;
    }

    private void close() {
        if (trySave()) this.hide();
    }

    public void showError(String message) {
        BaseDialog d = new BaseDialog("@error");
        d.cont.add(message);
        d.closeOnBack();
        d.addCloseButton();
        d.show();
    }

    private void createSideBar(Table root) {
        sidebar = new Table();
        sidebar.background(style.toolBarBackground);

        Button btn = sidebar.button(Icon.infoCircle, Styles.clearNonei, () -> {
//            SearchFunction.setObjThis(null);
//            SearchFunction.setOnUpload(str -> {
//                codeArea.getScene().setKeyboardFocus(codeArea);
//                String text = codeArea.getText();
//                int cursor = codeArea.getCursorPosition();
//                if(cursor == 0) {
//                    codeArea.setText(str+text);
//                    codeArea.setCursorPosition(str.length());
//                    codeArea.setSelection(0, str.length());
//                    return;
//                }
//                char c = text.charAt(cursor-1);
//                for (int i = str.length()-1; i >= 0; i--) {
//                    if(c == str.charAt(i) && i < cursor && text.substring(cursor-i-1, cursor).equals(str.substring(0, i+1))) {
//                        int finalCursor = cursor - i - 1 + str.length();
//                        codeArea.setText(
//                                text.substring(0, cursor-i-1) +
//                                        str +
//                                        text.substring(cursor)
//                        );
//                        codeArea.setCursorPosition(finalCursor);
//                        codeArea.setSelection(cursor-i-1, finalCursor);
//                        return;
//                    }
//                }
//                codeArea.setText(text.substring(0, cursor) + str + text.substring(cursor));
//                codeArea.setCursorPosition(cursor + str.length());
//                codeArea.setSelection(cursor, cursor + str.length());
//            });
            SearchFunction.show();
        }).size(40).padBottom(5).padTop(5).get();
        btn.addListener(new Tooltip(t -> t.add("Search for functions")));

        sidebar.row();
        Button consoleBtn = sidebar.button(Icon.terminal, Styles.clearNonei, this::toggleConsole)
                .size(40).padBottom(5).get();
        consoleBtn.addListener(new Tooltip(t -> t.add("Toggle Console")));

        sidebar.row();
        sidebar.top();
        root.add(sidebar).width(50).growY();
    }

    private void createConsole() {
        resizeHandle = new Table();
        resizeHandle.background(Tex.whiteui);
        resizeHandle.setColor(Color.gray);
        resizeHandle.touchable = Touchable.enabled;

        resizeHandle.addListener(new DragListener() {

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float deltaY, int pointer) {
                consoleHeight += deltaY;
                consoleHeight = Mathf.clamp(consoleHeight, 50, Core.graphics.getHeight() * 0.8f);

                if (consoleCell != null) {
                    consoleCell.height(consoleHeight);
                    consoleContainer.invalidateHierarchy();
                    mainArea.layout();
                }
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
                Core.graphics.cursor(Graphics.Cursor.SystemCursor.verticalResize);
                resizeHandle.setColor(Color.white);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor) {
                Core.graphics.cursor(Graphics.Cursor.SystemCursor.arrow);
                resizeHandle.setColor(Color.gray);
            }
        });
        // Adiciona a barra ao layout, mas começa com altura 0 (escondida)

        // A área do console em si
        consoleTable = new Table();
//        consoleTable.background(Styles.black); // Fundo escuro igual abas

        consoleLabel = new Label(ConsoleDisplayText);
        consoleLabel.setAlignment(Align.topLeft);
        consoleLabel.setWrap(true);

        ScrollPane pane = new ScrollPane(consoleLabel) {
            @Override
            public void layout() {
                boolean runToFinal = isBottomEdge();
                super.layout();
                if (runToFinal) setScrollPercentY(1.0f);
            }
        };
        consoleLabel.setText(() -> ConsoleDisplayReRender ?
                this.ConsoleDisplayText = String.join("\n", ConsoleText) :
                this.ConsoleDisplayText
        );

        consoleTable.setBackground(Styles.black);
        consoleTable.add(pane).grow().top().left();
        consoleTable.top();
    }

    public void toggleConsole() {
        isConsoleOpen = !isConsoleOpen;

        if (isConsoleOpen) {
            consoleContainer.add(resizeHandle).growX().height(5).row();
            consoleCell = consoleContainer.add(consoleTable).growX().height(consoleHeight);
            consoleContainer.row();
        } else {
            consoleContainer.clearChildren();
        }

        mainArea.invalidateHierarchy();
    }

    public void setFooter(Prov<CharSequence> footerBuilder) {
        footerLabel.setText(footerBuilder);
    }

    public static class BetterIdeDialogStyle {
        public Drawable mainBackground;
        public Drawable selectedTabBackground;
        public Drawable toolBarBackground;

        public Drawable tabSelectedUp;
        public Drawable tabSelectedDown;
        public Drawable tabSelectedOver;
        public Color tabSelectedFontColor;
        public Color tabSelectedFileSyncIconColor;
        public Color tabSelectedFileUnsyncIconColor;

        public Drawable tabUnselectedUp;
        public Drawable tabUnselectedDown;
        public Drawable tabUnselectedOver;
        public Color tabUnselectedFontColor;
        public Color tabUnselectedFileSyncIconColor;
        public Color tabUnselectedFileUnsyncIconColor;

        public BetterIdeDialogStyle() {
            this.mainBackground = ((TextureRegionDrawable) Tex.whiteui).tint(Color.valueOf("181818"));
            this.selectedTabBackground = ((TextureRegionDrawable) Tex.whiteui).tint(Color.valueOf("1f1f1f"));
            this.toolBarBackground = ((TextureRegionDrawable) Tex.whiteui).tint(Color.valueOf("2a2a2a"));

            this.tabSelectedUp = selectedTabBackground;
            this.tabSelectedDown = selectedTabBackground;
            this.tabSelectedOver = selectedTabBackground;
            this.tabSelectedFontColor = Color.white;
            this.tabSelectedFileSyncIconColor = Color.green;
            this.tabSelectedFileUnsyncIconColor = Color.gray;

            this.tabUnselectedUp = mainBackground;
            this.tabUnselectedDown = selectedTabBackground;
            this.tabUnselectedOver = selectedTabBackground;
            this.tabUnselectedFontColor = Color.grays(0.62f);
            this.tabUnselectedFileSyncIconColor = Color.green;
            this.tabUnselectedFileUnsyncIconColor = Color.gray;
        }
    }
}