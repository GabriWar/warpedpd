/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

package xyz.gabriwar.warpedpixeldungeon.levels.traps;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Sheep;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokoban;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanBlack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanCorner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanStop;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanSwitch;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;

public class FleecingTrap extends Trap {

	{
		color = VIOLET;
		shape = WAVES;
	}

	@Override
	public void activate() {

		Char ch = Actor.findChar(pos);

		if (ch instanceof Sheep
				|| ch instanceof SheepSokoban
				|| ch instanceof SheepSokobanCorner
				|| ch instanceof SheepSokobanSwitch
				|| ch instanceof SheepSokobanBlack
				|| ch instanceof SheepSokobanStop) {
			// Sheep are instantly destroyed by the fleecing trap
			Camera.main.shake(2, 0.3f);
			ch.destroy();
			ch.sprite.killAndErase();
			CellEmitter.get(pos).burst(ShadowParticle.UP, 5);
			return;
		}

		if (ch != null) {
			Camera.main.shake(2, 0.3f);
			CellEmitter.get(pos).burst(ShadowParticle.UP, 5);

			if (ch == Dungeon.hero) {
				Hero hero = Dungeon.hero;

				//shears half of the hero's current HP. below 10% of max HP
				//there is nothing left to shear - the trap kills outright
				if (hero.HP <= hero.HT / 10) {
					GLog.n(Messages.get(this, "shear_kill"));
					hero.damage(hero.HP, this);
				} else {
					GLog.n(Messages.get(this, "shear"));
					hero.damage(hero.HP / 2, this);
				}
				Sample.INSTANCE.play(Assets.Sounds.CURSED);
			}
			// Non-hero, non-sheep characters: visual effect only (no damage in Sprouted)
		}
	}
}
