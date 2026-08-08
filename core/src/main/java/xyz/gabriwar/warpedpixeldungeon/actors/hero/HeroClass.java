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

package xyz.gabriwar.warpedpixeldungeon.actors.hero;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.QuickSlot;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.ArmorAbility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.Trinity;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.Challenge;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.Feint;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.SpectralBlades;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.mage.WildMagic;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.warrior.Endure;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.warrior.Shockwave;
import xyz.gabriwar.warpedpixeldungeon.items.BrokenSeal;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.items.Torch;
import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.CloakOfShadows;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.HolyTome;
import xyz.gabriwar.warpedpixeldungeon.items.bags.VelvetPouch;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfInvisibility;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLiquidFlame;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMindVision;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfPurity;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfIdentify;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfLullaby;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMirrorImage;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRage;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Cudgel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Dagger;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Gloves;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Rapier;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WornShortsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingSpike;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingStone;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.DeviceCompat;

public enum HeroClass {

	WARRIOR( HeroSubClass.BERSERKER, HeroSubClass.GLADIATOR ),
	MAGE( HeroSubClass.BATTLEMAGE, HeroSubClass.WARLOCK ),
	ROGUE( HeroSubClass.ASSASSIN, HeroSubClass.FREERUNNER ),
	HUNTRESS( HeroSubClass.SNIPER, HeroSubClass.WARDEN ),
	DUELIST( HeroSubClass.CHAMPION, HeroSubClass.MONK ),
	CLERIC( HeroSubClass.PRIEST, HeroSubClass.PALADIN );

	private HeroSubClass[] subClasses;

	HeroClass( HeroSubClass...subClasses ) {
		this.subClasses = subClasses;
	}

	public void initHero( Hero hero ) {

		hero.heroClass = this;
		Talent.initClassTalents(hero);

		Item i = new ClothArmor().identify();
		if (!Challenges.isItemBlocked(i)) hero.belongings.armor = (ClothArmor)i;

		i = new Food();
		if (!Challenges.isItemBlocked(i)) i.collect();

		//two torches in the pack and a third already burning: nobody walks into
		//the dark on their first step
		i = new Torch().quantity( 2 );
		if (!Challenges.isItemBlocked(i)) {
			i.collect();
			Light.burn( hero, Light.DURATION );
		}

		//the run opens in a snowbound town: the first forty turns are warm ones,
		//long enough to get your bearings before the cold starts counting
		hero.coldGrace = 40;

		new VelvetPouch().collect();
		Dungeon.LimitedDrops.VELVET_POUCH.drop();

		Waterskin waterskin = new Waterskin();
		waterskin.collect();

		new ScrollOfIdentify().identify();

		switch (this) {
			case WARRIOR:
				initWarrior( hero );
				break;

			case MAGE:
				initMage( hero );
				break;

			case ROGUE:
				initRogue( hero );
				break;

			case HUNTRESS:
				initHuntress( hero );
				break;

			case DUELIST:
				initDuelist( hero );
				break;

			case CLERIC:
				initCleric( hero );
				break;
		}

		// skill tree: fresh run, fresh points, class-matched tree
		xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.availableSkill =
				xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.STARTING_SKILL;
		hero.heroSkills = xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills.forHero(hero);
		hero.heroSkills.init(hero);

		// skills system: every class starts with a mana pool sized to how
		// casty it is (Skillful PD gave 20/40/30/35 to the original four)
		switch (this) {
			case WARRIOR:  hero.MP = hero.MT = 20; break;
			case MAGE:     hero.MP = hero.MT = 40; break;
			case ROGUE:    hero.MP = hero.MT = 30; break;
			case HUNTRESS: hero.MP = hero.MT = 35; break;
			case DUELIST:  hero.MP = hero.MT = 25; break;
			case CLERIC:   hero.MP = hero.MT = 40; break;
		}

		if (WPDSettings.quickslotWaterskin()) {
			for (int s = 0; s < QuickSlot.SIZE; s++) {
				if (Dungeon.quickslot.getItem(s) == null) {
					Dungeon.quickslot.setSlot(s, waterskin);
					break;
				}
			}
		}

	}

	public Badges.Badge masteryBadge() {
		switch (this) {
			case WARRIOR:
				return Badges.Badge.MASTERY_WARRIOR;
			case MAGE:
				return Badges.Badge.MASTERY_MAGE;
			case ROGUE:
				return Badges.Badge.MASTERY_ROGUE;
			case HUNTRESS:
				return Badges.Badge.MASTERY_HUNTRESS;
			case DUELIST:
				return Badges.Badge.MASTERY_DUELIST;
			case CLERIC:
				return Badges.Badge.MASTERY_CLERIC;
		}
		return null;
	}

	private static void initWarrior( Hero hero ) {
		(hero.belongings.weapon = new WornShortsword()).identify();
		ThrowingStone stones = new ThrowingStone();
		stones.identify().collect();

		Dungeon.quickslot.setSlot(0, stones);

		if (hero.belongings.armor != null){
			hero.belongings.armor.affixSeal(new BrokenSeal());
			Catalog.setSeen(BrokenSeal.class); //as it's not added to the inventory
		}

		new PotionOfHealing().identify();
		new ScrollOfRage().identify();
	}

	private static void initMage( Hero hero ) {
		MagesStaff staff;

		staff = new MagesStaff(new WandOfMagicMissile());

		(hero.belongings.weapon = staff).identify();
		hero.belongings.weapon.activate(hero);

		Dungeon.quickslot.setSlot(0, staff);

		new ScrollOfUpgrade().identify();
		new PotionOfLiquidFlame().identify();
	}

	private static void initRogue( Hero hero ) {
		(hero.belongings.weapon = new Dagger()).identify();

		CloakOfShadows cloak = new CloakOfShadows();
		(hero.belongings.artifact = cloak).identify();
		hero.belongings.artifact.activate( hero );

		ThrowingKnife knives = new ThrowingKnife();
		knives.identify().collect();

		Dungeon.quickslot.setSlot(0, cloak);
		Dungeon.quickslot.setSlot(1, knives);

		new ScrollOfMagicMapping().identify();
		new PotionOfInvisibility().identify();
	}

	private static void initHuntress( Hero hero ) {

		(hero.belongings.weapon = new Gloves()).identify();
		SpiritBow bow = new SpiritBow();
		bow.identify().collect();

		Dungeon.quickslot.setSlot(0, bow);

		new PotionOfMindVision().identify();
		new ScrollOfLullaby().identify();
	}

	private static void initDuelist( Hero hero ) {

		(hero.belongings.weapon = new Rapier()).identify();
		hero.belongings.weapon.activate(hero);

		ThrowingSpike spikes = new ThrowingSpike();
		spikes.quantity(2).identify().collect(); //set quantity is 3, but Duelist starts with 2

		Dungeon.quickslot.setSlot(0, hero.belongings.weapon);
		Dungeon.quickslot.setSlot(1, spikes);

		new PotionOfStrength().identify();
		new ScrollOfMirrorImage().identify();
	}

	private static void initCleric( Hero hero ) {

		(hero.belongings.weapon = new Cudgel()).identify();
		hero.belongings.weapon.activate(hero);

		HolyTome tome = new HolyTome();
		(hero.belongings.artifact = tome).identify();
		hero.belongings.artifact.activate( hero );

		Dungeon.quickslot.setSlot(0, tome);

		new PotionOfPurity().identify();
		new ScrollOfRemoveCurse().identify();
	}

	public String title() {
		return Messages.get(HeroClass.class, name());
	}

	public String desc(){
		return Messages.get(HeroClass.class, name()+"_desc");
	}

	public String shortDesc(){
		return Messages.get(HeroClass.class, name()+"_desc_short");
	}

	public HeroSubClass[] subClasses() {
		return subClasses;
	}

	public ArmorAbility[] armorAbilities(){
		switch (this) {
			case WARRIOR: default:
				return new ArmorAbility[]{new HeroicLeap(), new Shockwave(), new Endure()};
			case MAGE:
				return new ArmorAbility[]{new ElementalBlast(), new WildMagic(), new WarpBeacon()};
			case ROGUE:
				return new ArmorAbility[]{new SmokeBomb(), new DeathMark(), new ShadowClone()};
			case HUNTRESS:
				return new ArmorAbility[]{new SpectralBlades(), new NaturesPower(), new SpiritHawk()};
			case DUELIST:
				return new ArmorAbility[]{new Challenge(), new ElementalStrike(), new Feint()};
			case CLERIC:
				return new ArmorAbility[]{new AscendedForm(), new Trinity(), new PowerOfMany()};
		}
	}

	public String spritesheet() {
		switch (this) {
			case WARRIOR: default:
				return Assets.Sprites.WARRIOR;
			case MAGE:
				return Assets.Sprites.MAGE;
			case ROGUE:
				return Assets.Sprites.ROGUE;
			case HUNTRESS:
				return Assets.Sprites.HUNTRESS;
			case DUELIST:
				return Assets.Sprites.DUELIST;
			case CLERIC:
				return Assets.Sprites.CLERIC;
		}
	}

	public String splashArt(){
		switch (this) {
			case WARRIOR: default:
				return Assets.Splashes.WARRIOR;
			case MAGE:
				return Assets.Splashes.MAGE;
			case ROGUE:
				return Assets.Splashes.ROGUE;
			case HUNTRESS:
				return Assets.Splashes.HUNTRESS;
			case DUELIST:
				return Assets.Splashes.DUELIST;
			case CLERIC:
				return Assets.Splashes.CLERIC;
		}
	}
	
	public boolean isUnlocked(){
		//always unlock on debug builds
		if (DeviceCompat.isDebug()) return true;

		switch (this){
			case WARRIOR: default:
				return true;
			case MAGE:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_MAGE);
			case ROGUE:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_ROGUE);
			case HUNTRESS:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_HUNTRESS);
			case DUELIST:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_DUELIST);
			case CLERIC:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_CLERIC);
		}
	}
	
	public String unlockMsg() {
		return shortDesc() + "\n\n" + Messages.get(HeroClass.class, name()+"_unlock");
	}

}
