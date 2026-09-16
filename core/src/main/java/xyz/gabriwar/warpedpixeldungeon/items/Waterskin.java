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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.VialOfBlood;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;

import java.util.ArrayList;

public class Waterskin extends Item {

	private static final int BASE_MAX_VOLUME = 100;
	private static final int UPGRADED_MAX_VOLUME = 300;

	public int maxVolume() {
		return Dungeon.skinCapacity ? UPGRADED_MAX_VOLUME : BASE_MAX_VOLUME;
	}

	private static final String AC_DRINK	= "DRINK";
	private static final String AC_SIP		= "SIP";
	private static final String AC_MEASURE	= "MEASURE";

	private static final float TIME_TO_DRINK = 1f;

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
		if (volume > 0 && Dungeon.measuredDraught) {
			actions.add( AC_MEASURE );
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

		} else if (action.equals( AC_MEASURE )) {

			if (volume > 0) {
				GameScene.show( new xyz.gabriwar.warpedpixeldungeon.windows.WndMeasuredDraught( this, hero ) );
			} else {
				GLog.w( Messages.get(this, "empty") );
			}

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

		if (Dungeon.dewCondenser){
			int turns = xyz.gabriwar.warpedpixeldungeon.actors.WeatherAttunement.turnsPerDrop(
					xyz.gabriwar.warpedpixeldungeon.actors.WeatherAttunement.condenseRate() );
			info += "\n\n" + (turns > 0
					? Messages.get(xyz.gabriwar.warpedpixeldungeon.actors.WeatherAttunement.class, "skin_condenser", turns)
					: Messages.get(xyz.gabriwar.warpedpixeldungeon.actors.WeatherAttunement.class, "skin_condenser_dry"));
		}

		if (Dungeon.measuredDraught && volume > 0){
			info += "\n\n" + Messages.get(this, "desc_measure");
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

	//the Tinkerer's measured draught: each drop is 5% of max health and 5% of max mana
	public static final float DROP_FRACTION = 0.05f;

	public int measuredHeal( Hero hero, int drops ){
		return Math.min( hero.HT - hero.HP, Math.round( hero.HT * DROP_FRACTION * drops ) );
	}

	public int measuredMana( Hero hero, int drops ){
		int maxMP = hero.MT + xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic.manaBonus( hero );
		return Math.max( 0, Math.min( maxMP - hero.MP, Math.round( hero.MT * DROP_FRACTION * drops ) ) );
	}

	public void drinkMeasured( Hero hero, int drops ){
		drops = (int) GameMath.gate( 1, drops, volume );
		int mana = measuredMana( hero, drops );
		Dewdrop.consumeDew( drops, hero, true );
		if (mana > 0){
			hero.MP += mana;
			hero.sprite.showStatus( 0x6688FF, "+%d MP", mana );
		}
		volume -= drops;
		Catalog.countUses( Dewdrop.class, drops );

		hero.spend( TIME_TO_DRINK );
		hero.busy();

		Sample.INSTANCE.play( Assets.Sounds.DRINK );
		hero.sprite.operate( hero.pos );

		updateQuickslot();
	}

	/** dew condensed out of the air by the Tinkerer's upgrade: silent, capped at the skin */
	public void condense( int drops ) {
		if (drops <= 0 || volume >= maxVolume()) return;
		volume = Math.min( maxVolume(), volume + drops );
		if (volume >= maxVolume()) GLog.p( Messages.get(this, "full") );
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

}
