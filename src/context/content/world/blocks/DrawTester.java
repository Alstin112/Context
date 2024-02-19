package context.content.world.blocks;

import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import context.content.TestersModes;
import context.ui.CodeIde;
import context.ui.dialogs.FileSyncTypeDialog;
import context.ui.elements.CodingTabArea;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.mod.Scripts;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import rhino.Script;

import java.util.Objects;

import static mindustry.Vars.*;

public class DrawTester extends CodableTester {
    public DrawTester(String name) {
        super(name);

        config(Object[].class, (DrawTesterBuild b, Object[] config) -> {
            b.active = (boolean) config[0];
            b.setCode((String) config[1]);
        });
        config(String.class, DrawTesterBuild::setCode);
        config(Boolean.class, (DrawTesterBuild b, Boolean config) -> b.active = config);
    }

    public class DrawTesterBuild extends CodableTesterBuild {

        private String code = "";
        public boolean active = true;

        public Runnable drawFn = () -> {
        };
        public String errorMessage = "";
        public boolean compileError = false;
        private Fi synchronizedFile = null;

        @Override
        public void buildConfiguration(Table table) {
            table.button(Icon.pencil, Styles.cleari, () -> {
                CodeIde ide = new CodeIde();
                CodingTabArea tab = new CodingTabArea();

                ide.addTab(tab);
                ide.maxByteOutput = 65522; // (65535 = Max bytes size) - (12 = build properties) - (1 = build version)
                tab.setCode(code);
                ide.setOnSave(codeIde -> {
                    this.configure(tab.getCode());
                    if(synchronizedFile != null) lastFileModified = synchronizedFile.lastModified();
                    lastEditByPlayer = true;
                });
                tab.setOnSynchronize(file -> this.synchronizedFile = file);

                if (synchronizedFile == null) {
                    ide.show();
                    deselect();
                    return;
                }

                final boolean FileChanged = synchronizedFile.lastModified() != lastFileModified;
                final boolean CodeChanged = !lastEditByPlayer;

                if(FileChanged && CodeChanged) {
                    new FileSyncTypeDialog(false, true, type -> {
                        if(type == FileSyncTypeDialog.SyncType.CANCEL) return;
                        tab.setSync(synchronizedFile, type == FileSyncTypeDialog.SyncType.UPLOAD);
                        ide.show();
                        deselect();
                    });
                    return;
                }
                tab.setSync(synchronizedFile, CodeChanged);
                ide.show();
                deselect();
            }).size(40f);

            ImageButton btn;
            btn = new ImageButton(new ImageButton.ImageButtonStyle(Styles.cleari));
            btn.getStyle().imageUp = Icon.eyeSmall;
            btn.getStyle().imageChecked = Icon.eyeOffSmall;
            btn.resizeImage(Icon.eyeSmall.imageSize());
            btn.setChecked(!active);
            btn.clicked(() -> configure(!active));
            table.add(btn).size(40f);
        }

        @Override
        public Object[] config() {
            return new Object[]{active, code};
        }


        @Override
        public void drawSelect() {
            if (renderer.pixelator.enabled()) return;
            if (errorMessage.isEmpty()) return;

            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean usesIntegerPositions = font.usesIntegerPositions();
            font.getData().setScale(1 / 4f / Scl.scl(1f));
            font.setUseIntegerPositions(false);

            l.setText(font, errorMessage, Color.scarlet, 90f, Align.left, true);
            float offset = 1f;

            Draw.color(0f, 0f, 0f, 0.2f);
            Fill.rect(x, y - tilesize / 2f - l.height / 2f - offset, l.width + offset * 2f, l.height + offset * 2f);
            Draw.color();
            font.setColor(Color.scarlet);
            font.draw(errorMessage, x - l.width / 2f, y - tilesize / 2f - offset, 90f, Align.left, true);
            font.setUseIntegerPositions(usesIntegerPositions);

            font.getData().setScale(1f);

            Pools.free(l);
        }

        @Override
        public void draw() {
            super.draw();
            if (mode != TestersModes.ACTIVE && mode != TestersModes.RUNTIME_ERROR) return;

            try {
                drawFn.run();
                errorMessage = "";
            } catch (Throwable e) {
                errorMessage = e.getMessage();
            }
        }

        @Override
        public void updateMode() {
            if (!active) {
                mode = TestersModes.INACTIVE;
            } else if (code.isEmpty()) {
                mode = TestersModes.EMPTY;
            } else if (!errorMessage.isEmpty()) {
                if (compileError) {
                    mode = TestersModes.COMPILER_ERROR;
                } else {
                    mode = TestersModes.RUNTIME_ERROR;
                }
            } else mode = TestersModes.ACTIVE;
        }

        public void updateDrawFn(String value) {
            if (value.trim().isEmpty()) {
                return;
            }

            Scripts scripts = Vars.mods.getScripts();
            try {
                String code = "(function(){" + value + "\n}).apply(Vars.world.build(" + this.tile.x + "," + this.tile.y + "))";
                Script script = scripts.context.compileString(code, "drawTester", 1);

                if (script == null) drawFn = () -> {};
                else drawFn = () -> script.exec(scripts.context, scripts.scope);

                errorMessage = "";
                compileError = false;
            } catch (Throwable e) {
                errorMessage = e.getMessage();
                compileError = true;
            }
        }

        public void setCode(String code) {
            if(!Objects.equals(code, this.code)) lastEditByPlayer = false;

            this.code = code;
            updateDrawFn(code);
        }

        public String getCode(){
            return code;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.bool(active);
            write.str(code);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            active = read.bool();
            setCode(read.str());
        }
    }

}
