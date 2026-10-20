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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.PlaceLight;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ChimneySmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.HomesteadStation;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound;
import xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

/**
 * A homestead station's sprite (homestead/stations): the station's own frame of
 * sprites/homestead.png at rest and its work frames while it is busy (a kiln's, a forge's or a
 * hearth's fire, the cooking pot's steam, a hide on the tanning rack). Furniture: it never turns
 * to face anything.
 *
 * Each station has its own kind of it (Workbench .. Forge below), which knows the station's look
 * by itself: a co-op guest is sent only the sprite's name (NetSprites.wireName) and draws its
 * stand-in (SpectatorReceiver.SpectatorMob) with it. Whether the station is busy is read every
 * frame from the station on the host, and from the stand-in on a guest (its "working", sent with
 * the mob as StateSerializer's "wk").
 *
 * A station with a fire (FIRE kinds), while busy - its fire lit - also gives light: a glow in the
 * air and a pool on the floor, held as a place's light (PlaceLight: only the stations nearest the
 * hero hold one, within the lights' budget), made again whenever the station's cell is not the one
 * it was made for (a move of the overworld's window: shiftWorld). It drops an ember now and then,
 * and is heard: the kiln and the forge roar (the KILN sound, played again before each play ends so
 * it runs on as one roar), the hearth crackles, from its cell (SpatialSound), quieter the further
 * the hero is, and not at all past HEARD cells. Its chimney smokes, gently: a column of the
 * chimneys' grey puffs (Smoke) out of the top of its frame, leaning with the wind over the fog
 * where the hero has seen its cell, a puff every SMOKE_EVERY s (fewer as the effects' budget thins);
 * lit while the sprite watched, its smoke starts at the mouth, found lit it is already smoking, and
 * put out it thins away.
 */
public abstract class StationSprite extends MobSprite {

	/** What fire a station holds: none, or the kiln's, the forge's or the hearth's. */
	public enum Fire { NONE, KILN, FORGE, HEARTH }

	/** How far off a fire is heard, cells. */
	public static final int HEARD = 8;
	//the kiln's roar is played again every 1.2-1.4 s (docs/homesteads-art-items.md); the crackle
	//(3.2 s long) a little before each one ends
	private static final float ROAR_MIN = 1.2f, ROAR_MAX = 1.4f, CRACKLE_MIN = 2.6f, CRACKLE_MAX = 3.0f;
	private static final float ROAR_VOLUME = 0.6f, CRACKLE_VOLUME = 0.7f;
	private static final float EMBER_MIN = 2f, EMBER_MAX = 5f;
	/** A puff of its chimney's smoke every this many s, by fire (none, the kiln's, the forge's, the
	 *  hearth's); how far the budget may thin them (to a quarter). */
	public static final float[] SMOKE_EVERY = { 0f, 0.4f, 0.45f, 0.5f };
	public static final float SMOKE_FLOOR = 0.25f;
	//its chimney's mouth in its frame {x, y, w, h}, by fire: the kiln's vent, the forge's hood, the
	//hearth's flue
	private static final int[][] MOUTH = { null, { 6, 0, 4, 2 }, { 6, 0, 4, 1 }, { 5, 0, 6, 1 } };

	private final Fire fire;
	private final Animation still, working;
	private boolean showingWork;
	private PlaceLight light;
	//the cell the light was made for
	private int lightCell = -1;
	private float soundIn, emberIn;
	//its chimney's smoke while its fire burns, and whether this sprite has seen its fire out (lit
	//after that, the smoke starts at the mouth)
	private Smoke smoke;
	private boolean wasOut;

	/** A station's look: its frame at rest, its work frames (null to keep its frame) played at
	 *  `fps`, and its fire. */
	protected StationSprite( int frame, Object[] work, float fps, Fire fire ){
		super();
		this.fire = fire;

		texture( HomesteadSprites.TEXTURE );
		TextureFilm film = HomesteadSprites.film();
		still = new Animation( 1, true );
		still.frames( film, frame );
		if (work != null){
			working = new Animation( Math.round( fps ), true );
			working.frames( film, work );
		} else {
			working = null;
		}
		looks( still );
	}

	//every slot a station could play: its look at rest, and clones of it (never the idle instance
	//itself, see TemperingAnvilSprite)
	private void looks( Animation rest ){
		idle = rest;
		run = rest.clone();
		attack = rest.clone();
		die = rest.clone();
		play( idle, true );
	}

	@Override
	public void linkVisuals( Char ch ){
		super.linkVisuals( ch );
		showingWork = false;
		looks( still );
	}

	@Override
	public void turnTo( int from, int to ){
		//furniture faces where it was built
	}

	//is the station hard at work: the host's station itself, or a guest's stand-in for it
	private boolean busy(){
		Char c = ch;
		if (c instanceof HomesteadStation) return ((HomesteadStation) c).busy();
		if (c instanceof SpectatorReceiver.SpectatorMob) return ((SpectatorReceiver.SpectatorMob) c).working;
		return false;
	}

	@Override
	public void update(){
		super.update();
		if (ch == null) return;

		boolean busy = busy();
		if (busy != showingWork && working != null){
			showingWork = busy;
			looks( busy ? working : still );
		}

		if (fire == Fire.NONE || !busy || ch.pos < 0){
			putOutLight();
			putOutSmoke();
			wasOut = true;
			return;
		}
		if (light != null && lightCell != ch.pos) putOutLight();
		if (light == null){
			light = makeLight( ch.pos );
			lightCell = ch.pos;
		}
		smoke();
		if (visible){
			light.update();
			if ((emberIn -= Game.elapsed) <= 0){
				emberIn = FxRandom.Float( EMBER_MIN, EMBER_MAX );
				ember();
			}
		}
		if ((soundIn -= Game.elapsed) <= 0){
			soundIn = fire == Fire.HEARTH ? FxRandom.Float( CRACKLE_MIN, CRACKLE_MAX ) : FxRandom.Float( ROAR_MIN, ROAR_MAX );
			sound();
		}
	}

	private void putOutLight(){
		if (light != null) light.out();
		light = null;
		lightCell = -1;
	}

	//its chimney's smoke, made as its fire is lit (or found lit) and kept over its mouth: the scene's
	//slide of the overworld's window moves the two together, a co-op guest's re-labelled window
	//places the sprite again and the smoke follows it here
	private void smoke(){
		int[] m = MOUTH[fire.ordinal()];
		float mx = x + m[0], my = y + m[1];
		if (smoke == null || !smoke.exists || smoke.parent == null){
			float every = SMOKE_EVERY[fire.ordinal()] / Math.max( SMOKE_FLOOR, FxBudget.factor( FxBudget.P2 ) );
			smoke = wasOut ? ChimneySmokeParticle.kindle( Smoke.class, mx, my, m[2], m[3], every, 0, 0, true )
					: ChimneySmokeParticle.column( Smoke.class, mx, my, m[2], m[3], every, 0, 0, true );
			return;
		}
		if (smoke.x != mx || smoke.y != my) smoke.slide( mx - smoke.x, my - smoke.y );
	}

	//its fire out: the smoke thins away, and the column is the scene's again once it has
	private void putOutSmoke(){
		if (smoke != null && smoke.exists && smoke.parent != null) smoke.on = false;
		smoke = null;
	}

	/** A station's chimney smoke: the chimneys' column, but a kind of its own, so the scene never
	 *  hands a station a column SettlementAmbience still holds as it fades, nor the other way round. */
	public static final class Smoke extends ChimneySmokeParticle.Column {}

	//the overworld's window moved under it: the light, made for a cell of the old window, is made
	//again for the station's new cell on the next frame
	@Override
	public void shiftWorld( float sx, float sy ){
		super.shiftWorld( sx, sy );
		putOutLight();
	}

	//the fire's light: a hearth's warm and wide, a forge's fierce, a kiln's low through its mouth
	private PlaceLight makeLight( int cell ){
		PointF p = DungeonTilemap.tileCenterToWorld( cell );
		float floor = p.y + DungeonTilemap.SIZE / 2f + 2;
		switch (fire){
			case HEARTH:
				return new PlaceLight( cell, p.x, p.y + 2, 31, p.x, floor, 23, 0xFFA050, 0.85f, FxLight.HEARTH );
			case FORGE:
				return new PlaceLight( cell, p.x, p.y, 31, p.x, floor, 15, 0xFF8030, 0.8f, FxLight.TORCH );
			default:
				return new PlaceLight( cell, p.x, p.y + 3, 23, p.x, floor, 15, 0xFF9040, 0.6f, FxLight.HEARTH );
		}
	}

	//a spark out of the fire's mouth, falling a little and going out (as a prison torch's)
	private void ember(){
		PointF p = DungeonTilemap.tileCenterToWorld( ch.pos );
		float fx = p.x, fy = p.y + 2;
		FxEmitter glow = Fx.glowAir();
		if (glow == null || !FxBudget.keep( FxBudget.P3, fx, fy )) return;
		((FxParticle) glow.recycle( FxParticle.class )).reset( fx + FxRandom.Float( -2f, 2f ), fy )
				.look( FxFrames.EMBER_1 ).ramp( Element.FIRE.ember, true ).life( 0.8f ).alpha( 1f, 0, 0.3f )
				.vel( FxRandom.Float( -4f, 4f ), -10f ).gravity( 30f )
				.ground( fy + FxRandom.Float( 6f, 9f ), FxParticle.DIE, 0 ).fate( FxFactories.EMBER_END )
				.snap().priority( FxBudget.P3 );
	}

	//the fire heard from its cell, quieter the further off and silent past HEARD; round a wall it
	//is muffled as anything heard out of sight is (SpatialSound.playUnseen)
	private void sound(){
		Level l = Dungeon.level;
		int cell = ch.pos;
		if (l == null || Dungeon.hero == null || cell < 0 || cell >= l.length()) return;
		int d = l.distance( cell, Dungeon.hero.pos );
		if (d > HEARD) return;
		float near = 1f - d / (float)(HEARD + 1);
		boolean hearth = fire == Fire.HEARTH;
		Object id = hearth ? AmbientSound.CRACKLE.takes[0] : Assets.Sounds.KILN;
		float volume = (hearth ? CRACKLE_VOLUME : ROAR_VOLUME) * near;
		float pitch = hearth ? FxRandom.Float( 0.95f, 1.08f ) : fire == Fire.FORGE ? 1.1f : 0.9f;
		if (l.heroFOV != null && l.heroFOV[cell]) SpatialSound.play( id, cell, volume, pitch );
		else SpatialSound.playUnseen( id, cell, volume, pitch );
	}

	@Override
	public void kill(){
		super.kill();
		putOutLight();
		putOutSmoke();
	}

	// ----------------------------------------------------------------- each station's

	public static class Workbench extends StationSprite {
		public Workbench(){
			super( HomesteadSprites.WORKBENCH, null, 0, Fire.NONE );
		}
	}

	public static class MasonsBench extends StationSprite {
		public MasonsBench(){
			super( HomesteadSprites.MASONS_BENCH, null, 0, Fire.NONE );
		}
	}

	public static class Quern extends StationSprite {
		public Quern(){
			super( HomesteadSprites.QUERN, null, 0, Fire.NONE );
		}
	}

	//a hide hangs on it while it has work
	public static class TanningRack extends StationSprite {
		public TanningRack(){
			super( HomesteadSprites.TANNING_RACK, HomesteadSprites.frames( HomesteadSprites.TANNING_RACK_HIDE, 1 ), 8, Fire.NONE );
		}
	}

	//it steams while it cooks
	public static class CookingPot extends StationSprite {
		public CookingPot(){
			super( HomesteadSprites.COOKING_POT, HomesteadSprites.frames( HomesteadSprites.COOKING_POT_STEAM, HomesteadSprites.STEAM_FRAMES ), 3, Fire.NONE );
		}
	}

	public static class Kiln extends StationSprite {
		public Kiln(){
			super( HomesteadSprites.KILN, HomesteadSprites.frames( HomesteadSprites.KILN_LIT, HomesteadSprites.LIT_FRAMES ), 8, Fire.KILN );
		}
	}

	public static class Forge extends StationSprite {
		public Forge(){
			super( HomesteadSprites.FORGE, HomesteadSprites.frames( HomesteadSprites.FORGE_LIT, HomesteadSprites.LIT_FRAMES ), 10, Fire.FORGE );
		}
	}

	public static class Hearth extends StationSprite {
		public Hearth(){
			super( HomesteadSprites.HEARTH, HomesteadSprites.frames( HomesteadSprites.HEARTH_LIT, HomesteadSprites.LIT_FRAMES ), 8, Fire.HEARTH );
		}
	}
}
