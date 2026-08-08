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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Water;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Levitation;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.VialOfBlood;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Waterskin extends Item {

	private static final int BASE_MAX_VOLUME = 100;
	private static final int WINGS_MAX_VOLUME = 300;

	public int maxVolume() {
		return Dungeon.wings ? WINGS_MAX_VOLUME : BASE_MAX_VOLUME;
	}

	private static final String AC_DRINK	= "DRINK";
	private static final String AC_SIP		= "SIP";
	private static final String AC_SPLASH	= "SPLASH";
	private static final String AC_WATER	= "WATER";
	private static final String AC_BLESS	= "BLESS";

	private static final float TIME_TO_DRINK = 1f;
	private static final float TIME_TO_WATER = 3f;
	private static final float TIME_TO_BLESS = 1f;

	private static final String TXT_STATUS	= "%d/%d";

	{
		image = ItemSpriteSheet.WATERSKIN;

		defaultAction = AC_DRINK;

		unique = true;
	}

	private int volume = 0;

	private static final String VOLUME	= "volume";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( VOLUME, volume );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		volume	= bundle.getInt( VOLUME );
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (volume > 0) {
			actions.add( AC_SIP );
		}
		if (volume > 2) {
			actions.add( AC_DRINK );
		}
		if (volume > 29) {
			actions.add( AC_SPLASH );
		}
		if (volume > 49 && Dungeon.dewWater) {
			actions.add( AC_WATER );
		}
		if (volume >= maxVolume()) {
			actions.add( AC_BLESS );
		}
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_SIP )) {

			if (volume > 0) {

				int dropsToConsume = Math.min(3, volume);

				if (Dewdrop.consumeDew(dropsToConsume, hero, true)){
					volume -= dropsToConsume;
					Catalog.countUses(Dewdrop.class, dropsToConsume);

					hero.spend(TIME_TO_DRINK);
					hero.busy();

					Sample.INSTANCE.play(Assets.Sounds.DRINK);
					hero.sprite.operate(hero.pos);

					updateQuickslot();
				}

			} else {
				GLog.w( Messages.get(this, "empty") );
			}

		} else if (action.equals( AC_DRINK )) {

			if (volume > 0) {

				float missingHealthPercent = 1f - (hero.HP / (float)hero.HT);

				//each drop is worth 5% of total health, same as original waterskin
				float dropsNeeded = missingHealthPercent / 0.05f;

				//we are getting extra heal value, scale back drops needed accordingly
				if (dropsNeeded > 1.01f && VialOfBlood.delayBurstHealing()){
					dropsNeeded /= VialOfBlood.totalHealMultiplier();
				}

				//add extra drops if we can gain shielding
				int curShield = 0;
				if (hero.buff(Barrier.class) != null) curShield = hero.buff(Barrier.class).shielding();
				int maxShield = Math.round(hero.HT *0.2f*hero.pointsInTalent(Talent.SHIELDING_DEW));
				if (hero.hasTalent(Talent.SHIELDING_DEW)){
					float missingShieldPercent = 1f - (curShield / (float)maxShield);
					missingShieldPercent *= 0.2f*hero.pointsInTalent(Talent.SHIELDING_DEW);
					if (missingShieldPercent > 0){
						dropsNeeded += missingShieldPercent / 0.05f;
					}
				}

				//trimming off 0.01 drops helps with floating point errors
				int dropsToConsume = (int)Math.ceil(dropsNeeded - 0.01f);
				dropsToConsume = (int)GameMath.gate(1, dropsToConsume, volume);

				if (Dewdrop.consumeDew(dropsToConsume, hero, true)){
					volume -= dropsToConsume;
					Catalog.countUses(Dewdrop.class, dropsToConsume);

					hero.spend(TIME_TO_DRINK);
					hero.busy();

					Sample.INSTANCE.play(Assets.Sounds.DRINK);
					hero.sprite.operate(hero.pos);

					updateQuickslot();
				}


			} else {
				GLog.w( Messages.get(this, "empty") );
			}

		} else if (action.equals( AC_SPLASH )) {

			Buff.affect(hero, Haste.class, Haste.DURATION);
			Buff.affect(hero, Invisibility.class, Invisibility.DURATION);
			if (Dungeon.wings && Dungeon.depth < 51) {
				Buff.affect(hero, Levitation.class, Levitation.DURATION);
			}

			GLog.i( Messages.get(this, "refreshed") );

			volume -= 10;

			hero.spend(TIME_TO_DRINK);
			hero.busy();

			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			hero.sprite.operate(hero.pos);

			updateQuickslot();

		} else if (action.equals( AC_WATER )) {

			int length = Dungeon.level.length();
			for (int i = 0; i < length; i++) {
				if (Dungeon.level.heroFOV[i]) {
					int terr = Dungeon.level.map[i];
					if (terr == Terrain.GRASS || terr == Terrain.HIGH_GRASS
							|| terr == Terrain.FURROWED_GRASS
							|| terr == Terrain.EMPTY
							|| terr == Terrain.EMBERS) {
						GameScene.add(Blob.seed(i, 40, Water.class));
					}
				}
			}

			volume -= 2;

			GLog.i( Messages.get(this, "watered") );

			hero.sprite.operate(hero.pos);
			hero.busy();
			hero.spend(TIME_TO_WATER);

			updateQuickslot();

		} else if (action.equals( AC_BLESS )) {

			if (Dungeon.dewDraw) {
				GameScene.selectItem(blessItemSelector);
				return; // time spent inside selector callback
			}

			//uncurse all equipped items
			boolean procced = ScrollOfRemoveCurse.uncurse(hero,
					hero.belongings.weapon,
					hero.belongings.armor,
					hero.belongings.artifact,
					hero.belongings.misc,
					hero.belongings.ring);

			//also uncurse backpack items
			for (Item item : hero.belongings.backpack) {
				if (item.cursed) {
					procced = ScrollOfRemoveCurse.uncurse(hero, item) || procced;
				}
			}

			//chance to upgrade each upgradeable item
			int levelLimit = 5 + Dungeon.scalingDepth() / 3;
			float upgradeChance = 0.33f;
			boolean upgraded = false;

			for (Item item : hero.belongings) {
				if (item != null && Random.Float() < upgradeChance
						&& item.isUpgradable() && item.buffedLvl() < levelLimit) {
					item.upgrade();
					upgraded = true;
					GLog.p( Messages.get(this, "looks_better", item.name()) );
					Badges.validateItemLevelAquired(item);
				}
			}

			if (upgraded) {
				hero.sprite.emitter().start(Speck.factory(Speck.UP), 0.2f, 3);
			}

			if (procced) {
				GLog.p( Messages.get(this, "procced") );
				hero.sprite.emitter().start(ShadowParticle.UP, 0.05f, 10);
			} else if (!upgraded) {
				GLog.i( Messages.get(this, "not_procced") );
			}

			volume -= 50;

			hero.spend(TIME_TO_BLESS);
			hero.busy();

			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			hero.sprite.operate(hero.pos);

			updateQuickslot();

		}
	}

	@Override
	public String info() {
		String info = super.info();

		if (volume == 0){
			info += "\n\n" + Messages.get(this, "desc_water");
		} else {
			info += "\n\n" + Messages.get(this, "desc_heal");
		}

		if (volume > 29){
			info += "\n\n" + Messages.get(this, "desc_splash");
		}

		if (Dungeon.dewWater && volume > 49){
			info += "\n\n" + Messages.get(this, "desc_water_action");
		}

		if (volume >= maxVolume()){
			info += "\n\n" + Messages.get(this, "desc_bless");
		}

		if (isFull()){
			info += "\n\n" + Messages.get(this, "desc_full");
		}

		return info;
	}

	public void empty() {
		volume = 0;
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

	public boolean isFull() {
		return volume >= maxVolume();
	}

	public int getVolume() {
		return volume;
	}

	public void consumeVolume(int amount) {
		volume = Math.max(0, volume - amount);
		updateQuickslot();
	}

	public void collectDew( Dewdrop dew ) {
		int amount;
		if      (dew instanceof VioletDewdrop) amount = 50;
		else if (dew instanceof RedDewdrop)    amount = 5;
		else if (dew instanceof YellowDewdrop) amount = 2;
		else                                   amount = 1;

		GLog.i( Messages.get(this, "collected") );
		volume += amount * dew.quantity;
		if (volume >= maxVolume()) {
			volume = maxVolume();
			GLog.p( Messages.get(this, "full") );
		}

		updateQuickslot();
	}

	public void fill() {
		volume = maxVolume();
		updateQuickslot();
	}

	@Override
	public String status() {
		return Messages.format( TXT_STATUS, volume, maxVolume() );
	}

	// Sprouted dewDraw mode: player selects which item to upgrade (costs 90 volume)
	private final WndBag.ItemSelector blessItemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return Messages.get(Waterskin.class, "select_item");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			int levelLimit = 5 + Dungeon.scalingDepth() / 3;
			return item.isUpgradable() && item.buffedLvl() < levelLimit;
		}

		@Override
		public void onSelect(Item item) {
			if (item != null) {
				Hero hero = Dungeon.hero;
				item.upgrade();
				GLog.p( Messages.get(Waterskin.class, "looks_better", item.name()) );
				hero.sprite.emitter().start(Speck.factory(Speck.UP), 0.2f, 3);
				Badges.validateItemLevelAquired(item);

				volume -= 90;
				if (volume < 0) volume = 0;

				hero.spend(TIME_TO_BLESS);
				hero.busy();

				Sample.INSTANCE.play(Assets.Sounds.DRINK);
				hero.sprite.operate(hero.pos);

				updateQuickslot();
			}
		}
	};

}
