package context.ui.dialogs;

import arc.Core;
import arc.func.Cons;
import arc.input.KeyCode;
import arc.scene.Scene;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.mod.Scripts;
import mindustry.ui.Menus;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import rhino.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class SearchFunction {

    private static final BaseDialog bd = new BaseDialog("@context.search-function.title") {
        @Override
        public void hide() {
            onClose();
            super.hide();
        }
    };
    /** Shows if there is no result for the search */
    private static final Label LABEL_EMPTY = new Label("@context.search-function.empty-field");
    /** Table where will be the information about a selection */
    private static final Table infoContent = new Table();
    /** Button where leads the user to the url with more information about the method */
    private static final Table infoDocsButton = new TextButton("@context.search-function.docs");
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
                    if (disabled) return false;
                    if (imeData != null) return true;

                    if (keycode == KeyCode.enter && selectedLine != -1) {
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

    // FILTER BUTTONS
    private static final CheckBox checkOnlyAvailable = new CheckBox("[green]\uE88E[] " + Core.bundle.get("context.search-function.only-available"));
    private static final CheckBox checkMethods = new CheckBox(ButtonInfoType.METHOD.v + Core.bundle.get("context.search-function.methods"));
    private static final CheckBox checkFields = new CheckBox(ButtonInfoType.FIELD.v + Core.bundle.get("context.search-function.fields"));
    /** WIP, is "global variables" map, in that way the SearchFunction can see the values of variables */
    private static final Map<String, Object> arguments = new HashMap<>();

    /** URL to documentation */
    private static String infoDocsUrl = "";
    private static Scriptable objThis = null;
    private static Button selectedButton = null;
    private static Cons<String> onUpload = null;
    private static int selectedLine = -1;

    // Search Buttons

    private static final Button copyButton = new ImageButton(Icon.copy);
    private static final Button uploadButton = new ImageButton(Icon.upload);

    static {
        bd.cont.setBackground(Tex.button);
        resultsShow.defaults().left();

        // Search Area
        copyButton.clicked(() -> Core.app.setClipboardText(searchField.getText()));
        uploadButton.clicked(() -> {
            if (getOnUpload() != null) getOnUpload().get(searchField.getText());
            bd.hide();
        });
        uploadButton.visible = false;
        bd.cont.table(tableSearch -> {
            tableSearch.add(copyButton).size(40f).padRight(5f);
            tableSearch.add(uploadButton).size(40f).padRight(5f);
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
        info.row();
        info.add(infoDocsButton).growX();

        // Info - docsButton
        infoDocsButton.clicked(() -> Menus.openURI(infoDocsUrl));
        infoDocsButton.visible = false;

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

        // first search
        search();
        bd.buttons.button("@back", Icon.left, bd::hide).size(210f, 64f);
    }

    public static void setText(String text) {
        searchField.setText(text);
        searchField.setCursorPosition(text.length());
    }

    public static void show() {
        bd.show();
        searchField.getScene().setKeyboardFocus(searchField);
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
            String filter = i < toSearch.length() ? toSearch.substring(i) : "";

            displaySearch(path, filter);
        }
    }

    /**
     * Display the results filtered with { @code starts}
     * @param text The string to be searched
     * @param starts Filter only the value that start with this
     * @return if found anything
     */
    private static boolean displaySearch(String text, String starts) {
        // Reset the contents
        infoContent.clearChildren();
        infoDocsButton.visible = false;

        // Obtain the value of the text
        Object obj;
        ArrayList<String> objProperties = new ArrayList<>();
        try {
            obj = execute(text);
            NativeArray rawKeys = (NativeArray) execute("Object.keys(" + text + ")");
            for (Object key : rawKeys) {
                objProperties.add((String) key);
            }
        } catch (RuntimeException e) {
            infoContent.add("@context.not-found");
            return false;
        }

        // If is a class
        if (obj instanceof NativeJavaClass nObj) {
            Class<?> cl;
            try {
                cl = (Class<?>) nObj.unwrap();
            } catch (ClassCastException e) {
                return false;
            }

            // Making Overload buttons
            createButtonsFromClass(text, starts, cl, objProperties);

            infoContent.add(Core.bundle.format("context.search-function.info-class", cl.getName()));
            infoDocsButton.visible = changeUrl(cl.getName(), null);
            return true;
        }

        // If is an Instance of a class
        if (obj instanceof NativeJavaObject nObj) {
            Class<?> cl = nObj.unwrap().getClass();

            // Making Overload buttons
            createButtonsFromClass(text, starts, cl, objProperties);

            // Info management
            infoContent.add(Core.bundle.format("context.search-function.info-instance", cl.getName()));
            infoDocsButton.visible = changeUrl(cl.getName(), null);
            return true;
        }

        // If is a Method
        if (obj instanceof NativeJavaMethod nObj) {

            String methodName = text.substring(text.lastIndexOf(".") + 1);
            try {
                if (!text.contains(".")) throw new ClassNotFoundException();
                String clString = text.substring(0, text.lastIndexOf("."));
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
                for (Method method : cl.getMethods())
                    if (method.getName().equals(methodName)) met.add(method);

                if (met.isEmpty()) throw new ClassNotFoundException();

                // Making Overload buttons
                for (Method method : met) {
                    String show = methodToString(method);
                    ButtonInfo btn = new ButtonInfo(show);
                    btn.setPath(text + show.substring(show.indexOf('('), show.indexOf(')')));
                    btn.setType(ButtonInfoType.NULL);
                    btn.addTo(resultsShow, () -> {
                        searchField.setText(btn.getPath());
                        search();
                    });
                }

                // Info management
                infoContent.add(Core.bundle.format("context.search-function.info-method", methodName, cl.getName(), met.size()));
                infoDocsButton.visible = changeUrl(cl.getName(), "method-summary");
            } catch (Exception e) {
                String[] values = nObj.toString().split("\n");
                infoContent.add(Core.bundle.format("context.search-function.info-method", methodName, "Not found", values.length));
                for (String value : values) {
                    resultsShow.row();
                    ButtonInfo btn = new ButtonInfo(value.trim());
                    btn.setPath(text + value.trim());
                    btn.setType(ButtonInfoType.NULL);
                    btn.addTo(resultsShow, () -> {
                        searchField.setText(btn.getPath());
                        search();
                    });
                }
            }
            return true;
        }

        // If is a Property/Object
        if (obj instanceof NativeObject) {
            // ! To change
            infoContent.add("@context.search-function.info-object");
            for (String key : objProperties) {
                ButtonInfo btn = new ButtonInfo(key);
                btn.setPath(text + "." + key);
                btn.addTo(resultsShow, () -> {
                    searchField.setText(btn.getPath());
                    search();
                });
            }
            if (objProperties.isEmpty()) resultsShow.add(LABEL_EMPTY);
            return true;
        }

        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
            if (obj instanceof String str) {
                str = str.replaceAll("[\n\t]", " ");
                if (str.length() > 20) str = str.substring(0, 17) + "...";
                obj = str;
            }
            String value = "Value: " + obj;
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
        Function fn = s.context.compileFunction(s.scope, "function(" + args + "){return " + code + "}", "SearchTerms", 1);
        return fn.call(s.context, s.scope, objThis == null ? s.scope : objThis, objValues.toArray());
    }

    private static void createButtonsFromClass(String toSearch, String starts, Class<?> cl, List<String> availableKeys) {
        Method[] allMethods = cl.getMethods();
        ArrayList<ButtonInfo> buttonsToAdd = new ArrayList<>();
        Set<String> knownStrings = new HashSet<>();
        Set<String> namesAdded = new HashSet<>();

        for (Method method : allMethods) {
            String methodName = method.getName();
            boolean isAvailable = availableKeys.contains(methodName);
            knownStrings.add(methodName);

            if (checkOnlyAvailable.isChecked() && !isAvailable
                    || !methodName.startsWith(starts)
                    || !checkMethods.isChecked()
                    || namesAdded.contains(methodName)
            ) continue;


            ButtonInfo btn = new ButtonInfo(methodName);
            btn.setType(ButtonInfoType.METHOD);
            if (isAvailable) btn.setPath(toSearch + "." + methodName);
            buttonsToAdd.add(btn);
            namesAdded.add(methodName);
        }
        Field[] allFields = cl.getFields();
        for (Field field : allFields) {
            String fieldName = field.getName();
            boolean isAvailable = availableKeys.contains(fieldName);
            knownStrings.add(fieldName);
            if (checkOnlyAvailable.isChecked() && !isAvailable
                    || !fieldName.startsWith(starts)
                    || !checkFields.isChecked()
                    || namesAdded.contains(fieldName)
            ) continue;

            ButtonInfo btn = new ButtonInfo(fieldName);
            btn.setType(ButtonInfoType.FIELD);
            if (isAvailable) btn.setPath(toSearch + "." + fieldName);
            buttonsToAdd.add(btn);
            namesAdded.add(fieldName);
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

    protected static String methodToString(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName());
        sb.append("(");
        ArrayList<Parameter> params = new ArrayList<>(Arrays.asList(method.getParameters()));
        ArrayList<Class<?>> types = new ArrayList<>(Arrays.asList(method.getParameterTypes()));
        for (int i = 0; i < params.size(); i++) {
            Parameter param = params.get(i);
            Class<?> type = types.get(i);
            if (param.isNamePresent())
                sb.append(param.getName()).append(": ").append(type.getSimpleName());
            else sb.append(type.getSimpleName());
            if (i < params.size() - 1) sb.append(", ");
        }
        sb.append("): ");
        sb.append(method.getReturnType().getSimpleName());
        return sb.toString();
    }

    /**
     * Change the URL to the documentation
     * @param path The path to the class
     * @param elementId (nullable) The element id added at end of the link
     * @return if this link exist
     */
    private static boolean changeUrl(String path, String elementId) {
        if (path.startsWith("mindustry.gen")) return false;

        if (path.startsWith("java")) infoDocsUrl = "https://docs.oracle.com/en/java/javase/16/docs/api/java.base/";
        else infoDocsUrl = "https://mindustrygame.github.io/docs/";

        infoDocsUrl += path.replace(".", "/") + ".html";
        if (elementId != null) infoDocsUrl += "#" + elementId;

        return true;
    }

    public static void setOnUpload(Cons<String> onUpload) {
        SearchFunction.onUpload = onUpload;
        uploadButton.visible = onUpload != null;
    }

    public static void setObjThis(Object objThis) {
        SearchFunction.objThis = rhino.Context.toObject(objThis, Vars.mods.getScripts().scope);
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
