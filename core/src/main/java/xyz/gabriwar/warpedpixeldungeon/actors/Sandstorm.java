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

package xyz.gabriwar.warpedpixeldungeon.actors;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sandblown;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.misc.DesertWear;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

/**
 * The desert's sandstorm: the climate's wind (ClimateManager) lifting the sand wherever it blows
 * at WIND m/s or more over desert under the surface's open sky, the very rule its sound and its
 * driven sand are laid by (WeatherSounds.sandstorm, WeatherOverlay.sandstorm). It is the place's
 * own weather, as the swamps' dawn fog is: OverworldLevel.localSandstorm hands the scene's
 * answer to ClimateManager.isSandstorm.
 *
 * How hard it blows over a cell (strength): from FLOOR at WIND to full at FULL, as the overlay
 * draws it. Whoever stands in it out in the open takes it (exposure), unless the wind's sand
 * cannot reach him: inside a room he built (homestead/Shelter), or in the lee of something solid
 * standing upwind of him within LEE cells (a wall, a boulder, a palm, a statue, a house). A hero
 * carrying sand goggles or a desert scarf (DesertWear) takes none of it.
 *
 * What it does to the exposed:
 *   - sight: every eye in it sees SIGHT_LEAST of its distance at full, half of it as it begins
 *     (Level.updateFieldOfView; the beasts that see in the dark, as the night spares them, too);
 *   - a hero's legs: his steps take longer, SLOW_LEAST of his speed at full (Hero.speed);
 *   - a hero's eyes: a BLIND_BASE + BLIND_RISE chance a turn of sand in them, blind for
 *     BLIND_TURNS turns, then BLIND_REST turns before it can happen again (Sandblown).
 * His Sandblown buff shows while it lasts and says all of it.
 */
public final class Sandstorm {

	private Sandstorm(){}

	/** m/s: where the sand lifts (WeatherScape's gale), and where it blows its hardest. */
	public static final float WIND = 10f, FULL = 20f;
	/** Its strength as it begins, of its full (WeatherOverlay's SAND_FLOOR). */
	public static final float FLOOR = 0.3f;
	/** How far upwind something solid shelters a cell: the cell beside him, or the one past it. */
	public static final int LEE = 2;
	/** The share of sight kept: half as it begins, a quarter at full. */
	public static final float SIGHT_START = 0.5f, SIGHT_LEAST = 0.25f;
	/** The share of a hero's speed kept: four fifths as it begins, two thirds at full. */
	public static final float SLOW_START = 0.8f, SLOW_LEAST = 0.67f;
	/** The chance a turn of sand in an exposed hero's eyes: BLIND_BASE + BLIND_RISE at full. */
	public static final float BLIND_BASE = 0.03f, BLIND_RISE = 0.05f;
	/** Blind for this long, and this long before it can happen again. */
	public static final float BLIND_TURNS = 2f;
	public static final int BLIND_REST = 4;

	// ------------------------------------------------------------------ where it blows

	/** How hard the sand blows over a cell of a level now, 0..1: 0 for none. */
	public static float strengthAt( Level level, int cell ){
		if (!(level instanceof OverworldLevel) || cell < 0 || cell >= level.length()) return 0f;
		OverworldLevel ow = (OverworldLevel) level;
		if (ow.altitude() != 0 || !ow.openSky()) return 0f;
		float wind = ClimateManager.localWindSpeed();
		if (wind < WIND) return 0f;
		//a blizzard is its own wind (WeatherScape.wind): no sand blows in it
		if (ClimateManager.localPrecipType() == PrecipType.BLIZZARD && ClimateManager.localPrecipRate() > 0f) return 0f;
		//in a room the hero built the weather stays outside (homestead/Shelter)
		if (!xyz.gabriwar.warpedpixeldungeon.homestead.Shelter.windReaches( level, cell )) return 0f;
		if (!ow.desertAt( cell )) return 0f;
		return strength( wind );
	}

	/** Its strength at a wind blowing over the desert: FLOOR at WIND, full at FULL and over. */
	public static float strength( float wind ){
		if (wind < WIND) return 0f;
		float t = Math.min( 1f, (wind - WIND) / (FULL - WIND) );
		return FLOOR + (1f - FLOOR) * t;
	}

	/** The step the wind's sand comes at a cell from: the neighbour upwind, as a cell offset
	 *  {dx, dy}. The climate's wind direction is where it blows TO (0 north, 90 east). */
	public static int[] upwind( float windDir ){
		double a = Math.toRadians( windDir );
		//where it blows to, on the screen: east is +x, north is -y; the sand comes from the other way
		int dx = -(int) Math.round( Math.sin( a ) );
		int dy = (int) Math.round( Math.cos( a ) );
		return new int[]{ dx, dy };
	}

	/** Is a cell in the lee: something solid (not water, not a pit) upwind of it within LEE cells? */
	public static boolean sheltered( Level level, int cell ){
		int w = level.width();
		int[] up = upwind( ClimateManager.surfaceWindDir() );
		int x = cell % w, y = cell / w;
		for (int k = 1; k <= LEE; k++){
			int nx = x + up[0] * k, ny = y + up[1] * k;
			if (nx < 0 || ny < 0 || nx >= w || ny >= level.height()) return false;
			int n = nx + ny * w;
			if (level.solid[n] && !level.water[n] && !level.pit[n]) return true;
		}
		return false;
	}

	/** Does a char carry what keeps the sand off: sand goggles or a desert scarf (DesertWear)? */
	public static boolean wears( Char ch ){
		return ch instanceof Hero && ((Hero) ch).belongings != null
				&& ((Hero) ch).belongings.getItem( DesertWear.class ) != null;
	}

	/** How hard the sand takes a char where it stands now: the strength over its cell, 0 in the lee
	 *  or with goggles or a scarf. */
	public static float exposure( Char ch ){
		if (ch == null || Dungeon.level == null) return 0f;
		float s = strengthAt( Dungeon.level, ch.pos );
		if (s <= 0f || wears( ch ) || sheltered( Dungeon.level, ch.pos )) return 0f;
		return s;
	}

	/** The share of its sight a char keeps in the sand (Level.updateFieldOfView): 1 out of it. */
	public static float sightFactor( Char ch ){
		return sightFactor( exposure( ch ) );
	}

	public static float sightFactor( float exposure ){
		if (exposure <= 0f) return 1f;
		float t = (exposure - FLOOR) / (1f - FLOOR);
		return SIGHT_START + (SIGHT_LEAST - SIGHT_START) * Math.max( 0f, Math.min( 1f, t ) );
	}

	/** The share of his speed a hero keeps in the sand (Hero.speed): 1 out of it. */
	public static float speedFactor( Hero hero ){
		return speedFactor( exposure( hero ) );
	}

	public static float speedFactor( float exposure ){
		if (exposure <= 0f) return 1f;
		float t = (exposure - FLOOR) / (1f - FLOOR);
		return SLOW_START + (SLOW_LEAST - SLOW_START) * Math.max( 0f, Math.min( 1f, t ) );
	}

	/** The chance a turn that the sand gets in an exposed hero's eyes. */
	public static float blindChance( float exposure ){
		if (exposure <= 0f) return 0f;
		float t = (exposure - FLOOR) / (1f - FLOOR);
		return BLIND_BASE + BLIND_RISE * Math.max( 0f, Math.min( 1f, t ) );
	}

	// ------------------------------------------------------------------ a hero's turn in it

	/**
	 * Once a hero turn (Hero.checkWeatherEffects, on the host for every hero): caught in it out in
	 * the open he is Sandblown and may be blinded a while; in the lee, or with his goggles or scarf,
	 * he is told once that it does not reach him. The lines go to that hero's own log.
	 */
	public static void onHeroTurn( Hero hero ){
		if (Dungeon.level == null || !hero.isAlive()) return;
		float strength = strengthAt( Dungeon.level, hero.pos );
		Sandblown blown = hero.buff( Sandblown.class );
		if (strength <= 0f){
			if (blown != null) blown.detach();
			return;
		}
		boolean wears = wears( hero ), lee = !wears && sheltered( Dungeon.level, hero.pos );
		boolean fresh = blown == null;
		if (fresh) blown = Buff.affect( hero, Sandblown.class );
		blown.set( strength, wears, lee );
		if (fresh){
			NetManager.heroLog( hero, GLog.WARNING + Messages.get( Sandblown.class, wears ? "onset_worn" : lee ? "onset_lee" : "onset" ) );
			xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.reveal( xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.TAG_SANDSTORM );
		}
		float exposure = wears || lee ? 0f : strength;
		if (exposure > 0f && blown.mayBlind() && hero.buff( Blindness.class ) == null
				&& Random.Float() < blindChance( exposure )){
			Buff.prolong( hero, Blindness.class, BLIND_TURNS );
			blown.blinded();
			NetManager.heroLog( hero, GLog.NEGATIVE + Messages.get( Sandblown.class, "blinded" ) );
			if (hero == Dungeon.hero) Dungeon.observe();
		}
	}

}
