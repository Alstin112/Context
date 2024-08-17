package context.ui;

import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.TextArea;

public class AdvancedTextArea extends TextArea {

    public AdvancedTextArea(String newText) {
        super(newText);
        focusTraversal = false;
        onlyFontChars = false;

        addCaptureListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
                requestScroll();
            }
        });
    }

    public void scroll(int lines) {
        firstLineShowing = Math.max(Math.min(firstLineShowing + lines, linesBreak.size / 2 - 1), 0);
    }

    @Override
    protected InputListener createInputListener() {
        return new AdvancedTextAreaListener();
    }

    public class AdvancedTextAreaListener extends TextAreaListener {
        @Override
        public boolean keyDown(InputEvent event, KeyCode keycode) {
            if (keycode == KeyCode.tab && hasSelection) {
                int start = Math.min(cursor, selectionStart);
                int end = Math.max(cursor, selectionStart);
                boolean deleteLastChar = true;

                String oldText = getText();
                StringBuilder sb = new StringBuilder();
                int totalAdded = 0;
                for (int index = 0; index < linesBreak.size; index += 2) {
                    if (linesBreak.get(index) >= end) {
                        deleteLastChar = false;
                        sb.append(oldText, linesBreak.get(index), oldText.length());
                        break;
                    }

                    if (linesBreak.get(index + 1) > start) {
                        sb.append('\t');
                        totalAdded++;
                    }
                    sb.append(oldText, linesBreak.get(index), linesBreak.get(index + 1));
                    sb.append("\n");
                }
                if(deleteLastChar) sb.deleteCharAt(sb.length() - 1);
                if (cursor == start) {
                    cursor += 1;
                    selectionStart += totalAdded;
                } else {
                    selectionStart += 1;
                    cursor += totalAdded;
                }

                text = sb.toString();
                scheduleKeyRepeatTask(keycode);
                return true;
            }
            return super.keyDown(event, keycode);
        }

        @Override
        public boolean keyTyped(InputEvent event, char character) {
            if (character == TAB && hasSelection) {
                updateDisplayText();
                return true;
            }
            return super.keyTyped(event, character);
        }

        @Override
        public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
            scroll((int) amountY);
            return true;
        }
    }

}
