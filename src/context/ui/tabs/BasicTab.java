package context.ui.tabs;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.Button;
import arc.scene.ui.Label;
import context.ui.BetterIdeDialog;

public class BasicTab {
    public String name;
    public Button tab;
    public Label tabLabel;

    public BasicTab(String name) {
        this.name = name;
        tab = new Button(new Button.ButtonStyle());
        tab.clearChildren();
        tabLabel = tab.add(name).color(Color.lightGray).get();
    }

    public Element MainArea() {
        return new Element();
    }

    public Button TabButton() {
        return tab;
    }

    public int totalExportedBytes() {
        return 0;
    }

    public void UpdateStyle(BetterIdeDialog.BetterIdeDialogStyle style, boolean selected) {
        Button.ButtonStyle defStyle = tab.getStyle();
        Label.LabelStyle labelStyle = tabLabel.getStyle();
        if (selected) {
            defStyle.up = style.tabSelectedUp;
            defStyle.down = style.tabSelectedDown;
            defStyle.over = style.tabSelectedOver;
            labelStyle.fontColor = style.tabSelectedFontColor;
        } else {
            defStyle.up = style.tabUnselectedUp;
            defStyle.down = style.tabUnselectedDown;
            defStyle.over = style.tabUnselectedOver;
            labelStyle.fontColor = style.tabUnselectedFontColor;
        }
        tab.setStyle(defStyle);
        tabLabel.setStyle(labelStyle);
    }
}
