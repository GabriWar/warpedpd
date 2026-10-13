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

package xyz.gabriwar.warpedpixeldungeon.journal.guidedemos;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Brute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Crab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shaman;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.FortuneTellerFolk;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.LostBackpack;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.keys.GoldenKey;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfHaste;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfIdentify;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.RatSkull;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrinketCatalyst;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.GuideEffect;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WornDartTrap;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;

//Demos for the first chapter, the basics (ch_basics: the tome of dungeon mastery's subjects).
//Words: messages/guide/demos_basics.properties.
public final class BasicsDemos {

	private BasicsDemos(){}

	public static void register( DemoBook book ){
		firstPage( book );
		examining( book );
		searching( book );
		surpriseAttack( book );
		doorAmbush( book );
		corridor( book );
		trampledGrass( book );
		standingGrass( book );
		hunger( book );
		identifying( book );
		strength( book );
		upgrade( book );
		levelUp( book );
		leap( book );
		wandBolt( book );
		caster( book );
		goldenKey( book );
		mimic( book );
		alchemy( book );
		enchanting( book );
		farming( book );
		blessedAnkh( book );
		plainAnkh( book );
	}

	//A guide page lying loose glints (DescentPage.emitter: the light specks); picked up, it settles into
	//the guide and the journal button flashes, the hero's own word over him (GameScene.adviseGuide).
	private static void firstPage( DemoBook book ){
		book.demo( "basics_intro" ).under( "journal button flashes" )
				.stage( Demo.Region.SEWERS,
						"######",
						"......",
						"..,..." )
				.hero( "you", 0, 1 )
				.sprite( "page", ItemSpriteSheet.GUIDE_PAGE, 4, 2 )
				.pour( GuideEffect.LIGHT, 4, 2 )
				.caption( "lies" )
				.turn()
				.move( "you", 1, 1, 2, 1, 3, 2, 4, 2 )
				.turn()
				.hide( "page" ).stop()
				.status( "you", "scenes.gamescene.guide_hint_status", CharSprite.POSITIVE )
				.caption( "flash" )
				.turns( 2 );
	}

	//The magnifier, then a tile: the snake's window tells why it is so hard to hit (Snake: 25 evasion),
	//and a plant examined is known without a step on it (GameScene.examineObject: Bestiary.setSeen).
	private static void examining( DemoBook book ){
		book.demo( "basics_examining" ).under( "first sewer snake" )
				.stage( Demo.Region.SEWERS,
						"######",
						"......",
						".,....",
						"......" )
				.hero( "you", 0, 2 )
				.mob( "snake", Snake.class, 4, 1 )
				.plant( "plant", Firebloom.class, 3, 3 )
				.face( "you", 4, 1 )
				.caption( "tap" )
				.turn()
				.mark( Demo.Mark.HINT, 4, 1 )
				.caption( "snake" )
				.turns( 2 )
				.unmark()
				.face( "you", 3, 3 )
				.mark( Demo.Mark.HINT, 3, 3 )
				.caption( "plant" )
				.turns( 2 )
				.unmark();
	}

	//A deliberate search reaches every tile next to you (Hero.search: distance 1) and always finds a
	//hidden door or a searchable trap there; it takes 2 turns (TIME_TO_SEARCH).
	private static void searching( DemoBook book ){
		book.demo( "basics_searching" ).under( "Press the magnifier twice" )
				.stage( Demo.Region.SEWERS,
						"#S###",
						".....",
						".t..." )
				.hero( "you", 2, 1 )
				.trap( "trap", WornDartTrap.class, 1, 2 )
				.hide( "trap" )
				.caption( "hidden" )
				.turn()
				.operate( "you", 2, 1 )
				.status( "you", "actors.hero.hero.search", CharSprite.DEFAULT )
				.mark( Demo.Mark.HINT, 1, 0, 2, 0, 3, 0, 1, 1, 3, 1, 1, 2, 2, 2, 3, 2 )
				.caption( "search" )
				.turns( 2 )
				.unmark()
				.terrain( Terrain.DOOR, 1, 0 )
				.show( "trap" )
				.caption( "found" )
				.turns( 2 );
	}

	//A sleeping snake has not seen you (Mob.surprisedBy: !enemySeen), so its defense is 0 and the
	//blow cannot miss (Mob.defenseSkill); the game shows its exclamation (Surprise.hit). Awake, the
	//same snake dodges: its 25 defense against a starting hero's 10 accuracy (Snake: HP 4, bite 1-4).
	private static void surpriseAttack( DemoBook book ){
		book.demo( "basics_surprise" )
				.stage( Demo.Region.SEWERS,
						"######",
						"..,...",
						"......",
						"....,." )
				.mob( "snake", Snake.class, 4, 2 )
				.hero( "you", 1, 2 )
				.face( "snake", 1, 2 )
				.icon( "snake", Icons.SLEEP )
				.caption( "asleep" )
				.turn()
				.move( "you", 2, 2 )
				.turn()
				.move( "you", 3, 2 )
				.turn()
				//it never saw you: a surprise attack, which cannot miss
				.attack( "you", 4, 2 )
				.beat()
				.surprise( "snake" ).hurt( "snake", 3 ).icon( "snake", Icons.ALERT )
				.caption( "surprise" )
				.turn()
				//awake now, it bites back
				.clear( "snake" )
				.attack( "snake", 3, 2 ).beat().hurt( "you", 2 )
				.turn()
				//and slips aside from the blow that would have finished it
				.attack( "you", 4, 2 ).beat().status( "snake", "actors.char.def_verb", CharSprite.NEUTRAL )
				.caption( "dodges" )
				.turns( 2 );
	}

	//The door trick (the snake's own description): it chases you through a door, you step aside, and
	//the closed door (Terrain.DOOR blocks sight) hides you. A hunting monster that lost sight of you
	//is surprised (Mob.surprisedBy: its view of your tile is gone), so the blow as it steps into the
	//doorway cannot miss.
	private static void doorAmbush( DemoBook book ){
		book.demo( "basics_surprise" ).under( "chase you through a door" )
				.stage( Demo.Region.SEWERS,
						"#######",
						"...#...",
						"...+...",
						"...#..." )
				.hero( "you", 4, 2 )
				.mob( "snake", Snake.class, 6, 2 )
				.icon( "snake", Icons.ALERT )
				.caption( "chase" )
				.turn()
				.terrain( Terrain.OPEN_DOOR, 3, 2 )
				.move( "you", 3, 2 )
				.move( "snake", 5, 2 )
				.turn()
				.move( "you", 2, 1 )
				.terrain( Terrain.DOOR, 3, 2 )
				.move( "snake", 4, 2 )
				.icon( "snake", Icons.LOST )
				.caption( "aside" )
				.turn()
				.terrain( Terrain.OPEN_DOOR, 3, 2 )
				.move( "snake", 3, 2 )
				.face( "you", 3, 2 )
				.beat()
				.attack( "you", 3, 2 )
				.beat()
				.surprise( "snake" ).hurt( "snake", 3 ).clear( "snake" )
				.caption( "strike" )
				.turns( 2 );
	}

	//One tile past a door, in a corridor a tile wide, only the doorway reaches you: the walls take
	//every other neighbour. The rats behind wait their turn (Rat: HP 8, bite 1-4).
	private static void corridor( DemoBook book ){
		book.demo( "basics_positioning" ).under( "Step one tile past a door" )
				.stage( Demo.Region.SEWERS,
						"#######",
						"...####",
						"...+...",
						"...####" )
				.hero( "you", 4, 2 )
				.mob( "rat1", Rat.class, 1, 2 )
				.mob( "rat2", Rat.class, 0, 1 )
				.mob( "rat3", Rat.class, 0, 3 )
				.face( "you", 3, 2 )
				.caption( "corridor" )
				.turn()
				.move( "rat1", 2, 2 ).move( "rat2", 1, 1 ).move( "rat3", 1, 3 )
				.turn()
				.terrain( Terrain.OPEN_DOOR, 3, 2 )
				.move( "rat1", 3, 2 ).move( "rat2", 2, 1 ).move( "rat3", 2, 3 )
				.mark( Demo.Mark.HINT, 3, 2 )
				.caption( "one" )
				.beat()
				.attack( "rat1", 4, 2 ).beat().hurt( "you", 2 )
				.turn()
				.attack( "you", 3, 2 ).beat().hurt( "rat1", 8 ).die( "rat1" )
				.turn()
				.move( "rat2", 3, 2 ).move( "rat3", 2, 2 )
				.caption( "next" )
				.beat()
				.attack( "you", 3, 2 ).beat().hurt( "rat2", 5 )
				.turns( 2 )
				.unmark();
	}

	//Tall grass blocks sight (Terrain.HIGH_GRASS: LOS_BLOCKING), but whoever walks into it tramples it
	//to short grass (HighGrass.trample: Terrain.GRASS), which blocks nothing: the gnoll sees the warrior
	//down the lane he cut.
	private static void trampledGrass( DemoBook book ){
		book.demo( "basics_positioning" ).under( "tramples it flat" )
				.stage( Demo.Region.SEWERS,
						"#######",
						"...||..",
						"...||..",
						"...||.." )
				.hero( "you", HeroClass.WARRIOR, 2, 2 )
				.mob( "gnoll", Gnoll.class, 0, 2 )
				.icon( "gnoll", Icons.ALERT )
				.caption( "chased" )
				.turn()
				.move( "you", 3, 2 ).move( "gnoll", 1, 2 )
				.terrain( Terrain.GRASS, 3, 2 )
				.turn()
				.move( "you", 4, 2 ).move( "gnoll", 2, 2 )
				.terrain( Terrain.GRASS, 4, 2 )
				.arrow( Demo.Mark.DANGER, 2, 2, 4, 2 )
				.caption( "trampled" )
				.turn()
				.unarrow()
				.move( "you", 5, 2 ).move( "gnoll", 3, 2 )
				.turn()
				.move( "you", 6, 2 ).move( "gnoll", 4, 2 )
				.turns( 2 );
	}

	//The huntress tramples tall grass to furrowed grass, which still blocks sight (HighGrass.trample;
	//Terrain.FURROWED_GRASS = HIGH_GRASS's flags). The gnoll last saw her on the grass's edge: it walks
	//there (Mob.Hunting keeps its target), finds the next column standing and gives up.
	private static void standingGrass( DemoBook book ){
		book.demo( "basics_positioning" ).under( "leaves it standing" )
				.stage( Demo.Region.SEWERS,
						"#######",
						"...||..",
						"...||..",
						"...||.." )
				.hero( "you", HeroClass.HUNTRESS, 2, 2 )
				.mob( "gnoll", Gnoll.class, 0, 2 )
				.icon( "gnoll", Icons.ALERT )
				.caption( "chased" )
				.turn()
				.move( "you", 3, 2 ).move( "gnoll", 1, 2 )
				.terrain( Terrain.FURROWED_GRASS, 3, 2 )
				.turn()
				.move( "you", 4, 2 ).move( "gnoll", 2, 2 )
				.terrain( Terrain.FURROWED_GRASS, 4, 2 )
				.caption( "huntress" )
				.turn()
				.move( "you", 5, 1 ).move( "gnoll", 3, 2 )
				.terrain( Terrain.GRASS, 3, 2 )
				.turn()
				.move( "you", 6, 1 )
				.icon( "gnoll", Icons.LOST )
				.caption( "lost" )
				.turn()
				.move( "gnoll", 2, 3 )
				.turns( 2 );
	}

	//1 hunger every 2 turns (Hunger.act: hungerDelay 2): hungry at 300, starving at 450, which hurts
	//1 at once and a thousandth of your health each turn after; a ration takes off 300 (Food.energy).
	private static void hunger( DemoBook book ){
		book.demo( "basics_hunger" ).under( "At 300 you are hungry" )
				.stage( Demo.Region.SEWERS,
						"#####",
						".....",
						"..,.." )
				.hero( "you", 1, 1 )
				.item( "food", Food.class, 3, 2 )
				.buff( "you", Hunger.class, BuffIndicator.HUNGER )
				.caption( "hungry" )
				.turns( 2 )
				.buff( "you", Hunger.class, BuffIndicator.STARVATION )
				.hurt( "you", 1 )
				.caption( "starving" )
				.turns( 2 )
				.move( "you", 2, 2, 3, 2 )
				.turn()
				.hide( "food" )
				.operate( "you", 3, 2 )
				.clear( "you" )
				.caption( "eat" )
				.turns( 2 );
	}

	//A Scroll of Identify names one item (ScrollOfIdentify); the town's fortune teller reads one a season
	//(TownLedger.readingSpent) for 50 gold, 40 under a full moon and 65 under a new one (FortuneTellerFolk:
	//COST_ONE, moonRate).
	private static void identifying( DemoBook book ){
		book.demo( "basics_identifying" ).under( "names an item at once" )
				.stage( Demo.Region.SEWERS,
						"######",
						"......",
						"..,..." )
				.hero( "you", 0, 1 )
				.item( "scroll", ScrollOfIdentify.class, 1, 1 )
				.item( "sword", Sword.class, 2, 2 )
				.mob( "teller", FortuneTellerFolk.class, 5, 1 )
				.item( "ring", RingOfHaste.class, 4, 2 )
				.caption( "unknown" )
				.turn()
				.move( "you", 1, 1 )
				.hide( "scroll" )
				.beat()
				.anim( "you", "read" )
				.beat()
				.burst( GuideEffect.LIGHT, 2, 2 )
				.status( "you", "known", CharSprite.POSITIVE )
				.caption( "identify" )
				.turns( 2 )
				.move( "you", 2, 1, 3, 1, 4, 1 )
				.turn()
				.face( "you", 5, 1 )
				.burst( GuideEffect.COINS, 4, 1 )
				.status( "you", "gold", CharSprite.NEUTRAL )
				.burst( GuideEffect.LIGHT, 4, 2 )
				.caption( "teller" )
				.turns( 2 );
	}

	//Each point of strength short divides accuracy by 1.5 (Weapon.accuracyFactor); a Potion of
	//Strength gives 1 (PotionOfStrength: STR++), and with enough strength the blow lands.
	private static void strength( DemoBook book ){
		book.demo( "basics_strength" ).under( "Each point you lack" )
				.stage( Demo.Region.SEWERS,
						"#####",
						".....",
						"....." )
				.hero( "you", 1, 1 )
				.mob( "rat", Rat.class, 2, 1 )
				.item( "potion", PotionOfStrength.class, 1, 2 )
				.caption( "short" )
				.turn()
				.attack( "you", 2, 1 ).beat().status( "rat", "actors.char.def_verb", CharSprite.NEUTRAL )
				.turn()
				.attack( "rat", 1, 1 ).beat().hurt( "you", 2 )
				.turn()
				.move( "you", 1, 2 )
				.hide( "potion" )
				.operate( "you", 1, 2 )
				.status( "you", "stronger", CharSprite.POSITIVE )
				.caption( "potion" )
				.turn()
				.move( "you", 1, 1 )
				.turn()
				.attack( "you", 2, 1 ).beat().hurt( "rat", 8 ).die( "rat" )
				.caption( "lands" )
				.turns( 2 );
	}

	//A Scroll of Upgrade raises one item by 1 (ScrollOfUpgrade.upgradeItem); 3 lie in every region
	//of five floors (Dungeon.souNeeded).
	private static void upgrade( DemoBook book ){
		book.demo( "basics_upgrades" ).under( "raises one item by 1" )
				.stage( Demo.Region.SEWERS,
						"#####",
						".....",
						"..,.." )
				.hero( "you", 0, 1 )
				.item( "scroll", ScrollOfUpgrade.class, 2, 1 )
				.caption( "scroll" )
				.turn()
				.move( "you", 1, 1, 2, 1 )
				.hide( "scroll" )
				.turn()
				.anim( "you", "read" )
				.beat()
				.burst( GuideEffect.LIGHT, 2, 1 )
				.status( "you", "plus_one", CharSprite.POSITIVE )
				.caption( "one" )
				.turns( 2 )
				.caption( "three" )
				.turns( 2 );
	}

	//A rat's kill is worth 1 (Rat: EXP 1); a level gives 5 more max health and heals it (Hero.updateHT),
	//1 accuracy and evasion, 2 mana and 3 points (Hero.earnExp).
	private static void levelUp( DemoBook book ){
		book.demo( "basics_levelling" ).under( "Each level gives" )
				.stage( Demo.Region.SEWERS,
						"#####",
						".....",
						".,..." )
				.hero( "you", 1, 1 )
				.mob( "rat", Rat.class, 2, 1 )
				.caption( "kill" )
				.turn()
				.attack( "you", 2, 1 ).beat().hurt( "rat", 8 ).die( "rat" )
				.beat()
				.status( "you", "exp", CharSprite.POSITIVE )
				.turn()
				.status( "you", "actors.hero.hero.level_up", CharSprite.POSITIVE )
				.heal( "you", 5 )
				.caption( "level" )
				.turn()
				.status( "you", "plus_points", CharSprite.POSITIVE )
				.caption( "points" )
				.turns( 2 );
	}

	//The warrior's Leap is a skill of tier 3, so each of its levels costs 3 points (Skill.upgradeCost:
	//tier); at its first level it reaches 3 tiles (Leap.range: 2 + level), lands beside the foe and
	//strikes, leaving it stunned for a turn, for mana (Leap.getManaCost).
	private static void leap( DemoBook book ){
		book.demo( "basics_skills" ).under( "Most skills are actions" )
				.stage( Demo.Region.CAVES,
						"######",
						"......",
						"......",
						"..,..." )
				.hero( "you", HeroClass.WARRIOR, 0, 2 )
				.mob( "gnoll", Gnoll.class, 3, 2 )
				.caption( "tier" )
				.turn()
				.jump( "you", 2, 2 )
				.beat()
				.attack( "you", 3, 2 )
				.beat()
				.burst( GuideEffect.ROCKS, 3, 2 )
				.hurt( "gnoll", 9 )
				.status( "gnoll", "stunned", CharSprite.WARNING )
				.freeze( "gnoll" )
				.caption( "mana" )
				.turn()
				.thaw( "gnoll" )
				.turns( 2 );
	}

	//A wand's bolt (Wand.fx: MagicMissile.MAGIC_MISSILE) hits with no roll and no armor between
	//(WandOfMagicMissile.onZap: ch.damage), so even a crab's shell does not stop it.
	private static void wandBolt( DemoBook book ){
		book.demo( "basics_magic" ).under( "never misses" )
				.stage( Demo.Region.SEWERS,
						"######",
						"......",
						"..,..." )
				.hero( "you", 0, 1 )
				.mob( "crab", Crab.class, 4, 1 )
				.face( "you", 4, 1 )
				.caption( "bolt" )
				.turn()
				.zap( "you", 4, 1 ).bolt( "you", MagicMissile.MAGIC_MISSILE, 4, 1 )
				.beat()
				.burst( GuideEffect.ARCANE, 4, 1 )
				.hurt( "crab", 6 )
				.turn()
				.move( "crab", 3, 1 )
				.zap( "you", 3, 1 ).bolt( "you", MagicMissile.MAGIC_MISSILE, 3, 1 )
				.beat()
				.burst( GuideEffect.ARCANE, 3, 1 )
				.hurt( "crab", 7 )
				.caption( "armor" )
				.turns( 2 );
	}

	//A gnoll shaman zaps along a clear line from any distance (Shaman.doAttack: its bolt, 6 to 15; a red
	//one's is ShamanSprite.Red's SHAMAN_RED), but next to you it fights by hand (2 to 7 on depth 1).
	private static void caster( DemoBook book ){
		book.demo( "basics_magic" ).under( "Casters like the" )
				.stage( Demo.Region.SEWERS,
						"######",
						"......",
						"..,..." )
				.hero( "you", 0, 1 )
				.mob( "shaman", Shaman.RedShaman.class, 4, 1 )
				.face( "shaman", 0, 1 )
				.caption( "range" )
				.turn()
				.zap( "shaman", 0, 1 ).bolt( "shaman", MagicMissile.SHAMAN_RED, 0, 1 ).beat().hurt( "you", 9 )
				.turn()
				.move( "you", 1, 1 )
				.turn()
				.move( "you", 2, 1 )
				.turn()
				.move( "you", 3, 1 )
				.beat()
				.attack( "shaman", 3, 1 ).beat().hurt( "you", 3 )
				.caption( "close" )
				.turns( 2 );
	}

	//A golden key opens a locked chest on its own floor (Hero.actOpenChest: Notes.keyCount of the
	//floor's GoldenKey).
	private static void goldenKey( DemoBook book ){
		book.demo( "basics_looting" ).under( "Every key works" )
				.stage( Demo.Region.SEWERS,
						"######",
						"......",
						"..,..." )
				.hero( "you", 0, 1 )
				.item( "key", GoldenKey.class, 2, 1 )
				.sprite( "chest", ItemSpriteSheet.LOCKED_CHEST, 4, 2 )
				.item( "prize", RingOfHaste.class, 4, 2 )
				.hide( "prize" )
				.caption( "key" )
				.turn()
				.move( "you", 1, 1, 2, 1 )
				.hide( "key" )
				.turn()
				.move( "you", 3, 2 )
				.operate( "you", 4, 2 )
				.beat()
				.hide( "chest" ).show( "prize" )
				.burst( GuideEffect.LIGHT, 4, 2 )
				.caption( "open" )
				.turns( 2 );
	}

	//Some chests are mimics (Mimic): opened, the chest bites.
	private static void mimic( DemoBook book ){
		book.demo( "basics_looting" ).under( "Some chests are" )
				.stage( Demo.Region.SEWERS,
						"#####",
						".....",
						".,..." )
				.hero( "you", 0, 1 )
				.sprite( "chest", ItemSpriteSheet.CHEST, 3, 1 )
				.mob( "mimic", Mimic.class, 3, 1 )
				.hide( "mimic" )
				.caption( "chest" )
				.turn()
				.move( "you", 1, 1, 2, 1 )
				.turn()
				.operate( "you", 3, 1 )
				.beat()
				.hide( "chest" ).show( "mimic" )
				.surprise( "you" )
				.beat()
				.attack( "mimic", 2, 1 ).beat().hurt( "you", 4 )
				.caption( "bite" )
				.turns( 2 );
	}

	//Energizing a potion gives 6 (Potion.energyVal) and identifies it; the catalyst and 6 energy
	//(TrinketCatalyst.Recipe.cost) offer 4 trinkets to choose from (WndTrinket: NUM_TRINKETS).
	private static void alchemy( DemoBook book ){
		book.demo( "basics_alchemy" ).under( "energize spare items" )
				.stage( Demo.Region.SEWERS,
						"#####",
						"..A..",
						"....." )
				.hero( "you", 0, 2 )
				.item( "potion", PotionOfHealing.class, 1, 2 )
				.item( "catalyst", TrinketCatalyst.class, 3, 2 )
				.item( "trinket", RatSkull.class, 3, 1 )
				.hide( "trinket" )
				.caption( "energize" )
				.turn()
				.move( "you", 1, 2 )
				.hide( "potion" )
				.turn()
				.move( "you", 2, 2 )
				.operate( "you", 2, 1 )
				.burst( GuideEffect.BUBBLES, 2, 1 )
				.status( "you", "plus_six", CharSprite.POSITIVE )
				.turn()
				.move( "you", 3, 2 )
				.hide( "catalyst" )
				.caption( "catalyst" )
				.turn()
				.operate( "you", 2, 1 )
				.burst( GuideEffect.BUBBLES, 2, 1 )
				.status( "you", "minus_six", CharSprite.NEUTRAL )
				.beat()
				.show( "trinket" )
				.burst( GuideEffect.LIGHT, 3, 1 )
				.turns( 2 );
	}

	//The pedestal weaves a new enchantment for 40 energy (EnchantingScene.INFUSE_COST) and empowers
	//one by a level for 10, 20, 30, 40, then 50 (EnchantingScene.empowerCost).
	private static void enchanting( DemoBook book ){
		book.demo( "basics_enchanting" ).under( "works on gear for alchemical energy" )
				.stage( Demo.Region.CITY,
						"=======",
						"=_____=",
						"=__P__=",
						"=_____=" )
				.mob( "pedestal", EnchantingStation.class, 3, 2 )
				.hero( "you", 3, 3 )
				.caption( "weave" )
				.turn()
				.operate( "you", 3, 2 )
				.beat()
				.burst( GuideEffect.ARCANE, 3, 2 )
				.status( "you", "minus_forty", CharSprite.NEUTRAL )
				.turns( 2 )
				.caption( "empower" )
				.operate( "you", 3, 2 )
				.beat()
				.burst( GuideEffect.ARCANE, 3, 2 )
				.status( "you", "minus_ten", CharSprite.NEUTRAL )
				.turns( 2 );
	}

	//A seed sown on farmland grows into a crop, drawn as its plant (Crop.plant); its first cycle ripens
	//it, and a sickle cuts a blandfruit bush for blandfruit (hs_farming).
	private static void farming( DemoBook book ){
		book.demo( "basics_farming" ).under( "Make farmland" )
				.stage( Demo.Region.SEWERS,
						"#####",
						"\"\"\"\"\"",
						"\"\"\"\"\"" )
				.hero( "you", 1, 1 )
				.item( "seed", BlandfruitBush.Seed.class, 2, 1 )
				.plant( "crop", BlandfruitBush.class, 2, 1 )
				.item( "fruit", Blandfruit.class, 2, 1 )
				.hide( "seed" ).hide( "crop" ).hide( "fruit" )
				.caption( "sow" )
				.turn()
				.operate( "you", 2, 1 )
				.beat()
				.show( "seed" )
				.turns( 2 )
				.hide( "seed" ).show( "crop" )
				.caption( "grow" )
				.turns( 2 )
				.operate( "you", 2, 1 )
				.beat()
				.hide( "crop" ).show( "fruit" )
				.caption( "cut" )
				.turns( 2 );
	}

	//A blessed ankh saves you on the spot at a quarter of your health (Hero.die: HP = HT / 4; 5 of a
	//starting hero's 20).
	private static void blessedAnkh( DemoBook book ){
		book.demo( "basics_defeat" ).under( "An ankh brings you back" )
				.stage( Demo.Region.CAVES,
						"#####",
						".....",
						"..,.." )
				.hero( "you", 1, 1 )
				.mob( "brute", Brute.class, 2, 1 )
				.caption( "blessed" )
				.turn()
				.attack( "brute", 1, 1 ).beat().hurt( "you", 14 ).die( "you" )
				.turn()
				.burst( GuideEffect.LIGHT, 1, 1 )
				.show( "you" )
				.heal( "you", 5 )
				.caption( "quarter" )
				.turns( 2 );
	}

	//A plain ankh wakes you elsewhere on the floor (InterlevelScene.resurrect: a respawn cell) with the
	//two items you chose (WndResurrect); the rest lie in a lost backpack where you died.
	private static void plainAnkh( DemoBook book ){
		book.demo( "basics_defeat" ).under( "A plain one wakes you" )
				.stage( Demo.Region.CAVES,
						"######",
						"......",
						"..,...",
						"......" )
				.hero( "you", 1, 1 )
				.mob( "brute", Brute.class, 2, 1 )
				.item( "pack", LostBackpack.class, 1, 1 )
				.hide( "pack" )
				.caption( "plain" )
				.turn()
				.attack( "brute", 1, 1 ).beat().hurt( "you", 14 ).die( "you" )
				.turn()
				.show( "pack" )
				.hide( "you" )
				.jump( "you", 5, 3 )
				.beat()
				.show( "you" )
				.caption( "pack" )
				.turns( 2 );
	}
}
