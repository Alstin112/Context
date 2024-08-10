package context.content.world.blocks;

import arc.Core;
import arc.graphics.g2d.Font;
import arc.math.geom.Vec2;
import arc.scene.ui.Button;
import arc.scene.ui.ImageButton;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

import java.util.HashSet;
import java.util.Scanner;

import static mindustry.Vars.tilesize;

public class IconDictionary extends BaseContextBlock {
    private static CharSequence iconsChar;
    private static CharSequence iconsImg;
    private static CharSequence iconsMind;

    public IconDictionary(String name) {
        super(name);
    }

    public static void reloadCharIcons() {
        StringBuilder sbChar = new StringBuilder();
        StringBuilder sbImg = new StringBuilder();
        StringBuilder sbMind = new StringBuilder();
        HashSet<Integer> icons = new HashSet<>();


        try(Scanner scan = new Scanner(Core.files.internal("icons/icons.properties").read(512))){
            while(scan.hasNextLine()){
                String line = scan.nextLine();
                String[] split = line.split("=");
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
        sbImg.append('\u26A0');


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

            // TODO - when the game start, the char icons don't appear, needs to press f8 to reload (idk why)
            // TODO - This categories isn't precise, but i don't feel that is a good way picking manually for each ascii character
            // TODO - When opened some char, the menu gets offset, and idk how fix.

            // Char icons button
            Button cButton = new ImageButton(Fonts.getGlyph(Fonts.def, '\ue242'), Styles.cleari);
            // Mindustry icons button
            Button mButton = new ImageButton(Fonts.getGlyph(Fonts.def, '\uf869'), Styles.cleari);
            // Image icons button
            Button iButton = new ImageButton(Icon.info, Styles.cleari);


            cButton.clicked(() -> {
                table.clearChildren();
                Table optionsTab = new Table();
                for (int i = 0; i < iconsChar.length(); i++) {
                    char c = iconsChar.charAt(i);
                    optionsTab.button(Fonts.getGlyph(Fonts.def, iconsChar.charAt(i)), Styles.cleari, () -> selectedIcon(table, c)).size(40f);
                    if (i % 4 == 3) optionsTab.row();
                }
                for (int i = 0; i < 3 - (3 + iconsChar.length()) % 4; i++) {
                    optionsTab.add(new Table(Styles.black6)).size(40f);
                }
                ScrollPane pane = table.pane(optionsTab).size(180f, 160f).get();
                pane.setScrollingDisabledX(true);
                pane.setOverscroll(false, false);
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
                    optionsTab.button(Fonts.getGlyph(Fonts.def, iconsMind.charAt(i)), Styles.cleari, () -> selectedIcon(table, c)).size(40f);
                    if (i % 4 == 3) optionsTab.row();
                }
                for (int i = 0; i < 3 - (3 + iconsMind.length()) % 4; i++) {
                    optionsTab.add(new Table(Styles.black6)).size(40f);
                }
                ScrollPane pane = table.pane(optionsTab).size(180f, 160f).get();
                pane.setScrollingDisabledX(true);
                pane.setOverscroll(false, false);
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
                    optionsTab.button(Fonts.getGlyph(Fonts.def, iconsImg.charAt(i)), Styles.cleari, () -> selectedIcon(table, c)).size(40f);
                    if (i % 4 == 3) optionsTab.row();
                }
                for (int i = 0; i < 3 - (3 + iconsImg.length()) % 4; i++) {
                    optionsTab.add(new Table(Styles.black6)).size(40f);
                }
                ScrollPane pane = table.pane(optionsTab).size(180f, 160f).get();
                pane.setScrollingDisabledX(true);
                pane.setOverscroll(false, false);
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
            if (code != null && Icon.icons.containsKey(code)) {
                copyCharTab.button(Icon.icons.get(code), Styles.defaulti, () -> Core.app.setClipboardText("Icon." + code)).size(40f);
                if (Icon.icons.containsKey(code + "Small")) {
                    copyCharTab.button(Icon.icons.get(code + "Small"), Styles.defaulti, () -> Core.app.setClipboardText("Icon." + code + "Small")).size(40f);
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
