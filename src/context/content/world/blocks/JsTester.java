package context.content.world.blocks;

import arc.files.Fi;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import context.ui.CodeIde;
import context.ui.dialogs.FileSyncTypeDialog;
import context.ui.elements.CodingTabArea;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.mod.Scripts;
import mindustry.ui.Styles;
import rhino.Script;

import java.util.Objects;

public class JsTester extends CodableTester {
    public JsTester(String name) {
        super(name);

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

            table.button(Icon.play, Styles.cleari, this::run).size(40f);
        }

        public void run() {
            try {
                this.runFn.run();
                setError();
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

            Scripts scripts = Vars.mods.getScripts();
            try {
                String textCode = "(function(){" + value + " \n}).apply(Vars.world.build(" + this.tile.x + "," + this.tile.y + "))";

                Script script = scripts.context.compileString(textCode, "JsTester", 1);

                if (script == null) runFn = () -> {
                };
                else runFn = () -> script.exec(scripts.context, scripts.scope);

                setError();
            } catch (Exception e) {
                setError(e.getMessage(), true);
            }
        }

        @Override
        public String config() {
            return code;
        }

        public void setCode(String code) {
            if (!Objects.equals(code, this.code)) lastEditByPlayer = false;

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
