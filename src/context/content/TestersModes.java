package context.content;

import arc.graphics.Color;
import arc.math.Mathf;

public enum TestersModes {
    EMPTY(Color.white.rgba8888()),
    ACTIVE(Color.gold.rgba8888()),
    INACTIVE(Color.gray.rgba8888()),
    COMPILER_ERROR(Color.red.rgba8888(), 0x93240eff, 4f),
    RUNTIME_ERROR(Color.red.rgba8888(), 0x93240eff, 4f);

    public final int color;
    public final int colorRange;
    public final float frequency;

    TestersModes(int color) {
        this.colorRange = this.color = color;
        this.frequency = 0f;
    }

    TestersModes(int color, int colorRange, float frequency) {
        this.color = color;
        this.colorRange = colorRange;
        this.frequency = frequency;
    }

    public Color getColor() {
        return new Color(color);
    }

    public Color getColor(float time) {
        if(frequency == 0f) return new Color(color);
        return new Color(color).lerp(new Color(colorRange), Mathf.absin(time, frequency, 1f));
    }
}
