package context.content.world.blocks;

import arc.files.Fi;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import context.Utils;
import context.ui.CodeIde;
import context.ui.dialogs.ConfigurationDialog;
import context.ui.dialogs.FileSyncTypeDialog;
import context.ui.elements.CodingTabArea;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.gen.Icon;
import mindustry.mod.Scripts;
import mindustry.ui.Styles;
import rhino.Function;

import java.util.Objects;

public class EffectTester extends CodableTester {
    public EffectTester(String name) {
        super(name);

        config(String.class, EffectTesterBuild::setCode);
        config(Object[].class, (EffectTesterBuild b, Object[] config) -> {
            int i = 0;

            if (config[i] instanceof String) b.setCode((String) config[i++]);

            b.effect.lifetime = (float) config[i++];
            b.effect.clip = (float) config[i++];
            b.safeRunning = ((int) config[i] & 0x1) != 0;

        });
    }

    public class EffectTesterBuild extends CodableTesterBuild {
        /** The code to be executed */
        private String code = "";
        /** The effect to be executed */
        public final Effect effect = new Effect(20, e -> {
        });
        /** Desktop file to be synchronized with the build */
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

                ide.setOnSave(codeIde -> {
                    this.configure(tab.getCode());
                    if (synchronizedFile != null) lastTimeFileModified = synchronizedFile.lastModified();
                    lastEditByPlayer = true;
                });
                tab.setOnSynchronize(file -> this.synchronizedFile = file);
                ide.hideTabs(true);


                if (synchronizedFile == null) {
                    ide.show();
                    deselect();
                    return;
                }

                final boolean FileChanged = synchronizedFile.lastModified() != lastTimeFileModified;
                final boolean CodeChanged = !lastEditByPlayer;

                if (FileChanged && CodeChanged) {
                    new FileSyncTypeDialog(false, true, type -> {
                        if (type == FileSyncTypeDialog.SyncType.CANCEL) return;
                        tab.setSync(synchronizedFile, type == FileSyncTypeDialog.SyncType.UPLOAD);
                    });
                } else {
                    tab.setSync(synchronizedFile, false);
                }
                ide.show();
                deselect();
            }).size(40f);
            table.button(Icon.settings, Styles.cleari, () -> {
                ConfigurationDialog cd = new ConfigurationDialog("@editmessage");

                cd.addTitle("@block.context-effect-tester.category-effect");
                cd.addReadOnlyField("@block.context-effect-tester.id", effect.id + "");
                cd.addFloatInput("@block.context-effect-tester.lifetime", effect.lifetime);
                cd.addFloatInput("@block.context-effect-tester.clipsize", effect.clip);

                cd.addTitle("@block.context-effect-tester.category-code");
                cd.addBooleanInput("@block.context-effect-tester.safemode", safeRunning);

                cd.setOnClose(values -> {
                    int v = 0;
                    if ((boolean) values.get("@block.context-effect-tester.safemode")) v |= 0x1;

                    configure(new Object[]{
                            values.get("@block.context-effect-tester.lifetime"),
                            values.get("@block.context-effect-tester.clip-size"),
                            v
                    });
                });
                cd.show();
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
        public boolean isEmpty() {
            return code.isEmpty();
        }

        public void updateRunFn(String value) {
            if (value.trim().isEmpty()) return;

            if (safeRunning) Utils.applySafeRunning(value);

            Scripts scripts = Vars.mods.getScripts();
            try {
                String codeStr = "function(e){" + value + "\n}";
                Function fn = scripts.context.compileFunction(scripts.scope, codeStr, "EffectTester", 1);
                effect.renderer = e -> {
                    setError();
                    try {
                        fn.call(scripts.context, scripts.scope, rhino.Context.toObject(this, scripts.scope), new Object[]{e, this});
                    } catch (Exception e1) {
                        setError(e1.getMessage(), false);
                    }
                };

                setError();
            } catch (Throwable e) {
                setError(e.getMessage(), true);
            }
        }

        private void setCode(String code) {
            if (!Objects.equals(code, this.code)) lastEditByPlayer = false;

            this.code = code;
            updateRunFn(code);
        }

        public String getCode() {
            return code;
        }

        @Override
        public Object config() {
            int v = 0;
            if (safeRunning) v |= 0x1;

            return new Object[]{
                    code,
                    effect.lifetime,
                    effect.clip,
                    v
            };
        }
        @Override
        public void write(Writes write) {
            super.write(write);
            write.str(getCode());
            write.f(effect.lifetime);
            write.f(effect.clip);
            byte v = 0;
            if (safeRunning) v |= 0x1;
            write.b(v);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            setCode(read.str());
            effect.lifetime = read.f();
            effect.clip = read.f();
            byte v = read.b();
            safeRunning = (v & 0x1) != 0;
        }
    }

}
