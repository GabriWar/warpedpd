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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * The hero's weapon's night light: an emissive enchantment (fire, lightning, holy light, a glowing
 * metal...) in his hand casts one lasting GLOW_7 at LEVEL on his weapon side, breathing 8% either
 * way (FxLight.BREATH), shown as lasting lights are (strongest at night: FxLight's own day share).
 * It asks FxLight.room(1, 1) first and, finding none, asks again later (a lasting light never puts
 * another out); it follows his sprite (FxLight.follow), turning with him; it goes out while he is
 * invisible or shows no emissive look, and when his sprite goes. One at most: the local hero's.
 * Render thread (WieldLook steps it from his sprite's draw).
 */
public final class WeaponLight {

	private WeaponLight(){}

	/** Its level; how far off his middle it sits (Crackle.WEAPON_SIDE), and how far down. */
	public static final float LEVEL = 0.15f, SIDE = 5f, DOWN = 1f;
	/** How long it waits to ask again when there was no room. */
	public static final float RETRY = 0.5f;

	private static FxLight light;
	private static int setups;
	private static CharSprite holder;
	private static float retryAt = -1e9f;

	/** Once a frame for the hero's sprite: his weapon's emissive colour, or -1 for none. */
	public static void step( CharSprite s, int color, float now ){
		boolean want = color >= 0 && s != null && s.ch != null && s.visible && s.parent != null && s.ch.invisible <= 0;
		if (!want || s != holder){
			out( 0.3f );
			if (!want) return;
		}
		float dx = s.flipHorizontal ? -SIDE : SIDE;
		if (!held()){
			if (now < retryAt) return;
			if (!FxLight.room( 1, 1 )){
				retryAt = now + RETRY;
				return;
			}
			float cx = s.x + s.width() / 2f + dx, cy = s.y + s.height() / 2f + DOWN;
			light = FxLight.glow( cx, cy, 7, color, LEVEL ).persistent().flicker( FxLight.BREATH );
			setups = light.setups();
			holder = s;
		}
		light.hardlight( color );
		light.follow( s, dx, DOWN );
	}

	/** Whether it is lit now. */
	public static boolean held(){
		return light != null && light.alive && light.setups() == setups;
	}

	/** Put out over `fade` s and let go of. Render thread. */
	public static void out( float fade ){
		if (held()) light.putOut( fade );
		light = null;
		holder = null;
	}

	/** His sprite went (any thread): the light goes with it. */
	public static void gone( final CharSprite s ){
		Fx.post( () -> {
			if (s == holder) out( 0.2f );
		} );
	}
}
