package context.content.world.blocks;

import arc.*;
import arc.files.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import context.*;
import context.ui.*;
import context.ui.dialogs.*;
import context.ui.tabs.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import rhino.*;

import static mindustry.Vars.*;

public class UITester extends CodableTester {
	public UITester(String name) {
		super(name);

		config(Object[].class, (UITesterBuild build, Object[] list) -> {
			build.code = (String) list[0];
			build.safeRunning = (boolean) list[1];
			build.dialog = (boolean) list[2];
		});
		config(String.class, (UITesterBuild build, String code) -> build.code = code);
	}

	public class UITesterBuild extends CodableTesterBuild {
		private final Table extensionTable = new Table();
		private final Table buttonsTable = new Table();

		private String code = "";

		public boolean dialog;

		private Fi synchronizedFile;

		@Override
		public void buildConfiguration(Table table) {
			WidgetGroup group = new WidgetGroup();
			table.add(group);

			buttonsTable.clear();
			buttonsTable.button(Icon.pencil, Styles.cleari, () -> {
				BetterIdeDialog ideDialog = new BetterIdeDialog();
				ideDialog.sidebar.button(Icon.play, Styles.clearNonei, () -> {
					boolean saved = ideDialog.trySave();
					if (saved) {
						run();
						if (!dialog) {
							ideDialog.hide();
							control.input.config.showConfig(this);
						}
					}
				}).size(40).padBottom(5);

				ideDialog.MaxByteOutput = 65000; // i don't wanna count, so leaving 500 or so as a buffer should allow for everything else that might need that space

				CodingTab codingTab = new CodingTab("code.js");
				codingTab.setText(code);

				ideDialog.createTab(codingTab);
				ideDialog.onSave = () -> {
					configure(codingTab.getText());
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
			buttonsTable.button(Icon.settings, Styles.cleari, () -> {
				ConfigurationDialog cd = new ConfigurationDialog("@context.testers.configuration");

				cd.addSeparator("@block.context-interface-tester.category-code");

				cd.addBooleanInput("dialog", "@context.testers.dialog-mode", dialog);
				cd.addBooleanInput("safe", "@context.testers.safe-running", safeRunning);

				cd.setOnClose(values -> configure(new Object[]{code, values.get("safe"), values.get("dialog")}));
				cd.show();
			}).size(40f);
			buttonsTable.button(Icon.play, Styles.cleari, this::run).size(40f);

			group.addChild(buttonsTable);
			group.addChild(extensionTable);

			buttonsTable.invalidate();
			buttonsTable.pack();
			extensionTable.invalidate();
			extensionTable.pack();
		}

		@Override
		public Object[] config() {
			return new Object[]{code, safeRunning, dialog};
		}

		@Override public boolean isEmpty() {
			return code.isEmpty();
		}

		@Override
		public void read(Reads read, byte revision) {
			super.read(read, revision);

			code = read.str();
			safeRunning = read.bool();
			dialog = read.bool();
		}

		public void run() {
			extensionTable.clear();
			if (dialog) {
				runDialog();
			} else runTable();
		}
		public void runDialog() {
			if (code.trim().isEmpty()) {
				Dialog dialog = new BaseDialog(Core.bundle.get("block.context-interface-tester.dialog-title"), new Dialog.DialogStyle() {{
					background = Core.scene.getStyle(Dialog.DialogStyle.class).background;
					titleFont = Core.scene.getStyle(Dialog.DialogStyle.class).titleFont;
					titleFontColor = Core.scene.getStyle(Dialog.DialogStyle.class).titleFontColor;
					stageBackground = Core.scene.getStyle(Dialog.DialogStyle.class).stageBackground;
				}});
				dialog.closeOnBack();
				if (mobile) dialog.addCloseButton();
				dialog.show();
				return;
			}

			Scripts scripts = Vars.mods.getScripts();

			String textCode;
			if(safeRunning) textCode = "function(dialog, cont, buttons, titleTable){" + Utils.applySafeRunning(code) + " \n}";
			else textCode = "function(dialog, cont, buttons, titleTable){"  + code + " \n}";

			Function fn;

			try {
				fn = scripts.context.compileFunction(scripts.scope, textCode, "JsTester", 1);
				setError();
			} catch (Throwable e) {
				setError(e.getMessage(), true);
				return;
			}

			try {
				Dialog dialog = new BaseDialog(Core.bundle.get("block.context-ui-tester.dialog-title"));
				fn.call(scripts.context, scripts.scope, rhino.Context.toObject(this, scripts.scope), new Object[]{dialog, dialog.cont, dialog.buttons, dialog.titleTable});
				dialog.closeOnBack();
				dialog.show();
				setError();
			} catch (Throwable e) {
				setError(e.getMessage(), false);
			}
		}
		public void runTable() {
			if (code.trim().isEmpty()) {
				return;
			}

			Scripts scripts = Vars.mods.getScripts();

			String textCode;
			if(safeRunning) textCode = "function(table){" + Utils.applySafeRunning(code) + " \n}";
			else textCode = "function(table){"  + code + " \n}";

			Function fn;

			try {
				fn = scripts.context.compileFunction(scripts.scope, textCode, "JsTester", 1);
				setError();
			} catch (Throwable e) {
				setError(e.getMessage(), true);
				return;
			}

			try {
				fn.call(scripts.context, scripts.scope, rhino.Context.toObject(this, scripts.scope), new Object[]{extensionTable});
				setError();
				extensionTable.invalidate();
				extensionTable.pack();
			} catch (Throwable e) {
				setError(e.getMessage(), false);
			}
		}

		@Override
		public void updateTableAlign(Table table){
			Vec2 pos = Tmp.v1.set(Core.input.mouseScreen(x, y));
			table.setPosition(pos.x, pos.y, Align.center);

			pos.sub(Core.input.mouseScreen(x, y + size * tilesize / 2f + 1));

			buttonsTable.setPosition(pos.x, pos.y * -1, Align.bottom);
			extensionTable.setPosition(pos.x, pos.y, Align.top);
		}

		@Override
		public void write(Writes write) {
			super.write(write);

			write.str(code);
			write.bool(safeRunning);
			write.bool(dialog);
		}
	}
}
