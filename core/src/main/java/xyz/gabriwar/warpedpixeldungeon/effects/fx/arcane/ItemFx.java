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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * What scrolls, potions, stones and wands add to what they always showed (their own bursts and
 * flares stay, untouched, and so do their Random draws): a scroll's runes rising off the hands
 * that read it, an upgrade's gold ghost, a curse's ink pulled out, a teleport's implosion and its
 * shaft of arrival, magic mapping's wave of light across the floor and the stars on its secrets,
 * a recharge's flash as its stream converges, shock rings of fear, rage and anti-magic, a bloom
 * where a blast once only flashed the screen, notes and hearts flying to who hears them, a
 * potion's fizz as it is drunk and its glass breaking where it is thrown.
 *
 * Callable from any thread: each hands itself to the render thread (Fx.post) and reads its
 * positions there. FxRandom only.
 */
public final class ItemFx {

	private ItemFx(){}

	/** The arcane's light: a scroll's runes and glow where nothing else is given. */
	public static final int SCROLL = Element.ARCANE.light;
	/** A scroll's read: its runes (at most), over how long; its glow. */
	public static final int READ_RUNES = 8;
	public static final float READ_FOR = 0.5f, READ_GLOW = 0.3f;
	/** Magic mapping: its wave's speed (px/s), the cells it lights at most, how far it reaches (cells). */
	public static final float WAVE_SPEED = 240f;
	public static final int WAVE_CELLS = 60, WAVE_REACH = 7;
	public static final float WAVE_ALPHA = 0.12f;
	/** A shock ring's widths and life. */
	public static final int SHOCK_FROM = 13, SHOCK_TO = 55;
	public static final float SHOCK_LIFE = 0.5f;
	/** A potion's fizz as it is drunk; its glass as it breaks; its stain. */
	public static final int FIZZ_MIN = 3, FIZZ_MAX = 4, SHARDS_MIN = 3, SHARDS_MAX = 4;
	public static final float STAIN_ALPHA = 0.4f, STAIN_FOR = 1.2f;
	/** Notes and hearts sent flying at most, and how long they take. */
	public static final int FLIERS = 6;
	public static final float FLIGHT = 0.6f;

	// ------------------------------------------------------------------ scrolls

	/** A scroll read: runes rising off the reader's hands over the read, a glow in its colour. */
	public static void read( final Char reader, final int color ){
		Fx.post( () -> {
			CharSprite s = reader.sprite;
			if (s == null || !s.visible || !Arc.onScene()) return;
			float x = s.x + s.width() / 2f, y = s.y + s.height() / 2f + 2;
			FxLight l = Arc.glow( x, y, 11, color, READ_GLOW );
			if (l != null) l.life( 0.08f, READ_FOR * 0.6f, 0.25f );
			for (int i = 0; i < READ_RUNES; i++){
				FxParticle p = Arc.light( x + FxRandom.Float( -4f, 4f ), y, FxBudget.P2 );
				if (p == null) break;
				//one after another over the read: each shows a little later than the last
				float late = READ_FOR * i / READ_RUNES;
				p.look( FxFrames.RUNE_3[FxRandom.Int( FxFrames.RUNE_3.length )] ).paint( color )
						.vel( FxRandom.Float( -3f, 3f ), -FxRandom.Float( 10f, 16f ) ).drag( 1f )
						.life( FxRandom.Float( 0.5f, 0.7f ) + late ).alpha( 1f, 0.05f + late, 0.25f ).snap();
			}
		} );
	}

	/** An upgrade: a gold ghost of the one upgraded rising off him. */
	public static void upgrade( final Char ch ){
		Fx.post( () -> {
			if (ch.sprite == null || !ch.sprite.visible || !Arc.onScene()) return;
			Afterimage.of( ch.sprite, 0xFFE070, 0.45f, 0.5f, 0, -3 );
			FxLight l = Arc.glow( ch.sprite.x + ch.sprite.width() / 2f, ch.sprite.y + ch.sprite.height() / 2f, 23, 0xFFE070, 0.3f );
			if (l != null) l.life( 0.05f, 0.1f, 0.4f );
		} );
	}

	/** A curse lifted: ink pulled out of him and dissolving, holy motes rising. */
	public static void uncurse( final Char ch ){
		Fx.post( () -> {
			if (ch.sprite == null || !ch.sprite.visible || !Arc.onScene()) return;
			float x = ch.sprite.x + ch.sprite.width() / 2f, y = ch.sprite.y + ch.sprite.height() / 2f;
			for (int i = 0; i < 6; i++){
				float a = (float)(2 * Math.PI * i / 6) + FxRandom.Float( -0.3f, 0.3f );
				FxParticle p = Arc.matter( x, y, FxBudget.P1 );
				if (p == null) break;
				p.look( FxFrames.INK_WISP, 6 ).paint( 0x2E1648 ).polar( a, FxRandom.Float( 14f, 22f ) ).drag( 2f )
						.life( FxRandom.Float( 0.6f, 0.9f ) ).alpha( 0.85f, 0, 0.5f ).smooth();
			}
			for (int i = 0; i < 6; i++){
				FxParticle p = Arc.light( x + FxRandom.Float( -6f, 6f ), y + FxRandom.Float( -2f, 6f ), FxBudget.P2 );
				if (p == null) break;
				p.look( FxFrames.MOTE_2 ).paint( 0xFFE070 ).vel( 0, -FxRandom.Float( 10f, 18f ) ).drag( 1f )
						.life( FxRandom.Float( 0.6f, 0.9f ) ).alpha( 1f, 0.1f, 0.3f ).snap();
			}
		} );
	}

	/**
	 * A teleport from one cell to another: an implosion (8 motes in 0.15 s) and a shaft where it
	 * left (if seen), a taller shaft, a ring and (on water) a splash where it arrives (if seen).
	 */
	public static void teleport( final int from, final int to, final boolean seenFrom, final boolean seenTo ){
		Fx.post( () -> {
			if (!Arc.onScene() || Dungeon.level == null) return;
			int w = Dungeon.level.width();
			if (seenFrom && from >= 0 && from != to){
				float x = (from % w + 0.5f) * DungeonTilemap.SIZE, y = (from / w + 0.5f) * DungeonTilemap.SIZE;
				BoltFx.implosion( x, y - 2, 0xE0D0FF, 8, 12f, 0.15f );
				BoltFx.shaft( x, y + Arc.GROUND, FxFrames.Light.SHAFT_32, 0xE0D0FF, 0.5f, 0.2f );
			}
			if (seenTo && to >= 0) arrive( (to % w + 0.5f) * DungeonTilemap.SIZE, (to / w + 0.5f) * DungeonTilemap.SIZE );
		} );
	}

	/** Arriving at a cell's middle (x, y): a shaft of light, a ring, a splash on water. Render thread. */
	public static void arrive( float x, float y ){
		float gy = y + Arc.GROUND;
		BoltFx.shaft( x, gy, FxFrames.Light.SHAFT_40, 0xE8E0FF, 0.6f, 0.3f );
		if (WaterFX.onWater( x, gy )) WaterFX.splash( x, gy, WaterFX.M, Element.NONE );
		else Arc.groundRing( x, gy, 9, 23, 0.4f, 0xE0D0FF, 0.5f, true );
		BoltFx.pool( x, gy, 23, 0xE0D0FF, 0.3f, 0.4f );
	}

	/** Magic mapping: a wave of pale light across the floor from the reader, and a star on every
	 *  secret it uncovers (secret()). */
	public static void mapping( final Char reader ){
		Fx.post( () -> {
			Level level = Dungeon.level;
			if (reader.sprite == null || level == null || !Arc.onScene()) return;
			int w = level.width(), hx = reader.pos % w, hy = reader.pos / w;
			int made = 0;
			for (int r = 1; r <= WAVE_REACH && made < WAVE_CELLS; r++){
				for (int dy = -r; dy <= r && made < WAVE_CELLS; dy++){
					for (int dx = -r; dx <= r && made < WAVE_CELLS; dx++){
						//the ring r cells out, every other cell of it
						if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r || Math.floorMod( dx + dy, 2 ) != 0) continue;
						int x = hx + dx, y = hy + dy;
						if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) continue;
						int c = x + y * w;
						if (!level.passable[c] || !level.discoverable[c]) continue;
						float px = (x + 0.5f) * DungeonTilemap.SIZE, py = (y + 0.5f) * DungeonTilemap.SIZE;
						float d = (float)Math.sqrt( dx * dx + dy * dy ) * DungeonTilemap.SIZE;
						ArcFlash f = ArcFlash.light( px, py + 2, FxFrames.Light.POOL_15, 0xC8E0FF, WAVE_ALPHA, 0.3f );
						if (f == null) return;
						f.after( d / WAVE_SPEED ).fade( 0.06f, 0.2f ).seenAt( px, py - Arc.GROUND );
						made++;
					}
				}
			}
		} );
	}

	/** A secret found: a four-pointed star on its cell. */
	public static void secret( final int cell ){
		Fx.post( () -> {
			if (Dungeon.level == null || !Arc.onScene()) return;
			int w = Dungeon.level.width();
			float x = (cell % w + 0.5f) * DungeonTilemap.SIZE, y = (cell / w + 0.5f) * DungeonTilemap.SIZE;
			ArcFlash s = ArcFlash.at( false, true, x, y, FxFrames.STAR4_7, 0xE0F0FF, 1f, 0.5f );
			if (s != null) s.swap( FxFrames.CROSS_5, 0.1f ).fade( 0.05f, 0.3f );
		} );
	}

	/** A recharge: as its stream of energy converges, a flash on him. */
	public static void recharge( final Char ch ){
		Fx.post( () -> {
			if (ch.sprite == null || !ch.sprite.visible || !Arc.onScene()) return;
			float x = ch.sprite.x + ch.sprite.width() / 2f, y = ch.sprite.y + ch.sprite.height() / 2f;
			ArcFlash f = ArcFlash.light( x, y, FxFrames.Light.GLOW_15, 0xFFF0A0, 0.6f, 0.3f );
			if (f != null) f.after( 0.85f ).fade( 0.03f, 0.22f );
		} );
	}

	/** A shock of fear, rage or anti-magic from him: a ring 13 to 55 px across in its colour, and
	 *  (for terror) six wisps of ink thrown out. */
	public static void shock( final Char ch, final int color, final boolean ink ){
		Fx.post( () -> {
			if (ch.sprite == null || !ch.sprite.visible || !Arc.onScene()) return;
			float x = ch.sprite.x + ch.sprite.width() / 2f, y = ch.sprite.y + ch.sprite.height() / 2f;
			Arc.groundRing( x, y + Arc.GROUND, SHOCK_FROM, SHOCK_TO, SHOCK_LIFE, color, 0.6f, true );
			BoltFx.pool( x, y + Arc.GROUND, 31, color, 0.25f, 0.4f );
			if (!ink) return;
			for (int i = 0; i < 6; i++){
				FxParticle p = Arc.matter( x, y, FxBudget.P2 );
				if (p == null) break;
				p.look( FxFrames.INK_WISP, 6 ).paint( 0x1A0C2C ).polar( (float)(2 * Math.PI * i / 6), FxRandom.Float( 20f, 30f ) )
						.drag( 2.5f ).life( 0.7f ).alpha( 0.8f, 0, 0.4f ).smooth();
			}
		} );
	}

	/** A blast of light from him (a retribution, a sun): a bloom where the screen once only flashed. */
	public static void bloom( final Char ch ){
		Fx.post( () -> {
			if (ch.sprite == null || !Arc.onScene()) return;
			FxRecipes.bloom( ch.sprite.x + ch.sprite.width() / 2f, ch.sprite.y + ch.sprite.height() / 2f, 0xFFF4D0, 126, 0.7f, 0.4f );
		} );
	}

	/** The same from a cell (a potion of the sun shattering there). */
	public static void bloom( final int cell ){
		Fx.post( () -> {
			if (Dungeon.level == null || !Arc.onScene()) return;
			int w = Dungeon.level.width();
			FxRecipes.bloom( (cell % w + 0.5f) * DungeonTilemap.SIZE, (cell / w + 0.5f) * DungeonTilemap.SIZE, 0xFFF4D0, 126, 0.7f, 0.4f );
		} );
	}

	/** Notes (or hearts) flying from him to the mobs he sees within `radius` cells, FLIERS at
	 *  most, the nearest first found. Picked on the caller's thread (as it reads the mobs). */
	public static void flyToNear( Char from, int radius, int speck ){
		Level level = Dungeon.level;
		if (level == null || level.heroFOV == null) return;
		Char[] to = new Char[FLIERS];
		int n = 0;
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (n >= FLIERS) break;
			if (m != from && level.heroFOV[m.pos] && level.distance( from.pos, m.pos ) <= radius) to[n++] = m;
		}
		if (n > 0) fly( from, to, speck );
	}

	/** Notes (or hearts) flying from him to those who hear them, FLIERS at most. */
	public static void fly( final Char from, final Char[] to, final int speck ){
		Fx.post( () -> {
			if (from.sprite == null || !Arc.onScene()) return;
			FxEmitter air = Fx.air();
			if (air == null) return;
			float x = from.sprite.x + from.sprite.width() / 2f, y = from.sprite.y + from.sprite.height() / 2f - 4;
			int n = 0;
			for (Char c : to){
				if (n >= FLIERS) break;
				if (c == null || c.sprite == null || !c.sprite.visible) continue;
				float tx = c.sprite.x + c.sprite.width() / 2f, ty = c.sprite.y + c.sprite.height() / 2f - 4;
				Speck s = (Speck) air.recycle( Speck.class );
				s.reset( 0, x, y, speck );
				s.speed.set( (tx - x) / FLIGHT, (ty - y) / FLIGHT );
				s.acc.set( 0, 0 );
				n++;
			}
		} );
	}

	/** A potion strengthening him: arrows rising, a warm glow. */
	public static void strength( final Char ch ){
		Fx.post( () -> {
			if (ch.sprite == null || !ch.sprite.visible || !Arc.onScene()) return;
			float x = ch.sprite.x + ch.sprite.width() / 2f, y = ch.sprite.y + ch.sprite.height() / 2f;
			FxEmitter e = GameScene.fxEmitter();
			if (e != null){
				e.pos( x - 6, y - 4, 12, 8 );
				e.burst( Speck.factory( Speck.UP ), 4 );
			}
			FxLight l = Arc.glow( x, y, 23, 0xFFB070, 0.4f );
			if (l != null) l.life( 0.05f, 0.15f, 0.4f );
		} );
	}

	// ------------------------------------------------------------------ potions

	/** A potion drunk: three or four bubbles of its colour fizzing up at his mouth. */
	public static void drink( final Char ch, final int color ){
		Fx.post( () -> {
			if (ch.sprite == null || !ch.sprite.visible || !Arc.onScene()) return;
			float x = ch.sprite.x + ch.sprite.width() / 2f, y = ch.sprite.y + ch.sprite.height() * 0.4f;
			int n = FxRandom.IntRange( FIZZ_MIN, FIZZ_MAX );
			for (int i = 0; i < n; i++){
				FxParticle p = Arc.matter( x + FxRandom.Float( -2f, 2f ), y, FxBudget.P2 );
				if (p == null) break;
				p.look( FxFrames.BUBBLE_2 ).paint( color ).vel( FxRandom.Float( -2f, 2f ), -10f ).life( 0.4f )
						.alpha( 0.9f, 0.05f, 0.15f ).at( p.px, p.py - i ).snap();
			}
		} );
	}

	/**
	 * A potion's glass breaking at a world point (its splash already made, as ever): three or
	 * four shards turning a quarter at a time, falling, landing and glinting, and a stain of its
	 * colour; on water a ring of its colour instead of the stain.
	 */
	public static void shatter( final float x, final float y, final int color ){
		Fx.post( () -> {
			if (!Arc.onScene()) return;
			float gy = y + 4;
			boolean water = WaterFX.onWater( x, gy );
			int n = FxRandom.IntRange( SHARDS_MIN, SHARDS_MAX );
			for (int i = 0; i < n; i++){
				FxParticle p = Arc.matter( x, y, FxBudget.P2 );
				if (p == null) break;
				p.look( FxFrames.SHARD_GLASS[FxRandom.Int( FxFrames.SHARD_GLASS.length )] ).paint( 0xCCD8E0 )
						.polar( FxRandom.Float( (float)-Math.PI, 0 ), FxRandom.Float( 30f, 60f ) ).gravity( 240f ).spin( 0.08f )
						.ground( gy + FxRandom.Float( -2f, 3f ), water ? FxParticle.RING : FxParticle.SETTLE, 0 ).landDry( FxParticle.SETTLE )
						.strength( 0.1f ).life( 0.6f ).alpha( 1f, 0, 0.3f ).fate( GLINT_ON_LANDING );
			}
			if (water){
				WaterFX.drop( x, gy, 0.5f, color );
				WaterFX.ring( x, gy, WaterFX.M, color, 0.6f );
			} else {
				FxDecal.at( x, gy, FxFrames.WET_9, color, STAIN_ALPHA, 0.1f, STAIN_FOR, FxRandom.chance( 0.5f ) );
			}
		} );
	}

	/** The same at a cell's middle. */
	public static void shatter( int cell, int color ){
		Level level = Dungeon.level;
		if (level == null || cell < 0) return;
		int w = level.width();
		shatter( (cell % w + 0.5f) * DungeonTilemap.SIZE, (cell / w + 0.5f) * DungeonTilemap.SIZE, color );
	}

	//a shard's glint as it comes to rest
	private static final FxParticle.Fate GLINT_ON_LANDING = ( p, landed ) -> {
		if (landed && FxRandom.chance( 0.6f )) Arc.glint( p.px, p.py - 1, 0xFFFFFF );
	};
}
