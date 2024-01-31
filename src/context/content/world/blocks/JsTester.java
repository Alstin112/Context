package context.content.world.blocks;

import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import context.content.TestersModes;
import context.ui.CodeIde;
import context.ui.elements.CodingTabArea;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.mod.Scripts;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import rhino.Script;

import java.io.BufferedWriter;
import java.io.Writer;

import static mindustry.Vars.renderer;
import static mindustry.Vars.tilesize;

public class JsTester extends CodableTester {
    public JsTester(String name) {
        super(name);

        config(String.class, JsTesterBuild::setCode);
    }

    public class JsTesterBuild extends CodableTesterBuild {

        public String code = "";
        public Runnable RunFn = () -> {
        };
        public String errorMessage = "";
        public boolean compileError = false;
        public Fi synchronizedFile = null;

        @Override
        public void buildConfiguration(Table table) {

            table.button(Icon.pencil, Styles.cleari, () -> {
                CodeIde ide = new CodeIde();

                CodingTabArea tab = new CodingTabArea();
                ide.addTab(tab);
                ide.hideTabs(true);
                ide.maxByteOutput = 65523; // (65535 = Max bytes size) - (11 = build properties) - (1 = build version)

                tab.setCode(code);

                ide.addButton(new TextButton("@context.hide-and-run")).clicked(() -> {
                    if (ide.trySave()) {
                        ide.close();
                        this.run();
                    };
                });
                ide.addButton(new TextButton("@context.only-run")).clicked(() -> {
                    if (ide.trySave()) this.run();
                });

                if (synchronizedFile != null) tab.setSync(synchronizedFile, true);

                ide.setOnSave(codeIde -> {
                    this.configure(tab.code);
                });
                tab.onSynchronize = (file) -> {
                    this.synchronizedFile = file;
                };

                ide.show();
                deselect();
            }).size(40f);

            table.button(Icon.play, Styles.cleari, this::run).size(40f);
        }

        public void run() {
            try {
                this.RunFn.run();
            } catch (Throwable e) {
                errorMessage = e.getMessage();
            }
        }

        @Override
        public void configure(Object value) {
            if (value instanceof String) {
                setCode((String) value);
            } else {
                Log.err("Reporte esse BUG");
            }
            super.configure(value);
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
            } else mode = TestersModes.ACTIVE;
        }

        public void updateRunFn(String value) {
            if (value.trim().isEmpty()) return;

            Scripts scripts = Vars.mods.getScripts();
            try {
                String code = "(function(){" + value + " \n})()";
                Script script = scripts.context.compileString(code, "JsTester", 1);

                if (script == null) RunFn = () -> {
                };
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

        private void setCode(String code) {
            this.code = code;
            updateRunFn(code);
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.str(code);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            setCode(read.str());
        }
    }

}
