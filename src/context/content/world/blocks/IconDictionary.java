package context.content.world.blocks;

import arc.Core;
import arc.graphics.g2d.Font;
import arc.math.geom.Vec2;
import arc.scene.ui.Button;
import arc.scene.ui.ImageButton;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

import java.util.HashSet;
import java.util.Scanner;

import static mindustry.Vars.headless;
import static mindustry.Vars.tilesize;

public class IconDictionary extends BaseContextBlock {
    private static CharSequence iconsChar;
    private static CharSequence iconsImg;
    private static CharSequence iconsMind;
    private static TextButton.TextButtonStyle tStyle;

    public IconDictionary(String name) {
        super(name);
    }

    private static void loadStyles() {
        tStyle = new TextButton.TextButtonStyle(Styles.cleart);
        tStyle.down = Styles.flatDown;
        tStyle.up = Styles.black6;
        tStyle.over = Styles.flatOver;
        tStyle.disabled = Styles.black8;
    }

    public static void reloadCharIcons() {
        if(headless) return;

        StringBuilder sbChar = new StringBuilder();
        StringBuilder sbImg = new StringBuilder();
        StringBuilder sbMind = new StringBuilder();
        HashSet<Integer> icons = new HashSet<>();


        try(Scanner scan = new Scanner(Core.files.internal("icons/icons.properties").read(512))){
            while(scan.hasNextLine()){
                String line = scan.nextLine();
                String[] split = line.split("=");
                String texture = split[1].split("\\|")[1];

                if(!Core.atlas.has(texture)) continue;

                String character = split[0];
                icons.add(Integer.parseInt(character));
            }
        }

        for (int i = 0xE000; i <= 0xF8FF; i++) {
            Font.Glyph g = Fonts.def.getData().getGlyph((char) i);
            if (g == null) continue;
            char c = (char) i;

            if (Iconc.all.indexOf(i) != -1 || c == '\uF018' || c == '\uF02D' || c == '\uF0EC' || c == '\uF113')
                sbImg.append(c);
            else if (icons.contains(i)) sbMind.append(c);
            else sbChar.append(c);
        }
        sbImg.append('⚠');


        iconsChar = sbChar.toString();
        iconsImg = sbImg.toString();
        iconsMind = sbMind.toString();
    }

    @SuppressWarnings("unused")
    public class IconTesterBuild extends BaseContextBuild {

        @Override
        public void buildConfiguration(Table table) {
            if(headless) return;

            if (iconsChar == null) reloadCharIcons();
            Table catButtons = new Table();

            if(tStyle == null) loadStyles();

            // Char icons button
            Button cButton = new TextButton("\ue242", tStyle);
            // Mindustry icons button
            Button mButton = new ImageButton(Fonts.getGlyph(Fonts.def, '\uf869'), Styles.cleari);
            // Image icons button
            Button iButton = new ImageButton(Icon.info, Styles.cleari);


            cButton.clicked(() -> {
                table.clearChildren();
                Table optionsTab = new Table();
                createOptionsTable(table, iconsChar);
                table.add(catButtons);
                cButton.setDisabled(true);
                mButton.setDisabled(false);
                iButton.setDisabled(false);
                table.pack();
            });
            mButton.clicked(() -> {
                table.clearChildren();
                Table optionsTab = new Table();
                createOptionsTable(table, iconsMind);
                table.add(catButtons);
                cButton.setDisabled(false);
                mButton.setDisabled(true);
                iButton.setDisabled(false);
                table.pack();
            });
            iButton.clicked(() -> {
                table.clearChildren();
                createOptionsTable(table, iconsImg);
                table.add(catButtons);
                cButton.setDisabled(false);
                mButton.setDisabled(false);
                iButton.setDisabled(true);
                table.pack();
            });

            catButtons.add(cButton).size(60f, 40f);
            catButtons.add(mButton).size(60f, 40f);
            catButtons.add(iButton).size(60f, 40f);
            table.add(catButtons);
        }

        public void createOptionsTable(Table table, CharSequence text) {
            Table optionsTab = new Table();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                optionsTab.button(Fonts.getGlyph(Fonts.def, text.charAt(i)), Styles.cleari, () -> selectedIcon(table, c)).size(40f);
                if (i % 4 == 3) optionsTab.row();
            }
            for (int i = 0; i < 3 - (3 + text.length()) % 4; i++) {
                optionsTab.add(new Table(Styles.black6)).size(40f);
            }
            ScrollPane pane = table.pane(optionsTab).size(180f, 160f).get();
            pane.setScrollingDisabledX(true);
            pane.setOverscroll(false, false);
            table.row();
        }

        /**
         * Creates a menu for copying a character
         * @param t the table to change
         * @param c the character to be copied
         */
        public void selectedIcon(Table t, char c) {
            t.clearChildren();
            Table table = t.table().get();
            table.setBackground(Styles.black6);
            table.label(() -> "@block.context-icon-dictionary.click-to-copy").colspan(2);
            table.row();
            Table copyCharTab = new Table();
            copyCharTab.button(Icon.fileText, Styles.defaulti, () -> Core.app.setClipboardText(c + "")).size(40f);

            String code = Iconc.codes.findKey(c);
            String codeSmall = code + "Small";
            if (code != null && Icon.icons.containsKey(code)) {
                copyCharTab.button(Icon.icons.get(code), Styles.defaulti, () -> Core.app.setClipboardText("Icon." + code)).size(40f);
                if (Icon.icons.containsKey(codeSmall)) {
                    copyCharTab.button(Icon.icons.get(codeSmall), Styles.defaulti, () -> Core.app.setClipboardText("Icon." + codeSmall)).size(40f);
                }
            }
            table.add(copyCharTab).growX();
        }

        @Override
        public void updateTableAlign(Table table) {
            // Probably my attempt to fix the offset of the table
            Vec2 pos = Core.input.mouseScreen(x, y + size * tilesize / 2f + 1);
            table.setPosition(pos.x, pos.y, Align.bottom);
        }
    }
}
