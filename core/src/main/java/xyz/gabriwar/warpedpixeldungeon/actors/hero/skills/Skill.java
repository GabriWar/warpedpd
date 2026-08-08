/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
 *
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class Skill {

	public static final String AC_ADVANCE    = "Advance";
	public static final String AC_ACTIVATE   = "Activate";
	public static final String AC_DEACTIVATE = "Deactivate";
	public static final String AC_SUMMON     = "Summon";
	public static final String AC_CAST       = "Cast";

	public static final String SKILL_LEVEL = "LEVEL";
	public static final String SKILL_ACTIVE = "ACTIVE";

	public String tag = "";

	public static final int MAX_LEVEL = 3;

	public static final int STARTING_SKILL = 2;

	public static int availableSkill = STARTING_SKILL;

	public static final float TIME_TO_USE = 1f;

	//name/castText are the English fallbacks for name()/castText(); the bundle wins
	//when a key exists, exactly as for every other display string in the game
	public String name = "Skill";
	public String castText = "";
	public int level = 0;
	public int tier = 1;
	public int mana = 0;
	public int image = 159;

	public boolean active = false;

	public boolean multiTargetActive = false;

	/** the other half of a mutually exclusive fork; investing in one locks the other */
	public Skill exclusiveWith = null;

	public boolean requestUpgrade(){
		if (exclusiveWith != null && exclusiveWith.level > 0){
			GLog.w( Messages.get(Skill.class, "exclusive_choice", exclusiveWith.name()) );
			return false;
		}
		if (availableSkill >= tier && level < MAX_LEVEL){
			if (upgrade()){
				level++;
				availableSkill -= tier;
				return true;
			}
		} else {
			GLog.w( Messages.get(Skill.class, "fail_advance") );
		}
		return false;
	}

	protected boolean upgrade(){
		return false;
	}

	public void setLevel(int level){
		this.level = level;
	}

	public float incomingDamageModifier(){ return 1f; }

	public float damageModifier(){ return 1f; }

	public float rangedDamageModifier(){ return 1f; }

	public boolean aimedShot(){ return false; }

	public int damageBonus(int hp){ return damageBonus(hp, false); }

	public int damageBonus(int hp, boolean castText){ return 0; }

	public int toHitBonus(){ return 0; }

	public int healthRegenerationBonus(){ return 0; }

	public int weaponLevelBonus(){ return 0; }

	public int fletching(){ return 0; }

	public int hunting(){ return 0; }

	public boolean knocksBack(){ return false; }

	public boolean AoEDamage(){ return false; }

	public int manaRegenerationBonus(){ return 0; }

	public int incomingDamageReduction(int damage){ return 0; }

	public int image(){ return image; }

	/** bundle-backed display name, falling back to the hardcoded field when no key exists */
	public String name(){
		String name = Messages.get(this, "name");
		return name.equals(Messages.NO_TEXT_FOUND) ? this.name : name;
	}

	/** bundle-backed cast shout, falling back to the hardcoded field when no key exists */
	public String castText(){
		String text = Messages.get(this, "cast");
		return text.equals(Messages.NO_TEXT_FOUND) ? castText : text;
	}

	public String info(){ return Messages.get(this, "desc") + "\n" + costUpgradeInfo(); }

	public ArrayList<String> actions( Hero hero ){
		return new ArrayList<>();
	}

	public void execute( Hero hero, String action ){
	}

	public float getAlpha(){
		return 0.1f + level * 0.3f;
	}

	public int upgradeCost(){ return tier; }

	protected int totalSpent(){ return 0; }

	protected int nextUpgradeCost(){ return 0; }

	protected boolean canUpgrade(){ return false; }

	public String costUpgradeInfo(){
		return Messages.get(Skill.class, "level_info", name(), level) + "\n"
				+ (level < Skill.MAX_LEVEL ? Messages.get(Skill.class, "upgrade_cost", upgradeCost(), name())
										   : Messages.get(Skill.class, "maxed", name()))
				+ (level > 0 && mana > 0 ? "\n" + Messages.get(Skill.class, "mana_cost", name(), getManaCost()) : "");
	}

	public int getManaCost(){ return mana; }

	public void castTextYell(){
		if (!castText().equals("") && Dungeon.hero.sprite != null){
			Dungeon.hero.sprite.showStatus( CharSprite.NEUTRAL, castText() );
		}
	}

	public float wandRechargeSpeedReduction(){ return 1f; }

	public int summoningLimitBonus(){ return 0; }

	public float wandDamageBonus(){ return 1f; }

	public int lootBonus(int gold){ return 0; }

	public int stealthBonus(){ return 0; }

	public boolean disableTrap(){ return false; }

	public boolean venomousAttack(){ return false; }

	public int venomBonus(){ return 0; }

	public boolean instantKill(){ return false; }

	public boolean dodgeChance(){ return false; }

	public float toHitModifier(){ return 1f; }

	public boolean cripple(){ return false; }

	public int passThroughTargets(boolean shout){ return 0; }

	public boolean doubleShot(){ return false; }

	public boolean doubleStab(){ return false; }

	public boolean arrowToBomb(){ return false; }

	public boolean goToSleep(){ return false; }

	/** generic on-hit hook for subclass skills; return the (possibly grown) damage */
	public int onHitProc( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int damage, boolean ranged ){ return damage; }

	/** generic on-defend hook for subclass skills; return the (possibly shrunk) damage */
	public int onDefendProc( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int damage ){ return damage; }

	public void storeInBundle(Bundle bundle){
		bundle.put( SKILL_LEVEL + " " + tag, level );
		bundle.put( SKILL_ACTIVE + " " + tag, active );
	}

	public void restoreInBundle(Bundle bundle){
		level = bundle.getInt( SKILL_LEVEL + " " + tag );
		//a save from before the stance was bundled simply has no key: the skill starts off
		active = bundle.getBoolean( SKILL_ACTIVE + " " + tag );
	}
}
