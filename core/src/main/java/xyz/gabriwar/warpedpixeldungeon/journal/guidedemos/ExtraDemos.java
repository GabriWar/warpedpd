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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.CountDown;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BanditKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MagicEye;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShadowYog;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shell;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShellCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkeletonHand1;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpectralRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ThiefKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ZotPhase;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokoban;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanBlack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanSwitch;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Skull;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.GuideEffect;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ChangeSheepTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FleecingTrap;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

//Demos for the side chapters: the boss dens, Sokoban, Zot's level and the topics.
//Words: messages/guide/demos_extra.properties.
public final class ExtraDemos {

	private ExtraDemos(){}

	public static void register( DemoBook book ){
		shadowYogVanishes( book );
		skeletonHandCurse( book );
		crabShellCharge( book );
		crabShellFirst( book );
		thiefKingLane( book );
		banditCountdown( book );
		zotBlinks( book );
		zotParalysed( book );
		fleecingTrap( book );
		blackSheep( book );
		sheepPush( book );
		switchSheep( book );
		wellsAndSwitches( book );
		skeletonBurst( book );
	}

	// ------------------------------------------------------------------ the boss dens

	//Below an eighth of its health, every hit on a Shadow Yog has an even chance to teleport it
	//(ShadowYog.damage: HP < HT/8 && Random.Int( 2 ) == 0) to a respawn cell, out of the hero's sight,
	//with the light it leaves behind (Speck.LIGHT). If the floor holds fewer monsters than twice your
	//level, a spectral rat may rise on each of the 4 tiles beside where it lands (3 in 4 each).
	private static void shadowYogVanishes( DemoBook book ){
		book.demo( "boss_shadowyog" ).under( "Below an eighth of its health" )
				.stage( Demo.Region.CAVES,
						".,.....",
						".......",
						"...,...",
						".......",
						"..,...." )
				.mob( "yog", ShadowYog.class, 3, 2 )
				.hero( "you", 2, 2 )
				.face( "yog", 2, 2 )
				.mob( "rat1", SpectralRat.class, 5, 3 ).hide( "rat1" )
				.mob( "rat2", SpectralRat.class, 4, 4 ).hide( "rat2" )
				.mob( "rat3", SpectralRat.class, 6, 4 ).hide( "rat3" )
				.caption( "eighth" )
				.turn()
				//this hit, the coin says stay
				.attack( "you", 3, 2 ).beat().hurt( "yog", 42 )
				.turn()
				//this one, it says go
				.attack( "you", 3, 2 ).beat().hurt( "yog", 38 )
				.burst( GuideEffect.LIGHT, 3, 2 ).hide( "yog" ).jump( "yog", 5, 4 )
				.caption( "vanish" )
				.turn()
				.show( "yog" ).face( "yog", 2, 2 )
				.show( "rat1" ).show( "rat2" ).show( "rat3" )
				.caption( "rats" )
				.turns( 2 );
	}

	//Each hit of the burning hand has an even chance to set you on fire (SkeletonHand1.attackProc:
	//Random.Int( 2 ) == 0), and then it flees until the fire is gone. Burning hurts each turn
	//(1 to 3 + a quarter of the depth) and ends once you stand in water (Burning.act); the hand
	//hunts you again (SkeletonHand1.act), to wherever you went.
	private static void skeletonHandCurse( DemoBook book ){
		book.demo( "boss_skeletonking" ).under( "Each hit has a 1 in 2 chance to curse you" )
				.stage( Demo.Region.HALLS,
						"#######",
						".~.....",
						".~.....",
						".~...,.",
						".~....." )
				.mob( "hand", SkeletonHand1.class, 3, 3 )
				.hero( "you", 2, 3 )
				.face( "you", 3, 3 )
				.caption( "curse" )
				.turn()
				.attack( "hand", 2, 3 ).beat().hurt( "you", 22 )
				.buff( "you", Burning.class ).pour( GuideEffect.FIRE, 2, 3 )
				.caption( "burning" )
				.beat()
				.move( "hand", 4, 2, 5, 1 )
				.turn()
				//a turn on fire, then into the water
				.hurt( "you", 6 )
				.beat()
				.stop().move( "you", 1, 3 ).clear( "you" )
				.caption( "water" )
				.turn()
				//cured: it comes back, to the ground you chose
				.move( "hand", 4, 2, 3, 3, 2, 3 )
				.caption( "back" )
				.turn()
				.attack( "hand", 1, 3 ).beat().hurt( "you", 17 )
				.turns( 2 );
	}

	//While the lightning shell lives, a hit over 5 (a quarter of a shell crab's 20) on a crab is cut to
	//1 and the whole of it stored as charge (ShellCrab.damage). Away from you, the shell zaps you with
	//lightning for a quarter to a half of it, spent (Shell.doAttack): from 14, 5. Hit the shell from
	//beside it while it holds charge and it shocks everything around it with lightning to each
	//(Shell.zapAdjacentCells): you for the charge to twice it (from 9, 12), each crab for 1 to 2 + a
	//quarter of it. The hall is water: half again in water, so 8, 18 and 5 and 3 (Math.round of 7.5,
	//18, 4.5 and 3).
	private static void crabShellCharge( DemoBook book ){
		book.demo( "boss_crabking" ).under( "While the shell lives" )
				.stage( Demo.Region.SEWERS,
						"#######",
						"~~~~~~~",
						"~~~~~~~",
						"~~~~~~~" )
				.mob( "shell", Shell.class, 3, 1 )
				.mob( "crab", ShellCrab.class, 4, 2 )
				.mob( "crab2", ShellCrab.class, 2, 2 )
				.hero( "you", 5, 1 )
				.face( "you", 4, 2 )
				.caption( "absorb" )
				.turn()
				.attack( "you", 4, 2 ).beat().hurt( "crab", 1 ).status( "shell", "stored", CharSprite.WARNING )
				.turn()
				//it spends the charge on you, down the clear line
				.lightning( "shell", 5, 1 ).arrow( Demo.Mark.DANGER, 3, 1, 5, 1 ).beat().hurt( "you", 8 )
				.caption( "zap" )
				.turn()
				.unarrow()
				.move( "you", 4, 1 )
				.turn()
				//beside it, while it still holds charge: everything around it is shocked
				.attack( "you", 3, 1 ).beat().hurt( "shell", 9 )
				.lightning( "shell", 4, 1 ).lightning( "shell", 4, 2 ).lightning( "shell", 2, 2 )
				.mark( Demo.Mark.DANGER, 2, 1, 4, 1, 2, 2, 3, 2, 4, 2 )
				.hurt( "you", 18 ).hurt( "crab", 5 ).hurt( "crab2", 3 )
				.caption( "shock" )
				.turns( 2 );
	}

	//With no charge held, the shell does nothing (Shell.doAttack and damage spend only shellCharge), so
	//it can be worn down; its death clears the charge (Shell.die), and the crabs take their hits whole.
	private static void crabShellFirst( DemoBook book ){
		book.demo( "boss_crabking" ).under( "So kill the shell first" )
				.stage( Demo.Region.SEWERS,
						"#######",
						"~~~~~~~",
						"~~~~~~~",
						"~~~~~~~" )
				.mob( "shell", Shell.class, 3, 1 )
				.mob( "crab", ShellCrab.class, 4, 2 )
				.hero( "you", 2, 2 )
				.face( "you", 3, 1 )
				.caption( "first" )
				.turn()
				.attack( "you", 3, 1 ).beat().hurt( "shell", 31 )
				.turn()
				//its 600 health worn down, the last blow
				.caption( "worn" )
				.attack( "you", 3, 1 ).beat().hurt( "shell", 28 ).die( "shell" )
				.turn()
				.move( "you", 3, 2 )
				.turn()
				.attack( "you", 4, 2 ).beat().hurt( "crab", 14 )
				.caption( "whole" )
				.turn()
				.attack( "you", 4, 2 ).beat().hurt( "crab", 12 ).die( "crab" )
				.turns( 2 );
	}

	//The Thief King shoots darts (ThiefKingSprite.attack) at any distance along a line
	//Ballistica.STOP_SOLID walks clear (ThiefKing.canAttack): a chasm is not passable but is "avoid", so
	//the line goes over it; a wall ends it. From (0, 1) to (6, 2) the line drops a row at (3, 2) and
	//meets the wall at (5, 2).
	private static void thiefKingLane( DemoBook book ){
		book.demo( "boss_thiefking" ).under( "Chasm tiles do not block his shots" )
				.stage( Demo.Region.CITY,
						"#######",
						"...o...",
						"...o.#.",
						"...o..." )
				.mob( "king", ThiefKing.class, 0, 1 )
				.hero( "you", 6, 1 )
				.face( "king", 6, 1 )
				.caption( "lane" )
				.turn()
				.anim( "king", "cast" ).toss( "king", Dart.class, 6, 1 ).arrow( Demo.Mark.DANGER, 0, 1, 6, 1 )
				.beat().hurt( "you", 47 )
				.turn()
				.unarrow()
				.move( "you", 6, 2 )
				.caption( "cover" )
				.turns( 3 );
	}

	//A bandit king's hit (a thief's 1 to 10) puts Countdown on you if you have none, and it flees (BanditKing.attackProc).
	//Countdown ticks 6 turns (CountDown.act); then you lose a quarter of your max health and every
	//bandit king alive heals an eighth of it: from 160, 40 and 20.
	private static void banditCountdown( DemoBook book ){
		book.demo( "boss_thiefking" ).under( "Each of their hits puts" )
				.stage( Demo.Region.CITY,
						"#######",
						".......",
						".......",
						"......." )
				.mob( "bandit", BanditKing.class, 4, 2 )
				.mob( "bandit2", BanditKing.class, 0, 1 )
				.hero( "you", 3, 2 )
				.face( "you", 4, 2 )
				.caption( "countdown" )
				.turn()
				.attack( "bandit", 3, 2 ).beat().hurt( "you", 7 )
				.buff( "you", CountDown.class )
				.beat()
				.move( "bandit", 5, 1, 6, 1 )
				.caption( "flies" )
				.turn().status( "you", "n5", CharSprite.WARNING )
				.turn().status( "you", "n4", CharSprite.WARNING )
				.turn().status( "you", "n3", CharSprite.WARNING )
				.turn().status( "you", "n2", CharSprite.WARNING )
				.turn().status( "you", "n1", CharSprite.WARNING )
				.turn()
				.clear( "you" ).hurt( "you", 40 )
				.caption( "quarter" )
				.beat()
				.heal( "bandit", 20 ).heal( "bandit2", 20 )
				.turns( 2 );
	}

	// ------------------------------------------------------------------ Zot

	//Zot closes in by teleporting (Zot.getCloser: jump) to a free cell you see but never beside you.
	//Each jump first leaves a Zot phase on a tile beside where he stood (Zot.spawnPhase), while there
	//are fewer than 7. He hits for 300 to 400 beside you, or along a clear line with a skull thrown
	//(Zot.canAttack: STOP_SOLID; ZotSprite.attack).
	private static void zotBlinks( DemoBook book ){
		book.demo( "boss_zot" ).under( "To approach he teleports" )
				.stage( Demo.Region.HALLS,
						"#######",
						".....,.",
						".......",
						".......",
						"..,...." )
				.mob( "zot", Zot.class, 5, 1 )
				.mob( "phase", ZotPhase.class, 6, 2 ).hide( "phase" )
				.hero( "you", 1, 3 )
				.face( "zot", 1, 3 )
				.caption( "blink" )
				.turn()
				.hide( "zot" ).jump( "zot", 4, 3 )
				.show( "phase" ).face( "phase", 1, 3 )
				.caption( "phase" )
				.beat()
				.show( "zot" ).anim( "zot", "run" ).face( "zot", 1, 3 )
				.turn()
				.anim( "zot", "cast" ).toss( "zot", Skull.class, 1, 3 ).arrow( Demo.Mark.DANGER, 4, 3, 1, 3 )
				.beat().hurt( "you", 352 )
				.caption( "hit" )
				.turns( 2 );
	}

	//Each turn Zot lies paralysed he heals 200 and a magic eye appears on a free tile beside you
	//(Zot.act: Paralysis, spawnEye), on top of his own 50 to 100 a turn. An eye's blow is a death ray
	//as its attack ends (MagicEyeSprite).
	private static void zotParalysed( DemoBook book ){
		book.demo( "boss_zot" ).under( "Don't paralyse him" )
				.stage( Demo.Region.HALLS,
						"######",
						"......",
						"......",
						"..,..." )
				.mob( "zot", Zot.class, 1, 2 )
				.mob( "eye", MagicEye.class, 4, 1 ).hide( "eye" )
				.mob( "eye2", MagicEye.class, 4, 3 ).hide( "eye2" )
				.hero( "you", 3, 2 )
				.face( "you", 1, 2 )
				.buff( "zot", Paralysis.class ).freeze( "zot" )
				.caption( "paralysed" )
				.turn()
				.heal( "zot", 200 ).show( "eye" ).face( "eye", 3, 2 )
				.caption( "eye" )
				.turn()
				.heal( "zot", 200 ).show( "eye2" ).face( "eye2", 3, 2 )
				.turn()
				.attack( "eye", 3, 2 ).beat().ray( "eye", Effects.Type.DEATH_RAY, 3, 2 ).hurt( "you", 74 )
				.turns( 2 );
	}

	// ------------------------------------------------------------------ Otiluke's folded rooms

	//A fleecing trap takes half your current health, or all of it at a tenth of your max or less
	//(FleecingTrap.activate). From 60 of 100, 30; from 10 of 100, 10.
	private static void fleecingTrap( DemoBook book ){
		book.demo( "sokoban_overview" ).under( "take half your current health" )
				.stage( Demo.Region.PRISON,
						"#####",
						".....",
						"....." )
				.trap( "trap", FleecingTrap.class, 2, 1 )
				.hero( "you", 0, 1 )
				.caption( "half" )
				.turn()
				.move( "you", 1, 1, 2, 1 )
				.beat().beat().hurt( "you", 30 )
				.turns( 2 )
				//again, with a tenth of your health left
				.hide( "you" ).jump( "you", 0, 1 )
				.caption( "tenth" )
				.turn()
				.show( "you" )
				.turn()
				.move( "you", 1, 1, 2, 1 )
				.beat().beat().hurt( "you", 10 ).die( "you" )
				.turns( 2 );
	}

	//Walked into, a black sheep looks for fleecing traps within 5 tiles (SheepSokobanBlack.interact):
	//it leaves in a flash of light and lands on one, and the trap is sprung under it (a sheep on a
	//fleecing trap is fleeced away, SokobanIntroLevel.occupyCell); you step into its place.
	private static void blackSheep( DemoBook book ){
		book.demo( "sok_practice" ).under( "jumps onto a fleecing trap" )
				.stage( Demo.Region.PRISON,
						"#######",
						".......",
						"......." )
				.mob( "sheep", SheepSokobanBlack.class, 1, 1 )
				.trap( "trap", FleecingTrap.class, 5, 2 )
				.hero( "you", 0, 1 )
				.face( "you", 1, 1 )
				.caption( "black" )
				.turn()
				.burst( GuideEffect.LIGHT, 1, 1 ).hide( "sheep" )
				.move( "you", 1, 1 )
				.beat()
				.hide( "trap" )
				.caption( "sprung" )
				.turns( 2 )
				.move( "you", 2, 2, 3, 2, 4, 2, 5, 2 )
				.turns( 2 );
	}

	//Walked into, a plain sheep moves one tile straight away from you, and you take its place
	//(SheepSokoban.interact: only from the four sides, never a diagonal). On a prize tile it makes
	//a prize appear (SokobanIntroLevel.occupyCell: HeapGenTrap's light, then the prize drops), and the
	//tile is spent.
	private static void sheepPush( DemoBook book ){
		book.demo( "sok_practice" ).under( "Push a sheep onto a prize tile" )
				.stage( Demo.Region.PRISON,
						"######",
						"......",
						"......",
						"......" )
				.mob( "sheep", SheepSokoban.class, 1, 2 )
				.item( "key", IronKey.class, 5, 1 ).hide( "key" )
				.hero( "you", 0, 2 )
				.face( "you", 1, 2 )
				.mark( Demo.Mark.HINT, 3, 2 )
				.caption( "push" )
				.turn()
				.move( "sheep", 2, 2 ).move( "you", 1, 2 )
				.turn()
				.move( "sheep", 3, 2 ).move( "you", 2, 2 )
				.beat()
				.burst( GuideEffect.LIGHT, 3, 2 ).unmark( 3, 2 ).show( "key" )
				.caption( "prize" )
				.turn()
				.move( "you", 2, 1 )
				.turn()
				.face( "you", 3, 2 ).status( "sheep", "baa", CharSprite.DEFAULT )
				.caption( "diagonal" )
				.turns( 2 );
	}

	//Walked into, a switch sheep swaps places with you (SheepSokobanSwitch.interact). Standing on a
	//change trap, that puts it on the trap, which turns it into a plain sheep (ChangeSheepTrap.activate,
	//in green sparks) and is spent (SokobanPuzzlesLevel.occupyCell: an inactive trap). A plain sheep
	//can be pushed.
	private static void switchSheep( DemoBook book ){
		book.demo( "sok_puzzles" ).under( "To convert one" )
				.stage( Demo.Region.PRISON,
						"######",
						"......",
						"......" )
				.trap( "trap", ChangeSheepTrap.class, 2, 1 )
				.mob( "switch", SheepSokobanSwitch.class, 3, 1 )
				.mob( "plain", SheepSokoban.class, 2, 1 ).hide( "plain" )
				.hero( "you", 2, 1 )
				.face( "you", 3, 1 )
				.caption( "stand" )
				.turn()
				.move( "switch", 2, 1 ).move( "you", 3, 1 )
				.beat()
				.burst( GuideEffect.GREEN_FIRE, 2, 1 ).hide( "switch" ).hide( "trap" ).show( "plain" )
				.caption( "plain" )
				.turn()
				.face( "you", 2, 1 )
				.turn()
				.move( "plain", 1, 1 ).move( "you", 2, 1 )
				.caption( "once" )
				.turns( 2 );
	}

	//A live well sends the hero to its destination, in light at both ends (SokobanPortalTrap). A sheep
	//pushed onto a switch sets where one well leads, "Click!" in the log, and the switch is spent
	//(SokobanTeleportLevel.occupyCell).
	private static void wellsAndSwitches( DemoBook book ){
		book.demo( "sok_portals" ).under( "Stepping on a live well teleports you" )
				.stage( Demo.Region.PRISON,
						"#######",
						".w.....",
						"......w",
						"......." )
				.mob( "sheep", SheepSokoban.class, 4, 3 )
				.hero( "you", 1, 2 )
				.mark( Demo.Mark.HINT, 3, 3 )
				.caption( "well" )
				.turn()
				.move( "you", 1, 1 )
				.beat()
				.burst( GuideEffect.LIGHT, 1, 1 ).hide( "you" ).jump( "you", 5, 3 )
				.beat().beat()
				.burst( GuideEffect.LIGHT, 5, 3 ).show( "you" ).face( "you", 4, 3 )
				.turn()
				.move( "sheep", 3, 3 ).move( "you", 4, 3 )
				.beat()
				.unmark( 3, 3 ).status( "sheep", "click", CharSprite.DEFAULT )
				.caption( "switch" )
				.turns( 3 );
	}

	// ------------------------------------------------------------------ the topics

	//A skeleton bursts as it dies (SkeletonSprite.die: its bones), hurting everything on the 8 tiles
	//around it for 6 to 12, armor counted twice against it (Skeleton.die).
	private static void skeletonBurst( DemoBook book ){
		book.demo( "topic_bestiary" ).under( "A skeleton bursts when it dies" )
				.stage( Demo.Region.SEWERS,
						"######",
						"......",
						"..,...",
						"......" )
				.mob( "skeleton", Skeleton.class, 3, 2 )
				.hero( "you", 2, 2 )
				.face( "you", 3, 2 )
				.caption( "burst" )
				.turn()
				.attack( "you", 3, 2 ).beat().hurt( "skeleton", 9 )
				.die( "skeleton" ).burst( GuideEffect.BONES, 3, 2 )
				.mark( Demo.Mark.DANGER, 2, 1, 3, 1, 4, 1, 2, 2, 4, 2, 2, 3, 3, 3, 4, 3 )
				.beat().hurt( "you", 7 )
				.caption( "beside" )
				.turns( 2 );
	}
}
