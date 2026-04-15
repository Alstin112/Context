package context.content.world.blocks;

import arc.files.Fi;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import context.Utils;
import context.ui.BetterIdeDialog;
import context.ui.dialogs.ConfigurationDialog;
import context.ui.dialogs.FileSyncTypeDialog;
import context.ui.tabs.CodingTab;
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
            if (config[i] instanceof String) b.setCodeSilent((String) config[i++]);

            int v = (int) config[i];
            b.safeRunning = (v & 0x1) != 0;

            b.updateRunFn();
        });
        config(String.class, JsTesterBuild::setCode);
    }

    public class JsTesterBuild extends CodableTesterBuild {

        /** The code to be executed */
        private String code = "";
        /** The function to be executed */
        private @Nullable Runnable runFn = null;
        /** File that the building will replace when code changes*/
        private Fi synchronizedFile = null;
        /** should run instead of close */
        private boolean runInsteadOfClose = false;

        /** Run the block */
        public void run() {
            try {
                if(runFn != null) runFn.run();
                setError();
            } catch (Exception e) {
                setError(e.getMessage(), false);
            }
        }
        /** Update the runnable that runs the code */
        public void updateRunFn() {
            if (code.trim().isEmpty()) {
                runFn = null;
                return;
            }

            Scripts scripts = Vars.mods.getScripts();

            String textCode;
            if(safeRunning) textCode = "function(){" + Utils.applySafeRunning(code) + " \n}";
            else textCode = "function(){"  + code + " \n}";

            try {
                Function fn = scripts.context.compileFunction(scripts.scope, textCode, "JsTester", 1);
                runFn = () -> fn.call(scripts.context, scripts.scope, rhino.Context.toObject(this, scripts.scope), new ArrayList<>().toArray());
                setError();
            } catch (Throwable e) {
                setError(e.getMessage(), true);
            }
        }
        /** Change the code without updating */
        private void setCodeSilent(String code) {
            if (!Objects.equals(code, this.code)) lastEditByPlayer = false;
            this.code = code;
        }
        /** Change the code updating the runnable */
        public void setCode(String code) {
            setCodeSilent(code);
            updateRunFn();
        }

        @Override
        public void buildConfiguration(Table table) {

            table.button(Icon.pencil, Styles.cleari, () -> {
                BetterIdeDialog ideDialog = new BetterIdeDialog();
                ideDialog.sidebar.button(Icon.play, Styles.clearNonei, () -> {
                    boolean saved = ideDialog.trySave();
                    if (saved) this.run();
                }).size(40).padBottom(5);

                ideDialog.MaxByteOutput = 65523; // (65535 = Max bytes size) - (11 = build properties) - (1 = build version)

                CodingTab codingTab = new CodingTab("code.js");
                codingTab.setText(code);

                ideDialog.createTab(codingTab);
                ideDialog.onSave = () -> {
                    this.configure(codingTab.getText());
                    if (synchronizedFile != null) lastTimeFileModified = synchronizedFile.lastModified();
                    lastEditByPlayer = true;
                };
                codingTab.setOnSynchronize (fi -> synchronizedFile = fi);
                ideDialog.setFooter(() -> Strings.format("Used Bytes: @/@",3+codingTab.getText().length(), ideDialog.MaxByteOutput));

                if (synchronizedFile == null) {
                    ideDialog.show();
                    deselect();
                    return;
                }

                final boolean FileChanged = synchronizedFile.lastModified() != lastTimeFileModified;
                final boolean CodeChanged = !lastEditByPlayer;
                if (FileChanged && CodeChanged) {
                    new FileSyncTypeDialog(false, true, type -> {
                        if (type == FileSyncTypeDialog.SyncType.CANCEL) return;
                        codingTab.synchronizeFile(synchronizedFile, type == FileSyncTypeDialog.SyncType.UPLOAD);
                        ideDialog.show();
                        deselect();
                    });
                    return;
                }

                codingTab.synchronizeFile(synchronizedFile, CodeChanged);
                ideDialog.show();
                deselect();
            }).size(40f);
            table.button(Icon.settings, Styles.cleari, () -> {
                ConfigurationDialog cd = new ConfigurationDialog("@context.testers.configuration");
                cd.addBooleanInput("safe","@context.testers.safe-running", safeRunning);

                cd.setOnClose(values -> {
                    int v = 0;
                    if ((boolean) values.get("safe")) v |= 0x1;
                    configure(new Object[]{v});
                });
                cd.show();
            }).size(40f);
            table.button(Icon.play, Styles.cleari, this::run).size(40f);
        }

        @Override
        public boolean isEmpty() {
            return code.isEmpty();
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
            if (runInsteadOfClose) v |= 0x2;
            write.b(v);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            setCode(read.str());
            byte v = read.b();
            safeRunning = (v & 0x1) != 0;
            runInsteadOfClose = (v & 0x2) != 0;
        }
    }

}
