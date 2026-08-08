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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.spells;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.Trinity;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.HolyTome;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

public class BodyForm extends ClericSpell {

	public static BodyForm INSTANCE = new BodyForm();

	@Override
	public int icon() {
		return HeroIcon.BODY_FORM;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", duration()) + "\n\n" + Messages.get(this, "charge_cost", (int)chargeUse(Dungeon.hero));
	}

	@Override
	public float chargeUse(Hero hero) {
		return 2;
	}

	@Override
	public boolean canCast(Hero hero) {
		return super.canCast(hero) && hero.hasTalent(Talent.BODY_FORM);
	}

	@Override
	public void onCast(HolyTome tome, Hero hero) {

		GameScene.show(new Trinity.WndItemtypeSelect(tome, this));

	}

	public static int duration(){
		return Math.round(13.33f + 6.67f* Dungeon.hero.pointsInTalent(Talent.BODY_FORM));
	}

	public static class BodyFormBuff extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		private Bundlable effect;

		@Override
		public int icon() {
			return BuffIndicator.TRINITY_FORM;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1, 0, 0);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (duration() - visualcooldown()) / duration());
		}

		public void setEffect(Bundlable effect){
			this.effect = effect;
		}

		public Weapon.Enchantment enchant(){
			if (effect instanceof Weapon.Enchantment){
				return (Weapon.Enchantment) effect;
			}
			return null;
		}

		public Armor.Glyph glyph(){
			if (effect instanceof Armor.Glyph){
				return (Armor.Glyph) effect;
			}
			return null;
		}

		@Override
		public String desc() {
			if (enchant() != null){
				return Messages.get(this, "desc", Messages.titleCase(enchant().name()), dispTurns());
			} else if (glyph() != null){
				return Messages.get(this, "desc", Messages.titleCase(glyph().name()), dispTurns());
			}
			return super.desc();
		}

		private static final String EFFECT = "effect";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(EFFECT, effect);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			effect = bundle.get(EFFECT);
		}
	}

}
