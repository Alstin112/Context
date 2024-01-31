package context.ui.elements;

import arc.Core;
import arc.scene.ui.Button;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Label;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.mod.Scripts;
import mindustry.ui.Styles;
import rhino.NativeArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

public class SearchFunction {

    public Table cont = new Table();
    private String classStr = "";
    private final TextField searchField = new TextField();
    private final Table resultsShow = new Table();
    private final CheckBox checkMethods = new CheckBox("[red]\uE282[] " + Core.bundle.get("context.search-function.methods"));
    private final CheckBox checkFields = new CheckBox("[gold]\uE286[] " + Core.bundle.get("context.search-function.fields"));


    public SearchFunction() {
        cont.label(() -> Core.bundle.format("context.search-function.class", classStr.isEmpty() ? Core.bundle.get("context.not-found") : classStr)).colspan(2);
        cont.row();

        Table tableSearch = new Table();
        tableSearch.label(() -> "@context.search");
        tableSearch.add(searchField).growX();
        searchField.changed(this::search);
        searchField.setValidator(s -> s.matches("^[\\w.]*$"));
        cont.add(tableSearch).growX();
        cont.row();

        Table screen = cont.table().grow().get();
        Table bigResult = screen.table().grow().colspan(3).get();
        bigResult.setBackground(Tex.button);
        bigResult.pane(resultsShow).grow();
        resultsShow.top();

        Table BigFilters = screen.table().grow().get();
        Table filters = BigFilters.table().get();
        filters.setBackground(Tex.button);
        filters.add(checkMethods).left().padTop(5).get().changed(this::search);
        checkMethods.setChecked(true);
        filters.row();
        filters.add(checkFields).left().padTop(5).get().changed(this::search);
        checkFields.setChecked(true);
    }

    private void search() {
        Scripts s = Vars.mods.getScripts();
        String toSearch = searchField.getText();
        String path = null;
        if (toSearch.contains(".")) path = toSearch.substring(0, toSearch.lastIndexOf("."));

        resultsShow.clear();
        boolean success = displaySearch(s, toSearch, "");
        if (!success && path != null) {
            int i = toSearch.indexOf(".")+1;
            if(i < toSearch.length()) displaySearch(s, path, toSearch.substring(i));
            else displaySearch(s, path, "");
        }

    }

    private boolean displaySearch(Scripts s, String toSearch, String starts) {
        String str;
        NativeArray keys;
        try {
            Object obj = s.context.evaluateString(s.scope,
                    "[typeof @,String(@), Object.keys(@).sort().reduce((a,b)=>[].concat(a,[b,typeof @[b]]),[])]".replaceAll("@", toSearch),
                    "Testing",
                    1
            );

            NativeArray list = (NativeArray) obj;
            String type = (String) list.get(0);
            str = (String) list.get(1);
            // If it's not an object or function, or it's a class, return
            if (!(type).equals("object") && !(type).equals("function") || str.startsWith("[object ")) return true;
            keys = (NativeArray) list.get(2);

        } catch (Throwable e) {
            return false;
        }

        ArrayList<String> strKeys = filterAndToString(keys, starts);

        if (isMethod(str)) {
            // Just cut the function name() {\* from the string
            String overLoadsTxt = str.substring(str.indexOf("/*") + 2, str.length() - 4);
            ArrayList<String> overLoads = Arrays.stream(overLoadsTxt.split("\n")).filter(s1 -> !s1.trim().isEmpty()).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            for (String overload : overLoads) {
                resultsShow.add(new Label(overload)).growX();
                resultsShow.row();
            }
            if (overLoads.isEmpty()) resultsShow.add(new Label("@context.search-function.empty-field"));
            return true;
        }
        if (isInstance(str)) {
            classStr = str.substring(11, str.length() - 1);
            for (int i = 0; i < strKeys.size(); i += 2) {
                addButton(strKeys.get(i), "function".equals(strKeys.get(i + 1)));
            }
            if (strKeys.isEmpty()) resultsShow.add(new Label("@context.search-function.empty-field"));
            return true;
        }
        if (isClass(str)) {
            classStr = str.substring(0, str.indexOf("@"));
            for (int i = 0; i < strKeys.size(); i += 2) {
                addButton(strKeys.get(i), "function".equals(strKeys.get(i + 1)));
            }
            if (strKeys.isEmpty()) resultsShow.add(new Label("@context.search-function.empty-field"));
            return true;
        }

        return false;
    }

    private static ArrayList<String> filterAndToString(NativeArray keys, String starts) {
        ArrayList<String> strKeys = new ArrayList<>();
        if (starts.isEmpty()) {
            for (int i = 0; i < keys.getLength(); i += 1) {
                strKeys.add((String) keys.get(i));
            }
        } else {
            for (int i = 0; i < keys.getLength(); i += 2) {
                String key = (String) keys.get(i);
                if (!key.startsWith(starts)) continue;
                strKeys.add(key);
                strKeys.add((String) keys.get(i + 1));
            }
        }
        return strKeys;
    }

    private static boolean isClass(String str) {
        return Pattern.matches("^([.\\w]+)@\\w+?$", str);
    }

    private static boolean isInstance(String str) {
        return str.startsWith("[JavaClass ");
    }

    private boolean isMethod(String str) {
        return str.startsWith("function ") && str.endsWith("\n*/}\n") && str.contains("/*");
    }

    private void addButton(String key, boolean isFunction) {
        Button btn = (Button) new Button(Styles.cleart).left();
        String txt = "";
        if (isFunction) {
            if (!checkMethods.isChecked()) return;
            txt = "[red]\uE282[] " + key;
        } else {
            if (!checkFields.isChecked()) return;
            txt = "[gold]\uE286[] " + key;
        }
        btn.add(new Label(txt));
        btn.clicked(() -> {
            searchField.setText(searchField.getText() + "." + key);
            search();
        });
        resultsShow.add(btn).growX();
        resultsShow.row();
    }

}
