package context.ui.elements;

import arc.Core;
import arc.scene.ui.Button;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Label;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.mod.Scripts;
import mindustry.ui.Styles;
import rhino.NativeArray;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class SearchFunction {

    public static final Label LABEL_EMPTY = new Label("@context.search-function.empty-field");
    public static final Table cont = new Table();
    public static final Table infoContent = new Table();
    private static final TextField searchField = new TextField("Vars");
    private static final Table resultsShow = new Table();
    private static final CheckBox checkOnlyAvailable = new CheckBox("[green]\uE88E[] " + Core.bundle.get("context.search-function.only-available"));
    private static final CheckBox checkMethods = new CheckBox("[red]\uE282[] " + Core.bundle.get("context.search-function.methods"));
    private static final CheckBox checkFields = new CheckBox("[gold]\uE286[] " + Core.bundle.get("context.search-function.fields"));


    public SearchFunction() {
        cont.clearChildren();
        resultsShow.defaults().left();
        // Search Area
        Table tableSearch = new Table();
        tableSearch.label(() -> "@context.search");
        tableSearch.marginBottom(10);
        tableSearch.add(searchField).growX();
        searchField.changed(this::search);
        searchField.setValidator(s -> s.matches("^[\\w.]*$"));
        cont.add(tableSearch).growX();
        cont.row();

        // Main Screen with the results | options
        Table screen = cont.table().grow().get();

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
        info.label(() -> "@context.search-function.info");
        info.row();
        info.add(infoContent).grow();

        optionsSide.row();

        // Filters
        Table filters = optionsSide.table().grow().get();
        filters.label(() -> "@context.search-function.filters");
        filters.row();
        filters.setBackground(Tex.button);
        filters.add(checkOnlyAvailable).left().padTop(5).get().changed(this::search);
        checkMethods.setChecked(true);
        filters.row();
        filters.add(checkMethods).left().padTop(5).get().changed(this::search);
        checkMethods.setChecked(true);
        filters.row();
        filters.add(checkFields).left().padTop(5).get().changed(this::search);
        checkFields.setChecked(true);

        search();
    }

    private void search() {
        String toSearch = searchField.getText();
        String path = null;
        if (toSearch.contains(".")) path = toSearch.substring(0, toSearch.lastIndexOf("."));

        resultsShow.clear();
        boolean success = displaySearch(toSearch, "");
        if (!success && path != null) {
            int i = toSearch.lastIndexOf(".") + 1;
            if (i < toSearch.length()) displaySearch(path, toSearch.substring(i));
            else displaySearch(path, "");
        }
    }

    private boolean displaySearch(String toSearch, String starts) {
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
            infoContent.label(() -> Core.bundle.get("context.not-found"));
            return false;
        }

        if (obj instanceof rhino.NativeJavaClass nObj) {
            Class<?> cl;
            try {
                cl = (Class<?>) nObj.unwrap();
            } catch (ClassCastException e) {
                return false;
            }

            // ! To change
            infoContent.label(() -> "Type: Class");
            infoContent.row();
            infoContent.label(() -> "Name: " + cl.getName());
            createButtonsFromClass(toSearch, starts, cl, availableKeys);
            return true;
        }

        if (obj instanceof rhino.NativeJavaObject nObj) {
            Class<?> cl = nObj.unwrap().getClass();

            // ! To change
            infoContent.label(() -> "Type: Instance");
            infoContent.row();
            infoContent.label(() -> "Class: " + cl.getName());
            createButtonsFromClass(toSearch, starts, cl, availableKeys);
            return true;
        }

        if (obj instanceof rhino.NativeJavaMethod nObj) {

            // ! To change
            infoContent.label(() -> "Type: Method");
            infoContent.row();
            infoContent.label(() -> "Class: <report this error>");
            String[] values = nObj.toString().split("\n");
            for (String value : values) {
                resultsShow.add(new Label(value.trim())).growX();
                resultsShow.row();
            }
            return true;
        }

        if(obj instanceof rhino.NativeObject) {
            // ! To change
            infoContent.label(() -> "Type: Object");
            for(String key : availableKeys) {
                ButtonInfo btn = new ButtonInfo(key);
                btn.setPath(toSearch + "." + key);
                btn.addTo(resultsShow, () -> {
                    searchField.setText(btn.getPath());
                    search();
                });
            }
            if(availableKeys.isEmpty()) resultsShow.add(LABEL_EMPTY);
            return true;
        }

        if(obj instanceof java.lang.String || obj instanceof java.lang.Number || obj instanceof java.lang.Boolean) {
            // ! To change
            infoContent.label(() -> "Type: " + obj.getClass().getSimpleName());
            infoContent.row();
            if(obj instanceof java.lang.String str) {
                str = str.replaceAll("[\n\t]"," ");
                if(str.length() > 20) str = str.substring(0,17)+"...";
                infoContent.add(new Label("Value: " + str));
            } else {
            infoContent.label(() -> "Value: " + obj);
            }
            return true;
        }

        // ! To change
        infoContent.label(() -> "Type: Unknown");
        infoContent.row();
        infoContent.label(() -> "JavaClass: "+obj.getClass().getName());
        return true;
    }

    private static Object execute(String code) {
        Scripts s = Vars.mods.getScripts();
        return s.context.evaluateString(s.scope, code, "SearchTerms", 1);
    }
    private void createButtonsFromClass(String toSearch, String starts, Class<?> cl, List<String> availableKeys) {
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

    private static class ButtonInfo {
        public final String text;
        private String path = null;
        private ButtonInfoType type = ButtonInfoType.UNKNOWN;

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
    }

    enum ButtonInfoType {
        METHOD("[red]\uE282[] "),
        FIELD("[gold]\uE286[] "),
        UNKNOWN("[purple]\uEE89[] ");

        public final String v;
        ButtonInfoType(String v) {
            this.v = v;
        }
    }
}
