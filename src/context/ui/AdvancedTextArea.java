package context.ui;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.graphics.g2d.ScissorStack;
import arc.input.KeyCode;
import arc.math.geom.Rect;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextArea;
import arc.struct.*;
import arc.util.Align;
import arc.util.Nullable;
import mindustry.gen.Tex;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancedTextArea extends TextArea {

    public ArrayList<ColorPoint> colors = new ArrayList<>();
    public ArrayList<ColorPoint> underline = new ArrayList<>();
    public Lexer lexer = new Lexer();
    public HistoryManager history = new HistoryManager();
    public KeybindManager keybindManager = new KeybindManager();
    private static final TextureRegionDrawable underlineTex = (TextureRegionDrawable) Tex.whiteui;
    public ScrollPane parentScroll = null;
    public String highlightingType = "js";

    public Cons<String> onUpdateDisplay;

    public AdvancedTextArea(String newText) {
        super(newText);
        focusTraversal = false;
        onlyFontChars = false;

    }

    @Override
    public float getPrefHeight() {
        float pref = getLines() * style.font.getLineHeight();
        return pref + style.font.getLineHeight();
    }

    public void deleteTextRaw(int startIndex, int size) {
        if (text == null || text.isEmpty()) return;
        if (startIndex < 0 || startIndex >= text.length()) return;
        int endIndex = Math.min(startIndex + size, text.length());
        StringBuilder sb = new StringBuilder(text);
        sb.delete(startIndex, endIndex);
        text = sb.toString();
        updateDisplayText();
    }

    public void insertTextRaw(int index, String str) {
        if (text == null) {
            text = str;
        } else {
            if (index < 0 || index > text.length()) {
                index = text.length();
            }
            StringBuilder sb = new StringBuilder(text);
            sb.insert(index, str);
            text = sb.toString();
        }
        updateDisplayText();
    }

    public int binarySearchListIndex(int charIndex, ArrayList<ColorPoint> list) {
        int left = 0;
        int right = list.size() - 1;
        int resultIndex = -1;
        while (left < right) {
            int mid = right - (right - left) / 2;
            if (list.get(mid).charIndex == charIndex) {
                return mid;
            } else if (list.get(mid).charIndex > charIndex) {
                right = mid - 1;
            } else {
                resultIndex = mid; // potential candidate
                left = mid;
            }
        }
        return resultIndex;
    }

    public int i = 0;

    @Override
    protected void drawText(Font font, float x, float y) {
        if (text == null || text.isEmpty()) return;

        float lineHeight = font.getLineHeight();
        int totalLines = getLines();

        Rect scissor = ScissorStack.peek();

        int startLine = 0;
        int endLine = totalLines;
        if (scissor != null) {
            startLine = (int) ((y - scissor.height) / lineHeight) - 1;
            endLine = (int) (y / lineHeight) + 1;
        }

        startLine = Math.max(0, startLine);
        endLine = Math.min(totalLines, endLine + 1);

        if (startLine >= endLine) return;

        boolean had = font.getData().markupEnabled;
        font.getData().markupEnabled = false;
        float offsetY = -startLine * lineHeight;
        String replacedText = displayText.toString().replaceAll("\t", " ");

        Color defaultColor = style.fontColor;
        int lineBreakIndex = startLine * 2;
        if (lineBreakIndex > linesBreak.size) {
            return;
        }
        int charIndex = linesBreak.get(lineBreakIndex);
        int colorIndex = binarySearchListIndex(charIndex, colors);
        font.setColor(defaultColor);
        float offsetX = 0;
        float gliphwidth = this.glyphPositions.size > 1 ? this.glyphPositions.get(1) : 0;
        while (lineBreakIndex < (endLine) * 2 && lineBreakIndex < linesBreak.size) {
            int charEndIndex = linesBreak.get(lineBreakIndex + 1);
            if (colors.size() > colorIndex + 1) {
                int charColorEnd = colors.get(colorIndex + 1).charIndex;

                if (charColorEnd <= charEndIndex) {
                    if (charIndex > charColorEnd) {
                        colorIndex++;
                        font.setColor(colors.get(colorIndex).color);
                        continue;
                    }
                    font.draw(replacedText, x + offsetX, y + offsetY, charIndex, charColorEnd, 0, Align.left, false);
                    offsetX += ((charColorEnd - charIndex) * gliphwidth);
                    charIndex = charColorEnd;
                    colorIndex++;
                    font.setColor(colors.get(colorIndex).color);
                    continue; // Re-evaluate the line with updated charIndex
                }
            }
            font.draw(replacedText, x + offsetX, y + offsetY, charIndex, charEndIndex, 0, Align.left, false);
            offsetX = 0;
            charIndex = charEndIndex + 1;
            font.setColor(defaultColor);
            offsetY -= font.getLineHeight();
            lineBreakIndex += 2;
        }

        lineBreakIndex = startLine * 2;
        charIndex = linesBreak.get(lineBreakIndex);
        colorIndex = binarySearchListIndex(charIndex, underline);
        Color currentColor = colorIndex == -1 ? null : underline.get(colorIndex).color;
        offsetX = 0;
        offsetY = ((float) -startLine - 0.8f) * lineHeight;
        gliphwidth = this.glyphPositions.size > 1 ? this.glyphPositions.get(1) : 0;
        while (lineBreakIndex < (endLine) * 2 && lineBreakIndex < linesBreak.size) {
            int charEndIndex = linesBreak.get(lineBreakIndex + 1);
            if (underline.size() > colorIndex + 1) {
                // Exists a next color
                int charColorEnd = colors.get(colorIndex + 1).charIndex;

                if (charColorEnd < charEndIndex) {
                    // The color change is within this line
                    if (currentColor != null) {
                        underlineTex.tint(currentColor).draw(x + offsetX, y + offsetY, (charColorEnd - charIndex) * gliphwidth, font.getLineHeight() * 0.2f);
                    }
                    offsetX += ((charColorEnd - charIndex) * gliphwidth);
                    charIndex = charColorEnd;
                    colorIndex++;
                    currentColor = underline.get(colorIndex).color;

                    continue; // Re-evaluate the line with updated charIndex
                }
            }
            if (currentColor != null) {
                underlineTex.tint(currentColor).draw(x + offsetX, y + offsetY, (charEndIndex - charIndex) * gliphwidth, font.getLineHeight() * 0.2f);
            }
            offsetX = 0;
            charIndex = charEndIndex + 1;
            offsetY -= font.getLineHeight();
            lineBreakIndex += 2;
        }
        underlineTex.tint(Color.white);
        font.getData().markupEnabled = had;
    }

    @Override
    protected InputListener createInputListener() {
        return new AdvancedTextAreaListener();
    }

    public void setColor(Color color, int startIndex, int endIndex) {
        Color lastColor = style.fontColor;
        int indexPut = binarySearchListIndex(startIndex, colors) + 1;
        while (indexPut < colors.size() && colors.get(indexPut).charIndex < endIndex) {
            lastColor = colors.get(indexPut).color;
            colors.remove(indexPut);
        }

        // Insert new color point
        colors.add(indexPut, new ColorPoint(startIndex, color));
        if (!lastColor.equals(color)) {
            // No need to add end point
            colors.add(indexPut + 1, new ColorPoint(endIndex, lastColor));
        }
    }

    public void setUnderline(@Nullable Color color, int startIndex, int endIndex) {
        Color lastColor = null;
        int indexPut = binarySearchListIndex(startIndex, underline) + 1;
        while (indexPut < underline.size() && underline.get(indexPut).charIndex < endIndex) {
            lastColor = underline.get(indexPut).color;
            underline.remove(indexPut);
        }

        // Insert new color point
        if (color != null) {
            underline.add(indexPut, new ColorPoint(startIndex, color));
            if (lastColor != color) {
                // No need to add end point
                underline.add(indexPut + 1, new ColorPoint(endIndex, lastColor));
            }
        } else {
            // Just remove underline
            if (lastColor != null) {
                underline.add(indexPut, new ColorPoint(endIndex, lastColor));
            }
        }
    }

    @Override
    public void setText(String str) {
        super.setText(str);
    }

    public String lineNumbers() {
        if (text == null || text.isEmpty()) return "1\n";
        StringBuilder sb = new StringBuilder();
        int lineNumber = firstLineShowing + 1;
        sb.append(lineNumber).append("\n");
        for (int i = 0; i < linesShowing; i++) {
            if (linesBreak.size <= (firstLineShowing + i) * 2) break;
            int index = linesBreak.get((firstLineShowing + i) * 2 + 1);
            if (index >= text.length()) break;
            char c = text.charAt(index);
            if (c == '\n') {
                lineNumber++;
                sb.append(lineNumber);
            }
            sb.append('\n');
            if (linesBreak.get((firstLineShowing + i) * 2 + 1) >= text.length()) {
                break;
            }
        }
        return sb.toString();
    }

    public void ensureCursorVisible() {
        if (text == null || text.isEmpty() || parentScroll == null) return;

        int currentLine = 0;
        int limit = Math.min(cursor, text.length());
        for (int j = 0; j < limit; j++) {
            if (text.charAt(j) == '\n') currentLine++;
        }

        float lineHeight = style.font.getLineHeight();
        float lineY = getHeight() - ((currentLine + 1) * lineHeight);

        float lh = style.font.getLineHeight();
        float margin = lh * 3;
        parentScroll.scrollTo(-margin, lineY - margin, margin * 2, margin * 2 + lh);
        firstLineShowing = 0;
    }

    @Override
    protected void updateDisplayText() {
        if (lexer != null) lexer.apply(this);
        if (onUpdateDisplay != null) onUpdateDisplay.get(text);


        Font.FontData data = style.font.getData();
        int textLength = text.length();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < textLength; i++) {
            char c = text.charAt(i);
            buffer.append(data.hasGlyph(c) ? c : ' ');
        }
        displayText = buffer.toString();

        layout.setText(style.font, displayText.toString().replace('\n', ' ').replace('\r', ' '));
        glyphPositions.clear();
        float x = 0;
        if (layout.runs.size > 0) {
            GlyphLayout.GlyphRun run = layout.runs.first();
            FloatSeq xAdvances = run.xAdvances;
            fontOffset = xAdvances.first();
            for (int i = 1, n = xAdvances.size; i < n; i++) {
                glyphPositions.add(x);
                x += xAdvances.get(i);
            }
        } else {
            fontOffset = 0;
        }
        glyphPositions.add(x);
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
                if (deleteLastChar) sb.deleteCharAt(sb.length() - 1);
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


            if (Core.input.ctrl() && !Core.input.alt()) {
                if (keycode == KeyCode.z) {
                    history.undo(AdvancedTextArea.this);
                    return true;
                }
                if (keycode == KeyCode.y) {
                    history.redo(AdvancedTextArea.this);
                    return true;
                }
                // This still buggy because of the firstLineShowing updating (idk why)
                if (keycode == KeyCode.v) {
                    String str = Core.app.getClipboardText();
                    history.addChange(HistoryManager.TextChange.Type.INSERT, cursor, str);
                    paste(str, false);
                    return true;
                }
            }
            if (keycode == KeyCode.backspace) {
                if (hasSelection) {
                    int start = Math.min(cursor, selectionStart);
                    int end = Math.max(cursor, selectionStart);
                    String deleted = text.substring(start, end);
                    history.addChange(HistoryManager.TextChange.Type.DELETE, start, deleted);
                } else if (cursor > 0) {
                    String deleted = String.valueOf(text.charAt(cursor - 1));
                    history.addChange(HistoryManager.TextChange.Type.DELETE, cursor - 1, deleted);
                }
                return super.keyDown(event, KeyCode.backspace);
            }

            ensureCursorVisible();
            return super.keyDown(event, keycode);
        }

        @Override
        protected void setCursorPosition(float x, float y) {
            moveOffset = -1;

            Drawable background = style.background;

            if (background != null) {
                x -= background.getLeftWidth();
            }
            x = Math.max(0, x);
            if (background != null) {
                y -= background.getTopHeight();
            }

            super.setCursorPosition(x, y);
        }

        @Override
        public boolean keyTyped(InputEvent event, char character) {
            if (character == TAB && hasSelection) {
                updateDisplayText();
                return true;
            }


            if (character >= 32 && character != DELETE) {
                if (hasSelection) {
                    int start = Math.min(cursor, selectionStart);
                    int end = Math.max(cursor, selectionStart);
                    String deleted = text.substring(start, end);
                    history.addChange(HistoryManager.TextChange.Type.DELETE, start, deleted);
                    history.addChange(HistoryManager.TextChange.Type.INSERT, start, String.valueOf(character));
                } else {
                    history.addChange(HistoryManager.TextChange.Type.INSERT, cursor, String.valueOf(character));
                }
            }
            if (character == DELETE) {
                if (hasSelection) {
                    int start = Math.min(cursor, selectionStart);
                    int end = Math.max(cursor, selectionStart);
                    String deleted = text.substring(start, end);
                    history.addChange(HistoryManager.TextChange.Type.DELETE, start, deleted);
                } else if (cursor < text.length()) {
                    // DEL simples
                    String deleted = String.valueOf(text.charAt(cursor));
                    history.addChange(HistoryManager.TextChange.Type.DELETE, cursor, deleted);
                }
            }
            if (character == '\n' || character == '\r') {
                if (hasSelection) {
                    int start = Math.min(cursor, selectionStart);
                    int end = Math.max(cursor, selectionStart);
                    String deleted = text.substring(start, end);
                    history.addChange(HistoryManager.TextChange.Type.DELETE, start, deleted);
                }
                history.addChange(HistoryManager.TextChange.Type.INSERT, cursor, "\n");
            }


            return super.keyTyped(event, character);
        }

        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
            ensureCursorVisible();
            super.enter(event, x, y, pointer, fromActor);
        }
    }

    public static class ColorPoint {
        public int charIndex;
        public Color color;

        public ColorPoint(int charIndex, Color color) {
            this.charIndex = charIndex;
            this.color = color;
        }

        @Override
        public String toString() {
            return "ColorPoint{" +
                    "charIndex=" + charIndex +
                    ", color=" + color +
                    '}';
        }
    }

    public static class Lexer {
        private static final Color
                COLOR_DEFAULT = Color.white,
                COLOR_KEYWORD = Color.valueOf("ff79c6"),
                COLOR_STRING = Color.valueOf("f1fa8c"),
                COLOR_COMMENT = Color.valueOf("6272a4"),
                COLOR_NUMBER = Color.valueOf("bd93f9"),
                COLOR_SPECIAL = Color.valueOf("8be9fd");

        private static final ObjectMap<String, Pattern> patterns = ObjectMap.of(
          "js", Pattern.compile(
            "(?<COMMENT>//.*|/\\*[\\s\\S]*?\\*/)" +
              "|(?<STRING>\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|`(?:[^`\\\\]|\\\\.)*`)" +
              "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)" +
              "|(?<KEYWORD>\\b(?:function|var|let|const|if|else|for|while|return|switch|case|break|continue|new|try|catch|class|extends|import|export|default)\\b)" +
              "|(?<SPECIAL>\\b(?:true|false|null|undefined|this|super)\\b)"
          ),
//          "glsl", Pattern.compile(
//            "(?<COMMENT>//.*|/\\*[\\s\\S]*?\\*/)" +
//              "|(?<STRING>\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|`(?:[^`\\\\]|\\\\.)*`)" +
//              "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)" +
//              "|(?<KEYWORD>\\b(?:function|int|float|if|else|for|while|return|switch|case|break|continue|new|try|catch|class|main|uniform|varying)\\b)" +
//              "|(?<SPECIAL>\\b(?:true|false|vec2|vec3|vec4|main)\\b)"
//          )
          "glsl", Pattern.compile(
            "(?<COMMENT>//.*|/\\*[\\s\\S]*?\\*/)" +
              "|(?<STRING>\\b(?:if|for)\\b)" +
              "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)" +
              "|(?<KEYWORD>\\b(?:function|void|discard|uniform|varying|attribute|const|define)\\b)" +
              "|(?<SPECIAL>\\b(?:true|false|vec2|vec3|vec4|int|float|sampler2D|main)\\b)"
          )
        );

//        private static final Pattern PATTERN = Pattern.compile(
//                "(?<COMMENT>//.*|/\\*[\\s\\S]*?\\*/)" +
//                        "|(?<STRING>\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|`(?:[^`\\\\]|\\\\.)*`)" +
//                        "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)" +
//                        "|(?<KEYWORD>\\b(?:function|var|let|const|if|else|for|while|return|switch|case|break|continue|new|try|catch|class|extends|import|export|default)\\b)" +
//                        "|(?<SPECIAL>\\b(?:true|false|null|undefined|this|super)\\b)"
//        );

        public Lexer() {
        }

        public void apply(AdvancedTextArea textArea) {
            String text = textArea.getText();

            // 1. Limpar formatação antiga
            textArea.colors.clear();
            textArea.underline.clear();

            // 2. Adicionar cor padrão no início (índice 0)
            // Isso garante que qualquer coisa que não seja token fique com a cor padrão
            textArea.colors.add(new ColorPoint(0, COLOR_DEFAULT));

            if (text == null || text.isEmpty()) return;

//            Matcher matcher = PATTERN.matcher(text);
            Matcher matcher = patterns.get(textArea.highlightingType).matcher(text);

            // 3. Iterar sobre todos os tokens encontrados
            while (matcher.find()) {
                Color color = null;

                // Verificar qual grupo da Regex deu match
                if (matcher.group("COMMENT") != null) {
                    color = COLOR_COMMENT;
                } else if (matcher.group("STRING") != null) {
                    color = COLOR_STRING;
                } else if (matcher.group("NUMBER") != null) {
                    color = COLOR_NUMBER;
                } else if (matcher.group("KEYWORD") != null) {
                    color = COLOR_KEYWORD;
                } else if (matcher.group("SPECIAL") != null) {
                    color = COLOR_SPECIAL;
                }

                // Se encontrou um token válido
                if (color != null) {
                    int start = matcher.start();
                    int end = matcher.end();

                    // Adiciona o ponto de cor onde começa o token
                    addOrUpdateColor(textArea, start, color);

                    // Adiciona o ponto de cor onde termina o token (voltando para o padrão)
                    // Mas só se o texto continuar depois
                    if (end < text.length()) {
                        addOrUpdateColor(textArea, end, COLOR_DEFAULT);
                    }
                }
            }
        }

        /**
         * Auxiliar method to add or update a color point in the list without using binary search.
         */
        private void addOrUpdateColor(AdvancedTextArea area, int index, Color color) {
            ArrayList<ColorPoint> list = area.colors;

            // Se a lista não está vazia e o último ponto é no mesmo índice, atualiza a cor
            if (!list.isEmpty()) {
                ColorPoint last = list.get(list.size() - 1);
                if (last.charIndex == index) {
                    last.color = color;
                    return;
                }
                // Otimização: Se a cor nova é igual a cor anterior, não precisa adicionar ponto novo
                if (last.color.equals(color)) {
                    return;
                }
            }

            list.add(new ColorPoint(index, color));
        }
    }
}

