package context.content.world.blocks;

import arc.files.Fi;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import context.content.TestersModes;
import context.ui.CodeIde;
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

        config(Object[].class, (DrawTesterBuild b, Object[] config) -> {
            b.active = (boolean) config[0];
            b.setCode((String) config[1]);
        });
        config(String.class, DrawTesterBuild::setCode);
        config(Boolean.class, (DrawTesterBuild b, Boolean config) -> b.active = config);
    }

    public class DrawTesterBuild extends CodableTesterBuild {

        private String code = "";

        private Runnable drawFn = () -> {
        };
        private Fi synchronizedFile = null;

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
        public void draw() {
            super.draw();
            if (mode != TestersModes.ACTIVE && mode != TestersModes.RUNTIME_ERROR) return;

            try {
                drawFn.run();
                setError();
            } catch (Exception e) {
                setError(e.getMessage(), false);
            }
        }

        @Override
        public boolean isEmpty() {
            return code.isEmpty();
        }

        public void updateDrawFn(String value) {
            if (value.trim().isEmpty()) {
                return;
            }

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
            if(!Objects.equals(code, this.code)) lastEditByPlayer = false;

            this.code = code;
            updateDrawFn(code);
        }

        public String getCode(){
            return code;
        }

        public Runnable getDrawFn() {
            return drawFn;
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
