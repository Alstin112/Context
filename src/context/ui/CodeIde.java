package context.ui;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.scene.style.ScaledNinePatchDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Tmp;
import mindustry.gen.Tex;
import mindustry.ui.dialogs.BaseDialog;

import java.util.ArrayList;

public class CodeIde {
    // Main Fields
    public Seq<TabArea> tabs = new Seq<>();
    public BaseDialog dialog;
    private Table tabArea;
    private Table tabGroup;
    private Cons<CodeIde> onSave = ide -> {
    };
    private TabArea selectedTab = null;

    private boolean tabGroupHidded = false;

    // Blinking Tab Area
    public float blinkDuration = 0.5f;
    public long lastBlink = 0;
    public Color BlinkColor = Color.white.cpy();

    // Custom Buttons
    public ArrayList<Button> customButtons = new ArrayList<>();

    // Max Byte Output
    public int maxByteOutput = -1;

    public CodeIde() {
        createChilds();
        generateBase();
    }

    private void createChilds() {
        dialog = new BaseDialog("@editmessage");
        dialog.setFillParent(true);
        dialog.cont.row();

        tabArea = new Table() {
            @Override
            public void draw() {
                if (System.currentTimeMillis() - lastBlink < blinkDuration * 1000) {
                    float t = (System.currentTimeMillis() - lastBlink) / (blinkDuration * 1000);
                    this.setBackground(((ScaledNinePatchDrawable) Tex.button).tint(Tmp.c1.set(BlinkColor).lerp(Color.white, t)));
                }
                super.draw();
            }
        };
        tabArea.background(Tex.button);

        tabGroup = new Table().top();
    }

    public void generateBase() {
        dialog.cont.clear();

        dialog.cont.add(tabArea).grow().colspan(6);
        if(!tabGroupHidded) dialog.cont.add(tabGroup).grow();

        // adding the first tab
        generateButtons();
    }

    public void show() {
        dialog.show();
    }

    public void hideTabs(boolean hide) {
        tabGroupHidded = hide;
        generateBase();
    }

    private void generateButtons() {

        dialog.buttons.clear();
        if (selectedTab != null) selectedTab.addButtons(dialog.buttons);
        dialog.buttons.button("@ok", () -> {
            if(trySave()) this.close();
        }).size(130f, 60f);
        for (Button button : customButtons) {
            dialog.buttons.add(button).size(130f, 60f);
        }
    }

    public void addTab(TabArea tab) {
        tab.setIde(this);
        tabs.add(tab);
        tabGroup.button("Tab " + tabs.size, () -> {
            selectTab(tabs.size - 1);
        }).growX().top().height(60f);
        if (tabs.size == 1) this.selectTab(0);
    }

    public <T extends Button> T addButton(T button) {
        customButtons.add(button);
        generateButtons();
        return button;
    }

    public void selectTab(int index) {
        if (index < 0 || index >= tabs.size) return;
        selectedTab = tabs.get(index);
        if (selectedTab == null) return;
        tabArea.clear();
        selectedTab.generateBase(tabArea);
        generateButtons();
    }

    public void close() {
        tabs.each(TabArea::close);
        if (this.dialog != null) dialog.hide();
    }

    public void setOnSave(Cons<CodeIde> onSave) {
        this.onSave = onSave;
    }

    public boolean trySave() {
        if(maxByteOutput >= 0) {
            int bytes = 0;
            for (TabArea tab : tabs) {
                bytes += tab.totalExportedBytes();
            }
            if(bytes > maxByteOutput) {
                showError(Core.bundle.format("context.error.byte-limit", bytes, maxByteOutput));
                return false;
            }
        }
        onSave.get(this);
        return true;
    }

    public void blinkArea(Color color, float duration) {
        this.BlinkColor = color;
        this.blinkDuration = duration;
        this.lastBlink = System.currentTimeMillis();
    }

    public void showError(String message) {
        BaseDialog d = new BaseDialog("@error");
        d.cont.add(message);
        d.closeOnBack();
        d.addCloseButton();
        d.show();
    }

}
