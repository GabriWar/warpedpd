/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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

package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.stones.Runestone;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * The moves the skill tree makes visible. Everything here is cosmetic: the game
 * has already decided what happened, these only show it. Callbacks fire when a
 * streak lands and are meant for a flash or a sound, never for damage, so a
 * multiplayer client and the host never disagree about who was hit.
 *
 * The vocabulary every skill shares: what the ground answers with where something comes down on
 * it (a splash on water, blades flicked on grass, a puff of snow, dust and chips of stone), and a
 * landing.
 */
public final class SkillFX {

	private SkillFX(){}

	private static Group layer(){
		return Dungeon.hero != null && Dungeon.hero.sprite != null ? Dungeon.hero.sprite.parent : null;
	}

	/** a thrown weapon's streak from one cell to another */
	public static void streak( int from, int to, Item look, Callback onArrive ){
		if (look == null || from == to || layer() == null) { if (onArrive != null) onArrive.call(); return; }
		((MissileSprite) layer().recycle( MissileSprite.class )).reset( from, to, look, onArrive );
	}

	/** the same, leaving from a sprite (the thrower) rather than a cell */
	public static void streak( Visual from, int to, Item look, Callback onArrive ){
		if (look == null || layer() == null) { if (onArrive != null) onArrive.call(); return; }
		((MissileSprite) layer().recycle( MissileSprite.class )).reset( from, to, look, onArrive );
	}

	/** How many falling things a frame strike in full (the kit's impact: a core, a ring, a burst,
	 *  a pool of light); the rest of a volley landing that frame throw only sparks or grit. */
	public static final int RAIN_RICH = 2;

	/** things falling out of the sky onto a cell: count of them, spread over the tile, each
	 *  striking as it lands (a small impact of what it is: rainElement); the first calls onArrive */
	public static void rain( int cell, Item look, int count, Callback onArrive ){
		if (look == null || layer() == null) return;
		final Element e = rainElement( look );
		for (int i = 0; i < count; i++){
			PointF to = DungeonTilemap.tileCenterToWorld( cell );
			to.offset( Random.Float( -5, 5 ), Random.Float( -5, 5 ) );
			PointF from = new PointF( to.x + Random.Float( -8, 8 ), to.y - DungeonTilemap.SIZE * Random.Float( 5, 7 ) );
			final float ax = to.x, ay = to.y;
			final Callback then = i == 0 ? onArrive : null;
			((MissileSprite) layer().recycle( MissileSprite.class )).reset( from, to, look, () -> {
				if (Fx.seen( ax, ay )){
					if (SkillVfx.richThisFrame( RAIN_RICH )) FxRecipes.impact( ax, ay, e, 0.5f );
					else if (e == Element.EARTH) FxRecipes.dust( ax, ay, 2 );
					else FxRecipes.sparks( ax, ay, e, 2 );
				}
				if (then != null) then.call();
			} );
		}
	}

	/** What a falling thing strikes as: a bomb as fire, a stone as earth, the rest (arrows,
	 *  darts, blades) as steel. */
	public static Element rainElement( Item look ){
		if (look instanceof Bomb) return Element.FIRE;
		if (look instanceof Runestone) return Element.EARTH;
		return Element.STEEL;
	}

	/** a column of light down onto a cell, with a flare at its foot */
	public static void pillar( int cell, int color ){
		if (layer() == null) return;
		PointF foot = DungeonTilemap.raisedTileCenterToWorld( cell );
		PointF top = new PointF( foot.x, foot.y - DungeonTilemap.SIZE * 7 );
		layer().add( new Beam.LightRay( top, foot ) );
		if (SkillVfx.seen( cell )) PixelFlare.show( foot.x, foot.y, color, PixelFlare.L, 0.8f );
		CellEmitter.center( cell ).burst( Speck.factory( Speck.LIGHT ), 6 );
	}

	/** a heavy landing: dust, a thud and a shake */
	public static void land( int cell ){
		CellEmitter.bottom( cell ).burst( Speck.factory( Speck.DUST ), 8 );
		Fx.shake( 2, 0.25f );
		SpatialSound.play( Assets.Sounds.STURDY, cell, 1f, 0.8f );
		landing( cell );
	}

	/** a flash on whoever was struck, if anyone is there to see it */
	public static void flash( Char ch ){
		if (ch != null && ch.sprite != null && ch.isAlive()) ch.sprite.flash();
	}

	// ------------------------------------------------------------------ the shared vocabulary

	/** What the ground answers with where something comes down on the cell: on water a splash
	 *  (which brings its own ring), on grass blades flicked, on snow a puff, on the rest dust and chips of stone.
	 *  Any thread. */
	public static void ground( final int cell ){
		if (!SkillVfx.seen( cell )) return;
		Fx.post( () -> {
			if (!SkillVfx.valid( cell )) return;
			float x = SkillVfx.cx( cell ), y = SkillVfx.cy( cell ) + 4;
			if (WaterFX.onWater( x, y )){
				WaterFX.splash( x, y, WaterFX.M, Element.NONE );
			} else if (SkillVfx.grass( cell )){
				int[] leaves = { Dungeon.level.color1, Dungeon.level.color2 };
				for (int i = 0; i < 6; i++){
					FxParticle p = SkillVfx.matter( x + FxRandom.Float( -6f, 6f ), y + FxRandom.Float( -2f, 2f ), FxBudget.P3 );
					if (p == null) continue;
					p.look( FxRandom.chance( 0.5f ) ? FxFrames.BLADE_3 : FxFrames.BLADE_4 ).paint( FxRandom.element( leaves ) )
							.vel( FxRandom.Float( -18f, 18f ), FxRandom.Float( -40f, -24f ) ).gravity( 160f ).spin( 0.08f )
							.ground( y + 2, FxParticle.SETTLE, 0 ).life( 0.6f ).alpha( 1f, 0, 0.25f );
				}
			} else if (Dungeon.level.map[cell] == Terrain.SNOW){
				FxRecipes.puff( x, y, Element.STEAM, 3 );
			} else {
				FxRecipes.dust( x, y, 3 );
			}
		} );
	}

	/** A landing on the cell: a ring of dust spreading on the floor, and the ground's answer.
	 *  Any thread. */
	public static void landing( final int cell ){
		if (!SkillVfx.seen( cell )) return;
		Fx.post( () -> {
			if (!SkillVfx.valid( cell )) return;
			float x = SkillVfx.cx( cell ), y = SkillVfx.cy( cell ) + 5;
			if (!WaterFX.onWater( x, y )){
				FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), x, y, 9, 31, 0.4f,
						Element.EARTH.ramp.stop( 1 ), 0.6f, false );
			}
			ground( cell );
		} );
	}
}
