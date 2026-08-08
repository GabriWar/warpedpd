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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.StatueSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class ArmoredStatue extends Statue {

	{
		spriteClass = StatueSprite.class;
	}

	protected Armor armor;

	public ArmoredStatue(){
		super();

		//double HP
		HP = HT = 30 + Dungeon.depth * 10;
	}

	@Override
	public void createWeapon(boolean useDecks) {
		super.createWeapon(useDecks);

		armor = Generator.randomArmor();
		armor.cursed = false;
		armor.inscribe(Armor.Glyph.random());
	}

	private static final String ARMOR	= "armor";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( ARMOR, armor );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		armor = (Armor)bundle.get( ARMOR );
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( armor.DRMin(), armor.DRMax());
	}

	//used in some glyph calculations
	public Armor armor(){
		return armor;
	}

	@Override
	public int defenseProc(Char enemy, int damage) {
		damage = armor.proc(enemy, this, damage);
		return super.defenseProc(enemy, damage);
	}

	@Override
	public int glyphLevel(Class<? extends Armor.Glyph> cls) {
		if (armor != null && armor.hasGlyph(cls, this)){
			return Math.max(super.glyphLevel(cls), armor.buffedLvl());
		} else {
			return super.glyphLevel(cls);
		}
	}

	@Override
	public float glyphPower(Class<? extends Armor.Glyph> cls) {
		if (armor != null && armor.getGlyph(cls) != null){
			return armor.getGlyph(cls).power();
		} else {
			return super.glyphPower(cls);
		}
	}

	@Override
	public CharSprite sprite() {
		CharSprite sprite = super.sprite();
		if (armor != null) {
			((StatueSprite) sprite).setArmor(armor.tier);
		} else {
			((StatueSprite) sprite).setArmor(3);
		}
		return sprite;
	}

	@Override
	public int defenseSkill(Char enemy) {
		return Math.round(armor.evasionFactor(this, super.defenseSkill(enemy)));
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
	}

	@Override
	protected void dropExtraLoot() {
		super.dropExtraLoot(); // drops weapon via Statue.dropExtraLoot()
		armor.identify(false);
		trackedDrop(armor, 1);
	}

	@Override
	public String description() {
		String desc = Messages.get(this, "desc");
		if (weapon != null && armor != null){
			desc += "\n\n" + Messages.get(this, "desc_arm_wep", weapon.name(), armor.name());
		}
		return desc;
	}

}
