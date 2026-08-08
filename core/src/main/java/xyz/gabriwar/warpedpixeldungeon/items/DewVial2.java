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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Water;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class DewVial2 extends Item {

	private static final int MAX_VOLUME = 100;
	private static final int BLESS_VOLUME = 10;

	private static final String AC_DRINK  = "DRINK";
	private static final String AC_WATER  = "WATER";
	private static final String AC_SPLASH = "SPLASH";
	private static final String AC_BLESS  = "BLESS";

	private static final float TIME_TO_DRINK = 1f;
	private static final float TIME_TO_WATER = 3f;
	protected static final float TIME_TO_BLESS = 1f;

	{
		image = ItemSpriteSheet.VIAL;
		defaultAction = AC_DRINK;
		unique = true;
	}

	private int volume = 0;

	public void setVol(int vol) {
		volume = vol;
	}

	private static final String VOLUME = "volume";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(VOLUME, volume);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		volume = bundle.getInt(VOLUME);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (volume > 99) {
			actions.add(AC_DRINK);
			actions.add(AC_WATER);
			actions.add(AC_SPLASH);
			actions.add(AC_BLESS);
		} else if (volume > 70) {
			actions.add(AC_DRINK);
			actions.add(AC_WATER);
			actions.add(AC_SPLASH);
		} else if (volume > 49) {
			actions.add(AC_DRINK);
			actions.add(AC_SPLASH);
		} else if (volume > 0) {
			actions.add(AC_DRINK);
		}
		return actions;
	}

	@Override
	public void execute(final Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_DRINK)) {

			if (volume > 0) {

				int value = 1 + (Dungeon.depth - 1) / 5;
				if (hero.heroClass == HeroClass.HUNTRESS) {
					value++;
				}
				value *= volume;
				value = (int) Math.max(volume * volume * .01 * hero.HT, value);
				int effect = Math.min(hero.HT - hero.HP, value);
				if (effect > 0) {
					hero.HP += effect;
					hero.sprite.emitter().burst(Speck.factory(Speck.HEALING),
							volume > 5 ? 2 : 1);
					hero.sprite.showStatus(CharSprite.POSITIVE,
							Messages.get(this, "value", effect));
				}

				if (volume < 10) {
					volume = 0;
				} else {
					volume -= 10;
				}

				hero.spend(TIME_TO_DRINK);
				hero.busy();

				Sample.INSTANCE.play(Assets.Sounds.DRINK);
				hero.sprite.operate(hero.pos);

				updateQuickslot();

			} else {
				GLog.w(Messages.get(this, "empty"));
			}

		} else if (action.equals(AC_WATER)) {

			int length = Dungeon.level.length();
			for (int i = 0; i < length; i++) {
				if (Dungeon.level.heroFOV[i]) {
					int c = Dungeon.level.map[i];
					if (c == Terrain.GRASS) {
						GameScene.add(Blob.seed(i, 40, Water.class));
					}
				}
			}
			volume -= 2;

			GLog.i(Messages.get(this, "watered"));

			hero.sprite.operate(hero.pos);
			hero.busy();
			hero.spend(TIME_TO_WATER);

			updateQuickslot();

		} else if (action.equals(AC_SPLASH)) {

			Buff.affect(hero, Haste.class, Haste.DURATION);
			Buff.affect(hero, Invisibility.class, Invisibility.DURATION);
			GLog.i(Messages.get(this, "refreshed"));

			volume -= 10;

			hero.spend(TIME_TO_DRINK);
			hero.busy();

			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			hero.sprite.operate(hero.pos);

			updateQuickslot();

		} else if (action.equals(AC_BLESS)) {

			boolean procced = ScrollOfRemoveCurse.uncurse(hero,
					hero.belongings.weapon,
					hero.belongings.armor,
					hero.belongings.artifact,
					hero.belongings.misc,
					hero.belongings.ring);

			for (Item item : hero.belongings.backpack) {
				if (item.cursed) {
					procced = ScrollOfRemoveCurse.uncurse(hero, item) || procced;
				}
			}

			int levelLimit = Math.max(5, 5 + Statistics.deepestFloor / 3);
			float upgradeChance = 0.33f;
			if (hero.heroClass == HeroClass.MAGE) {
				levelLimit++;
				upgradeChance = 0.38f;
			}
			boolean upgraded = false;

			for (Item item : hero.belongings) {
				if (item != null && Random.Float() < upgradeChance
						&& item.isUpgradable() && item.buffedLvl() < levelLimit) {
					item.upgrade();
					upgraded = true;
					GLog.p(Messages.get(this, "looks_better", item.name()));
					Badges.validateItemLevelAquired(item);
				}
			}

			if (upgraded) {
				hero.sprite.emitter().start(Speck.factory(Speck.UP), 0.2f, 3);
				GLog.i(Messages.get(this, "blessed"));
			}

			if (procced) {
				GLog.p(Messages.get(this, "procced"));
				hero.sprite.emitter().start(ShadowParticle.UP, 0.05f, 10);
			} else if (!upgraded) {
				GLog.i(Messages.get(this, "not_procced"));
			}

			volume -= 50;

			hero.spend(TIME_TO_BLESS);
			hero.busy();

			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			hero.sprite.operate(hero.pos);

			updateQuickslot();
		}
	}

	public void empty() {
		volume -= 10;
		if (volume < 0) volume = 0;
		updateQuickslot();
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	public boolean isFullBless() {
		return volume >= BLESS_VOLUME;
	}

	public boolean isFull() {
		return volume >= MAX_VOLUME;
	}

	public void collectDew(Dewdrop dew) {
		GLog.i(Messages.get(this, "collected"));
		volume += dew.quantity;
		if (volume >= MAX_VOLUME) {
			volume = MAX_VOLUME;
			GLog.p(Messages.get(this, "full"));
		}
		updateQuickslot();
	}

	public void collectDew(RedDewdrop dew) {
		GLog.i(Messages.get(this, "collected"));
		volume += (dew.quantity * 5);
		if (volume >= MAX_VOLUME) {
			volume = MAX_VOLUME;
			GLog.p(Messages.get(this, "full"));
		}
		updateQuickslot();
	}

	public void collectDew(YellowDewdrop dew) {
		GLog.i(Messages.get(this, "collected"));
		volume += (dew.quantity * 2);
		if (volume >= MAX_VOLUME) {
			volume = MAX_VOLUME;
			GLog.p(Messages.get(this, "full"));
		}
		updateQuickslot();
	}

	public void fill() {
		volume += 50;
		if (volume >= MAX_VOLUME) {
			volume = MAX_VOLUME;
			GLog.p(Messages.get(this, "full"));
		}
		updateQuickslot();
	}

	@Override
	public String status() {
		return Messages.format("%d/%d", volume, MAX_VOLUME);
	}
}
