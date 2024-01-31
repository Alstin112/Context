package context.content.world.blocks;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import context.content.TestersModes;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.mod.Scripts;
import mindustry.ui.Fonts;
import mindustry.ui.Menus;
import mindustry.ui.Styles;
import rhino.Script;

import static mindustry.Vars.renderer;
import static mindustry.Vars.tilesize;

@SuppressWarnings("unused")
public class DialogTester extends CodableTester {
    public DialogTester(String name) {
        super(name);

        config(String.class, (DialogTesterBuild b, String value) -> b.code = value);
    }
    public class DialogTesterBuild extends CodableTesterBuild {
        public String code = "";
        public Runnable RunFn = () -> {};
        public String errorMessage = "";
        public boolean compileError = false;

        @Override
        public void buildConfiguration(Table table) {
            table.button(Icon.pencil, Styles.cleari, () -> Menus.infoMessage("Not implemented yet :P")).size(40f);

            table.button(Icon.play, Styles.cleari, () -> {
                try{
                    this.RunFn.run();
                } catch (Throwable e) {
                    errorMessage = e.getMessage();
                }
            }).size(40f);
        }

        @Override
        public void drawSelect() {
            if (renderer.pixelator.enabled()) return;
            if (errorMessage.isEmpty()) return;

            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.getData().setScale(1 / 4f / Scl.scl(1f));
            font.setUseIntegerPositions(false);
//            this.getClass().getMethods()


            l.setText(font, errorMessage, Color.scarlet, 90f, Align.left, true);
            float offset = 1f;

            Draw.color(0f, 0f, 0f, 0.2f);
            Fill.rect(x, y - tilesize / 2f - l.height / 2f - offset, l.width + offset * 2f, l.height + offset * 2f);
            Draw.color();
            font.setColor(Color.scarlet);
            font.draw(errorMessage, x - l.width / 2f, y - tilesize / 2f - offset, 90f, Align.left, true);
            font.setUseIntegerPositions(ints);

            font.getData().setScale(1f);

            Pools.free(l);
        }

        public void updateMode() {
            if (code.isEmpty()) mode = TestersModes.EMPTY;
            else if (!errorMessage.isEmpty()) {
                if (compileError) mode = TestersModes.COMPILER_ERROR;
                else mode = TestersModes.RUNTIME_ERROR;
            }
            else mode = TestersModes.ACTIVE;
        }

        public void updateRunFn(String value) {
            if (value.trim().isEmpty()) return;

            Scripts scripts = Vars.mods.getScripts();
            try {
                String code = "(function(){" + value + " \n})()";
                Script script = scripts.context.compileString(code, "drawTester", 1);

                if (script == null) RunFn = () -> {};
                else RunFn = () -> script.exec(scripts.context, scripts.scope);

                errorMessage = "";
                compileError = false;
            } catch (Throwable e) {
                errorMessage = e.getMessage();
                compileError = true;
            }
        }

        @Override
        public String config() {
            return code;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.str(code);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            this.code = read.str();
        }
    }

}
