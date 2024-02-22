package context.ui.dialogs;

import arc.Core;
import arc.func.Cons;
import arc.input.KeyCode;
import arc.scene.Scene;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.Button;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Label;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.mod.Scripts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import rhino.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.MessageFormat;
import java.util.*;

public class SearchFunction {

    private static final BaseDialog bd = new BaseDialog("@context.search-function.title") {
        @Override
        public void hide() {
            onClose();
            super.hide();
        }
    };
    private static final Label LABEL_EMPTY = new Label("@context.search-function.empty-field");
    private static final Table infoContent = new Table();
    private static final TextField searchField = new TextField("Vars") {
        @Override
        protected InputListener createInputListener() {
            return new TextFieldClickListener() {
                @Override
                public boolean keyDown(InputEvent event, KeyCode keycode) {

                    if (disabled) return false;
                    if (imeData != null) return true;
                    Scene stage = getScene();
                    if (stage == null || stage.getKeyboardFocus() != searchField) return false;

                    switch (keycode) {
                        case down -> {
                            selectedLine = (selectedLine + 1) % resultsShow.getChildren().size;
                            scheduleKeyRepeatTask(keycode);
                            return true;
                        }
                        case up -> {
                            selectedLine = (selectedLine - 1 + resultsShow.getChildren().size) % resultsShow.getChildren().size;
                            scheduleKeyRepeatTask(keycode);
                            return true;
                        }
                        case escape -> {
                            if (selectedLine >= 0) selectedLine = -1;
                            else searchField.getScene().setKeyboardFocus(null);
                            return true;
                        }
                        default -> {
                            return super.keyDown(event, keycode);
                        }
                    }
                }

                @Override
                public boolean keyUp(InputEvent event, KeyCode keycode) {
                    if(disabled) return false;
                    if(imeData != null) return true;

                    if(keycode == KeyCode.enter && selectedLine != -1) {
                        selectedButton.fireClick();
                        goEnd(true);
                        if (Core.input.ctrl()) {
                            if (getOnUpload() != null) getOnUpload().get(searchField.getText());
                            bd.hide();
                        }
                        return true;
                    }
                    return super.keyUp(event, keycode);
                }
            };
        }
    };
    private static final Table resultsShow = new Table();
    private static final CheckBox checkOnlyAvailable = new CheckBox("[green]\uE88E[] " + Core.bundle.get("context.search-function.only-available"));
    private static final CheckBox checkMethods = new CheckBox(ButtonInfoType.METHOD.v + Core.bundle.get("context.search-function.methods"));
    private static final CheckBox checkFields = new CheckBox(ButtonInfoType.FIELD.v + Core.bundle.get("context.search-function.fields"));

    private static Scriptable objThis = null;

    private static Button selectedButton = null;

    private static final Map<String, Object> arguments = new HashMap<>();

    static {
        bd.cont.setBackground(Tex.button);
        resultsShow.defaults().left();

        // Search Area
        bd.cont.table(tableSearch -> {
            tableSearch.button(Icon.copy, () -> Core.app.setClipboardText(searchField.getText())).size(40f).padRight(5f);
            tableSearch.button(Icon.upload, () -> {
                if (getOnUpload() != null) getOnUpload().get(searchField.getText());
                bd.hide();
            }).size(40f).padRight(5f);
            tableSearch.add("@context.search");
            tableSearch.marginBottom(10f);
            tableSearch.add(searchField).growX();
        }).growX();
        searchField.changed(SearchFunction::search);
        searchField.setValidator(s -> s.matches("^[\\w.]*$"));
        bd.cont.row();

        // Main Screen with the results | options
        Table screen = bd.cont.table().grow().get();

        // Results on left
        Table bigResult = screen.table().grow().colspan(4).get();
        bigResult.setBackground(Tex.button);
        bigResult.pane(resultsShow).grow();
        resultsShow.top();

        // All options
        Table optionsSide = screen.table().grow().get();

        // Info
        Table info = optionsSide.table().grow().get().top();
        info.setBackground(Tex.button);
        info.add("@context.search-function.info");
        info.row();
        info.add(infoContent).grow();

        optionsSide.row();

        // Filters
        Table filters = optionsSide.table().grow().get();
        filters.add("@context.search-function.filters");
        filters.row();
        filters.setBackground(Tex.button);
        filters.add(checkOnlyAvailable).left().padTop(5).get().changed(SearchFunction::search);
        checkMethods.setChecked(true);
        filters.row();
        filters.add(checkMethods).left().padTop(5).get().changed(SearchFunction::search);
        checkMethods.setChecked(true);
        filters.row();
        filters.add(checkFields).left().padTop(5).get().changed(SearchFunction::search);
        checkFields.setChecked(true);

        search();
        bd.buttons.button("@back", Icon.left, bd::hide).size(210f, 64f);
    }

    private static Cons<String> onUpload = null;
    private static int selectedLine = -1;


    public static void setText(String text) {
        searchField.setText(text);
        searchField.setCursorPosition(text.length());
    }

    public static void show() {
        show(null);
    }

    public static void show(Cons<String> onUpload) {
        bd.show();
        searchField.getScene().setKeyboardFocus(searchField);
        SearchFunction.onUpload = onUpload;
        search();
    }

    private static void search() {
        if (!searchField.isValid()) return;
        String toSearch = searchField.getText();
        String path = null;
        if (toSearch.contains(".")) path = toSearch.substring(0, toSearch.lastIndexOf("."));

        resultsShow.clear();
        ButtonInfo.resetActualId();
        boolean success = displaySearch(toSearch, "");
        if (!success && path != null) {
            int i = toSearch.lastIndexOf(".") + 1;
            if (i < toSearch.length()) displaySearch(path, toSearch.substring(i));
            else displaySearch(path, "");
        }
    }

    private static boolean displaySearch(String toSearch, String starts) {
        infoContent.clearChildren();
        Object obj;
        ArrayList<String> availableKeys;
        try {
            obj = execute(toSearch);
            NativeArray rawKeys = (NativeArray) execute("Object.keys(" + toSearch + ")");
            availableKeys = new ArrayList<>();
            for (Object key : rawKeys) {
                availableKeys.add((String) key);
            }
        } catch (RuntimeException e) {
            infoContent.add("@context.not-found");
            return false;
        }

        if (obj instanceof NativeJavaClass nObj) {
            Class<?> cl;
            try {
                cl = (Class<?>) nObj.unwrap();
            } catch (ClassCastException e) {
                return false;
            }

            infoContent.add(Core.bundle.format("context.search-function.info-class", cl.getName()));
            createButtonsFromClass(toSearch, starts, cl, availableKeys);
            return true;
        }

        if (obj instanceof NativeJavaObject nObj) {
            Class<?> cl = nObj.unwrap().getClass();

            infoContent.add(Core.bundle.format("context.search-function.info-instance", cl.getName()));
            createButtonsFromClass(toSearch, starts, cl, availableKeys);
            return true;
        }

        if (obj instanceof NativeJavaMethod nObj) {

            String methodName = toSearch.substring(toSearch.lastIndexOf(".") + 1);
            try {
                if (!toSearch.contains(".")) throw new ClassNotFoundException();
                String clString = toSearch.substring(0, toSearch.lastIndexOf("."));
                Class<?> cl;
                Object retObj = execute(clString);
                if (retObj instanceof NativeJavaClass njc) {
                    cl = (Class<?>) njc.unwrap();
                } else if (retObj instanceof NativeJavaObject njo) {
                    cl = njo.unwrap().getClass();
                } else {
                    throw new ClassNotFoundException();
                }
                ArrayList<Method> met = new ArrayList<>();
                for (Method method : cl.getMethods()) {
                    if (!method.getName().equals(methodName)) continue;
                    met.add(method);
                }
                if (met.isEmpty()) throw new ClassNotFoundException();

                infoContent.add(Core.bundle.format("context.search-function.info-method", methodName, cl.getName(), met.size()));
                for (Method method : met) {
                    StringBuilder args = new StringBuilder();

                    args.append("(");
                    ArrayList<Parameter> params = new ArrayList<>(Arrays.asList(method.getParameters()));
                    ArrayList<Class<?>> types = new ArrayList<>(Arrays.asList(method.getParameterTypes()));
                    for (int i = 0; i < params.size(); i++) {
                        Parameter param = params.get(i);
                        Class<?> type = types.get(i);
                        if (param.isNamePresent())
                            args.append(param.getName()).append(": ").append(type.getSimpleName());
                        else args.append(type.getSimpleName());
                        if (i < params.size() - 1) args.append(", ");
                    }
                    args.append(")");

                    String writeString = args.toString();
                    String show = MessageFormat.format("{0}{1}: {2}",
                            method.getName(),
                            writeString,
                            method.getReturnType().getSimpleName()
                    );

                    ButtonInfo btn = new ButtonInfo(show);
                    btn.setPath(toSearch + writeString);
                    btn.setType(ButtonInfoType.NULL);
                    btn.addTo(resultsShow, () -> {
                        searchField.setText(btn.getPath());
                        search();
                    });
                }

            } catch (Exception e) {
                String[] values = nObj.toString().split("\n");
                infoContent.add(Core.bundle.format("context.search-function.info-method", methodName, "Not found", values.length));
                for (String value : values) {
                    resultsShow.row();
                    ButtonInfo btn = new ButtonInfo(value.trim());
                    btn.setPath(toSearch + value.trim());
                    btn.setType(ButtonInfoType.NULL);
                    btn.addTo(resultsShow, () -> {
                        searchField.setText(btn.getPath());
                        search();
                    });
                }
            }
            return true;
        }

        if (obj instanceof NativeObject) {
            // ! To change
            infoContent.add("@context.search-function.info-object");
            for (String key : availableKeys) {
                ButtonInfo btn = new ButtonInfo(key);
                btn.setPath(toSearch + "." + key);
                btn.addTo(resultsShow, () -> {
                    searchField.setText(btn.getPath());
                    search();
                });
            }
            if (availableKeys.isEmpty()) resultsShow.add(LABEL_EMPTY);
            return true;
        }

        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
            String value;
            if (obj instanceof String str) {
                str = str.replaceAll("[\n\t]", " ");
                if (str.length() > 20) str = str.substring(0, 17) + "...";
                value = "Value: " + str;
            } else {
                value = "Value: " + obj;
            }
            infoContent.add(Core.bundle.format("context.search-function.info-primitive", obj.getClass().getSimpleName(), value));
            return true;
        }

        infoContent.add(Core.bundle.format("context.search-function.info-unknown", obj.getClass().getName()));
        return true;
    }

    private static Object execute(String code) {
        Scripts s = Vars.mods.getScripts();
        StringBuilder args = new StringBuilder();
        List<Object> objValues = new ArrayList<>(arguments.values());
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            args.append(entry.getKey()).append(",");
            objValues.add(entry.getValue());
        }
        Function fn = s.context.compileFunction(s.scope, "function("+args.toString()+"){return "+code+"}", "SearchTerms", 1);
        return fn.call(s.context, s.scope, objThis == null? s.scope : objThis, objValues.toArray());
    }

    private static void createButtonsFromClass(String toSearch, String starts, Class<?> cl, List<String> availableKeys) {
        Set<String> knownStrings = new HashSet<>();
        ArrayList<ButtonInfo> buttonsToAdd = new ArrayList<>();
        Method[] allMethods = cl.getMethods();
        for (Method method : allMethods) {
            knownStrings.add(method.getName());
            boolean isAvailable = availableKeys.contains(method.getName());
            if (checkOnlyAvailable.isChecked()
                    && !availableKeys.contains(method.getName())
                    || !method.getName().startsWith(starts)
                    || !checkMethods.isChecked()
                    || buttonsToAdd.stream().anyMatch(btn -> btn.text.equals(method.getName()))
            ) continue;

            ButtonInfo btn = new ButtonInfo(method.getName());
            btn.setType(ButtonInfoType.METHOD);
            if (isAvailable) btn.setPath(toSearch + "." + method.getName());

            buttonsToAdd.add(btn);
        }
        Field[] allFields = cl.getFields();
        for (Field field : allFields) {
            knownStrings.add(field.getName());
            boolean isAvailable = availableKeys.contains(field.getName());
            if (checkOnlyAvailable.isChecked() && !isAvailable
                    || !field.getName().startsWith(starts)
                    || !checkFields.isChecked()
                    || buttonsToAdd.stream().anyMatch(btn -> btn.text.equals(field.getName()))
            ) continue;

            ButtonInfo btn = new ButtonInfo(field.getName());
            btn.setType(ButtonInfoType.FIELD);
            if (isAvailable) btn.setPath(toSearch + "." + field.getName());

            buttonsToAdd.add(btn);
        }

        availableKeys.stream().filter(key -> !knownStrings.contains(key) && key.startsWith(starts))
                .forEach(key -> {
                    ButtonInfo btn = new ButtonInfo(key);
                    btn.setPath(toSearch + "." + key);
                    buttonsToAdd.add(btn);
                });
        buttonsToAdd.sort(Comparator.comparing(btn -> btn.text));

        for (ButtonInfo buttonInfo : buttonsToAdd) {
            buttonInfo.addTo(resultsShow, () -> {
                searchField.setText(buttonInfo.getPath());
                search();
            });
        }
        if (buttonsToAdd.isEmpty()) resultsShow.add(LABEL_EMPTY);
    }

    public static Cons<String> getOnUpload() {
        return onUpload;
    }

    public static void setOnUpload(Cons<String> onUpload) {
        SearchFunction.onUpload = onUpload;
    }

    public static void setObjThis(Object objThis) {
        SearchFunction.objThis = rhino.Context.toObject(objThis, Vars.mods.getScripts().scope);
    }

    public static void addVariable(String name, Object obj) {
        arguments.put(name, obj);
    }
    public static void addVariable(Map<String, Object> variableMap) {
        for (Map.Entry<String, Object> entry : variableMap.entrySet()) {
            arguments.put(entry.getKey(), entry.getValue());
        }
    }

    private static void onClose() {
        onUpload = null;
        objThis = null;
        arguments.clear();
    }

    private static class ButtonInfo {
        public final String text;
        private String path = null;
        private ButtonInfoType type = ButtonInfoType.UNKNOWN;
        private static int actualId = 0;

        /**
         * Creates a button to results
         * @param text Label text
         */
        public ButtonInfo(String text) {
            this.text = text;
        }

        public void addTo(Table resultsShow, Runnable onClick) {
            String labelText = getPath() == null ? "[red]\uE88F[] " : "[green]\uE88E[] ";
            labelText += getType().v;
            labelText += text;

            if (getPath() == null) {
                resultsShow.add(new Label(labelText));
                resultsShow.row();
                return;
            }
            Button btn = (Button) new Button(Styles.cleart).left();
            int id = actualId++;
            btn.update(() -> {
                if (btn.isOver()) selectedLine = id;
                if (id != -1 && selectedLine == id) {
                    btn.setStyle(Styles.grayt);
                    selectedButton = btn;
                } else {
                    btn.setStyle(Styles.cleart);
                }
            });
            btn.add(new Label(labelText));
            btn.clicked(onClick);
            resultsShow.add(btn).growX();
            resultsShow.row();
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public ButtonInfoType getType() {
            return type;
        }

        public void setType(ButtonInfoType type) {
            this.type = type;
        }

        public static void resetActualId() {
            actualId = 0;
        }
    }

    enum ButtonInfoType {
        METHOD("[red]\uE282[] "),
        FIELD("[gold]\uE286[] "),
        NULL(""),
        UNKNOWN("[purple]\uEE89[] ");

        public final String v;

        ButtonInfoType(String v) {
            this.v = v;
        }
    }
}
