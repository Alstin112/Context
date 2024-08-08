package context.content.world.blocks;

import arc.files.Fi;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import context.Utils;
import context.ui.CodeIde;
import context.ui.dialogs.ConfigurationDialog;
import context.ui.dialogs.FileSyncTypeDialog;
import context.ui.elements.CodingTabArea;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.mod.Scripts;
import mindustry.ui.Styles;
import rhino.Function;

import java.util.ArrayList;
import java.util.Objects;

public class JsTester extends CodableTester {
    public JsTester(String name) {
        super(name);

        config(Object[].class, (JsTesterBuild b, Object[] config) -> {
            int i = 0;

            if (config[i] instanceof String) b.setCode((String) config[i++]);

            int v = (int) config[i];
            b.safeRunning = (v & 0b00000001) != 0;
        });
        config(String.class, JsTesterBuild::setCode);
    }

    public class JsTesterBuild extends CodableTesterBuild {

        private String code = "";
        private Runnable runFn = () -> {
        };
        private Fi synchronizedFile = null;

        @Override
        public void buildConfiguration(Table table) {

            table.button(Icon.pencil, Styles.cleari, () -> {
                CodeIde ide = new CodeIde();

                CodingTabArea tab = new CodingTabArea();
                ide.addTab(tab);
                ide.hideTabs(true);
                ide.maxByteOutput = 65523; // (65535 = Max bytes size) - (11 = build properties) - (1 = build version)

                tab.setCode(code);
                tab.setObjThis(this);

                ide.addButton(new TextButton("@context.hide-and-run")).clicked(() -> {
                    if (ide.trySave()) {
                        ide.close();
                        this.run();
                    }
                });
                ide.addButton(new TextButton("@context.only-run")).clicked(() -> {
                    if (ide.trySave()) this.run();
                });

                ide.setOnSave(codeIde -> {
                    this.configure(tab.getCode());
                    if (synchronizedFile != null) lastTimeFileModified = synchronizedFile.lastModified();
                    lastEditByPlayer = true;
                });
                tab.setOnSynchronize(file -> this.synchronizedFile = file);

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
                ConfigurationDialog cd = new ConfigurationDialog("@editmessage");
                cd.addBooleanInput("@context.testers.safe-running", safeRunning);

                cd.setOnClose(values -> {
                    int v = 0;
                    if ((boolean) values.get("@context.testers.safe-running")) v |= 0x1;

                    configure(new Object[]{v});
                });
                cd.show();
            }).size(40f);
            table.button(Icon.play, Styles.cleari, this::run).size(40f);
        }

        public void run() {
            setError();
            try {
                this.runFn.run();
            } catch (Exception e) {
                setError(e.getMessage(), false);
            }
        }

        @Override
        public boolean isEmpty() {
            return code.isEmpty();
        }

        public void updateRunFn(String value) {
            if (value.trim().isEmpty()) return;

            if (safeRunning) Utils.applySafeRunning(value);

            ArrayList<Object> argsObj = new ArrayList<>();

            Scripts scripts = Vars.mods.getScripts();
            try {
                String textCode = "function(){" + value + " \n}";
                Function fn = scripts.context.compileFunction(scripts.scope, textCode, "JsTester", 1);
                runFn = () -> fn.call(scripts.context, scripts.scope, rhino.Context.toObject(this, scripts.scope), argsObj.toArray());
                setError();
            } catch (Throwable e) {
                setError(e.getMessage(), true);
            }
        }


        public void setCode(String code) {
            if (!Objects.equals(code, this.code)) lastEditByPlayer = false;

            this.code = code;
            updateRunFn(code);
        }

        @Override
        public Object[] config() {
            int v = 0;
            if (safeRunning) v |= 0x1;

            return new Object[]{code, v};
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.str(code);
            byte v = 0;
            if (safeRunning) v |= 0x1;
            write.b(v);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            setCode(read.str());
            byte v = read.b();
            safeRunning = (v & 0x1) != 0;
        }
    }

}
