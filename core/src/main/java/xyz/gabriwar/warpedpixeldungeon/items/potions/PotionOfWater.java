/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package xyz.gabriwar.warpedpixeldungeon.items.potions;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.items.EmptyBottle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PointF;

import java.util.ArrayList;

public class PotionOfWater extends Potion {

    public static final String AC_POUR = "POUR";

    {
        icon = ItemSpriteSheet.Icons.POTION_WATER;
    }

    @Override
    public void reset() {
        super.reset();
        image = ItemSpriteSheet.BOTTLE_OF_WATER;
        //known from the start, in the potions' records too (catalog, badges)
        if (handler != null && handler.contains( this ) && !handler.isKnown( this )) handler.know( this );
    }

    @Override
    public boolean isKnown() {
        return true;
    }

    @Override
    public String defaultAction() {
        return AC_POUR;
    }

    //poured, not drunk: pouring it on yourself is the way to get wet
    @Override
    public ArrayList<String> actions( Hero hero ) {
        ArrayList<String> actions = super.actions( hero );
        actions.remove( AC_DRINK );
        actions.add( AC_POUR );
        return actions;
    }

    @Override
    public void execute( Hero hero, String action ) {
        super.execute( hero, action );
        if (action.equals( AC_POUR )) {
            GameScene.selectCell( pourer );
        }
    }

    private final CellSelector.Listener pourer = new CellSelector.Listener() {
        @Override
        public void onSelect( Integer cell ) {
            if (cell == null) return;
            Hero hero = Dungeon.hero;
            if (!Dungeon.level.adjacent( hero.pos, cell ) && cell != hero.pos) {
                GLog.w( Messages.get( PotionOfWater.class, "too_far" ) );
                return;
            }

            detach( hero.belongings.backpack );
            EmptyBottle.giveBack( hero );

            boolean fire = douse( cell );
            SpatialSound.play( Assets.Sounds.WATER, cell );
            GLog.i( Messages.get( PotionOfWater.class, fire ? "doused" : "poured" ) );

            hero.sprite.operate( cell );
            hero.spendAndNext( 1f );
        }

        @Override
        public String prompt() {
            return Messages.get( PotionOfWater.class, "pour_prompt" );
        }
    };

    @Override
    public void apply(Hero hero) {
        identify();
        shatter(hero.pos);
    }

    @Override
    public void shatter(int cell) {
        splash(cell);
        if (Dungeon.level.heroFOV[cell]) {
            identify();
            SpatialSound.play(Assets.Sounds.SHATTER, cell);
        }
        douse(cell);
    }

    /**
     * Water poured or splashed on one cell: puts out its fire, drenches whoever stands there and,
     * on bare ground, leaves a puddle of water. Farmland, floors laid by hand, doors, stairs and
     * traps keep their terrain. Returns whether a fire went out.
     */
    public static boolean douse( int cell ) {
        Level level = Dungeon.level;
        boolean fire = false;
        for (Blob b : level.blobs.values()) {
            if (b instanceof Fire && b.cur != null && b.cur[cell] > 0) {
                b.clear( cell );
                fire = true;
            }
        }
        if (fire && level.heroFOV[cell]) {
            PointF c = DungeonTilemap.tileCenterToWorld( cell );
            FxRecipes.steam( c.x, c.y, 8 );
        }

        Char ch = Actor.findChar( cell );
        if (ch != null && !ch.flying) {
            Buff.prolong( ch, Drenched.class, Drenched.DURATION );
        }

        if (puddles( level.map[cell] ) && level.plants.get( cell ) == null) {
            Level.set( cell, Terrain.WATER );
            GameScene.updateMap( cell );
        }
        return fire;
    }

    /** The bare ground a splash of water turns into a puddle. */
    public static boolean puddles( int terrain ) {
        switch (terrain) {
            case Terrain.EMPTY: case Terrain.EMPTY_DECO: case Terrain.GRASS: case Terrain.FURROWED_GRASS:
            case Terrain.EMBERS: case Terrain.SNOW: case Terrain.DIRT_PATH:
                return true;
            default:
                return false;
        }
    }

    @Override
    public int value() {
        return isKnown() ? 30 * quantity : super.value();
    }
}
