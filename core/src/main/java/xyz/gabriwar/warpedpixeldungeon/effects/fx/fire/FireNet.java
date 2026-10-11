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


package xyz.gabriwar.warpedpixeldungeon.effects.fx.fire;

import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;

/**
 * Fire and heat's looks that answer to the game itself rather than to a particle burst a co-op
 * guest already replays: a firebolt's strike, the fireblast's heat, a burnt heap's ash, a burning
 * rift's crack, firedamp arming and going up, a meteor's blast. Each is sent by the host where it
 * is called (NetVisuals.recordFire: its kind, a cell, two numbers and a list of cells) and played
 * by the guest from his own view of the level (play). Visual only. The kinds are sent as numbers:
 * add new ones at the end, never renumber.
 */
public final class FireNet {

	private FireNet(){}

	public static final int FIREBOLT = 0, HEAT = 1, ASH = 2, RIFT_GLOW = 3, RIFT_TELEGRAPH = 4, RIFT_FLARE = 5,
			DAMP_ARMING = 6, DAMP_IGNITION = 7, METEOR = 8;

	/** The host's: one of these called, sent on to the guests (nothing off a host). */
	static void send( int kind, int cell, int n, int m, int[] cells ){
		NetVisuals.recordFire( kind, cell, n, m, cells );
	}

	/** A guest's: what the host sent, played as the host's call plays it (nothing sent on). */
	public static void play( int kind, int cell, int n, int m, int[] cells ){
		switch (kind){
			case FIREBOLT: FireHits.bolt( cell ); break;
			case HEAT: FireHits.heatOver( cell, n, m ); break;
			case ASH: Ash.fall( cell, n ); break;
			case RIFT_GLOW: if (cells != null) RiftFx.keepGlowing( cells ); break;
			case RIFT_TELEGRAPH: RiftFx.swell( cell ); break;
			case RIFT_FLARE: RiftFx.burst( cell ); break;
			case DAMP_ARMING: if (cells != null) FiredampFront.start( cells, false ); break;
			case DAMP_IGNITION: if (cells != null) FiredampFront.start( cells, true ); break;
			case METEOR: Explosion.meteorHint( cell ); break;
		}
	}
}
