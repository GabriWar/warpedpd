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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LivingPlant;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfEarthenArmor;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRage;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

public class PlantSummon extends Spell {

	{
		image = ItemSpriteSheet.PLANT_SUMMON;
		talentChance = 1 / (float) Recipe.OUT_QUANTITY;
	}

	@Override
	protected void onCast(Hero hero) {
		new Flare(8, 14).color(0x33AA55, true).show(hero.sprite, 0.5f);
		Invisibility.dispel();
		GLog.h(Messages.get(this, "awaken"));

		ArrayList<Plant> plantList = new ArrayList<>();
		for (Plant p : Dungeon.level.plants.values()) plantList.add(p);
		//Plant.spawnLivingPlant() already does this properly - it wakes the plant into
		//HUNTING, accepts `avoid` cells (which is where plants actually grow), pushes and
		//bursts leaves. This was hand-rolling a worse copy that left the whole summoned
		//army asleep, and withered the plant only when a cell happened to be free.
		for (Plant plant : plantList) {
			if (Dungeon.level.heroFOV[plant.pos] && plant.spawnLivingPlant()) {
				plant.wither();
			}
		}

		Sample.INSTANCE.play(Assets.Sounds.READ);
		detach(curUser.belongings.backpack);
		updateQuickslot();
		curUser.spendAndNext(Actor.TICK);
	}

	@Override
	public int value() {
		return Math.round(quantity * ((30 + 50) / 6f));
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		static final int OUT_QUANTITY = 6;

		{
			inputs     = new Class[]{ScrollOfRage.class, PotionOfEarthenArmor.class};
			inQuantity = new int[]{1, 1};
			cost       = 3;
			output     = PlantSummon.class;
			outQuantity = OUT_QUANTITY;
		}
	}
}
