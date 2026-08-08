/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.DumplingBomb;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.RiceBall;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class Rice extends Item {

	public static final String AC_COOK = "COOK";
	public static final String AC_COOKBOMB = "COOKBOMB";

	public static final float TIME_TO_COOK = 1;
	public static final float TIME_TO_COOK_BOMB = 4;

	private static final int BOMB_COST = 5;

	{
		image = ItemSpriteSheet.SEED_RICE;

		unique = true;

		defaultAction = AC_COOK;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_COOK);
		actions.add(AC_COOKBOMB);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_COOK)) {

			hero.spend(TIME_TO_COOK);
			hero.busy();
			hero.sprite.operate(hero.pos);

			RiceBall riceball = new RiceBall();
			if (riceball.doPickUp(hero, hero.pos)) {
				GLog.i(Messages.capitalize(Messages.get(this, "cooked", riceball.name())));
				Statistics.ballsCooked++;
			} else {
				Dungeon.level.drop(riceball, hero.pos).sprite.drop();
				Statistics.ballsCooked++;
			}

			checkDurability();

		} else if (action.equals(AC_COOKBOMB)) {

			hero.spend(TIME_TO_COOK_BOMB);
			hero.busy();
			hero.sprite.operate(hero.pos);

			DumplingBomb bomb = new DumplingBomb();
			if (bomb.doPickUp(hero, hero.pos)) {
				GLog.i(Messages.capitalize(Messages.get(this, "cooked", bomb.name())));
				Statistics.ballsCooked += BOMB_COST;
			} else {
				Dungeon.level.drop(bomb, hero.pos).sprite.drop();
				Statistics.ballsCooked += BOMB_COST;
			}

			checkDurability();
		}
	}

	private void checkDurability() {
		if (Statistics.ballsCooked > 200) {
			detach(Dungeon.hero.belongings.backpack);
			GLog.n(Messages.get(this, "crumbles"));
		} else if (Statistics.ballsCooked > 175) {
			GLog.w(Messages.get(this, "cracking"));
		} else if (Statistics.ballsCooked > 150) {
			GLog.w(Messages.get(this, "dull"));
		}
	}

	@Override
	public boolean doPickUp(Hero hero, int pos) {
		if (super.doPickUp(hero, pos)) {
			if (Dungeon.level != null && Dungeon.depth == 32) {
				for (Mob mob : Dungeon.level.mobs) {
					mob.beckon(Dungeon.hero.pos);
				}
				GLog.w(Messages.get(this, "oni"));
				CellEmitter.center(Dungeon.hero.pos).start(
						Speck.factory(Speck.SCREAM), 0.3f, 3);
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 10 * quantity;
	}
}
