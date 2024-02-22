package context.content.world.blocks;

import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import context.ui.CodeIde;
import context.ui.dialogs.FileSyncTypeDialog;
import context.ui.elements.CodingTabArea;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.gen.Icon;
import mindustry.mod.Scripts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import rhino.Function;

import java.util.Objects;

public class EffectTester extends CodableTester {
    public EffectTester(String name) {
        super(name);

        config(String.class, EffectTesterBuild::setCode);
        config(Object[].class, (EffectTesterBuild b, Object[] config) -> {
            b.effect.lifetime = (float) config[0];
            b.effect.clip = (float) config[1];
            b.setCode((String) config[2]);
        });
    }

    public class EffectTesterBuild extends CodableTesterBuild {
        private String code = "";
        public final Effect effect = new Effect(20, e -> {
        });
        private Fi synchronizedFile = null;

        @Override
        public void buildConfiguration(Table table) {
            table.button(Icon.pencil, Styles.cleari, () -> {
                CodeIde ide = new CodeIde();
                CodingTabArea tab = new CodingTabArea();
                ide.addTab(tab);
                ide.maxByteOutput = 65515; // (65535 = Max bytes size) - (19 = build properties) - (1 = build version)
                tab.setCode(getCode());
                tab.setObjThis(this);
                //tab.addVariable("e", effect.);

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
                    setError();
                } catch (Exception e) {
                    setError(e.getMessage(), false);
                }

            }).size(40f);
        }

        @Override
        public Object config() {
            if(effect.lifetime == 20f && effect.clip == 50f) return code;
            return new Object[]{effect.lifetime,effect.clip,code};
        }

        @Override
        public boolean isEmpty() {
            return code.isEmpty();
        }

        public void updateRunFn(String value) {
            if (value.trim().isEmpty()) return;

            Scripts scripts = Vars.mods.getScripts();
            try {
                String codeStr = "function(e){" + value + "\n}";
                Function fn = scripts.context.compileFunction(scripts.scope, codeStr, "EffectTester", 1);
                effect.renderer = e -> {
                    try {
                        fn.call(scripts.context,scripts.scope, rhino.Context.toObject(this, scripts.scope), new Object[]{e, this});
                        setError();
                    } catch (Exception e1) {
                        setError(e1.getMessage(), false);
                    }
                };
                scripts.context.evaluateString(scripts.scope, codeStr, "EffectTester", 1);

                setError();
            } catch (Throwable e) {
                setError(e.getMessage(),true);
            }
        }

        private void setCode(String code) {
            if(!Objects.equals(code, this.code)) lastEditByPlayer = false;

            this.code = code;
            updateRunFn(code);
        }
        public String getCode() {
            return code;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(effect.lifetime);
            write.f(effect.clip);
            write.str(getCode());
        }
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            effect.lifetime = read.f();
            effect.clip = read.f();
            setCode(read.str());
        }


    }

}
