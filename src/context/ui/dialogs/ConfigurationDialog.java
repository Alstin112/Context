package context.ui.dialogs;

import arc.func.Cons;
import arc.scene.ui.CheckBox;
import arc.scene.ui.TextField;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationDialog {

    /** Verifies if the txt is a valid float */
    static final TextField.TextFieldValidator FloatValidator = txt -> {
        try {
            Float.parseFloat(txt);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    };
    /** Verifies if the character is a valid float character to add*/
    static final TextField.TextFieldFilter FloatFilter = (textField, c) -> textField.getText().length() < 10 && (c >= '0' && c <= '9' || c == '.');
    public final BaseDialog bd;
    /** Runs when closing the configurations */
    private Cons<Map<String, Object>> onClose;
    /** The stored values */
    private final Map<String, Object> values = new HashMap<>();

    public ConfigurationDialog(String title) {
        bd = new BaseDialog(title);
        bd.closeOnBack();
        bd.buttons.defaults().size(210f, 64f);
        bd.buttons.button("@back", Icon.left, ()->{
            onClose.get(values);
            bd.hide();
        }).size(210f, 64f);
    }

    /**
     * Creates a separator between configurations
     * @param separator the text to be between
     */
    public void addSeparator(String separator) {
        bd.cont.add(separator).fontScale(1.3f).pad(30f).colspan(2).row();
    }

    /**
     * add a Text Field to the configuration page
     *
     * @param label the text that will display on side of the TextField
     * @param value the value of the TextField
     */
    public void addReadOnlyField(String label, String value) {
        TextField tf = new TextField(value);
        tf.setDisabled(true);
        bd.cont.add(label);
        bd.cont.add(tf).row();
    }

    /**
     * Adds a Text Field to the configuration page.
     * @param name the name of the configuration to refer later.
     * @param label the text that will display on side of the TextField.
     * @param defaultValue the default value of the TextField.
     */
    public void addTextField(String name, String label, String defaultValue) {
        TextField tf = new TextField(defaultValue);
        tf.changed(() -> values.put(name, tf.getText()));
        values.put(name, defaultValue);
        bd.cont.add(label);
        bd.cont.add(tf).row();
    }

    /**
     * Adds a FloatField to the configuration page.
     *
     * @param name         the name of the configuration to refer later.
     * @param label        the text that will display on side of the TextField.
     * @param defaultValue the default value of the TextField.
     */
    public void addFloatInput(String name, String label, Float defaultValue) {
        TextField tf = new TextField(defaultValue.toString());
        tf.setValidator(FloatValidator);
        tf.setFilter(FloatFilter);
        tf.changed(() -> values.put(name, Float.parseFloat(tf.getText())));
        values.put(name, defaultValue);
        bd.cont.add(label);
        bd.cont.add(tf).row();
    }

    /**
     * Adds a Boolean Field to the configuration page.
     *
     * @param name         the name of the configuration to refer later.
     * @param label        the text that will display on side of the CheckBox.
     * @param defaultValue the default value of the CheckBox.
     */
    public void addBooleanInput(String name, String label, Boolean defaultValue) {
        CheckBox cb = new CheckBox("");
        cb.setChecked(defaultValue);
        cb.changed(() -> values.put(name, cb.isChecked()));
        values.put(name, defaultValue);
        bd.cont.add(label);
        bd.cont.add(cb).row();
    }

    public void show() {
        bd.show();
    }

    public void setOnClose(Cons<Map<String, Object>> onClose) {
        this.onClose = onClose;
    }
}
