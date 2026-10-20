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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.ItemLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillFlight;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.stones.Runestone;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Crossbow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ScorpioSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.TenguSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * The moves the skill tree makes visible. Everything here is cosmetic: the game
 * has already decided what happened, these only show it. Callbacks fire when a
 * streak lands and are meant for a flash or a sound, never for damage, so a
 * multiplayer client and the host never disagree about who was hit.
 *
 * The vocabulary every skill shares: what the ground answers with where something comes down on
 * it (a splash on water, blades flicked on grass, a puff of snow, dust and chips of stone), and a
 * landing. A skill's falls and throws that are not items (a meteor, spirit arrows, a coin, a
 * snare) fly as the kit's pictures (Fall, Streak, SkillFlight) on the line and the clock an
 * item's MissileSprite would; real throws (Hurl, a hero's thrown weapon) stay the item.
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
				strike( ax, ay, e );
				if (then != null) then.call();
			} );
		}
	}

	/** What a skill drops out of the sky when it is not an item (rain(cell, drawsAs, fall, ...)):
	 *  a meteor (a stone of fire), spirit arrows of light, a blade of holy light, a deadfall's
	 *  counterweight on its rope, and a shock (nothing drawn: its time and its strike only). */
	public static final class Fall {
		private Fall(){}
		public static final int METEOR = SkillFlight.METEOR, SPIRIT_ARROW = SkillFlight.SPIRIT_ARROW,
				HOLY_BLADE = SkillFlight.HOLY_BLADE, WEIGHT = SkillFlight.WEIGHT, SHOCK = SkillFlight.SHOCK;
	}

	/** What a skill throws when it is not an item (streak(from, to, look, ...)): a coin spinning on
	 *  an arc, a spirit arrow of light, a feather dart, a pellet of ink lobbed, a blade of holy
	 *  light, a snare's two steel crescents tumbling. */
	public static final class Streak {
		private Streak(){}
		public static final int COIN = SkillFlight.COIN, SPIRIT_ARROW = SkillFlight.SPIRIT_ARROW,
				FEATHER_DART = SkillFlight.FEATHER_DART, PELLET = SkillFlight.PELLET, HOLY_BLADE = SkillFlight.HOLY_BLADE,
				SNARE = SkillFlight.SNARE;
	}

	/** MissileSprite's speed, px/s. */
	public static final float MISSILE_SPEED = 240f;

	/**
	 * Things of a skill falling out of the sky onto a cell, drawn as `fall` (Fall) rather than as
	 * the item: exactly rain(cell, drawsAs, count, onArrive)'s draws of the game's Random (four an
	 * item, to-x, to-y, from-x, from-height, in that order; none when drawsAs is null or there is no
	 * layer, returning as it did), the same paths, landing on the frames its MissileSprites would
	 * (drawsAs flies as fast as it would), each striking as its fall does (fallElement); the first
	 * calls onArrive.
	 */
	public static void rain( int cell, Item drawsAs, int fall, int count, Callback onArrive ){
		if (drawsAs == null || layer() == null) return;
		final Element e = fallElement( fall );
		final float speed = missileSpeed( drawsAs );
		for (int i = 0; i < count; i++){
			PointF to = DungeonTilemap.tileCenterToWorld( cell );
			to.offset( Random.Float( -5, 5 ), Random.Float( -5, 5 ) );
			PointF from = new PointF( to.x + Random.Float( -8, 8 ), to.y - DungeonTilemap.SIZE * Random.Float( 5, 7 ) );
			final float ax = to.x, ay = to.y;
			final Callback then = i == 0 ? onArrive : null;
			SkillFlight.fly( layer(), from.x, from.y, to.x, to.y, fall, speed, () -> {
				strike( ax, ay, e );
				if (then != null) then.call();
			} );
			forGuests( from.x, from.y, to.x, to.y, drawsAs );
		}
	}

	/** A co-op guest replays a host's missile as an item's (NetVisuals): he is sent the fall or
	 *  throw as the item it stands for, as before (its magic's trail too, as MissileSprite.reset
	 *  sends it), so he still sees what flies where (host only, no draw). Any thread. */
	public static void forGuests( float fx, float fy, float tx, float ty, Item item ){
		if (!NetManager.isHost()) return;
		float dx = tx - fx, dy = ty - fy;
		float spin = MissileSprite.angularSpeed( item.getClass() );
		float angle = 135 - (float) (Math.atan2( dx, dy ) / Math.PI * 180);
		if (dx < 0){
			spin = -spin;
			angle += 90;
		}
		NetVisuals.recordMissile( fx, fy, tx, ty, item.image, spin, angle, ItemLooks.sig( item ) );
	}

	/** What a fall strikes as: a meteor as fire, arrows and blades as steel, a weight and a shock
	 *  as earth. */
	public static Element fallElement( int fall ){
		switch (fall){
			case Fall.METEOR: return Element.FIRE;
			case Fall.SPIRIT_ARROW: case Fall.HOLY_BLADE: return Element.STEEL;
			default: return Element.EARTH;
		}
	}

	/** How fast a MissileSprite of this item flies, px/s (MissileSprite.setup's: a dart from a
	 *  crossbow's hand three times as fast, a spirit arrow half again). */
	public static float missileSpeed( Item item ){
		float speed = MISSILE_SPEED;
		if (item instanceof Dart && Dungeon.hero != null
				&& (Dungeon.hero.belongings.weapon() instanceof Crossbow || Dungeon.hero.belongings.secondWep() instanceof Crossbow)){
			speed *= 3f;
		} else if (item instanceof SpiritBow.SpiritArrow || item instanceof ScorpioSprite.ScorpioShot
				|| item instanceof TenguSprite.TenguShuriken){
			speed *= 1.5f;
		}
		return speed;
	}

	/** A skill's throw from one cell to another drawn as `look` (Streak), at MissileSprite's 240
	 *  px/s, calling onArrive as it lands (at once from a cell to itself or off the scene). */
	public static void streak( int from, int to, int look, Callback onArrive ){
		streak( from, to, look, MISSILE_SPEED, onArrive );
	}

	/** The same, flying as fast as `timedAs` would (a dart's replacement from a crossbow's hand
	 *  lands when the dart would have); a co-op guest is sent it as that item's flight, as before. */
	public static void streak( int from, int to, int look, Item timedAs, Callback onArrive ){
		if (timedAs == null) streak( from, to, look, MISSILE_SPEED, onArrive );
		else streak( from, to, look, timedAs, missileSpeed( timedAs ), onArrive );
	}

	private static void streak( int from, int to, int look, float speed, Callback onArrive ){
		if (from == to || layer() == null){ if (onArrive != null) onArrive.call(); return; }
		PointF a = DungeonTilemap.raisedTileCenterToWorld( from ), b = DungeonTilemap.raisedTileCenterToWorld( to );
		SkillFlight.fly( layer(), a.x, a.y, b.x, b.y, look, speed, landed( b.x, b.y, look, onArrive ) );
	}

	private static void streak( int from, int to, int look, Item timedAs, float speed, Callback onArrive ){
		streak( from, to, look, speed, onArrive );
		if (from != to && layer() != null){
			PointF a = DungeonTilemap.raisedTileCenterToWorld( from ), b = DungeonTilemap.raisedTileCenterToWorld( to );
			forGuests( a.x, a.y, b.x, b.y, timedAs );
		}
	}

	/** The same, leaving from a sprite (the thrower) rather than a cell. */
	public static void streak( Visual from, int to, int look, Callback onArrive ){
		streak( from, to, look, MISSILE_SPEED, onArrive );
	}

	public static void streak( Visual from, int to, int look, Item timedAs, Callback onArrive ){
		streak( from, to, look, timedAs == null ? MISSILE_SPEED : missileSpeed( timedAs ), onArrive );
		if (timedAs != null && from != null && layer() != null){
			PointF a = from.center(), b = DungeonTilemap.raisedTileCenterToWorld( to );
			forGuests( a.x, a.y, b.x, b.y, timedAs );
		}
	}

	private static void streak( Visual from, int to, int look, float speed, Callback onArrive ){
		if (from == null || layer() == null){ if (onArrive != null) onArrive.call(); return; }
		PointF a = from.center(), b = DungeonTilemap.raisedTileCenterToWorld( to );
		SkillFlight.fly( layer(), a.x, a.y, b.x, b.y, look, speed, landed( b.x, b.y, look, onArrive ) );
	}

	//a thrown look's landing: a coin's glint, then the caller's
	private static Callback landed( final float x, final float y, final int look, final Callback then ){
		if (look != Streak.COIN) return then;
		return () -> {
			if (Fx.seen( x, y )){
				FxParticle g = SkillVfx.glow( x, y - 2, FxBudget.P2 );
				if (g != null) g.look( FxFrames.STAR4_5 ).paint( Element.GOLD.light ).snap().life( 0.25f ).alpha( 1f, 0, 0.15f );
			}
			if (then != null) then.call();
		};
	}

	//a falling thing striking at (x, y): in full for the first RAIN_RICH a frame
	private static void strike( float x, float y, Element e ){
		if (!Fx.seen( x, y )) return;
		if (SkillVfx.richThisFrame( RAIN_RICH )) FxRecipes.impact( x, y, e, 0.5f );
		else if (e == Element.EARTH) FxRecipes.dust( x, y, 2 );
		else FxRecipes.sparks( x, y, e, 2 );
	}

	/** What a falling thing strikes as: a bomb as fire, a stone as earth, the rest (arrows,
	 *  darts, blades) as steel. */
	public static Element rainElement( Item look ){
		if (look instanceof Bomb) return Element.FIRE;
		if (look instanceof Runestone) return Element.EARTH;
		return Element.STEEL;
	}

	/** a column of light rising out of a cell (PillarRiseFX: from the ground up, never a beam
	 *  coming down), with a flare at its foot */
	public static void pillar( int cell, int color ){
		if (layer() == null) return;
		PointF foot = DungeonTilemap.raisedTileCenterToWorld( cell );
		PillarRiseFX.show( cell, color, null );
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
