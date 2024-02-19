package context.content.world.blocks;

import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import context.content.TestersModes;
import context.ui.CodeIde;
import context.ui.dialogs.FileSyncTypeDialog;
import context.ui.elements.CodingTabArea;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.gen.Icon;
import mindustry.mod.Scripts;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.util.Objects;

import static mindustry.Vars.renderer;
import static mindustry.Vars.tilesize;

public class EffectTester extends CodableTester {
    public EffectTester(String name) {
        super(name);

        config(String.class, EffectTesterBuild::setCode);
    }

    public class EffectTesterBuild extends CodableTesterBuild {
        private String code = "";
        public Effect effect = new Effect(20, e -> {
        });
        private String errorMessage = "";
        private boolean compileError = false;
        private Fi synchronizedFile = null;

        @Override
        public void buildConfiguration(Table table) {
            table.button(Icon.pencil, Styles.cleari, () -> {
                CodeIde ide = new CodeIde();
                CodingTabArea tab = new CodingTabArea();
                ide.addTab(tab);
                ide.maxByteOutput = 65523; // (65535 = Max bytes size) - (11 = build properties) - (1 = build version)
                tab.setCode(getCode());

                ide.setOnSave(codeIde -> {
                    this.configure(tab.getCode());
                    if(synchronizedFile != null) lastFileModified = synchronizedFile.lastModified();
                    lastEditByPlayer = true;
                });
                tab.setOnSynchronize(file -> this.synchronizedFile = file);
                ide.hideTabs(true);


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
            table.button(Icon.settings, Styles.cleari, () -> {
                TextField duration = new TextField(effect.lifetime+"");
                TextField clipSize = new TextField(effect.clip+"");
                TextField idField = new TextField(effect.id+"");

                TextField.TextFieldValidator listener = txt -> {
                    try{
                        Float.parseFloat(txt);
                        return true;
                    } catch (NumberFormatException ignored) {
                        return false;
                    }
                };

                duration.setValidator(listener);
                clipSize.setValidator(listener);
                idField.setColor(Color.gray);
                idField.setDisabled(true);

                duration.setFilter((textField, c) -> textField.getText().length() < 10 && (c >= '0' && c <= '9' || c == '.'));
                clipSize.setFilter((textField, c) -> textField.getText().length() < 10 && (c >= '0' && c <= '9' || c == '.'));
                idField.setFilter((textField, c) -> false);

                BaseDialog d = new BaseDialog("@editmessage");
                d.setFillParent(false);
                d.cont.label(()->"@block.context-effect-tester.id");
                d.cont.add(idField);
                d.cont.row();
                d.cont.label(()->"@block.context-effect-tester.lifetime");
                d.cont.add(duration);
                d.cont.row();
                d.cont.label(()->"@block.context-effect-tester.clipsize");
                d.cont.add(clipSize);
                d.buttons.button("@ok", () -> {
                    try {
                        float lt = Float.parseFloat(duration.getText());
                        float cs = Float.parseFloat(clipSize.getText());
                        effect.lifetime = lt;
                        effect.clip = cs;
                    } catch (NumberFormatException e) {
                        Log.err(e);
                    }
                    d.hide();
                }).size(130f, 60f);
                d.closeOnBack();
                d.show();
            }).size(40f);
            table.button(Icon.play, Styles.cleari, () -> {
                try {
                    effect.at(this.x, this.y);
                } catch (NumberFormatException e) {
                    setErrorMessage(e.getMessage());
                }

            }).size(40f);
        }

        @Override
        public String config() {
            return getCode();
        }

        @Override
        public void drawSelect() {
            if (renderer.pixelator.enabled()) return;
            if (getErrorMessage().isEmpty()) return;

            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean usesIntegerPositions = font.usesIntegerPositions();
            font.getData().setScale(1 / 4f / Scl.scl(1f));
            font.setUseIntegerPositions(false);

            l.setText(font, getErrorMessage(), Color.scarlet, 90f, Align.left, true);
            float offset = 1f;

            Draw.color(0f, 0f, 0f, 0.2f);
            Fill.rect(x, y - tilesize / 2f - l.height / 2f - offset, l.width + offset * 2f, l.height + offset * 2f);
            Draw.color();
            font.setColor(Color.scarlet);
            font.draw(getErrorMessage(), x - l.width / 2f, y - tilesize / 2f - offset, 90f, Align.left, true);
            font.setUseIntegerPositions(usesIntegerPositions);

            font.getData().setScale(1f);

            Pools.free(l);
        }

        @Override
        public void updateMode() {
            if (getCode().isEmpty()) mode = TestersModes.EMPTY;
            else if (!getErrorMessage().isEmpty()) {
                if (compileError) mode = TestersModes.COMPILER_ERROR;
                else mode = TestersModes.RUNTIME_ERROR;
            } else mode = TestersModes.ACTIVE;
        }

        public void updateRunFn(String value) {
            if (value.trim().isEmpty()) return;

            Scripts scripts = Vars.mods.getScripts();
            try {
                String codeStr = "Effect.get(" + effect.id + ").renderer=function(e){try{" + value + "\nVars.world.build(" + tile.x + "," + tile.y + ").errorMessage=\"\"}catch(e){Vars.world.build(" + tile.x + "," + tile.y + ").errorMessage=e}}";
                scripts.context.evaluateString(scripts.scope, codeStr, "EffectTester", 1);

                setErrorMessage("");
                compileError = false;
            } catch (RuntimeException e) {
                setErrorMessage(e.getMessage());
                compileError = true;
            }
            updateMode();
        }

        private void setCode(String code) {
            if(!Objects.equals(code, this.code)) lastEditByPlayer = false;

            this.code = code;
            updateRunFn(code);
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.str(getCode());
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            setCode(read.str());
        }

        public String getCode() {
            return code;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

}
