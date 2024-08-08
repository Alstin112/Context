package context.ui.dialogs;

import arc.func.Cons;
import arc.scene.ui.CheckBox;
import arc.scene.ui.TextField;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationDialog {

    static final TextField.TextFieldValidator FloatValidator = txt -> {
        try {
            Float.parseFloat(txt);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    };

    static final TextField.TextFieldFilter FloatFilter = (textField, c) -> textField.getText().length() < 10 && (c >= '0' && c <= '9' || c == '.');
    public final BaseDialog bd;
    private Cons<Map<String, Object>> onClose;
    private Map<String, Object> values = new HashMap<>();

    public ConfigurationDialog(String title) {
        bd = new BaseDialog(title);
        bd.closeOnBack();
        bd.buttons.defaults().size(210f, 64f);
        bd.buttons.button("@back", Icon.left, ()->{
            onClose.get(values);
            bd.hide();
        }).size(210f, 64f);
    }

    public void addTitle(String title) {
        bd.cont.add(title).fontScale(1.3f).pad(30f).colspan(2).row();
    }
    public TextField addReadOnlyField(String label, String defaultValue) {
        TextField tf = new TextField(defaultValue);
        tf.setDisabled(true);
        bd.cont.add(label);
        bd.cont.add(tf).row();
        return tf;
    }

    /**
     * Adds a Text Field to the configuration page.
     * @param name the name of the configuration to refer later.
     * @param label the text that will display on side of the TextField.
     * @param defaultValue the default value of the TextField.
     * @return the TextField
     */
    public TextField addTextField(String name, String label, String defaultValue) {
        TextField tf = new TextField(defaultValue);
        tf.changed(() -> values.put(name, tf.getText()));
        values.put(name, defaultValue);
        bd.cont.add(label);
        bd.cont.add(tf).row();
        return tf;
    }

    /**
     * Adds a Float Field to the configuration page.
     * @param name the name of the configuration to refer later.
     * @param label the text that will display on side of the TextField.
     * @param defaultValue the default value of the TextField.
     * @return the TextField
     */
    public TextField addFloatInput(String name, String label, Float defaultValue) {
        TextField tf = new TextField(defaultValue.toString());
        tf.setValidator(FloatValidator);
        tf.setFilter(FloatFilter);
        tf.changed(() -> values.put(name, Float.parseFloat(tf.getText())));
        values.put(name, defaultValue);
        bd.cont.add(label);
        bd.cont.add(tf).row();
        return tf;
    }

    /**
     * Adds a Boolean Field to the configuration page.
     * @param name the name of the configuration to refer later.
     * @param label the text that will display on side of the CheckBox.
     * @param defaultValue the default value of the CheckBox.
     * @return the CheckBox
     */
    public CheckBox addBooleanInput(String name, String label, Boolean defaultValue) {
        CheckBox cb = new CheckBox("");
        cb.setChecked(defaultValue);
        cb.changed(() -> values.put(name, cb.isChecked()));
        values.put(name, defaultValue);
        bd.cont.add(label);
        bd.cont.add(cb).row();
        return cb;
    }

    public void show() {
        bd.show();
    }

    public void setOnClose(Cons<Map<String, Object>> onClose) {
        this.onClose = onClose;
    }
}
