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

package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.HearthLight;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

/**
 * Smoke from a chimney or a fire: a thin column of puffs that climb, slow, swell and thin
 * out about three cells up, leaning the way the wind blows (both ways: a wind up the screen
 * draws the column out, one down it squashes it). Rain thins it, a storm cuts it short and
 * lays it nearly flat. It lives in a Column, the one smoke emitter of the settlements. A
 * house set alight in a raid (RaidEvent) pours SOOT instead: thick black puffs from the first,
 * climbing higher and thinning slower. A hot spring of the peaks (MountainSites) breathes STEAM:
 * thin white wisps that die a cell or so up, so a bather in the pool stays in plain sight. Under
 * no sky (a fire of the caves' places, CaveSites) the air is still: no wind, no storm, the column
 * goes straight up whatever the weather does above.
 */
public class ChimneySmokeParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory(){
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			((ChimneySmokeParticle) emitter.recycle( ChimneySmokeParticle.class )).reset( x, y );
		}
	};

	/** A burning house's smoke: thick and black (RaidEvent, through SettlementAmbience). */
	public static final Emitter.Factory SOOT = new Emitter.Factory(){
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			((ChimneySmokeParticle) emitter.recycle( ChimneySmokeParticle.class )).reset( x, y, true );
		}
	};

	/** A hot spring's steam: thin, pale and short-lived (MountainSites, through SettlementAmbience). */
	public static final Emitter.Factory STEAM = new Emitter.Factory(){
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			((ChimneySmokeParticle) emitter.recycle( ChimneySmokeParticle.class )).reset( x, y, false, true );
		}
	};

	//a mid grey: light enough to read on the dark ground, dark enough to read on snow
	private static final float R = 0xA0 / 255f, G = 0xA0 / 255f, B = 0xA8 / 255f;
	//soot: near black, a touch of brown from what is burning; after dark the fire under it lights
	//its lower puffs a dull ember red, or black smoke on the night sky would not be seen at all
	private static final float SOOT_R = 0x1E / 255f, SOOT_G = 0x1A / 255f, SOOT_B = 0x18 / 255f;
	private static final float EMBER_R = 0x8A / 255f, EMBER_G = 0x3A / 255f, EMBER_B = 0x12 / 255f;
	//steam: near white, a breath of blue
	private static final float STEAM_R = 0xE6 / 255f, STEAM_G = 0xEA / 255f, STEAM_B = 0xEE / 255f;
	//the climb slows by this much of its first speed every second
	private static final float SLOWING = 0.06f;

	private int stage;
	private float peak, sway, rise;
	private boolean storm, soot, steam, still;
	//the wind's pull on the puff just now, px/s east and south (see pull)
	private float windX, windY;

	public ChimneySmokeParticle(){
		super();
		color( R, G, B );
	}

	public void reset( float x, float y ){
		reset( x, y, false );
	}

	public void reset( float x, float y, boolean soot ){
		reset( x, y, soot, false );
	}

	public void reset( float x, float y, boolean soot, boolean steam ){
		revive();
		this.soot = soot;
		this.steam = steam;
		//centred on the spawn point: the first frame is 3x3
		this.x = x - 1;
		this.y = y - 1;
		//the surface's wind reaches no deeper than the sky does
		still = Dungeon.level instanceof OverworldLevel && !((OverworldLevel) Dungeon.level).openSky();
		storm = !still && ClimateManager.isStorming();
		//a storm cuts it short
		left = lifespan = Random.Float( 5f, 6.5f ) * (storm ? 0.55f : 1f) * (soot ? 1.2f : 1f);
		rise = Random.Float( 9f, 12f ) * (storm ? 0.5f : 1f) * (soot ? 1.3f : 1f);
		speed.set( Random.Float( -0.6f, 0.6f ), -rise );
		//the slowing climb is worked into the speed itself (update), so no acceleration
		acc.set( 0, 0 );
		//rain thins it
		peak = soot ? 0.7f : ClimateManager.isRaining() ? 0.36f : 0.5f;
		if (steam){
			//wisps off warm water: about a cell and a half up, thin enough to see through
			left = lifespan = Random.Float( 2.4f, 3.2f );
			rise = Random.Float( 6f, 8f );
			speed.set( Random.Float( -0.6f, 0.6f ), -rise );
			peak = 0.32f;
		}
		sway = Random.Float( 6.283f );
		stage = -1;
		frame = null;
		//soot billows out of the fire already a big puff
		grow( soot ? 2 : 0 );
	}

	//a puff already t seconds into its life: where it would be by now, as it would look
	//(a fresh column starts as a column, not as a puff at the mouth)
	private void age( float t ){
		t = Math.min( t, lifespan * 0.98f );
		left = lifespan - t;
		float l = lifespan;
		//the lean, integrated over the age: k * (0.25 t + 0.375 t^2 / l)
		float lean = 0.25f * t + 0.375f * t * t / l;
		pull();
		x += windX * lean;
		y += -rise * t + 0.5f * SLOWING * rise * t * t + windY * lean;
		speed.set( windX * (0.25f + 0.75f * t / l), -rise * (1f - SLOWING * t) + windY * (0.25f + 0.75f * t / l) );
		float age = t / l;
		if (!soot) grow( age < 0.22f ? 0 : age < 0.55f ? 1 : 2 );
	}

	//the wind's pull on the puff, px/s east and south (surfaceWindDir is where it blows TO)
	private void pull(){
		if (still){
			windX = windY = 0;
			return;
		}
		float rad = (float) Math.toRadians( ClimateManager.surfaceWindDir() );
		float k = ClimateManager.localWindSpeed() * (storm ? 1.1f : 0.45f) * (1f + 0.3f * WeatherSprites.gust());
		windX = (float) Math.sin( rad ) * k;
		windY = -(float) Math.cos( rad ) * k * 0.6f;
	}

	private void grow( int s ){
		if (s == stage) return;
		int[] was = frame;
		stage = s;
		frame( s == 0 ? WeatherSprites.GLOW_3 : s == 1 ? WeatherSprites.GLOW_5 : WeatherSprites.PUFF_S );
		//it swells about its middle
		if (was != null){
			x += (was[2] - frame[2]) / 2f;
			y += (was[3] - frame[3]) / 2f;
		}
	}

	@Override
	public void update(){
		super.update();
		float dt = Game.elapsed, age = 1f - left / lifespan;
		if (!soot) grow( age < 0.22f ? 0 : age < 0.55f ? 1 : 2 );
		//the wind takes the column more the higher it climbs
		pull();
		float lean = 0.25f + 0.75f * age;
		float wantX = windX * lean + (float) Math.sin( sway + age * 6f ) * 0.8f;
		float wantY = -rise * (1f - SLOWING * (lifespan - left)) + windY * lean;
		float ease = Math.min( 1f, dt * 1.5f );
		speed.x += (wantX - speed.x) * ease;
		speed.y += (wantY - speed.y) * ease;
		float seen = parent instanceof Column ? ((Column) parent).seen : 1f;
		am = envelope( 0.1f, 0.6f, peak ) * seen;
		//drawn above the night's tint, so it darkens itself
		float tint = GameScene.nightTintAlpha(), k = 1f - 0.85f * tint;
		if (steam){
			color( STEAM_R * k, STEAM_G * k, STEAM_B * k );
		} else if (soot){
			//firelit from below in the dark (the night's tint is about a quarter): the glow fades
			//out halfway up
			float glow = Math.min( 1f, tint * 3f ) * Math.max( 0f, 1f - age * 2f ) * 0.55f;
			color( SOOT_R * k + (EMBER_R - SOOT_R * k) * glow, SOOT_G * k + (EMBER_G - SOOT_G * k) * glow,
					SOOT_B * k + (EMBER_B - SOOT_B * k) * glow );
		} else {
			color( R * k, G * k, B * k );
		}
	}

	/**
	 * A column of smoke over the fog: shown where the hero sees (or has seen) its gate - the
	 * cell (gateDX, gateDY) from the one under the column's own top-left, read off its own
	 * position, so a window slide never leaves it stale. A gate other than that cell is a
	 * house front (a camera-facing wall, seen only from outside), and the column also shows
	 * whenever its own cell is in sight; ungated it always shows.
	 */
	public static class Column extends Emitter {

		public int gateDX, gateDY;
		public boolean gated;
		//a burning house's: it pours SOOT; a hot spring's: STEAM
		public boolean soot, steam;
		float seen;

		/** What it pours: STEAM for a spring, SOOT for a fire, FACTORY for a chimney. */
		public Emitter.Factory puffs(){
			return steam ? STEAM : soot ? SOOT : FACTORY;
		}

		@Override
		public void update(){
			seen += ((gated ? gate() : 1f) - seen) * Math.min( 1f, Game.elapsed * 4f );
			super.update();
		}

		/** Moves the column and every puff in it by (dx, dy) scene pixels: a network mirror's
		 *  window was re-labelled under it without a slide of the scene (SettlementAmbience.Mirror). */
		public void slide( float dx, float dy ){
			x += dx;
			y += dy;
			for (int i = 0; i < length; i++){
				Gizmo g = members.get( i );
				if (g instanceof Visual){
					((Visual) g).x += dx;
					((Visual) g).y += dy;
				}
			}
		}

		private float gate(){
			if (Dungeon.level == null || x + 8 < 0 || y + 8 < 0) return 0f;
			int w = Dungeon.level.width();
			int cx = (int)((x + 8) / DungeonTilemap.SIZE), cy = (int)((y + 8) / DungeonTilemap.SIZE);
			if (cx >= w) return 0f;
			int own = cx + cy * w;
			if (gateDX == 0 && gateDY == 0) return HearthLight.groundSeen( own );
			int gx = cx + gateDX;
			if (gx < 0 || gx >= w) return HearthLight.groundSeen( own );
			return Math.max( HearthLight.faceSeen( gx + (cy + gateDY) * w ), HearthLight.groundSeen( own ) );
		}
	}

	/** A smoke column with its mouth at (x, y, w, h), a puff every `interval` seconds, already
	 *  smoking. Render thread only; null without a scene. */
	public static Column column( float x, float y, float w, float h, float interval, int gateDX, int gateDY, boolean gated ){
		return column( Column.class, x, y, w, h, interval, gateDX, gateDY, gated, false, false );
	}

	/** The same pouring SOOT when `soot`: a house on fire. */
	public static Column column( float x, float y, float w, float h, float interval, int gateDX, int gateDY, boolean gated,
			boolean soot ){
		return column( Column.class, x, y, w, h, interval, gateDX, gateDY, gated, soot, false );
	}

	/** The same pouring STEAM when `steam`: a hot spring. */
	public static Column column( float x, float y, float w, float h, float interval, int gateDX, int gateDY, boolean gated,
			boolean soot, boolean steam ){
		return column( Column.class, x, y, w, h, interval, gateDX, gateDY, gated, soot, steam );
	}

	/** The same, in a column of a kind of its own: the scene hands a dead column back only to its
	 *  own exact class, so smoke kept apart from the chimneys' is never lit in one of theirs that
	 *  SettlementAmbience still holds as it fades, nor the other way round (WorldEventDecor). */
	public static <T extends Column> T column( Class<T> kind, float x, float y, float w, float h, float interval,
			int gateDX, int gateDY, boolean gated ){
		return column( kind, x, y, w, h, interval, gateDX, gateDY, gated, false, false );
	}

	//a column recycled by the scene may have been either kind: set to the one asked for
	private static <T extends Column> T column( Class<T> kind, float x, float y, float w, float h, float interval,
			int gateDX, int gateDY, boolean gated, boolean soot, boolean steam ){
		T made = GameScene.overFogEmitter( kind );
		if (made == null) return null;
		Column c = made;
		c.gateDX = gateDX;
		c.gateDY = gateDY;
		c.gated = gated;
		c.soot = soot;
		c.steam = steam;
		c.pos( x, y, w, h );
		c.seen = gated ? c.gate() : 1f;
		c.pour( c.puffs(), interval );
		for (int i = 0; i < 8; i++){
			ChimneySmokeParticle p = (ChimneySmokeParticle) c.recycle( ChimneySmokeParticle.class );
			p.reset( x + Random.Float( w ), y + Random.Float( h ), soot, steam );
			p.age( Random.Float( p.lifespan ) );
		}
		return made;
	}
}
