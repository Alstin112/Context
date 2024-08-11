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

import static mindustry.Vars.tilesize;

public abstract class CodableTester extends BaseContextBlock {
    protected TextureRegion topRegion;
    protected CodableTester(String name) {
        super(name);
        schematicPriority = 15;
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

        /** Controls if the building will replace the player's file */
        protected boolean lastEditByPlayer = false;

        /** Controls if the player's file will replace the building */
        protected long lastTimeFileModified = 0;
        /** Message to appear when get error */
        private String errorMessage = "";
        /** Determinate if the build can run while the error is showing */
        protected boolean compileError = false;
        /** Every function will be added some try-catch */
        protected boolean safeRunning = false;
        /** Internal storage of the build (mean to be used in-game), they will not store if copy or leave the world */
        private ObjectMap<String, Object> storage = new ObjectMap<>();

        /** If there is no code inside this build */
        public boolean isEmpty() {
            return false;
        }


        /** Clean the error */
        public void setError() {
            errorMessage = "";
            compileError = false;
        }

        /**
         * Set the block as having error
         * @param message Error message
         * @param compileError If the error is a compile error
         */
        public void setError(String message, boolean compileError) {
            errorMessage = message;
            this.compileError = compileError;
        }

        /** Get the error message */
        public String getError() {
            return errorMessage;
        }

        // Only mean to be used inside the game.
        /** Get the storage of the build */
        public ObjectMap<String, Object> getStorage() {
            return storage;
        }
        /** Set the storage of the build */
        public void setStorage(ObjectMap<String, Object> storage) {
            this.storage = storage;
        }

        /** Get the mode of this block (mean to be visual only)*/
        public TestersModes getMode() {
            if (isEmpty()) return TestersModes.EMPTY;
            if (getError().isEmpty()) return TestersModes.ACTIVE;
            if (compileError) return TestersModes.COMPILER_ERROR;
            return TestersModes.RUNTIME_ERROR;
        }
        @Override
        public void drawSelect() {
            TestersModes mode = getMode();
            if (mode != TestersModes.COMPILER_ERROR && mode != TestersModes.RUNTIME_ERROR) return;
            if (errorMessage.isEmpty()) return;

            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean intPos = font.usesIntegerPositions();
            font.getData().setScale(1 / 4f / Scl.scl(1f));
            font.setUseIntegerPositions(false);

            l.setText(font, errorMessage, Color.scarlet, 90f, Align.left, true);
            float offset = 1f;

            Draw.color(0f, 0f, 0f, 0.2f);
            Fill.rect(x, y - tilesize / 2f - l.height / 2f - offset, l.width + offset * 2f, l.height + offset * 2f);
            Draw.color();
            font.setColor(Color.scarlet);
            font.draw(errorMessage, x - l.width / 2f, y - tilesize / 2f - offset, 90f, Align.left, true);
            font.setUseIntegerPositions(intPos);

            font.getData().setScale(1f);

            Pools.free(l);
        }
        @Override
        public void draw() {
            Draw.rect(region, x, y);
            Draw.color(getMode().getColor(Time.time));
            Draw.rect(topRegion, x, y);
            Draw.reset();
        }
    }
}
