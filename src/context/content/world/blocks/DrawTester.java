package context.content.world.blocks;

import arc.files.Fi;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import context.Utils;
import context.content.TestersModes;
import context.ui.CodeIde;
import context.ui.dialogs.ConfigurationDialog;
import context.ui.dialogs.FileSyncTypeDialog;
import context.ui.elements.CodingTabArea;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.mod.Scripts;
import mindustry.ui.Styles;
import rhino.Function;

import java.util.Objects;

public class DrawTester extends CodableTester {

    public DrawTester(String name) {
        super(name);
        hasShadow = false;

        config(Object[].class, (DrawTesterBuild b, Object[] config) -> {
            int i = 0;

            if (config[i] instanceof String) b.setCode((String) config[i++]);

            // Getting the configs
            int v = (int) config[i];
            b.displaying = (v & 0b00000001) != 0;
            b.safeRunning = (v & 0b00000010) != 0;
            b.invisibleWhenDraw = (v & 0b00000100) != 0;
        });
        config(String.class, DrawTesterBuild::setCode);
        config(Boolean.class, (DrawTesterBuild b, Boolean config) -> b.displaying = config);
    }

    public class DrawTesterBuild extends CodableTesterBuild {

        /** The code to be executed */
        private String code = "";
        /** The function to be executed to draw*/
        private Runnable drawFn = () -> {
        };

        /** Self-explanatory */
        private boolean invisibleWhenDraw = false;
        /** desktop file to be synchronized with the build */
        private Fi synchronizedFile = null;
        /** Should this block be able to run */
        private boolean displaying = true;

        @Override
        public void buildConfiguration(Table table) {
            table.button(Icon.pencil, Styles.cleari, () -> {
                CodeIde ide = new CodeIde();
                CodingTabArea tab = new CodingTabArea();

                ide.addTab(tab);
                ide.maxByteOutput = 65522; // (65535 = Max bytes size) - (12 = build properties) - (1 = build version)
                tab.setCode(code);
                tab.setObjThis(this);

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
                final boolean LocalPlayerChanged = lastEditByPlayer;

                if (FileChanged && !LocalPlayerChanged) {
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
                cd.addTitle("@context.testers.configuration");
                cd.addBooleanInput("@context.testers.safe-running", safeRunning);
                cd.addBooleanInput("@block.context-draw-tester.invisible", invisibleWhenDraw);
                cd.setOnClose(values -> {
                    int b = 0;
                    if (displaying) b |= 0x1;
                    if ((boolean) values.get("context.testers.safe-running")) b |= 0x2;
                    if ((boolean) values.get("block.context-draw-tester.invisible")) b |= 0x4;

                    configure(new Object[]{b});
                });
                cd.show();
            }).size(40f);

            ImageButton btn;
            btn = new ImageButton(new ImageButton.ImageButtonStyle(Styles.cleari));
            btn.getStyle().imageUp = Icon.eyeSmall;
            btn.getStyle().imageChecked = Icon.eyeOffSmall;
            btn.resizeImage(Icon.eyeSmall.imageSize());
            btn.setChecked(!displaying);
            btn.clicked(() -> configure(!displaying));
            table.add(btn).size(40f);
        }

        @Override
        public void draw() {
            updateMode();
            if (mode != TestersModes.ACTIVE && mode != TestersModes.RUNTIME_ERROR) {
                super.draw();
                return;
            }
            if (!invisibleWhenDraw) super.draw();

            setError();
            try {
                drawFn.run();
            } catch (Exception e) {
                setError(e.getMessage(), false);
            }
        }

        @Override
        public boolean isEmpty() {
            return code.isEmpty();
        }

        public void updateDrawFn(String value) {
            if (value.trim().isEmpty()) return;

            if (safeRunning) value = Utils.applySafeRunning(value);

            Scripts scripts = Vars.mods.getScripts();
            try {
                String codeStr = "function(){" + value + "\n}";
                Function fn = scripts.context.compileFunction(scripts.scope, codeStr, "drawTester", 1);
                drawFn = () -> fn.call(scripts.context, scripts.scope, rhino.Context.toObject(this, scripts.scope), new Object[0]);

                setError();
            } catch (Exception e) {
                setError(e.getMessage(), true);
            }
        }

        public void setCode(String code) {
            if (!Objects.equals(code, this.code)) lastEditByPlayer = false;

            this.code = code;
            updateDrawFn(code);
        }

        public String getCode() {
            return code;
        }

        public Runnable getDrawFn() {
            return drawFn;
        }

        @Override
        public void updateMode() {
            if (!displaying) mode = TestersModes.INACTIVE;
            else super.updateMode();
        }

        @Override
        public Object[] config() {
            int v = 0;
            if (displaying) v |= 0x1;
            if (safeRunning) v |= 0x2;
            if (invisibleWhenDraw) v |= 0x4;

            return new Object[]{code, v};
        }
        @Override
        public void write(Writes write) {
            super.write(write);
            write.str(code);
            int v = 0;
            if (displaying) v |= 0x1;
            if (safeRunning) v |= 0x2;
            if (invisibleWhenDraw) v |= 0x4;
            write.b(v);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            setCode(read.str());
            int v = read.b();
            displaying = (v & 0b00000001) != 0;
            safeRunning = (v & 0b00000010) != 0;
            invisibleWhenDraw = (v & 0b00000100) != 0;
        }
    }

}
