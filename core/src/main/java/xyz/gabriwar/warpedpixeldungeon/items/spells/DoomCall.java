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

package xyz.gabriwar.warpedpixeldungeon.items.spells;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRage;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTerror;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class DoomCall extends Spell {

	{
		image = ItemSpriteSheet.DOOMCALL;
		talentChance = 1 / (float) Recipe.OUT_QUANTITY;
	}

	@Override
	protected void onCast(Hero hero) {
		ArrayList<Mob> mobs = new ArrayList<>();
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			mobs.add(mob);
		}
		if (!mobs.isEmpty()) {
			Mob mob = Random.element(mobs);
			if (!mob.properties().contains(Char.Property.BOSS)
					&& !mob.properties().contains(Char.Property.MINIBOSS)) {
				mob.die(hero);
				if (Dungeon.level.heroFOV[mob.pos]) {
					Sample.INSTANCE.play(Assets.Sounds.HIT);
					mob.sprite.emitter().burst(ShadowParticle.CURSE, 6);
					GLog.p(Messages.get(this, "crushed", mob.name()));
				}
			}
		}
		Invisibility.dispel();
		detach(curUser.belongings.backpack);
		updateQuickslot();
		curUser.spendAndNext(Actor.TICK);
	}

	@Override
	public int value() {
		return Math.round(quantity * ((30 + 30) / 6f));
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		static final int OUT_QUANTITY = 6;

		{
			inputs     = new Class[]{ScrollOfRage.class, ScrollOfTerror.class};
			inQuantity = new int[]{1, 1};
			cost       = 3;
			output     = DoomCall.class;
			outQuantity = OUT_QUANTITY;
		}
	}
}
