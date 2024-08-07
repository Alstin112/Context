package context.ui;

import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.TextArea;

public class AdvancedTextArea extends TextArea {

    public AdvancedTextArea(String text) {
        super(text);

        addCaptureListener(new InputListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
                requestScroll();
            }
        });
        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                scroll((int) amountY);
                return true;
            }


        });
    }

    public void scroll(int lines) {
        firstLineShowing = Math.max(Math.min(firstLineShowing + lines, linesBreak.size/2-1),0);
    }

    @Override
    protected InputListener createInputListener() {
        return new AdvancedTextAreaListener();
    }

    public class AdvancedTextAreaListener extends TextAreaListener {

    }

}
