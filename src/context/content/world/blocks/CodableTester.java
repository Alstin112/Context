package context.content.world.blocks;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.Scl;
import arc.struct.ObjectMap;
import arc.util.Align;
import arc.util.Time;
import arc.util.pooling.Pools;
import context.content.TestersModes;
import mindustry.ui.Fonts;

import static mindustry.Vars.renderer;
import static mindustry.Vars.tilesize;

public abstract class CodableTester extends BaseContextBlock {
    protected TextureRegion topRegion;
    protected CodableTester(String name) {
        super(name);
    }

    @Override
    public void load() {
        super.load();
        this.region = Core.atlas.find("context-tester-base");
        this.topRegion = Core.atlas.find(this.name);
    }

    @Override
    public TextureRegion[] icons() {
        return new TextureRegion[]{region, topRegion};
    }

    public class CodableTesterBuild extends BaseContextBuild {

        /**
         * Controls if the building will replace the player's file
         */
        protected boolean lastEditByPlayer = false;

        /**
         * Controls if the player's file will replace the building
         */
        protected long lastFileModified = 0;


        /**
         * Control the
         */
        protected TestersModes mode = TestersModes.EMPTY;
        private String errorMessage = "";
        protected boolean compileError = false;
        protected boolean active = true;
        private ObjectMap<String, Object> storage = new ObjectMap<>();

        public boolean isEmpty() {
            return false;
        }

        public void updateMode() {
            if (!active) mode = TestersModes.INACTIVE;
            else if (isEmpty()) mode = TestersModes.EMPTY;
            else if (!getError().isEmpty()) {
                if (compileError) mode = TestersModes.COMPILER_ERROR;
                else mode = TestersModes.RUNTIME_ERROR;
            } else mode = TestersModes.ACTIVE;
        }
        @Override
        public void drawSelect() {
            if (renderer.pixelator.enabled()) return;
            if (mode != TestersModes.COMPILER_ERROR && mode != TestersModes.RUNTIME_ERROR) return;
            if (errorMessage.isEmpty()) return;

            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.getData().setScale(1 / 4f / Scl.scl(1f));
            font.setUseIntegerPositions(false);

            l.setText(font, errorMessage, Color.scarlet, 90f, Align.left, true);
            float offset = 1f;

            Draw.color(0f, 0f, 0f, 0.2f);
            Fill.rect(x, y - tilesize / 2f - l.height / 2f - offset, l.width + offset * 2f, l.height + offset * 2f);
            Draw.color();
            font.setColor(Color.scarlet);
            font.draw(errorMessage, x - l.width / 2f, y - tilesize / 2f - offset, 90f, Align.left, true);
            font.setUseIntegerPositions(ints);

            font.getData().setScale(1f);

            Pools.free(l);
        }
        @Override
        public void draw() {
            updateMode();
            Draw.rect(region, x, y);
            Draw.color(mode.getColor(Time.time));
            Draw.rect(topRegion, x, y);
            Draw.reset();
        }

        public void setError() {
            errorMessage = "";
            compileError = false;
        }
        public void setError(String message, boolean compileError) {
            errorMessage = message;
            this.compileError = compileError;
        }
        public String getError() {
            return errorMessage;
        }

        public ObjectMap<String, Object> getStorage() {
            return storage;
        }

        public void setStorage(ObjectMap<String, Object> storage) {
            this.storage = storage;
        }

        public TestersModes getMode() {
            return mode;
        }
    }
}
