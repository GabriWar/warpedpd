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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.effects.CritterSprite;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.particles.Emitter;

import java.util.List;

/**
 * The small life of a slice of the world - the caves' (CaveLife) or the peaks' (PeakLife) -
 * run by the critter Field (OverworldCritters) in place of the surface's birds and hares.
 * Pictures and sounds only: no Actor, nothing saved, nothing to fight, the world's map read
 * but never written. Render thread, like the Field; the map is read only while it is the
 * one the pictures stand on (Field.frameAgrees).
 *
 * What may be out where and when is decided by the pure rules (allowed, and each life's
 * own), which the tests pin. Around the hero it looks once a step - or every two seconds,
 * so a crystal mined from where he stands goes dark without his moving - and caches what
 * it saw: nothing scans the window each frame.
 */
abstract class SliceLife {

	enum Kind { BATS, FLITTER, DRIP, GLOW, MOTE, SPORE, EMBER, FISH, SOUND, EAGLE, MARMOT, PLUME, CLOUDS }

	//pictures this far (Chebyshev, scene px) from the hero leave: twelve cells
	static final float DESPAWN_PX = 192f;
	//random cells tried per spawn attempt: a handful, never a scan of the window
	static final int PROBES = 6;
	//the look round the hero again, when he has not moved
	private static final float RESCAN = 2f;

	final OverworldCritters.Field field;
	final OverworldLevel level;
	final int altitude;
	private int stepCell = -1;
	private float rescanIn;

	SliceLife( OverworldCritters.Field field, OverworldLevel level ){
		this.field = field;
		this.level = level;
		altitude = level.altitude();
	}

	/** The life of the level's slice, or null on the surface (the Field's own). */
	static SliceLife of( OverworldCritters.Field field, OverworldLevel level ){
		switch (OverworldCritters.sliceOf( level.altitude() )){
			case CAVES: return new CaveLife( field, level );
			case PEAKS: return new PeakLife( field, level );
			default:    return null;
		}
	}

	/**
	 * May this kind be out on this slice at this hour, in this weather, on this ground?
	 * The caves know no hour and no weather; the peaks keep the surface's.
	 */
	static boolean allowed( Kind k, int altitude, Phase phase, OverworldCritters.Sky sky, boolean frozen, float wind ){
		switch (k){
			case BATS: case FLITTER: case DRIP: case GLOW: case MOTE: case FISH: case SOUND:
				return altitude < 0;
			case SPORE: case EMBER:
				return altitude <= OverworldFauna.DEEP_CAVES;
			case EAGLE:
				return altitude > 0 && phase == Phase.DAY && sky != OverworldCritters.Sky.FOUL;
			case MARMOT:
				return altitude > 0 && (phase == Phase.DAWN || phase == Phase.DAY)
						&& sky != OverworldCritters.Sky.FOUL && !frozen;
			case PLUME:
				return altitude > 0 && frozen && wind >= PeakLife.PLUME_WIND;
			case CLOUDS:
				return altitude >= PeakLife.CLOUD_ALTITUDE;
			default:
				return false;
		}
	}

	/** Every frame the Field is not frozen: a look round at each of his steps, then the life's own turn. */
	final void update( float dt, float hx, float hy ){
		int pos = Dungeon.hero.pos;
		rescanIn -= dt;
		if ((pos != stepCell || rescanIn <= 0f) && field.frameAgrees()){
			stepCell = pos;
			rescanIn = RESCAN;
			long tm = LagMonitor.begin();
			scan( pos );
			LagMonitor.end( "OW slice scan", tm );
		}
		tick( dt, hx, hy );
	}

	/** The window moved under the pictures (a rebase, a mirror's re-label): look again. */
	void rebased(){
		stepCell = -1;
	}

	//what is near the hero, from his cell: counted and cached, the static lights set
	abstract void scan( int hcell );
	//the frame's turn: timers, spawning, the visuals that follow the hero
	abstract void tick( float dt, float hx, float hy );
	//a fight at (px, py), scene pixels
	abstract void noise( float px, float py );
	//someone at (px, py): the hero (who makes them wary too) or anyone else
	abstract void react( float px, float py, boolean hero );
	//a network mirror's window was re-labelled: what the Field does not carry, moved by hand
	abstract void slide( float dx, float dy );
	//off the scene: everything it put there
	abstract void destroy();
	//debug scenes: a full set at once around the hero, every gate but the ground's waived
	abstract String showcase();

	/** The pictures of `list` farther than DESPAWN_PX from the hero leave; the gone ones are dropped. */
	static <T extends CritterSprite> void thin( List<T> list, float hx, float hy ){
		for (int i = list.size() - 1; i >= 0; i--){
			T c = list.get( i );
			if (!c.exists){
				list.remove( i );
			} else if (OverworldCritters.cheb( c.gx(), c.gy(), hx, hy ) > DESPAWN_PX){
				c.leave( 0.4f );
			}
		}
	}

	//a local emitter for a burst of the slice's air: never relayed (a recycled emitter keeps
	//the network tag it last had), or null with no scene
	static Emitter emitter(){
		Emitter e = GameScene.emitter();
		if (e != null) e.netCell = -1;
		return e;
	}
}
