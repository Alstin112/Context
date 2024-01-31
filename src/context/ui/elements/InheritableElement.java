package context.ui.elements;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.ScissorStack;
import arc.input.KeyCode;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputEvent.InputEventType;
import arc.scene.event.InputListener;
import arc.scene.event.SceneEvent;
import arc.scene.style.Drawable;
import arc.scene.style.Style;
import arc.scene.ui.Slider;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.WidgetGroup;
import arc.scene.utils.Cullable;

import static arc.Core.graphics;
import static arc.Core.scene;

/**
 * A group that scrolls a child widget using scrollbars and/or mouse or touch dragging.
 * <p>
 * The widget is sized to its preferred size. If the widget's preferred width or height is less than the size of this scroll pane,
 * it is set to the size of this scroll pane. Scrollbars appear when the widget is larger than the scroll pane.
 * <p>
 * The scroll pane's preferred size is that of the child widget. At this size, the child widget will not need to scroll, so the
 * scroll pane is typically sized by ignoring the preferred size in one or both directions.
 * @author mzechner
 * @author Nathan Sweet
 */
public class InheritableElement extends WidgetGroup{
    final Rect hScrollBounds = new Rect();
    final Rect vScrollBounds = new Rect();
    final Rect hKnobBounds = new Rect();
    final Rect vKnobBounds = new Rect();
    final Vec2 lastPoint = new Vec2();
    private final Rect widgetAreaBounds = new Rect();
    private final Rect widgetCullingArea = new Rect();
    private final Rect scissorBounds = new Rect();
    protected boolean disableX, disableY;
    boolean scrollX, scrollY;
    boolean vScrollOnRight = true;
    boolean hScrollOnBottom = true;
    float amountX, amountY;
    float visualAmountX, visualAmountY;
    float maxX, maxY;
    boolean touchScrollH, touchScrollV;
    float areaWidth, areaHeight;
    float fadeAlpha = 1f, fadeAlphaSeconds = 1, fadeDelay, fadeDelaySeconds = 1;
    boolean cancelTouchFocus = true;
    boolean flickScroll = true;
    float velocityX, velocityY;
    float flingTimer;
    float flingTime = 1f;
    int draggingPointer = -1;
    private ScrollPaneStyle style;
    private Element widget;
    private ElementGestureListener flickScrollListener;
    private boolean fadeScrollBars = false, smoothScrolling = true;
    private boolean overscrollX = true, overscrollY = true;
    private float overscrollDistance = 50, overscrollSpeedMin = 30, overscrollSpeedMax = 200;
    private boolean forceScrollX, forceScrollY;
    private boolean clamp = true;
    private boolean scrollbarsOnTop;
    private boolean variableSizeKnobs = true;
    private boolean clip = true;

    /** @param widget May be null. */
    public InheritableElement(Element widget){
        this(widget, scene.getStyle(ScrollPaneStyle.class));
    }

    /** @param widget May be null. */
    public InheritableElement(Element widget, ScrollPaneStyle style){
        if(style == null) throw new IllegalArgumentException("style cannot be null.");
        this.style = style;
        setWidget(widget);
        setSize(150, 150);
        setTransform(true);


    }

    void resetFade(){
        fadeAlpha = fadeAlphaSeconds;
        fadeDelay = fadeDelaySeconds;
    }

    void clamp(){
        if(!clamp) return;
        scrollX(overscrollX ? Mathf.clamp(amountX, -overscrollDistance, maxX + overscrollDistance)
                : Mathf.clamp(amountX, 0, maxX));
        scrollY(overscrollY ? Mathf.clamp(amountY, -overscrollDistance, maxY + overscrollDistance)
                : Mathf.clamp(amountY, 0, maxY));
    }


    @Override
    public void act(float delta){
        super.act(delta);

        boolean panning = flickScrollListener.getGestureDetector().isPanning();
        boolean animating = false;

        if(fadeAlpha > 0 && fadeScrollBars && !panning && !touchScrollH && !touchScrollV){
            fadeDelay -= delta;
            if(fadeDelay <= 0) fadeAlpha = Math.max(0, fadeAlpha - delta);
            animating = true;
        }

        if(flingTimer > 0){
            resetFade();

            float alpha = flingTimer / flingTime;
            amountX -= velocityX * alpha * delta;
            amountY -= velocityY * alpha * delta;
            clamp();

            // Stop fling if hit overscroll distance.
            if(amountX == -overscrollDistance) velocityX = 0;
            if(amountX >= maxX + overscrollDistance) velocityX = 0;
            if(amountY == -overscrollDistance) velocityY = 0;
            if(amountY >= maxY + overscrollDistance) velocityY = 0;

            flingTimer -= delta;
            if(flingTimer <= 0){
                velocityX = 0;
                velocityY = 0;
            }

            animating = true;
        }

        if(smoothScrolling && flingTimer <= 0 && !panning && //
                // Scroll smoothly when grabbing the scrollbar if one pixel of scrollbar movement is > 10% of the scroll area.
                ((!touchScrollH || (scrollX && maxX / (hScrollBounds.width - hKnobBounds.width) > areaWidth * 0.1f)) //
                        && (!touchScrollV || (scrollY && maxY / (vScrollBounds.height - vKnobBounds.height) > areaHeight * 0.1f))) //
        ){
            if(visualAmountX != amountX){
                if(visualAmountX < amountX)
                    visualScrollX(Math.min(amountX, visualAmountX + Math.max(200 * delta, (amountX - visualAmountX) * 7 * delta)));
                else
                    visualScrollX(Math.max(amountX, visualAmountX - Math.max(200 * delta, (visualAmountX - amountX) * 7 * delta)));
                animating = true;
            }
            if(visualAmountY != amountY){
                if(visualAmountY < amountY)
                    visualScrollY(Math.min(amountY, visualAmountY + Math.max(200 * delta, (amountY - visualAmountY) * 7 * delta)));
                else
                    visualScrollY(Math.max(amountY, visualAmountY - Math.max(200 * delta, (visualAmountY - amountY) * 7 * delta)));
                animating = true;
            }
        }else{
            if(visualAmountX != amountX) visualScrollX(amountX);
            if(visualAmountY != amountY) visualScrollY(amountY);
        }

        if(!panning){
            if(overscrollX && scrollX){
                if(amountX < 0){
                    resetFade();
                    amountX += (overscrollSpeedMin + (overscrollSpeedMax - overscrollSpeedMin) * -amountX / overscrollDistance)
                            * delta;
                    if(amountX > 0) scrollX(0);
                    animating = true;
                }else if(amountX > maxX){
                    resetFade();
                    amountX -= (overscrollSpeedMin
                            + (overscrollSpeedMax - overscrollSpeedMin) * -(maxX - amountX) / overscrollDistance) * delta;
                    if(amountX < maxX) scrollX(maxX);
                    animating = true;
                }
            }
            if(overscrollY && scrollY){
                if(amountY < 0){
                    resetFade();
                    amountY += (overscrollSpeedMin + (overscrollSpeedMax - overscrollSpeedMin) * -amountY / overscrollDistance)
                            * delta;
                    if(amountY > 0) scrollY(0);
                    animating = true;
                }else if(amountY > maxY){
                    resetFade();
                    amountY -= (overscrollSpeedMin
                            + (overscrollSpeedMax - overscrollSpeedMin) * -(maxY - amountY) / overscrollDistance) * delta;
                    if(amountY < maxY) scrollY(maxY);
                    animating = true;
                }
            }
        }

        if(animating){
            Scene stage = getScene();
            if(stage != null && stage.getActionsRequestRendering()) graphics.requestRendering();
        }
    }


    @Override
    public void layout(){
        final Drawable bg = style.background;
        final Drawable hScrollKnob = style.hScrollKnob;
        final Drawable vScrollKnob = style.vScrollKnob;

        float bgLeftWidth = 0, bgRightWidth = 0, bgTopHeight = 0, bgBottomHeight = 0;
        if(bg != null){
            bgLeftWidth = bg.getLeftWidth();
            bgRightWidth = bg.getRightWidth();
            bgTopHeight = bg.getTopHeight();
            bgBottomHeight = bg.getBottomHeight();
        }

        float width = getWidth();
        float height = getHeight();

        float scrollbarHeight = 0;
        if(hScrollKnob != null) scrollbarHeight = hScrollKnob.getMinHeight();
        if(style.hScroll != null) scrollbarHeight = Math.max(scrollbarHeight, style.hScroll.getMinHeight());
        float scrollbarWidth = 0;
        if(vScrollKnob != null) scrollbarWidth = vScrollKnob.getMinWidth();
        if(style.vScroll != null) scrollbarWidth = Math.max(scrollbarWidth, style.vScroll.getMinWidth());

        // Get available space size by subtracting background's padded area.
        areaWidth = width - bgLeftWidth - bgRightWidth;
        areaHeight = height - bgTopHeight - bgBottomHeight;

        if(widget == null) return;

        // Get widget's desired width.
        float widgetWidth, widgetHeight;
        widgetWidth = widget.getPrefWidth();
        widgetHeight = widget.getPrefHeight();

        // The bounds of the scrollable area for the widget.
        widgetAreaBounds.set(bgLeftWidth, bgBottomHeight, areaWidth, areaHeight);


        // If the widget is smaller than the available space, make it take up the available space.
        widgetWidth = disableX ? areaWidth : Math.max(areaWidth, widgetWidth);
        widgetHeight = disableY ? areaHeight : Math.max(areaHeight, widgetHeight);

        maxX = widgetWidth - areaWidth;
        maxY = widgetHeight - areaHeight;

        widget.setSize(widgetWidth, widgetHeight);
        widget.validate();
    }

    @Override
    public void draw(){
        if(widget == null) return;

        validate();

        // Setup transform for this group.
        applyTransform(computeTransform());

        if(scrollX)
            hKnobBounds.x = hScrollBounds.x + (int)((hScrollBounds.width - hKnobBounds.width) * getVisualScrollPercentX());
        if(scrollY)
            vKnobBounds.y = vScrollBounds.y + (int)((vScrollBounds.height - vKnobBounds.height) * (1 - getVisualScrollPercentY()));

        // Calculate the widget's position depending on the scroll state and available widget area.
        float y = widgetAreaBounds.y;
        if(!scrollY)
            y -= (int)maxY;
        else
            y -= (int)(maxY - visualAmountY);

        float x = widgetAreaBounds.x;
        if(scrollX) x -= (int)visualAmountX;

        if(!fadeScrollBars && scrollbarsOnTop){
            if(scrollX && hScrollOnBottom){
                float scrollbarHeight = 0;
                if(style.hScrollKnob != null) scrollbarHeight = style.hScrollKnob.getMinHeight();
                if(style.hScroll != null) scrollbarHeight = Math.max(scrollbarHeight, style.hScroll.getMinHeight());
                y += scrollbarHeight;
            }
            if(scrollY && !vScrollOnRight){
                float scrollbarWidth = 0;
                if(style.hScrollKnob != null) scrollbarWidth = style.hScrollKnob.getMinWidth();
                if(style.hScroll != null) scrollbarWidth = Math.max(scrollbarWidth, style.hScroll.getMinWidth());
                x += scrollbarWidth;
            }
        }

        widget.setPosition(x, y);

        if(widget instanceof Cullable){
            widgetCullingArea.x = -widget.x + widgetAreaBounds.x;
            widgetCullingArea.y = -widget.y + widgetAreaBounds.y;
            widgetCullingArea.width = widgetAreaBounds.width;
            widgetCullingArea.height = widgetAreaBounds.height;
            ((Cullable)widget).setCullingArea(widgetCullingArea);
        }

        // Draw the background ninepatch.
        if(style.background != null) style.background.draw(0, 0, getWidth(), getHeight());

        // Caculate the scissor bounds based on the batch transform, the available widget area and the camera transform. We need to
        // project those to screen coordinates for OpenGL ES to consume.
        scene.calculateScissors(widgetAreaBounds, scissorBounds);

        if(clip){
            // Enable scissors for widget area and draw the widget.
            if(ScissorStack.push(scissorBounds)){
                drawChildren();
                ScissorStack.pop();
            }
        }else{
            drawChildren();
        }

        // Render scrollbars and knobs on top.
        Draw.color(color.r, color.g, color.b, color.a * parentAlpha * Interp.fade.apply(fadeAlpha / fadeAlphaSeconds));
        if(scrollX && scrollY){
            if(style.corner != null){
                style.corner.draw(hScrollBounds.x + hScrollBounds.width, hScrollBounds.y, vScrollBounds.width,
                        vScrollBounds.y);
            }
        }
        if(scrollX){
            if(style.hScroll != null)
                style.hScroll.draw(hScrollBounds.x, hScrollBounds.y, hScrollBounds.width, hScrollBounds.height);
            if(style.hScrollKnob != null)
                style.hScrollKnob.draw(hKnobBounds.x, hKnobBounds.y, hKnobBounds.width, hKnobBounds.height);
        }
        if(scrollY){
            if(style.vScroll != null)
                style.vScroll.draw(vScrollBounds.x, vScrollBounds.y, vScrollBounds.width, vScrollBounds.height);
            if(style.vScrollKnob != null)
                style.vScrollKnob.draw(vKnobBounds.x, vKnobBounds.y, vKnobBounds.width, vKnobBounds.height);
        }

        resetTransform();
    }

    @Override
    public float getPrefWidth(){
        float width = 0;
        if(widget != null){
            validate();
            width = widget.getPrefWidth();
        }
        if(style.background != null) width += style.background.getLeftWidth() + style.background.getRightWidth();
        if(scrollY){
            float scrollbarWidth = 0;
            if(style.vScrollKnob != null) scrollbarWidth = style.vScrollKnob.getMinWidth();
            if(style.vScroll != null) scrollbarWidth = Math.max(scrollbarWidth, style.vScroll.getMinWidth());
            width += scrollbarWidth;
        }
        return width;
    }

    @Override
    public float getPrefHeight(){
        float height = 0;
        if(widget != null){
            validate();
            height = widget.getPrefHeight();
        }
        if(style.background != null) height += style.background.getTopHeight() + style.background.getBottomHeight();
        if(scrollX){
            float scrollbarHeight = 0;
            if(style.hScrollKnob != null) scrollbarHeight = style.hScrollKnob.getMinHeight();
            if(style.hScroll != null) scrollbarHeight = Math.max(scrollbarHeight, style.hScroll.getMinHeight());
            height += scrollbarHeight;
        }
        return height;
    }

    @Override
    public float getMinWidth(){
        return 0;
    }

    @Override
    public float getMinHeight(){
        return 0;
    }

    /** Returns the actor embedded in this scroll pane, or null. */
    public Element getWidget(){
        return widget;
    }

    /**
     * Sets the {@link Element} embedded in this scroll pane.
     * @param widget May be null to remove any current actor.
     */
    public void setWidget(Element widget){
        if(widget == this) throw new IllegalArgumentException("widget cannot be the InheritableElement.");
        if(this.widget != null) super.removeChild(this.widget);
        this.widget = widget;
        if(widget != null) super.addChild(widget);
    }

    @Override
    public boolean removeChild(Element actor){
        if(actor == null) throw new IllegalArgumentException("actor cannot be null.");
        if(actor != widget) return false;
        setWidget(null);
        return true;
    }

    @Override
    public boolean removeChild(Element actor, boolean unfocus){
        if(actor == null) throw new IllegalArgumentException("actor cannot be null.");
        if(actor != widget) return false;
        this.widget = null;
        return super.removeChild(actor, unfocus);
    }

    @Override
    public Element hit(float x, float y, boolean touchable){
        if(x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) return null;
        if(scrollX && hScrollBounds.contains(x, y)) return this;
        if(scrollY && vScrollBounds.contains(x, y)) return this;
        return super.hit(x, y, touchable);
    }

    /** Called whenever the x scroll amount is changed. */
    protected void scrollX(float pixelsX){
        this.amountX = pixelsX;
    }

    /** Called whenever the y scroll amount is changed. */
    protected void scrollY(float pixelsY){
        this.amountY = pixelsY;
    }

    /** Called whenever the visual x scroll amount is changed. */
    protected void visualScrollX(float pixelsX){
        this.visualAmountX = pixelsX;
    }

    /** Called whenever the visual y scroll amount is changed. */
    protected void visualScrollY(float pixelsY){
        this.visualAmountY = pixelsY;
    }

    public float getVisualScrollPercentX(){
        return Mathf.clamp(visualAmountX / maxX, 0, 1);
    }

    public float getVisualScrollPercentY(){
        return Mathf.clamp(visualAmountY / maxY, 0, 1);
    }

    public float getScrollPercentX(){
        if(Float.isNaN(amountX / maxX)) return 1f;
        return Mathf.clamp(amountX / maxX, 0, 1);
    }

    public float getScrollPercentY(){
        if(Float.isNaN(amountY / maxY)) return 1f;
        return Mathf.clamp(amountY / maxY, 0, 1);
    }

    /**
     * The style for a scroll pane, see {@link InheritableElement}.
     * @author mzechner
     * @author Nathan Sweet
     */
    public static class ScrollPaneStyle extends Style{
        /** Optional. */
        public Drawable background, corner;
        /** Optional. */
        public Drawable hScroll, hScrollKnob;
        /** Optional. */
        public Drawable vScroll, vScrollKnob;

        public ScrollPaneStyle(){
        }

        public ScrollPaneStyle(ScrollPaneStyle style){
            this.background = style.background;
            this.hScroll = style.hScroll;
            this.hScrollKnob = style.hScrollKnob;
            this.vScroll = style.vScroll;
            this.vScrollKnob = style.vScrollKnob;
        }
    }
}
