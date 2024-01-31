package context.content.world.blocks;

import arc.Core;
import arc.Graphics;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Player;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Env;

import static mindustry.Vars.state;
import static mindustry.Vars.tilesize;

public abstract class BaseContextBlock extends Block {
    //don't change this too much unless you want to run into issues with packet sizes

    public BaseContextBlock(String name) {
        super(name);
        configurable = true;
        solid = true;
        destructible = true;
        group = BlockGroup.logic;
        drawDisabled = false;
        envEnabled = Env.any;
        this.alwaysUnlocked = true;
        this.category = Category.logic;
        this.requirements = ItemStack.with();
        this.buildVisibility = BuildVisibility.sandboxOnly;
    }

    public boolean accessible(){
        return !privileged || state.rules.editor;
    }

    @Override
    public boolean canBreak(Tile tile){
        return accessible();
    }


    public class BaseContextBuild extends Building {

        @Override
        public boolean shouldShowConfigure(Player player){
            return accessible();
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(this == other || !accessible()){
                deselect();
                return false;
            }

            return true;
        }

        @Override
        public Graphics.Cursor getCursor(){
            return !accessible() ? Graphics.Cursor.SystemCursor.arrow : super.getCursor();
        }

        @Override
        public void damage(float damage){
            if(privileged) return;
            super.damage(damage);
        }

        @Override
        public boolean canPickup(){
            return false;
        }

        @Override
        public boolean collide(Bullet other){
            return !privileged;
        }

        @Override
        public void updateTableAlign(Table table){
            Vec2 pos = Core.input.mouseScreen(x, y + size * tilesize / 2f + 1);
            table.setPosition(pos.x, pos.y, Align.bottom);
        }
    }


}
