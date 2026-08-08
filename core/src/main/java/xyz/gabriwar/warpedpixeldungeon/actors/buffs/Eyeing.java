/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

// OV: reveals all hidden traps and secret doors on the current level
public class Eyeing extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	public static final float DURATION = 10f;

	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )) {
			revealSecrets();
			return true;
		}
		return false;
	}

	private void revealSecrets() {
		int length = Dungeon.level.length();
		int[] map = Dungeon.level.map;
		boolean[] discoverable = Dungeon.level.discoverable;

		boolean noticed = false;

		for (int i = 0; i < length; i++) {
			int terr = map[i];
			if (discoverable[i] && (Terrain.flags[terr] & Terrain.SECRET) != 0) {
				Dungeon.level.discover( i );
				if (Dungeon.level.heroFOV[i]) {
					GameScene.discoverTile( i, terr );
					CellEmitter.get( i ).start( Speck.factory( Speck.DISCOVER ), 0.1f, 4 );
					noticed = true;
				}
			}
		}

		GameScene.updateFog();

		if (noticed) {
			Sample.INSTANCE.play( Assets.Sounds.SECRET );
		}
	}

	@Override
	public void fx( boolean on ) {
		if (on) target.sprite.add( CharSprite.State.ILLUMINATED );
		else    target.sprite.remove( CharSprite.State.ILLUMINATED );
	}

	@Override
	public int icon() {
		return BuffIndicator.MIND_VISION;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 1f, 1f, 0.5f );
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, (DURATION - visualcooldown()) / DURATION );
	}

	@Override
	public String toString() {
		return Messages.get( this, "name" );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc", dispTurns() );
	}
}
