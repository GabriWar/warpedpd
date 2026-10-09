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

package xyz.gabriwar.warpedpixeldungeon.levels.ambience;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.WeatherState;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Comfy;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherOverlay;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.particles.Emitter;

/**
 * The weather heard around the hero (docs/ambience.md, "Weather"): the rain, soft and dark, from
 * either side, on the water and the leaves near him; the wind from its side when it really
 * blows, a gust as the rain leans over; the snow, the sleet, the hail, the blizzard, the sand;
 * thunder, far in heavy rain and with every strike of a storm; the trees dripping in the fog and
 * after the rain. Indoors only the rain on the roof and the storm's thunder, half as loud; in the
 * overworld's caves only the storm's thunder through the rock; deeper than the climate reaches,
 * nothing. Scheduled by a WeatherScape, on the ambience channel.
 *
 * A Gizmo GameScene adds right after AmbientSounds on every level: what it plays there it
 * decides itself. Render thread: it rolls AmbientPlayer's dice and only reads the climate.
 */
public final class WeatherSounds extends Gizmo {

	//the climate is looked at again this often (seconds)
	private static final float SKY_EVERY = 1f;
	//a frame longer than this (a hitch, the level loading) counts as only this long
	private static final float MAX_FRAME = 0.25f;

	private final Level level;
	final WeatherScape scape = new WeatherScape( AmbientPlayer.RNG );
	/** where the sounds go out: the ambience channel (a list in the tests) */
	WeatherScape.Out out = new Channel();
	private final Soundscape.Ground ground = new Soundscape.Ground();
	private float skyIn;
	//the overworld window's origin the cell kept was found in
	private int wx, wy;
	//the weather heard on the level shown: the storm's strikes are handed to it (strike)
	private static WeatherSounds heard;

	private WeatherSounds( Level level ){
		this.level = level;
		scape.sky.where = where( level );
		if (level instanceof OverworldLevel){
			OverworldLevel ow = (OverworldLevel) level;
			ground.world = ow;
			wx = ow.worldX();
			wy = ow.worldY();
		}
	}

	/** The weather's sounds on a level: every level has them, even where they are silent. */
	public static WeatherSounds forLevel( Level level ){
		return new WeatherSounds( level );
	}

	/**
	 * Whether the sand blows where the hero stands on a level, by the climate as it is now: the rule
	 * the sandstorm bed is laid by (WeatherScape.wind: open sky in the desert, 10 m/s and more, no
	 * blizzard heard), so the overlay draws the sandstorm (WeatherOverlay.sandstorm) just where it is
	 * heard. A blizzard landing on neither side of the hero (on thawed sand) is not heard, as in
	 * WeatherScape.tick: the sand blows.
	 */
	public static boolean sandstorm( Level level ){
		if (level == null || Dungeon.hero == null) return false;
		return WeatherScape.wind( where( level ), falling( level ), ClimateManager.localWindSpeed(), desert( level, Dungeon.hero.pos ) )
				== AmbientSound.SANDSTORM;
	}

	/**
	 * Whether the blizzard is heard where the hero stands on a level, by the climate as it is now:
	 * its bed laid (WeatherScape.fall: under the open sky or on a floor the climate reaches) on
	 * snowed-under ground to one side of him at least (WeatherScape.fallsOn), so the overlay draws
	 * the blizzard's own whites and driven snow (WeatherOverlay.blizzard) just where it is heard.
	 */
	public static boolean blizzard( Level level ){
		if (level == null || Dungeon.hero == null) return false;
		return falling( level ) == AmbientSound.BLIZZARD;
	}

	//the bed of what falls heard where the hero stands, by the climate as it is now: null for none,
	//and for one landing on neither side of him (WeatherScape.tick lays it on neither)
	private static AmbientSound falling( Level level ){
		PrecipType type = ClimateManager.localPrecipType();
		AmbientSound falling = WeatherScape.fall( where( level ), type, type == PrecipType.NONE ? 0f : ClimateManager.localPrecipRate() );
		Soundscape.Ground g = new Soundscape.Ground().set( level.map, level.width(), level.height(), Dungeon.hero.pos, null );
		g.world = level instanceof OverworldLevel ? (OverworldLevel) level : null;
		if (WeatherScape.fallsOn( g, falling, -1 ) == 0f && WeatherScape.fallsOn( g, falling, 1 ) == 0f) return null;
		return falling;
	}

	//the hero on the surface in the desert: the wind lifts the sand
	private static boolean desert( Level level, int cell ){
		if (!(level instanceof OverworldLevel)) return false;
		OverworldLevel ow = (OverworldLevel) level;
		return ow.altitude() == 0 && ow.biomeAtCell( cell ) == WorldModel.Biome.DESERT;
	}

	/** What reaches the hero on a level, of the weather. */
	static WeatherScape.Where where( Level level ){
		if (Comfy.indoors( level )) return WeatherScape.Where.INDOORS;
		if (level instanceof OverworldLevel){
			return ((OverworldLevel) level).openSky() ? WeatherScape.Where.OPEN : WeatherScape.Where.CAVE;
		}
		//the climate reaches the floors above the city, fainter the deeper (ClimateManager)
		return level.climateDepth() < 16 ? WeatherScape.Where.EXPOSED : WeatherScape.Where.SILENT;
	}

	@Override
	public void update(){
		super.update();
		if (Dungeon.level != level) return;
		heard = this;
		float dt = Math.min( Game.elapsed, MAX_FRAME );
		if (Dungeon.hero == null || !Dungeon.hero.isAlive() || !AmbientPlayer.audible()){
			scape.hush( dt );
			return;
		}
		//time stands still (the hourglass, swiftthistle), and so does the weather's clock
		if (Emitter.freezeEmitters) return;
		//the overworld's window is moving: its map and the hero's cell are not all in one frame
		if (level.fogHeld()) return;
		follow();
		if ((skyIn -= dt) <= 0f){
			skyIn = SKY_EVERY;
			readSky();
		}
		ground.set( level.map, level.width(), level.height(), Dungeon.hero.pos, level.heroFOV );
		scape.tick( dt, ground, WeatherSprites.gust(), out );
	}

	@Override
	public void destroy(){
		if (heard == this) heard = null;
		super.destroy();
	}

	/**
	 * A strike (WeatherOverlay: a storm's or a storm cloud's): its thunder, `tiles` away and `dx`
	 * tiles to the hero's right, `seen` when he sees the bolt, or sheet lightning far behind the
	 * clouds. False when no weather is heard here now (none on the level shown, the ambience not
	 * heard, the hero dead): the overlay's own crack plays instead, so a strike is never silent. A
	 * level the weather never reaches takes a strike out of sight and stays quiet, and rings with
	 * one in sight. False too while its clock holds (time frozen, the window moving): the overlay
	 * keeps flashing, and thunder queued on a clock standing still would all come at once after.
	 * And false for a bolt in sight the weather's thunder would not ring for with its flash (in its
	 * first quiet, over a crack still rolling: WeatherScape.strike).
	 */
	public static boolean strike( float dx, float tiles, boolean sheet, boolean seen ){
		WeatherSounds w = heard;
		if (w == null || w.level != Dungeon.level || Dungeon.hero == null || !Dungeon.hero.isAlive()
				|| !AmbientPlayer.audible() || Emitter.freezeEmitters || w.level.fogHeld()) return false;
		return w.scape.strike( dx, tiles, sheet, seen );
	}

	/** When the overlay's own crack plays after its flash, where the weather is not heard: as the
	 *  weather's thunder would start (WeatherScape.delay). */
	public static float thunderDelay( float tiles, boolean sheet, boolean seen ){
		return WeatherScape.delay( tiles, WeatherScape.cracks( tiles, sheet, seen ) );
	}

	//the overworld's window moved under the hero: the cell kept goes with it
	private void follow(){
		OverworldLevel ow = ground.world;
		if (ow == null) return;
		int x = ow.worldX(), y = ow.worldY();
		if (x == wx && y == wy) return;
		scape.shift( x - wx, y - wy, level.width(), level.height() );
		wx = x;
		wy = y;
	}

	private void readSky(){
		WeatherScape.Sky sky = scape.sky;
		PrecipType type = ClimateManager.localPrecipType();
		float rate = type == PrecipType.NONE ? 0f : ClimateManager.localPrecipRate();
		if (sky.where == WeatherScape.Where.INDOORS){
			//nothing falls indoors, but on the roof: the surface's, unless a debug rain (which
			//falls indoors too) is heavier
			PrecipType up = ClimateManager.precipType();
			if (type == PrecipType.NONE) type = up;
			rate = Math.max( rate, up == PrecipType.NONE ? 0f : ClimateManager.precipRate() );
		}
		sky.type = type;
		sky.rate = type == PrecipType.NONE ? 0f : rate;
		sky.wind = ClimateManager.localWindSpeed();
		sky.windDir = ClimateManager.surfaceWindDir();
		sky.storm = WeatherOverlay.netStormingOverride != null ? WeatherOverlay.netStormingOverride : ClimateManager.isStorming();
		sky.dripping = ClimateManager.isFoggy() || ClimateManager.weatherState() == WeatherState.CLEARING;
		OverworldLevel ow = ground.world;
		sky.peaks = ow != null && ow.altitude() > 0;
		sky.desert = desert( level, Dungeon.hero.pos );
	}

	//the ambience channel: the take, the pitch, the level and the pan as the scheduler picked them
	private static final class Channel implements WeatherScape.Out {
		@Override
		public void play( int bed, AmbientSound sound, int take, float level, float pitch, float pan ){
			AmbientPlayer.playTake( sound.takes[take], sound.gain, level, pitch, pan );
		}
	}
}
