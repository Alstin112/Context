package context.content.world.blocks;

import arc.Core;
import arc.graphics.g2d.Font;
import arc.math.geom.Vec2;
import arc.scene.ui.Button;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

import static mindustry.Vars.tilesize;

public class IconDictionary extends BaseContextBlock {
    private static CharSequence iconsChar;
    private static CharSequence iconsImg;
    private static CharSequence iconsMind;

    public IconDictionary(String name) {
        super(name);
    }

    @Override
    public void load() {
        super.load();
    }

    public static void reloadCharIcons() {
        StringBuilder sbChar = new StringBuilder();
        StringBuilder sbImg = new StringBuilder();
        StringBuilder sbMind = new StringBuilder();
        for (int i = 0xE000; i <= 0xF8FF; i++) {
            Font.Glyph g = Fonts.def.getData().getGlyph((char) i);
            if (g == null) continue;

            if (Iconc.all.indexOf(i) != -1) sbImg.append((char) i);
            else if (i > 0xF000) sbMind.append((char) i);
            else sbChar.append((char) i);
        }
        iconsChar = sbChar.toString();
        iconsImg = sbImg.toString();
        iconsMind = sbMind.toString();
    }

    @SuppressWarnings("unused")
    public class IconTesterBuild extends BaseContextBuild {

        @Override
        public void buildConfiguration(Table table) {
            if (iconsChar == null) reloadCharIcons();
            Table catButtons = new Table();

            Button cButton = new ImageButton(Fonts.getGlyph(Fonts.def, '\ue242'), Styles.cleari);
            Button mButton = new ImageButton(Fonts.getGlyph(Fonts.def, '\uf869'), Styles.cleari);
            Button iButton = new ImageButton(Icon.info, Styles.cleari);

            cButton.clicked(() -> {
                table.clearChildren();
                Table optionsTab = new Table();
                for (int i = 0; i < iconsChar.length(); i++) {
                    char c = iconsChar.charAt(i);
                    optionsTab.button(Fonts.getGlyph(Fonts.def, iconsChar.charAt(i)), Styles.cleari, () -> SelectedIcon(table, c)).size(40f);
                    if (i % 4 == 3) optionsTab.row();
                }
                for (int i = 0; i < 3 - (3 + iconsChar.length()) % 4; i++) {
                    optionsTab.add(new Table(Styles.black6)).size(40f);
                }
                table.pane(optionsTab).size(180f, 160f);
                table.row();
                table.add(catButtons);
                cButton.setDisabled(true);
                mButton.setDisabled(false);
                iButton.setDisabled(false);
            });
            mButton.clicked(() -> {
                table.clearChildren();
                Table optionsTab = new Table();
                for (int i = 0; i < iconsMind.length(); i++) {
                    char c = iconsMind.charAt(i);
                    optionsTab.button(Fonts.getGlyph(Fonts.def, iconsMind.charAt(i)), Styles.cleari, () -> SelectedIcon(table, c)).size(40f);
                    if (i % 4 == 3) optionsTab.row();
                }
                for (int i = 0; i < 3 - (3 + iconsMind.length()) % 4; i++) {
                    optionsTab.add(new Table(Styles.black6)).size(40f);
                }
                table.pane(optionsTab).size(180f, 160f);
                table.row();
                table.add(catButtons);
                cButton.setDisabled(false);
                mButton.setDisabled(true);
                iButton.setDisabled(false);
            });
            iButton.clicked(() -> {
                table.clearChildren();
                Table optionsTab = new Table();
                for (int i = 0; i < iconsImg.length(); i++) {
                    char c = iconsImg.charAt(i);
                    optionsTab.button(Fonts.getGlyph(Fonts.def, iconsImg.charAt(i)), Styles.cleari, () -> SelectedIcon(table, c)).size(40f);
                    if (i % 4 == 3) optionsTab.row();
                }
                for (int i = 0; i < 3 - (3 + iconsImg.length()) % 4; i++) {
                    optionsTab.add(new Table(Styles.black6)).size(40f);
                }
                table.pane(optionsTab).size(180f, 160f);
                table.row();
                table.add(catButtons);
                cButton.setDisabled(false);
                mButton.setDisabled(false);
                iButton.setDisabled(true);
            });

            catButtons.add(cButton).size(60f, 40f);
            catButtons.add(mButton).size(60f, 40f);
            catButtons.add(iButton).size(60f, 40f);
            table.add(catButtons);
        }

        public void SelectedIcon(Table t, char c) {
           // table.setBackground(Styles.black6);
            t.clearChildren();
            Table table = t.table().get();
            table.setBackground(Styles.black6);
            table.label(() -> "@context.icon-dictionary.click-to-copy").colspan(2);
            table.row();
            Table copyCharTab = new Table();
            copyCharTab.button(Icon.fileText, Styles.defaulti, () -> {
                //.showInfoFade("@context.icon-dictionary.copied-to-clipboard");
                Core.app.setClipboardText(c + "");
            }).size(40f);

            String code = Iconc.codes.findKey(c);
            if (code != null && Icon.icons.containsKey(code)) {
                copyCharTab.button(Icon.icons.get(code), Styles.defaulti, () -> {
                    //.showInfoFade("@context.icon-dictionary.copied-to-clipboard");
                    Core.app.setClipboardText("Icon."+code);
                }).size(40f);
                if(Icon.icons.containsKey(code+"Small")) {
                    copyCharTab.button(Icon.icons.get(code+"Small"), Styles.defaulti, () -> {
                        //.showInfoFade("@context.icon-dictionary.copied-to-clipboard");
                        Core.app.setClipboardText("Icon."+code+"Small");
                    }).size(40f);
                }
            }
            table.add(copyCharTab).growX();
        }

        @Override
        public void updateTableAlign(Table table) {
//            Log.info("deu update");
            Vec2 pos = Core.input.mouseScreen(x, y + size * tilesize / 2f + 1);
            table.setPosition(pos.x, pos.y, Align.bottom);
        }
    }
}
