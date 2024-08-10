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
import java.util.List;

public class CodeIde {
    // Main Fields
    public final Seq<TabArea> tabs = new Seq<>();
    private BaseDialog dialog;
    private Table tabArea;
    private Table tabGroup;
    private Cons<CodeIde> onSave = ide -> {
    };
    private TabArea selectedTab = null;

    private boolean tabGroupHidden = false;

    // <editor-fold desc="Blinking Tab Area">
    private float blinkDuration = 0.5f;
    private long lastBlink = 0;
    private Color blinkColor = Color.white.cpy();

    public void blinkArea(Color color, float duration) {
        this.blinkColor = color;
        this.blinkDuration = duration;
        this.lastBlink = System.currentTimeMillis();
    }
    // </editor-fold>

    // <editor-fold desc="Custom Buttons">
    public final List<Button> customButtons = new ArrayList<>();

    public <T extends Button> T addButton(T button) {
        customButtons.add(button);
        generateButtons();
        return button;
    }
    // </editor-fold>

    // Max Byte Output
    public int maxByteOutput = -1;

    public CodeIde() {
        createChildren();
        generateBase();
    }

    private void createChildren() {
        dialog = new BaseDialog("@editmessage");
        dialog.setFillParent(true);
        dialog.cont.row();

        tabArea = new Table() {
            @Override
            public void draw() {
                if (System.currentTimeMillis() - lastBlink < blinkDuration * 1000) {
                    float t = (System.currentTimeMillis() - lastBlink) / (blinkDuration * 1000);
                    this.setBackground(((ScaledNinePatchDrawable) Tex.button).tint(Tmp.c1.set(blinkColor).lerp(Color.white, t)));
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
        if(!tabGroupHidden) dialog.cont.add(tabGroup).grow();

        // adding the first tab
        generateButtons();
    }

    public void show() {
        dialog.show();
    }

    public void hideTabs(boolean hide) {
        tabGroupHidden = hide;
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
        tabGroup.button("Tab " + tabs.size, () -> selectTab(tabs.size - 1)).growX().top().height(60f);
        if (tabs.size == 1) this.selectTab(0);
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


    public void showError(String message) {
        BaseDialog d = new BaseDialog("@error");
        d.cont.add(message);
        d.closeOnBack();
        d.addCloseButton();
        d.show();
    }

}
