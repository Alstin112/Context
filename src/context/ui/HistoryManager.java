package context.ui;

import java.util.LinkedList;

public class HistoryManager {
    private final LinkedList<TextChange> undoStack = new LinkedList<>();
    private final LinkedList<TextChange> redoStack = new LinkedList<>();
    private final int maxHistory = 200;
    private boolean isLocked = false;

    public void addChange(TextChange.Type type, int index, String text) {
        if (isLocked) return;

        redoStack.clear();
        TextChange newChange = new TextChange(type, index, text);

        if (!undoStack.isEmpty()) {
            TextChange last = undoStack.getLast();
            if (last.merge(newChange)) {
                return;
            }
        }

        undoStack.add(newChange);
        if (undoStack.size() > maxHistory) {
            undoStack.removeFirst();
        }
    }

    public void undo(AdvancedTextArea area) {
        if (undoStack.isEmpty()) return;

        isLocked = true;
        TextChange change = undoStack.removeLast();
        redoStack.add(change);

        if (change.type == TextChange.Type.INSERT) {
            area.deleteTextRaw(change.index, change.text.length());
            area.setCursorPosition(change.index);
        } else {
            area.insertTextRaw(change.index, change.text);
        }
        isLocked = false;
    }

    public void redo(AdvancedTextArea area) {
        if (redoStack.isEmpty()) return;

        isLocked = true;
        TextChange change = redoStack.removeLast();
        undoStack.add(change);

        if (change.type == TextChange.Type.INSERT) {
            // Refazer inserção = Inserir novamente
            area.insertTextRaw(change.index, change.text);
        } else {
            // Refazer deleção = Deletar novamente
            area.deleteTextRaw(change.index, change.text.length());
        }
        isLocked = false;
    }

    public static class TextChange {
        public enum Type { INSERT, DELETE }

        public Type type;
        public int index;
        public String text;
        public long timestamp;

        public TextChange(Type type, int index, String text) {
            this.type = type;
            this.index = index;
            this.text = text;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean merge(TextChange next) {
            if (type != next.type || next.timestamp - timestamp > 1000) return false;

            if (type == Type.INSERT) {
                if (next.index == index + text.length()) {
                    text += next.text;
                    timestamp = next.timestamp;
                    return true;
                }
            } else if (type == Type.DELETE) {
                if (next.index == index - next.text.length()) {
                    text = next.text + text;
                    index = next.index;
                    timestamp = next.timestamp;
                    return true;
                }
                if (next.index == index) {
                    text += next.text;
                    timestamp = next.timestamp;
                    return true;
                }
            }
            return false;
        }
    }
}