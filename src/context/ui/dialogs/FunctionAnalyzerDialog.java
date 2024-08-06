package context.ui.dialogs;

import arc.Core;
import arc.func.FloatFloatf;
import arc.graphics.Color;
import arc.graphics.g2d.Lines;
import arc.math.Interp;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextArea;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.mod.Scripts;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import rhino.Function;

import java.lang.reflect.Field;

public class FunctionAnalyzerDialog {

    private static final BaseDialog bd = new BaseDialog("Function Visualizer");
    private static float[] values = new float[201];
    private static float graphUnitHeight = 0;
    private static float graphUnitWidth = 0;
    private static float graphOriginX = 0;
    private static float graphOriginY = 0;
    private static final Element graphic = new Element() {
        @Override
        public void draw() {
            float min = 0;
            float max = 1;
            for (int i = 0; i < values.length; i++) {
                min = Math.min(min, values[i]);
                max = Math.max(max, values[i]);
            }

            float width = this.getWidth();
            float height = this.getHeight();
            float defStroke = Lines.getStroke();
            float graphX = this.x + 30f;
            graphOriginX = this.x + 30f;
            graphOriginY = this.y + height * 0.2f;
            graphUnitWidth = width - 60f;
            graphUnitHeight = height*0.6f;

            Fonts.def.draw("1", this.x, this.y + height * 0.8f, 0.2f, Align.left, false);
            Fonts.def.draw("0", this.x, this.y + height * 0.2f, 0.2f, Align.left, false);

            Lines.stroke(1, Color.gray);
            Lines.line(graphX, this.y + height * 0.06f, graphX + graphUnitWidth, this.y + height * 0.06f);
            Lines.line(graphX, this.y + height * 0.32f, graphX + graphUnitWidth, this.y + height * 0.32f);
            Lines.line(graphX, this.y + height * 0.44f, graphX + graphUnitWidth, this.y + height * 0.44f);
            Lines.line(graphX, this.y + height * 0.56f, graphX + graphUnitWidth, this.y + height * 0.56f);
            Lines.line(graphX, this.y + height * 0.68f, graphX + graphUnitWidth, this.y + height * 0.68f);
            Lines.line(graphX, this.y + height * 0.92f, graphX + graphUnitWidth, this.y + height * 0.92f);

            Lines.line(graphX + graphUnitWidth * 0.2f, this.y, graphX + graphUnitWidth * 0.2f, this.y + height);
            Lines.line(graphX + graphUnitWidth * 0.4f, this.y, graphX + graphUnitWidth * 0.4f, this.y + height);
            Lines.line(graphX + graphUnitWidth * 0.6f, this.y, graphX + graphUnitWidth * 0.6f, this.y + height);
            Lines.line(graphX + graphUnitWidth * 0.8f, this.y, graphX + graphUnitWidth * 0.8f, this.y + height);

            Lines.stroke(2, Color.lightGray);
            Lines.line(graphX, this.y + height * 0.8f, graphX + graphUnitWidth, this.y + height * 0.8f);
            Lines.line(graphX + graphUnitWidth, this.y, graphX + graphUnitWidth, this.y + height);

            Lines.stroke(3, Color.white);
            Lines.line(graphX, this.y, graphX, this.y + height);
            Lines.line(graphX, this.y + height * 0.2f, graphX + graphUnitWidth, this.y + height * 0.2f);

            Lines.stroke(4, Color.cyan);
            Lines.beginLine();
            for (int i = 0; i < values.length; i++) {
                Lines.linePoint(graphX + (float) i / (values.length - 1) * graphUnitWidth, this.y + height * (values[i] * 0.6f + 0.2f));
            }
            Lines.endLine();
            Lines.stroke(defStroke, Color.white);
        }
    };

    private static final TextArea code = new TextArea("return t*t");

    private static FloatFloatf function = x -> x * x;

    static {

        code.setValidator(text -> {
            Scripts s = Vars.mods.getScripts();
            try {
                String codeStr = "function(t){" + text + ";\n}";
                Function fn = s.context.compileFunction(s.scope, codeStr, "function-analyzer", 1);
                // fn.call( context, scope, scope, Object[])
                function = v -> ((Number) fn.call(s.context, s.scope, s.scope, new Object[]{v})).floatValue();
                float[] newValues = new float[values.length];
                for (int i = 0; i < values.length; i++) {
                    newValues[i] = function.get((float) i / (values.length - 1));
                }
                values = newValues;
                return true;
            } catch (Exception err) {
                return false;
            }
        });

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
    }

    private FunctionAnalyzerDialog() {

    }

    private static Element createTabBoilerplate() {
        Table elementsInside = new Table(Styles.grayPanel).margin(0);
        Field[] fields = Interp.class.getFields();

        addButton(elementsInside, "INTERP BASICS", null);
        for (Field field : fields) {
            addButton(elementsInside, "Interp."+field.getName(), "return Interp." + field.getName() + ".apply(t);");
        }

        addButton(elementsInside, "USEFULL", null);
        addButton(elementsInside, "ciclicSine", "return 0.5-Mathf.cos(t,1/Mathf.PI2,0.5);");

        return new ScrollPane(elementsInside, Styles.defaultPane);
    }

    private static void addButton(Table tab,String name, String text) {
        if(text == null) {
            Label l = tab.add(name).height(40f).growX().center().get();
            l.setAlignment(Align.center);
            l.setScale(1.5f);
            l.setColor(Color.lightGray);
        } else {
            tab.button(name, Styles.cleart, () -> code.setText(text)).height(30f).growX().row();
        }
        tab.row();
    }

    private static Element createTabInformation() {
        Table area = new Table();
        float delta = 0.0001f;
        area.label(() -> {
            Tmp.v1.set(Core.input.mouse());
            Element hit = Core.scene.hit(Tmp.v1.x, Tmp.v1.y, true);
            if(hit == null || !hit.isDescendantOf(graphic)) return "Put your mouse\nover the graphic";
            Tmp.v1.set((Tmp.v1.x - graphOriginX)/graphUnitWidth, (Tmp.v1.y - graphOriginY)/graphUnitHeight);
            Tmp.v1.x = Math.max(0, Math.min(1, Tmp.v1.x));

            StringBuilder sb = new StringBuilder();
            sb.append("X: ").append(Tmp.v1.x).append("\n");
            sb.append("Y: ").append(Tmp.v1.y).append("\n");

            sb.append("Val: ").append(function.get(Tmp.v1.x)).append("\n");

            sb.append("Der.: ");
            float derivate = 0;
            if(Tmp.v1.x < delta) derivate = (function.get(2*delta) - function.get(0)) / (2*delta);
            else if(Tmp.v1.x > 1 - delta) derivate = (function.get(1) - function.get(1-2*delta)) / (2*delta);
            else derivate = (function.get(Tmp.v1.x + delta) - function.get(Tmp.v1.x - delta)) / (2*delta);

            // append derivate rounded 3 decimal
            sb.append(Math.round(derivate*1000)/1000f).append("\n");

            return sb.toString();
        }).get();

        return area;
    }


    public static void show() {
        bd.show();
    }
}
