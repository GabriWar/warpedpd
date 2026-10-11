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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;

/**
 * How each burning blob looks (docs/fx/fire-heat.md): its flame's element, its embers, the smoke
 * it sheds (none for the clean fires), the glow on its floor and the light it throws, the motes
 * among its flames, how fast it burns relative to plain fire, and its quirks (an inferno's vortex
 * and shimmer, a coal bed's low burn).
 */
public enum FireStyle {

	//                flame                ember                      smoke                                  floor     floorLv light     lightLv  motes     every  rate  kind
	FIRE(        Element.FIRE,        Element.FIRE.ember,        new Ramp( 0x8A847C, 0x5E5850, 0x3E3A34 ), 0xFF8A30, 0.35f, 0xFF9A48, 0.35f, -1,       0f,    1f,   Kind.OPEN ),
	HELL(        Element.HELL,        Element.HELL.ember,        new Ramp( 0x3A2426, 0x2A1C1E, 0x241618 ), 0xFF4A30, 0.35f, 0xFF5040, 0.35f, -1,       0f,    1f,   Kind.OPEN ),
	FIEND(       Element.FIEND,       Element.FIEND.ember,       Element.FIEND.smoke,                     0xF0507A, 0.32f, 0xFF6A8A, 0.35f, -1,       0f,    1f,   Kind.OPEN ),
	MAGIC(       Element.MAGIC_FIRE,  Element.MAGIC_FIRE.ember,  null,                                    0xE8B8FF, 0.32f, 0xE8B8FF, 0.35f, 0xFFF0FF, 0.5f,  1f,   Kind.OPEN ),
	HALOMETHANE( Element.HALOMETHANE, Element.HALOMETHANE.ember, null,                                    0x40CCFF, 0.30f, 0x60D8FF, 0.30f, 0xC8F8FF, 0.6f,  0.85f, Kind.OPEN ),
	INFERNO(     Element.FIRE,        Element.FIRE.ember,        new Ramp( 0x5A524A, 0x3A3430, 0x241E1A ), 0xFF7A20, 0.45f, 0xFF9A48, 0.60f, -1,       0f,    1.4f, Kind.INFERNO ),
	FIREWIND(    Element.FIRE,        Element.FIRE.ember,        null,                                    0,        0f,    0xFFA060, 0.30f, -1,       0f,    1f,   Kind.WIND ),
	FORGE(       Element.FIRE,        Element.FIRE.ember,        null,                                    0xFFD080, 0.55f, 0xFFB060, 0.45f, -1,       0f,    1f,   Kind.COAL ),
	CAMP(        Element.FIRE,        Element.FIRE.ember,        null,                                    0xFF6020, 0.35f, 0xFF9A48, 0.35f, -1,       0f,    1f,   Kind.COAL ),
	NEST(        Element.FIRE,        Element.FIRE.ember,        null,                                    0xFF6020, 0.35f, 0xFF9A48, 0.30f, -1,       0f,    1f,   Kind.COAL ),
	//its particles are its own (SacrificialParticle); only its floor glow and its light are a field's
	SACRIFICIAL( Element.SACRIFICIAL, Element.SACRIFICIAL.ember, null,                                    0x6AA8FF, 0.15f, 0x6AA8FF, 0.30f, -1,       0f,    1f,   Kind.OPEN );

	/** What burns: an open fire, an inferno, a wind of fire, a bed of coals. */
	public enum Kind { OPEN, INFERNO, WIND, COAL }

	public final Element flame;
	public final Ramp ember;
	/** The smoke's ramp, null for a clean fire. */
	public final Ramp smoke;
	/** The glow on its floor (0 for none) and its level. */
	public final int floor;
	public final float floorLevel;
	/** The light it throws and its base level (a 2x2 block's light: base + 0.1 a cell). */
	public final int light;
	public final float lightLevel;
	/** Motes among its flames (-1 for none), one a cell every `moteEvery` s. */
	public final int motes;
	public final float moteEvery;
	/** How fast it burns, against plain fire. */
	public final float rate;
	public final Kind kind;

	FireStyle( Element flame, Ramp ember, Ramp smoke, int floor, float floorLevel, int light, float lightLevel,
			int motes, float moteEvery, float rate, Kind kind ){
		this.flame = flame;
		this.ember = ember != null ? ember : Flames.emberRamp( flame );
		this.smoke = smoke;
		this.floor = floor;
		this.floorLevel = floorLevel;
		this.light = light;
		this.lightLevel = lightLevel;
		this.motes = motes;
		this.moteEvery = moteEvery;
		this.rate = rate;
		this.kind = kind;
	}

	/** Whether a cell of this volume burns (a coal bed's cells burn over 100; the rest over 0). */
	public boolean burning( int cur ){
		return kind == Kind.COAL ? cur > 100 : cur > 0;
	}

	/** How its floor glow flickers: a bed of coals breathes, the rest flicker as a hearth. */
	public int floorFlicker(){
		return kind == Kind.COAL ? FxLight.BREATH : FxLight.HEARTH;
	}

	/** Whether it always stands in shimmer (the inferno, the forge), not only where it is thick. */
	public boolean shimmers(){
		return this == INFERNO || this == FORGE;
	}
}
