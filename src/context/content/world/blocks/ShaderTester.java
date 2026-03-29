package context.content.world.blocks;

import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
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

public class ShaderTester extends CodableTester {
	public ShaderTester(String name) {
		super(name);

		config(Object[].class, (ShaderTesterBuild build, Object[] list) -> {
			build.vertexCode = (String) list[0];
			build.fragmentCode = (String) list[1];
			build.bindCode = (String) list[2];
			build.safeRunning = (boolean) list[3];
		});
	}

	public class ShaderTesterBuild extends CodableTesterBuild {
		private String vertexCode, fragmentCode, bindCode = "";

		private Shader currentShader;
		private Runnable apply;

		@Override
		public void buildConfiguration(Table table) {
			if (vertexCode == null || fragmentCode == null) initShader();

			table.button(Icon.pencil, Styles.cleari, this::show).size(40f);
			table.button(Icon.settings, Styles.cleari, () -> {
				ConfigurationDialog cd = new ConfigurationDialog("@context.testers.configuration");

				cd.addSeparator("@block.context-interface-tester.category-code");

				cd.addBooleanInput("safe", "@context.testers.safe-running", safeRunning);

				cd.setOnClose(values -> configure(new Object[]{vertexCode, fragmentCode, bindCode, values.get("safe")}));
				cd.show();
			}).size(40f);
			table.button(Icon.eyeSmall, Styles.cleari, this::run).size(40f);
		}

		@Override
		public Object[] config() {
			return new Object[]{vertexCode, fragmentCode, bindCode, safeRunning};
		}

		public void genShader() {
			Scripts scripts = Vars.mods.getScripts();

			String textCode;
			if(safeRunning) textCode = "function(shader){" + Utils.applySafeRunning(bindCode) + " \n}";
			else textCode = "function(shader){"  + bindCode + " \n}";

			try {
				Function fn = scripts.context.compileFunction(scripts.scope, textCode, "JsTester", 1);
				currentShader = new Shader(vertexCode, fragmentCode) {
					@Override
					public void apply() {
						try {
							apply.run();
						} catch (Exception e) {
							setError(e.getMessage(), false);
							currentShader = null;
							apply = null;
						}
					}
				};
				apply = () -> fn.call(scripts.context, scripts.scope, rhino.Context.toObject(this, scripts.scope), new Object[]{currentShader});
				setError();
			} catch (Exception e) {
				setError(e.getMessage(), true);
				currentShader = null;
				apply = null;
			}
		}

		public void initShader() {
			vertexCode = tree.get("shaders/screenspace.vert").readString();
			fragmentCode = tree.get("shaders/screenspace.frag").readString();
		}

		public void run() {
			genShader();
			BaseDialog shaderDialog = new BaseDialog("", new Dialog.DialogStyle() {{
				titleFont = Fonts.def;
			}}) {
				@Override
				public void draw() {
					super.draw();

					if (currentShader != null) {
						try {
							Draw.blit(currentShader);
						} catch (Exception e) {
							setError(e.getMessage(), false);
							currentShader = null;
							hide();
						}
					} else hide();
				}
			};
			shaderDialog.clear();
			shaderDialog.show();
			shaderDialog.closeOnBack();
			if (mobile) shaderDialog.addCloseButton();
		}

		public void show() {
			BetterIdeDialog ideDialog = new BetterIdeDialog();

			ideDialog.MaxByteOutput = 65000; // i don't wanna count, so leaving 500 or so as a buffer should allow for everything else that might need that space

			CodingTab vertTab = new CodingTab("shader.vert", false);
			vertTab.setText(vertexCode);
			CodingTab fragTab = new CodingTab("shader.frag", false);
			fragTab.setText(fragmentCode);
			CodingTab bindTab = new CodingTab("apply.js", false);
			bindTab.setText(bindCode);

			ideDialog.createTab(vertTab);
			ideDialog.createTab(fragTab);
			ideDialog.createTab(bindTab);

			ideDialog.sidebar.button(Icon.play, Styles.clearNonei, () -> {
				boolean saved = ideDialog.trySave();
				if (saved) run();
			}).size(40).padBottom(5).tooltip("@context.testers.compile-shader");

			ideDialog.onSave = () -> {
				configure(new Object[]{vertTab.getText(), fragTab.getText(), bindTab.getText(), safeRunning});
				setError();
			};
			ideDialog.setFooter(() -> Strings.format("Used Bytes: @/@",3 + ((CodingTab) ideDialog.tabs.get(ideDialog.selectedTab)).getText().length(), ideDialog.MaxByteOutput / 3));

			ideDialog.show();

			deselect();
		}
	}
}
