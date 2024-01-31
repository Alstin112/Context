package context.content.world.blocks;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Time;
import context.content.TestersModes;

public abstract class CodableTester extends BaseContextBlock {

    public TextureRegion topRegion;
    public CodableTester(String name) {
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
        public TestersModes mode = TestersModes.EMPTY;

        public void updateMode() {
            mode = TestersModes.ACTIVE;
        }

        @Override
        public void draw() {
            updateMode();
            Draw.rect(region, x, y);
            Draw.color(mode.getColor(Time.time));
            Draw.rect(topRegion, x, y);
            Draw.reset();
        }

    }
}
