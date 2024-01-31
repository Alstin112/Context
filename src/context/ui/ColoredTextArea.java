package context.ui;

import arc.graphics.Color;
import arc.scene.ui.TextArea;

import java.util.ArrayList;

public class ColoredTextArea extends TextArea {

    // COLORS PALLET
    public Color[] colorPallet = new Color[]{};
    /**
     * Array storing the position of the color and the index of the color in the colorPallet
     */
    public ArrayList<Short> textColors = new ArrayList<>();

    public ColoredTextArea(String text){
        super(text);

    }

    public ColoredTextArea(String text, TextFieldStyle style){
        super(text, style);
    }

}
