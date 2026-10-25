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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Alligator;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bison;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Kupua;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LitTower;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Otiluke;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldDragon;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.AresSword;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.GuideEffect;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FleecingTrap;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;

//Demos for the world above: the surface, the peaks, the deep caves, homesteads, the town and its mines.
//Words: messages/guide/demos_surface.properties.
//
//The stages draw a dungeon region's tiles: the deep caves and the kupua mines are cave tiles in the
//game too, the mines' sealed last floor and the dragon cave prison tiles. The open world has no
//region of its own here, so its scenes stand on the sewers' grass.
public final class SurfaceDemos {

	private SurfaceDemos(){}

	public static void register( DemoBook book ){
		wolfHunt( book );
		herdCharge( book );
		gatorDrag( book );
		goatButt( book );
		dragonSpit( book );
		firedamp( book );
		caveIn( book );
		kupuaGas( book );
		litTowers( book );
		otilukeCut( book );
		otilukeRelic( book );
		fleecingLane( book );
	}

	//A hunt's wolves run the quarry down and leave the hero be, unless he comes within 2 tiles of one
	//or hurts one (HuntPack.CROWDING): then the pack turns on him. A deer (14 health) dies to one bite
	//(GrayWolf: 20 to 35) and leaves mystery meat where it fell (Deer.die).
	private static void wolfHunt( DemoBook book ){
		book.demo( "surface_events" ).under( "On some nights a wolf pack hunts a deer near you" )
				.stage( Demo.Region.SEWERS,
						"\"\"\"\"\"\"\"",
						"\"\"\"\"\"\"\"",
						"\"\"\"\"\"\"\"",
						"\"\"\"\"\"\"\"",
						"\"\"\"\"\"\"\"" )
				.mob( "deer", Deer.class, 3, 2 )
				.mob( "wolf1", GrayWolf.class, 1, 1 )
				.mob( "wolf2", GrayWolf.class, 1, 3 )
				.hero( "you", 6, 4 )
				.item( "meat", MysteryMeat.class, 5, 1 ).hide( "meat" )
				.caption( "hunt" )
				.turn()
				.move( "deer", 4, 1 ).move( "wolf1", 2, 1 ).move( "wolf2", 2, 2 )
				.turn()
				.move( "deer", 5, 1 ).move( "wolf1", 3, 1 ).move( "wolf2", 3, 2 )
				.turn()
				//the pack runs it down, a few tiles from you: they leave you be
				.move( "wolf1", 4, 1 )
				.beat()
				.attack( "wolf1", 5, 1 ).beat().hurt( "deer", 27 ).die( "deer" )
				.turn()
				.show( "meat" )
				.caption( "meat" )
				.turn()
				//within 2 tiles of them: the whole pack turns on you
				.move( "you", 5, 3 )
				.beat()
				.icon( "wolf1", Icons.ALERT ).icon( "wolf2", Icons.ALERT )
				.face( "wolf1", 5, 3 ).face( "wolf2", 5, 3 )
				.caption( "close" )
				.turns( 2 );
	}

	//A bison struck by anyone turns its herd on him (Bison.provoke, HERD cells); a bison that ran at him
	//on its last turn strikes half again as hard (16 to 30, CHARGE_HIT) and throws him a tile straight
	//back (Bison.attackProc, Goat.shoveCell). Here the struck one's herd mate runs at him from two
	//tiles off and throws him diagonally away from it.
	private static void herdCharge( DemoBook book ){
		book.demo( "surface_beasts" ).under( "hit one and every bison within 8 tiles charges you" )
				.stage( Demo.Region.SEWERS,
						"\"\"\"\"\"\"\"",
						"\"\"\"\"\"\"\"",
						"\"\"\"\"\"\"\"",
						"\"\"\"\"\"\"\"",
						"\"\"\"\"\"\"\"" )
				.mob( "b1", Bison.class, 3, 2 )
				.mob( "b2", Bison.class, 1, 1 )
				.hero( "you", 4, 2 )
				.face( "you", 3, 2 )
				.caption( "graze" )
				.turn()
				.attack( "you", 3, 2 ).beat().hurt( "b1", 9 )
				.icon( "b1", Icons.ALERT ).icon( "b2", Icons.ALERT )
				.face( "b1", 4, 2 ).face( "b2", 4, 2 )
				.caption( "herd" )
				.turn()
				.move( "b2", 2, 1 )
				.turn()
				.move( "b2", 3, 1 )
				.beat()
				.attack( "b2", 4, 2 ).beat().hurt( "you", 36 )
				.knock( "you", 5, 3 )
				.caption( "throw" )
				.turns( 2 );
	}

	//An alligator lies in the swamp's water, its lurk showing only its eyes (AlligatorSprite); whoever
	//steps beside it it bursts out at: its ambush bite never misses and lands half again as hard (20
	//to 36, AMBUSH_HIT), and it backs into the water behind it and drags him onto the cell it left
	//(Alligator.drag).
	private static void gatorDrag( DemoBook book ){
		book.demo( "surface_beasts" ).under( "Step next to one and it bursts out" )
				.stage( Demo.Region.SEWERS,
						"\"\"\"\"\"\"\"",
						"\"\"~~~\"\"",
						"\"\"~~~\"\"",
						"\"\"\"\"\"\"\"",
						"\"\"\"\"\"\"\"" )
				.mob( "gator", Alligator.class, 3, 2 )
				.hero( "you", 3, 4 )
				.anim( "gator", "lurk" )
				.caption( "lurk" )
				.turn()
				.move( "you", 3, 3 )
				.turn()
				.attack( "gator", 3, 3 ).beat().hurt( "you", 42 )
				.caption( "bite" )
				.beat()
				.anim( "gator", "swim" )
				.knock( "gator", 3, 1 )
				.knock( "you", 3, 2 )
				.caption( "drag" )
				.turn()
				.anim( "gator", "lurk" )
				.turns( 2 );
	}

	//A goat struck by someone beside it with a drop at his back butts him (Goat.answers): 8 to 16, and
	//he is thrown a tile straight back (Goat.attackProc, WandOfBlastWave.throwChar), over the edge onto
	//the slice below.
	private static void goatButt( DemoBook book ){
		book.demo( "peaks_overview" ).under( "If you hit a goat while a drop is behind you" )
				.stage( Demo.Region.SEWERS,
						"\"\"\"\"\"\"",
						"o\"\"\"\"\"",
						"o\"\"\"\"\"",
						"o\"\"\"\"\"" )
				.hero( "you", 1, 2 )
				.mob( "goat", Goat.class, 2, 2 )
				.face( "you", 2, 2 ).face( "goat", 1, 2 )
				.caption( "edge" )
				.turn()
				.attack( "you", 2, 2 ).beat().hurt( "goat", 6 )
				.turn()
				.attack( "goat", 1, 2 ).beat().hurt( "you", 11 )
				.knock( "you", 0, 2 )
				.beat()
				.hide( "you" )
				.caption( "over" )
				.turns( 2 );
	}

	//The dragon spits at whatever it has a clear line to (AdultDragonViolet.canAttack: Ballistica
	//STOP_SOLID; AdultDragonVioletSprite.zap: a POISON bolt): twice its 150-300 bite, 300 to 600, and
	//poison. From (6, 2) the line to (3, 1) runs into the rock at (4, 1), so it cannot spit there; it
	//moves twice as fast as you (baseSpeed 2).
	private static void dragonSpit( DemoBook book ){
		book.demo( "boss_dragon" ).under( "spits poison" )
				.stage( Demo.Region.SEWERS,
						"\"\"\"\"#\"\"",
						"\"\"\"\"#\"\"",
						"\"\"\"\"\"\"\"",
						"\"\"\"\"#\"\"",
						"\"\"\"\"#\"\"" )
				.mob( "dragon", OverworldDragon.class, 6, 2 )
				.hero( "you", 2, 2 )
				.face( "you", 6, 2 ).face( "dragon", 2, 2 )
				.turn()
				//a clear line through the gap: it spits
				.anim( "dragon", "cast" ).bolt( "dragon", MagicMissile.POISON, 2, 2 )
				.beat()
				.hurt( "you", 412 ).buff( "you", Poison.class )
				.caption( "spit" )
				.turn()
				//rock between you: no line, no spit
				.move( "you", 3, 1 )
				.caption( "rock" )
				.turn()
				//but two of its steps to every one of yours
				.move( "dragon", 5, 2, 4, 2 )
				.caption( "fast" )
				.turns( 2 );
	}

	//Firedamp (HazardWatch.firedampTick): its haze about him (FiredampParticle); walking into the gas
	//with a flame warns at once, the flame licking at it (arm: FlameParticle); if the hero is still in
	//the gas after his next action, the pocket goes up (ignite: a bomb's blast from where it caught,
	//smoke off every gas cell): everyone in or beside the gas is hurt (3 + 2k to 8 + 3k, k 1 on the
	//fourth slice: 5 to 11, a hero never more than 35% of his health) and fire takes the gas cells
	//nobody stands on.
	private static final int[] POCKET = { 3,2, 4,2, 3,3, 4,3 };
	//the pocket and every open cell beside it
	private static final int[] BLAST = { 2,1, 3,1, 4,1, 5,1, 2,2, 3,2, 4,2, 5,2, 2,3, 3,3, 4,3, 5,3 };

	private static void firedamp( DemoBook book ){
		book.demo( "deep_hazards" ).under( "pockets of firedamp" )
				.stage( Demo.Region.CAVES,
						"#######",
						"#.....#",
						"......#",
						"#.....#",
						"#######" )
				.hero( "you", 1, 2 )
				.mob( "rat", AlbinoRat.class, 5, 1 )
				.buff( "you", Light.class )
				.pour( GuideEffect.FIREDAMP, POCKET )
				.caption( "haze" )
				.turn()
				.move( "you", 2, 2 )
				.turn()
				//in the gas with a torch: the warning
				.move( "you", 3, 2 )
				.beat()
				.burst( GuideEffect.FIRE, 3, 2 ).status( "you", "sss", CharSprite.WARNING )
				.mark( Demo.Mark.DANGER, BLAST )
				.caption( "hiss" )
				.turn()
				//still in it after the next action: it goes up
				.move( "you", 4, 2 )
				.beat()
				.stop().unmark()
				.burst( GuideEffect.BLAST, 3, 2 ).burst( GuideEffect.SMOKE, POCKET )
				.pour( GuideEffect.FIRE, 3,2, 3,3, 4,3 )
				.hurt( "you", 9 ).hurt( "rat", 7 )
				.caption( "boom" )
				.turn()
				.caption( "out" )
				.turns( 2 );
	}

	//Mining a rock that held the roof up (HazardWatch.collapse, unsupported): the mined cell and every
	//open cell round it are marked, always leaving the miner a free cell beside him, and the rocks fall
	//after his next action (CaveIn): 4 + deep to 8 + 2 deep (deep 3 on the fourth slice: 7 to 14),
	//crippled for 3 turns, a hero never losing more than a third of his health.
	private static final int[] FALL = { 2,1, 3,1, 1,2, 2,2, 3,2 };

	private static void caveIn( DemoBook book ){
		book.demo( "deep_hazards" ).under( "Mining can bring the roof down" )
				.stage( Demo.Region.CAVES,
						"######",
						"###..#",
						"#....#",
						"#....#",
						"######" )
				.hero( "you", 2, 2 )
				.mob( "rat", AlbinoRat.class, 3, 2 )
				.face( "you", 2, 1 )
				.turn()
				.operate( "you", 2, 1 )
				.beat()
				.terrain( Terrain.EMPTY_DECO, 2, 1 )
				.mark( Demo.Mark.TARGET, FALL )
				.caption( "groan" )
				.turn()
				.move( "you", 2, 3 )
				.caption( "step" )
				.turn()
				.unmark()
				.burst( GuideEffect.ROCKS, FALL )
				.beat()
				.hurt( "rat", 9 ).buff( "rat", Cripple.class )
				.caption( "rocks" )
				.turns( 2 );
	}

	//A kupua takes 1 to half of any blow but a relic's (Kupua.damage); a blow still over an eighth of
	//its 550 health after that, 69 or more, seeds corrupting gas at its feet, which kupua are immune to.
	//Its bite: 70 to 160.
	private static void kupuaGas( DemoBook book ){
		book.demo( "mines_f4" ).under( "The corrupting gas comes from a single hit" )
				.stage( Demo.Region.CAVES,
						"######",
						"......",
						"......",
						"......" )
				.hero( "you", 2, 2 )
				.mob( "kupua", Kupua.class, 3, 2 )
				.face( "you", 3, 2 ).face( "kupua", 2, 2 )
				.turn()
				.attack( "you", 3, 2 ).beat().hurt( "kupua", 41 )
				.caption( "small" )
				.turn()
				.attack( "kupua", 2, 2 ).beat().hurt( "you", 112 )
				.turn()
				.attack( "you", 3, 2 ).beat().hurt( "kupua", 83 )
				.pour( GuideEffect.CORROSIVE_GAS, 3, 2 )
				.caption( "gas" )
				.turn()
				//the cloud spreads round it: out of it
				.move( "you", 1, 2 )
				.pour( GuideEffect.CORROSIVE_GAS, 2,1, 3,1, 4,1, 2,2, 4,2, 2,3, 3,3, 4,3 )
				.caption( "back" )
				.turn()
				.move( "kupua", 2, 2 )
				.turns( 2 );
	}

	//While the golem lives, a lit tower strikes the hero whenever he is fewer than 5 tiles from it
	//(LitTower.act: Level.distance, which is the larger of the two offsets), for 300 to 600, wall or
	//no wall. From (0, 2) that is every cell up to x 4.
	private static final int[] TOWER_REACH = {
			0,1, 1,1, 2,1, 3,1, 4,1, 1,2, 2,2, 3,2, 4,2, 0,3, 1,3, 2,3, 3,3, 4,3, 0,4, 1,4, 2,4, 3,4, 4,4 };
	private static final int[] TOWER_SAFE = { 5,1, 6,1, 5,2, 6,2, 5,3, 6,3, 5,4, 6,4 };

	private static void litTowers( DemoBook book ){
		book.demo( "mines_boss" ).under( "Eight lit towers ring the western block" )
				.stage( Demo.Region.PRISON,
						"#######",
						".......",
						".......",
						".......",
						"......." )
				.mob( "tower", LitTower.class, 0, 2 )
				.hero( "you", 6, 2 )
				.face( "you", 0, 2 )
				.mark( Demo.Mark.DANGER, TOWER_REACH ).mark( Demo.Mark.SAFE, TOWER_SAFE )
				.caption( "reach" )
				.turn()
				.move( "you", 5, 2 )
				.turn()
				.move( "you", 4, 2 )
				.beat()
				.zap( "tower", 4, 2 ).lightning( "tower", 4, 2 ).beat().hurt( "you", 452 )
				.caption( "zap" )
				.turn()
				.move( "you", 5, 2 )
				.caption( "out" )
				.turns( 2 );
	}

	//The golem waits (state PASSIVE) until it is hit. Any blow but a relic's deals 1 to a quarter of
	//itself (Otiluke.damage); its own blow is 250 to 350, and from range it fires bolts of 100 to 160
	//that weaken the hero half the time (Otiluke.zap; OtilukeSprite: a SHADOW bolt).
	private static void otilukeCut( DemoBook book ){
		book.demo( "boss_otiluke" ).under( "It stays still until hit" )
				.stage( Demo.Region.PRISON,
						"######",
						"......",
						"......",
						"......" )
				.mob( "golem", Otiluke.class, 4, 2 )
				.hero( "you", 2, 2 )
				.face( "you", 4, 2 ).face( "golem", 2, 2 )
				.caption( "still" )
				.turn()
				.move( "you", 3, 2 )
				.turn()
				.attack( "you", 4, 2 ).beat().hurt( "golem", 7 )
				.caption( "cut" )
				.turn()
				.attack( "golem", 3, 2 ).beat().hurt( "you", 286 )
				.caption( "melee" )
				.turn()
				.move( "you", 2, 2 )
				.turn()
				.zap( "golem", 2, 2 ).bolt( "golem", MagicMissile.SHADOW, 2, 2 )
				.beat().hurt( "you", 131 ).buff( "you", Weakness.class )
				.caption( "bolt" )
				.turns( 2 );
	}

	//A relic's blow reaches the golem whole (RelicMeleeWeapon.isRelicHit), and a relic melee weapon
	//strikes it again for 1 to 2 times that (RelicMeleeWeapon.proc: Random.Int( damage, damage * 2 )).
	private static void otilukeRelic( DemoBook book ){
		book.demo( "boss_otiluke" ).under( "Forge a relic at the town altar first" )
				.stage( Demo.Region.PRISON,
						"######",
						"......",
						"......",
						"......" )
				.mob( "golem", Otiluke.class, 4, 2 )
				.hero( "you", 1, 2 )
				.item( "relic", AresSword.class, 2, 2 )
				.face( "you", 4, 2 ).face( "golem", 1, 2 )
				.caption( "forge" )
				.turn()
				.move( "you", 2, 2 ).hide( "relic" )
				.turn()
				.move( "you", 3, 2 )
				.turn()
				.attack( "you", 4, 2 ).beat().hurt( "golem", 64 ).beat().hurt( "golem", 101 )
				.caption( "relic" )
				.turns( 2 );
	}

	//A fleecing trap takes half the hero's health, or all of it at a tenth of his health or less
	//(FleecingTrap.activate), then goes dead (DragonCaveLevel: INACTIVE_TRAP).
	private static void fleecingLane( DemoBook book ){
		book.demo( "town_dragoncave" ).under( "A lake of fleecing traps" )
				.stage( Demo.Region.PRISON,
						"#######",
						".......",
						"#######" )
				.hero( "you", 1, 1 )
				.trap( "t1", FleecingTrap.class, 2, 1 )
				.trap( "t2", FleecingTrap.class, 3, 1 )
				.trap( "t3", FleecingTrap.class, 4, 1 )
				.trap( "t4", FleecingTrap.class, 5, 1 )
				.face( "you", 6, 1 )
				.turn()
				.move( "you", 2, 1 )
				.beat()
				.hurt( "you", 60 ).hide( "t1" ).terrain( Terrain.INACTIVE_TRAP, 2, 1 )
				.caption( "shear" )
				.turns( 2 )
				.heal( "you", 60 )
				.caption( "rest" )
				.turn()
				.move( "you", 3, 1 )
				.beat()
				.hurt( "you", 60 ).hide( "t2" ).terrain( Terrain.INACTIVE_TRAP, 3, 1 )
				.turn()
				.move( "you", 2, 1, 1, 1 )
				.caption( "lane" )
				.turns( 2 );
	}
}
