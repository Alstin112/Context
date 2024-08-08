package context.ui.dialogs;

import arc.Core;
import arc.func.FloatFloatf;
import arc.graphics.Color;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Interp;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextArea;
import arc.scene.ui.layout.Table;
import arc.util.*;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.mod.Scripts;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import rhino.Function;

import java.lang.reflect.Field;

public class FunctionAnalyzerDialog {

    private static final BaseDialog bd = new BaseDialog("@block.context-function-analyzer.name");
    private static float[] values = new float[201];
    private static float[] valuesHigh = new float[201];
    private static float graphUnitHeight = 0;
    private static float graphUnitWidth = 0;
    private static float graphOriginX = 0;
    private static float graphOriginY = 0;
    protected static long lastDisplayChange = 0;
    private static boolean isValid = false;
    private static float zoom = 1;
    private static float offsetX = 0;
    private static final Element graphic = new Element() {
        @Override
        public void draw() {
            float min = 0;
            float max = 1;
            for (float value : values) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            float width = this.getWidth();
            float height = this.getHeight();
            float defStroke = Lines.getStroke();
            float margin = 30f;
            float graphX = this.x + margin;
            graphOriginX = this.x + margin;
            graphOriginY = this.y + height * 0.2f;
            graphUnitWidth = width - margin*2;
            graphUnitHeight = height * 0.6f;

            if (min == 0) {
                Fonts.def.draw("0", this.x, this.y, 0.2f, Align.left, false);
            } else {
                Fonts.def.draw(String.valueOf(min), this.x, this.y, 0.2f, Align.left, false);
                Fonts.def.draw("0", this.x, this.y - height * min / (max - min), 0.2f, Align.left, false);
            }

            if (max == 1) {
                Fonts.def.draw("1", this.x, this.y + height, 0.2f, Align.left, false);
            } else {
                Fonts.def.draw(String.valueOf(max), this.x, this.y + height, 0.2f, Align.left, false);
                Fonts.def.draw("1", this.x, this.y + height * (1 - min) / (max - min), 0.2f, Align.left, false);
            }

            Lines.stroke(1, Color.gray);
            if (max - min <= 2) {
                for (int i = (int) Math.ceil(min * 5); i < max * 5; i++) {
                    float h = this.y + height * (i * 0.2f - min) / (max - min);
                    Lines.line(graphX, h, graphX + graphUnitWidth, h);
                }
            }


            for (int i = (int) Math.ceil(offsetX*5); i <= (int) Math.floor(offsetX*5+zoom*5); i++) {
                Lines.line(graphX + graphUnitWidth * (i / 5f - offsetX) / zoom, this.y, graphX + graphUnitWidth * (i / 5f - offsetX) / zoom, this.y + height);
            }

            Lines.stroke(3, Color.white);
            Lines.line(graphX, this.y, graphX + graphUnitWidth, this.y);
            Lines.line(graphX, this.y + height, graphX + graphUnitWidth, this.y + height);
            if (min != 0)
                Lines.line(graphX, this.y - height * min / (max - min), graphX + graphUnitWidth, this.y - height * min / (max - min));
            if (max != 1)
                Lines.line(graphX, this.y + height * (1 - min) / (max - min), graphX + graphUnitWidth, this.y + height * (1 - min) / (max - min));

            Lines.stroke(4, Color.cyan);
            Lines.beginLine();

            if(lastDisplayChange != -1 && lastDisplayChange < Time.millis()-1000) {
                FunctionAnalyzerDialog.getPointsHigh();
                lastDisplayChange = -1;
            }

            if(lastDisplayChange == -1) {
                for (int i = 0; i < valuesHigh.length; i++) {
                    Lines.linePoint(graphX + (float) i / (valuesHigh.length - 1) * graphUnitWidth, this.y + height * (valuesHigh[i] - min) / (max - min));
                }
            } else {
                int startIndex = (int) Math.floor(offsetX*(values.length-1));
                int endIndex   = (int) Math.ceil((offsetX+zoom)*(values.length-1));
                float startHeight = Interp.linear.apply(values[startIndex],values[startIndex+1], offsetX * (values.length - 1) - startIndex);
                float endHeight = Interp.linear.apply(values[endIndex],values[endIndex-1], endIndex - (offsetX+zoom) * (values.length - 1));

                Lines.linePoint(graphX, this.y + height * (startHeight - min) / (max - min));
                for (int i = startIndex+1; i < endIndex-1; i++) {
                    Lines.linePoint(graphX + graphUnitWidth*(i /(values.length - 1f)-offsetX)/zoom, this.y + height * (values[i] - min) / (max - min));
                }
                Lines.linePoint(graphX + graphUnitWidth, this.y + height * (endHeight - min) / (max - min));
            }

            Lines.endLine();
            Lines.stroke(defStroke, Color.white);
        }

    };

    private static final TextArea code = new TextArea("return t*t");

    private static FloatFloatf function = x -> x * x;

    static {
        graphic.addCaptureListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
                graphic.requestScroll();
                graphic.requestKeyboard();
            }
        });

        graphic.addListener(new InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                x = (x-30) / graphUnitWidth;
                float zoomAfter = Mathf.clamp((float) Math.pow(1.1, amountY)*zoom,1E-10f, 1f);
                if(zoom != zoomAfter) {
                    lastDisplayChange = Time.millis();
                    offsetX = Mathf.clamp(offsetX + x*(zoom - zoomAfter),0, 1 - zoomAfter);
                    zoom = zoomAfter;
                }
                return true;
            }

            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode) {
                float newOffsetX = offsetX;
                if (keycode == KeyCode.left) {
                    newOffsetX = Math.max(offsetX - 0.1f * zoom, 0);
                }
                if (keycode == KeyCode.right) {
                    newOffsetX = Math.min(offsetX + 0.1f * zoom, 1-zoom);
                }
                if(newOffsetX != offsetX) {
                    offsetX = newOffsetX;
                    lastDisplayChange = Time.millis();
                    return true;
                }
                return false;
            }
        });

        graphic.addListener(new ElementGestureListener() {
            @Override
            public void zoom(InputEvent event, float initialDistance, float distance) {
                lastDisplayChange = Time.millis();
                zoom = Mathf.clamp(zoom*distance/initialDistance,1E-10f, 1f);
                offsetX = Mathf.clamp(offsetX,0, 1 - zoom);
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                float newOffsetX = Mathf.clamp(offsetX - deltaX/graphUnitWidth*zoom, 0, 1-zoom);
                if(newOffsetX != offsetX) {
                    offsetX = newOffsetX;
                    lastDisplayChange = Time.millis();
                }
            }
        });

        code.setValidator(txt -> isValid);
        code.changed(FunctionAnalyzerDialog::getPoints);

        bd.cont.table(lvDivision -> {
            lvDivision.table(Tex.button, coding -> coding.add(code).grow().minHeight(200f)).grow();
            lvDivision.row();
            lvDivision.table(Tex.button, settings -> {
                Table area = new Table();
                area.defaults().grow();
                settings.table(tabs -> {
                    tabs.top();
                    tabs.defaults().grow();

                    Element boilerplate = createTabBoilerplate();
                    tabs.button("boilerplate", Styles.cleart, () -> {
                        area.clearChildren();
                        area.add(boilerplate);
                    }).row();

                    Element information = createTabInformation();
                    tabs.button("information", Styles.cleart, () -> {
                        area.clearChildren();
                        area.add(information);
                    }).row();


                }).growY().width(150f);
                settings.add(area).grow();
            }).grow();
        }).grow().colspan(2);
        bd.cont.table(Tex.button, graphicSide -> graphicSide.add(graphic).grow().colspan(2)).grow().colspan(3);

        bd.addCloseButton();
        getPoints();
    }

    private FunctionAnalyzerDialog() {

    }

    private static void getPoints() {
        Scripts s = Vars.mods.getScripts();
        try {
            String codeStr = "function(t){" + code.getText() + ";\n}";
            Function fn = s.context.compileFunction(s.scope, codeStr, "function-analyzer", 1);
            function = v -> ((Number) fn.call(s.context, s.scope, s.scope, new Object[]{v})).floatValue();
            float[] newValues = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                newValues[i] = function.get((float) i / (values.length - 1));
            }
            values = newValues;
            valuesHigh = newValues;
            isValid = true;
        } catch (Exception err) {
            isValid = false;
        }
    }

    private static void getPointsHigh() {
        Scripts s = Vars.mods.getScripts();
        try {
            String codeStr = "function(t){" + code.getText() + ";\n}";
            Function fn = s.context.compileFunction(s.scope, codeStr, "function-analyzer", 1);
            function = v -> ((Number) fn.call(s.context, s.scope, s.scope, new Object[]{v})).floatValue();
            float[] newValues = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                newValues[i] = function.get(offsetX + i*zoom / (values.length - 1));
            }
            valuesHigh = newValues;
            isValid = true;
        } catch (Exception err) {
            isValid = false;
        }
    }

    private static void setCode(String text) {
        code.setText(text);
        getPoints();
        lastDisplayChange = Time.millis();
    }

    private static Element createTabBoilerplate() {
        Table elementsInside = new Table(Styles.grayPanel).margin(0);
        Field[] fields = Interp.class.getFields();

        addButton(elementsInside, "INTERPO. BASICS", null);
        for (Field field : fields) {
            addButton(elementsInside, "Interp." + field.getName(), "return Interp." + field.getName() + ".apply(t);");
        }

        addButton(elementsInside, "USEFUL", null);
        addButton(elementsInside, "cyclicSine", "return 0.5-Mathf.cos(t,1/Mathf.PI2,0.5);");

        return new ScrollPane(elementsInside, Styles.defaultPane);
    }

    private static void addButton(Table tab, String name, String text) {
        if (text == null) {
            Label l = tab.add(name).height(40f).growX().center().get();
            l.setAlignment(Align.center);
            l.setScale(1.5f);
            l.setColor(Color.lightGray);
        } else {
            tab.button(name, Styles.cleart, () -> setCode(text)).height(30f).growX().row();
        }
        tab.row();
    }

    private static Element createTabInformation() {
        Table area = new Table();
        float delta = 0.0001f;
        area.label(() -> {
            Tmp.v1.set(Core.input.mouse());
            Element hit = Core.scene.hit(Tmp.v1.x, Tmp.v1.y, true);
            if (hit == null || !hit.isDescendantOf(graphic)) return "Put your mouse\nover the graphic";
            Tmp.v1.set((Tmp.v1.x - graphOriginX) / graphUnitWidth, (Tmp.v1.y - graphOriginY) / graphUnitHeight);
            Tmp.v1.x = Mathf.clamp(Tmp.v1.x, 0, 1);
            Tmp.v1.x = Tmp.v1.x*zoom + offsetX;

            StringBuilder sb = new StringBuilder();
            sb.append("X: ").append(Tmp.v1.x).append("\n");
            sb.append("Y: ").append(Tmp.v1.y).append("\n");

            sb.append("Val: ").append(function.get(Tmp.v1.x)).append("\n");

            sb.append("Der.: ");
            float derivate;
            if (Tmp.v1.x < delta) derivate = (function.get(2 * delta) - function.get(0)) / (2 * delta);
            else if (Tmp.v1.x > 1 - delta) derivate = (function.get(1) - function.get(1 - 2 * delta)) / (2 * delta);
            else derivate = (function.get(Tmp.v1.x + delta) - function.get(Tmp.v1.x - delta)) / (2 * delta);

            // append derivate rounded 3 decimal
            sb.append(Math.round(derivate * 1000) / 1000f).append("\n");

            return sb.toString();
        }).get();

        return area;
    }

    public static void show() {
        bd.show();
    }
}
