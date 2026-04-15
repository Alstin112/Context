package context.ui;

import arc.input.KeyCode;

public class KeybindManager {

    public static class KeybindCombination {
        boolean ctrl;
        boolean shift;
        boolean alt;
        KeyCode key;

        boolean ctrl2;
        boolean shift2;
        boolean alt2;
        KeyCode key2;

        public KeybindCombination(boolean ctrl, boolean shift, boolean alt, KeyCode key) {
            this.ctrl = ctrl;
            this.shift = shift;
            this.alt = alt;
            this.key = key;
            this.key2 = null;
        }

        public KeybindCombination(KeyCode key) {
            this(false, false, false, key);
        }

        public KeybindCombination(boolean ctrl, boolean shift, boolean alt, KeyCode key,
                                 boolean ctrl2, boolean shift2, boolean alt2, KeyCode key2) {
            this.ctrl = ctrl;
            this.shift = shift;
            this.alt = alt;
            this.key = key;
            this.ctrl2 = ctrl2;
            this.shift2 = shift2;
            this.alt2 = alt2;
            this.key2 = key2;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (ctrl) sb.append("Ctrl+");
            if (shift) sb.append("Shift+");
            if (alt) sb.append("Alt+");
            sb.append(key.name());

            if (key2 != null) {
                sb.append(" ");
                if (ctrl2) sb.append("Ctrl+");
                if (shift2) sb.append("Shift+");
                if (alt2) sb.append("Alt+");
                sb.append(key2.name());
            }

            return sb.toString();
        }


    }
}
