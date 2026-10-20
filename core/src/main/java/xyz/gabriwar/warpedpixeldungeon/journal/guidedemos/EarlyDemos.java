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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Adrenaline;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrokenRobot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Brute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM300;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Guard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Necromancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Pylon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderEgg;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderQueen;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderServant;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Spinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Swarm;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Thief;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.GuideEffect;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.TenguDartTrap;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.DM300Sprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.PylonSprite;
import com.watabou.noosa.TextureFilm;

//Demos for the first regions: the sewers, the prison and its spider nest, the caves with their mine and temple.
//Words: messages/guide/demos_early.properties.
public final class EarlyDemos {

	private EarlyDemos(){}

	public static void register( DemoBook book ){
		snakeAtTheDoor( book );
		swarmSplits( book );
		gooSlam( book );
		skeletonBurst( book );
		guardChain( book );
		necromancerSkeleton( book );
		tenguTraps( book );
		tenguBomb( book );
		queenEggs( book );
		bruteRage( book );
		spinnerWebs( book );
		robotBeam( book );
		pylonShocks( book );
		dm300Rockfall( book );
		dm300Supercharge( book );
	}

	//the cells of a rectangle, corner to corner, as (x, y) pairs
	private static int[] square( int x0, int y0, int x1, int y1 ){
		int[] out = new int[2 * (x1 - x0 + 1) * (y1 - y0 + 1)];
		int i = 0;
		for (int y = y0; y <= y1; y++){
			for (int x = x0; x <= x1; x++){
				out[i++] = x;
				out[i++] = y;
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ the sewers

	//A hunting snake that loses sight of you is surprised when it sees you again (Mob.surprisedBy:
	//!enemySeen): a closed door and the wall beside it hide you, so as it steps into the doorway your
	//blow cannot miss (its 25 defense drops to 0). It has 4 health.
	private static void snakeAtTheDoor( DemoBook book ){
		book.demo( "sewers_f1" ).under( "let it follow you through a door" )
				.stage( Demo.Region.SEWERS,
						"#######",
						"...#...",
						"...+...",
						"...#..." )
				.mob( "snake", Snake.class, 6, 2 )
				.hero( "you", 4, 2 )
				.caption( "follow" )
				.turn()
				.move( "you", 3, 2 ).terrain( Terrain.OPEN_DOOR, 3, 2 )
				.move( "snake", 5, 2 )
				.turn()
				//around the corner, the door shut between you
				.move( "you", 2, 1 )
				.move( "snake", 4, 2 )
				.beat().terrain( Terrain.DOOR, 3, 2 )
				.turn()
				.move( "snake", 3, 2 ).terrain( Terrain.OPEN_DOOR, 3, 2 )
				.beat()
				.attack( "you", 3, 2 )
				.beat()
				.surprise( "snake" ).hurt( "snake", 4 )
				.caption( "strike" )
				.beat()
				.die( "snake" )
				.turns( 2 );
	}

	//Every melee or thrown hit splits the swarm (Swarm.defenseProc) onto a free tile beside it, up,
	//down, left or right, the halves sharing what health is left: 80 hit for 10 is two swarms of 35.
	//A wand's zap is no attack, so it never splits it.
	private static void swarmSplits( DemoBook book ){
		book.demo( "sewers_f3" ).under( "Every weapon hit" )
				.stage( Demo.Region.SEWERS,
						"#######",
						"...,...",
						".......",
						".,....." )
				.mob( "swarm", Swarm.class, 3, 2 )
				.mob( "half", Swarm.class, 3, 2 ).hide( "half" )
				.mob( "quarter", Swarm.class, 3, 2 ).hide( "quarter" )
				.hero( "you", 2, 2 )
				.turn()
				.attack( "you", 3, 2 )
				.beat()
				.hurt( "swarm", 10 )
				.show( "half" ).knock( "half", 3, 1 )
				.caption( "split" )
				.turn()
				.attack( "you", 3, 2 )
				.beat()
				.hurt( "swarm", 10 )
				.show( "quarter" ).knock( "quarter", 4, 2 )
				.turn()
				.zap( "you", 3, 2 ).bolt( "you", MagicMissile.MAGIC_MISSILE, 3, 2 )
				.beat()
				.hurt( "swarm", 8 )
				.caption( "wand" )
				.turn()
				.attack( "half", 2, 2 ).beat().hurt( "you", 3 )
				.turns( 2 );
	}

	//Goo's slam reaches the tiles Ballistica walks clear from Goo to them and back
	//(Goo.canAttack, GooSprite.updateEmitters): 1 tile on the turn it swells, 2 on the next.
	//From (2, 2), with the pillar at (3, 2):
	private static final int[] GOO_REACH_1 = { 1,1, 2,1, 3,1, 1,2, 1,3, 2,3, 3,3 };
	//the pillar's shadow, (4, 1) (4, 2) (4, 3), is out of it: those lines cross the pillar one way or the other
	private static final int[] GOO_REACH_2 = { 0,1, 1,1, 2,1, 3,1, 0,2, 1,2, 0,3, 1,3, 2,3, 3,3, 0,4, 1,4, 2,4, 3,4, 4,4 };

	//Goo swells and shouts (Goo.doAttack: pumpedUp 1, its "!!!"), swells again, and two turns after
	//the shout slams you, still in reach, for triple damage (3x its 1-8: 3 to 24), its sparks bursting
	//on every tile it warned (GooSprite.triggerEmitters). Then it charges again, the hero steps behind
	//the pillar, and Goo, unable to reach, has to move: the charge is lost (Goo.getCloser: pumpedUp 0).
	private static void gooSlam( DemoBook book ){
		book.demo( "boss_goo" ).under( "swells and shouts" )
				.stage( Demo.Region.SEWERS,
						"#######",
						",......",
						"...#...",
						"......,",
						"..,...." )
				.mob( "goo", Goo.class, 2, 2 )
				.hero( "you", 3, 3 )
				.face( "you", 2, 2 )
				.turn()
				//it swells and shouts: the slam is charging
				.anim( "goo", "pump" ).status( "goo", "actors.mobs.goo.!!!", CharSprite.WARNING )
				.mark( Demo.Mark.DANGER, GOO_REACH_1 ).pour( GuideEffect.GOO, GOO_REACH_1 )
				.caption( "swells" )
				.beat()
				.attack( "you", 2, 2 ).beat().hurt( "goo", 4 )
				.turn()
				//swelling still: now it reaches 2 tiles, wherever the line to it is clear
				.stop().unmark()
				.mark( Demo.Mark.DANGER, GOO_REACH_2 ).pour( GuideEffect.GOO, GOO_REACH_2 )
				.caption( "reach" )
				.beat()
				.attack( "you", 2, 2 ).beat().hurt( "goo", 5 )
				.turn()
				//two turns after the shout: the slam
				.anim( "goo", "pumpAttack" )
				.beat()
				.stop().unmark().burst( GuideEffect.GREEN_FIRE, GOO_REACH_2 )
				.hurt( "you", 15 )
				.caption( "slam" )
				.turns( 2 )
				//another charge: this time, behind the pillar
				.caption( "hide" )
				.turn()
				.anim( "goo", "pump" ).status( "goo", "actors.mobs.goo.!!!", CharSprite.WARNING )
				.mark( Demo.Mark.DANGER, GOO_REACH_1 ).pour( GuideEffect.GOO, GOO_REACH_1 )
				.beat()
				.move( "you", 4, 2 )
				.turn()
				//it cannot reach you: it moves, and the charge is gone
				.stop().unmark()
				.move( "goo", 3, 3 )
				.caption( "lost" )
				.turns( 2 );
	}

	// ------------------------------------------------------------------ the prison

	//A dying skeleton's bones hit every creature beside it for 6 to 12, less twice their armor
	//(Skeleton.die; SkeletonSprite.die: its bones burst): the hero who finished it, and a thief
	//standing by, monster or not.
	private static void skeletonBurst( DemoBook book ){
		book.demo( "prison_f1" ).under( "They explode when they die" )
				.stage( Demo.Region.PRISON,
						"#####",
						".....",
						".,..." )
				.mob( "skeleton", Skeleton.class, 2, 2 )
				.mob( "thief", Thief.class, 3, 1 )
				.hero( "you", 1, 2 )
				.face( "thief", 2, 2 )
				.turn()
				.attack( "you", 2, 2 )
				.beat()
				.hurt( "skeleton", 9 )
				.die( "skeleton" ).burst( GuideEffect.BONES, 2, 2 )
				.beat()
				.hurt( "you", 5 ).hurt( "thief", 9 )
				.caption( "burst" )
				.turns( 2 )
				.caption( "armor" )
				.turns( 2 );
	}

	//A guard that sees you within 4 tiles but cannot hit you throws its chain, once (Guard.chain: Chains
	//to you): you are pulled to the first free tile beside it along the line and crippled for 4 turns
	//(Guard.pullEnemy). Its blows hit 4 to 12.
	private static void guardChain( DemoBook book ){
		book.demo( "prison_f2" ).under( "can pull you next to it with its chain" )
				.stage( Demo.Region.PRISON,
						"#######",
						"...,...",
						"......." )
				.mob( "guard", Guard.class, 1, 2 )
				.hero( "you", 5, 2 )
				.face( "you", 1, 2 )
				.caption( "chain" )
				.turn()
				.chain( "guard", 5, 2 ).arrow( Demo.Mark.DANGER, 5, 2, 2, 2 )
				.beat()
				.knock( "you", 2, 2 )
				.beat()
				.unarrow()
				.buff( "you", Cripple.class )
				.turn()
				.attack( "guard", 2, 2 ).beat().hurt( "you", 8 )
				.caption( "cripple" )
				.turn()
				.attack( "guard", 2, 2 ).beat().hurt( "you", 6 )
				.turns( 2 )
				.clear( "you" )
				.turn();
	}

	//Within 4 tiles a necromancer raises its skeleton on the free tile beside you nearest to it
	//(Necromancer.Hunting), there the turn after. The skeleton starts at 20 of its 25 health: a zap
	//heals it 5 (a fifth), and once it is whole the next zap enrages it (Adrenaline), each down a
	//health ray (Necromancer.onZapComplete: Beam.HealthRay) as the zap ends. Kill the
	//necromancer (40 health) and its skeleton dies with it (Necromancer.die). Skeletons hit 10 to 20.
	private static void necromancerSkeleton( DemoBook book ){
		book.demo( "prison_f3" ).under( "raises one skeleton next to you" )
				.stage( Demo.Region.PRISON,
						"#######",
						".......",
						".......",
						"..,...." )
				.mob( "necro", Necromancer.class, 6, 2 )
				.mob( "skeleton", Necromancer.NecroSkeleton.class, 4, 2 ).hide( "skeleton" )
				.hero( "you", 3, 2 )
				.turn()
				.zap( "necro", 4, 2 )
				.mark( Demo.Mark.HINT, 4, 2 )
				.caption( "raise" )
				.turn()
				.unmark()
				.show( "skeleton" )
				.turn()
				.attack( "skeleton", 3, 2 ).beat().hurt( "you", 14 )
				.zap( "necro", 4, 2 ).beat().ray( "necro", Effects.Type.HEALTH_RAY, 4, 2 ).heal( "skeleton", 5 )
				.caption( "heal" )
				.turn()
				.zap( "necro", 4, 2 ).beat().ray( "necro", Effects.Type.HEALTH_RAY, 4, 2 ).buff( "skeleton", Adrenaline.class )
				.turn()
				//go for the necromancer, around its skeleton
				.move( "you", 4, 1 )
				.attack( "skeleton", 4, 1 ).beat().hurt( "you", 15 )
				.caption( "kill" )
				.turn()
				.move( "you", 5, 1 )
				.move( "skeleton", 5, 2 )
				.turn()
				.attack( "you", 6, 2 ).beat().hurt( "necro", 20 )
				.attack( "skeleton", 5, 1 ).beat().hurt( "you", 12 )
				.turn()
				.attack( "you", 6, 2 ).beat().hurt( "necro", 20 )
				.die( "necro" )
				.beat()
				.die( "skeleton" )
				.turns( 2 );
	}

	//Phase 1: each 25 of his 200 health lost (Tengu.damage: HT/8), Tengu jumps somewhere in his cell
	//more than 3.5 tiles from you (Tengu.jump) and sows hidden dart traps (TenguDartTrap: poison,
	//never found by searching), each flashing once (Speck.LIGHT) and shown for a moment before it fades
	//(PrisonBossLevel.placeTrapsInTenguCell). Under 175 health about half his cell is traps, with a
	//safe path from him to you 4 to 7 tiles long: here 6, along the bottom row.
	private static final int[] TENGU_TRAPS = {
			1,2, 2,2, 3,2, 4,2,
			1,3, 2,3, 3,3, 4,3, 5,3,
			2,4, 3,4, 4,4 };

	private static void tenguTraps( DemoBook book ){
		Demo d = book.demo( "boss_tengu" ).under( "sows hidden poison dart traps" )
				.stage( Demo.Region.PRISON,
						"#######",
						".......",
						".......",
						".......",
						".......",
						"......." )
				.mob( "tengu", Tengu.class, 5, 1 )
				.hero( "you", 0, 4 );
		for (int i = 0; i < TENGU_TRAPS.length; i += 2){
			String id = trap( TENGU_TRAPS, i );
			d.trap( id, TenguDartTrap.class, TENGU_TRAPS[i], TENGU_TRAPS[i + 1] ).hide( id );
		}
		d.caption( "jump" )
				.turn()
				.zap( "you", 5, 1 ).bolt( "you", MagicMissile.MAGIC_MISSILE, 5, 1 )
				.beat()
				.hurt( "tengu", 25 )
				.beat()
				.jump( "tengu", 6, 5 )
				.beat()
				.burst( GuideEffect.LIGHT, TENGU_TRAPS )
				.caption( "sow" );
		for (int i = 0; i < TENGU_TRAPS.length; i += 2) d.show( trap( TENGU_TRAPS, i ) );
		//the hero takes in where they are; then they fade away
		d.turns( 3 );
		for (int i = 0; i < TENGU_TRAPS.length; i += 2) d.hide( trap( TENGU_TRAPS, i ) );
		d.caption( "hidden" )
				.turns( 2 )
				.zap( "you", 6, 5 ).bolt( "you", MagicMissile.MAGIC_MISSILE, 6, 5 )
				.beat()
				.hurt( "tengu", 12 )
				.caption( "range" )
				.turns( 2 )
				//chasing him instead
				.move( "you", 1, 4 )
				.caption( "chase" )
				.turn()
				.move( "you", 2, 4 )
				.beat()
				.show( "t24" )
				.buff( "you", Poison.class )
				.turns( 2 );
	}

	private static String trap( int[] cells, int i ){
		return "t" + cells[i] + cells[i + 1];
	}

	//Phase 2: the first trick is always a bomb (Tengu.useAbility), thrown on the tile beside you nearest
	//to him (Tengu.throwBomb: a BombItem, as MissileSprite throws it). It counts 3, 2, 1 and blows up
	//on the 4th turn, hurting everything but Tengu within 2 steps (BombAbility.act: PathFinder distance
	//2), its smoke pouring over that area.
	private static final int[] BOMB_BLAST = square( 2, 0, 6, 4 );

	private static void tenguBomb( DemoBook book ){
		book.demo( "boss_tengu" ).under( "The first is always a bomb" )
				.stage( Demo.Region.PRISON,
						"#######",
						".......",
						".......",
						".......",
						".......",
						"......." )
				.mob( "tengu", Tengu.class, 6, 1 )
				.hero( "you", 3, 3 )
				.sprite( "bomb", ItemSpriteSheet.TENGU_BOMB, 4, 2 ).hide( "bomb" )
				.turn()
				.zap( "tengu", 4, 2 ).toss( "tengu", Tengu.BombAbility.BombItem.class, 4, 2 )
				.beat()
				.show( "bomb" )
				.mark( Demo.Mark.DANGER, BOMB_BLAST ).pour( GuideEffect.SMOKE, BOMB_BLAST )
				.caption( "bomb" )
				.turn()
				.move( "you", 2, 3 )
				.turn()
				.move( "you", 1, 3 )
				.turn()
				.stop().unmark().hide( "bomb" )
				.burst( GuideEffect.FIRE, BOMB_BLAST )
				.caption( "clear" )
				.turns( 2 );
	}

	//The queen bites for 12 to 20 and poisons on half her hits (SpiderQueen.attackProc). Hunting, she
	//lays an egg on a free tile beside her 1 turn in 20 (SpiderQueen.act); an egg has 2 health
	//(SpiderEgg). Under half health she stops attacking and backs away from you (canAttack, getCloser)
	//while her brood comes on.
	private static void queenEggs( DemoBook book ){
		book.demo( "boss_spiderqueen" ).under( "lays an egg beside her" )
				.stage( Demo.Region.NEST,
						"#######",
						".......",
						".......",
						".......",
						"......." )
				.mob( "queen", SpiderQueen.class, 4, 2 )
				.mob( "egg", SpiderEgg.class, 4, 3 ).hide( "egg" )
				.mob( "spider", SpiderServant.class, 6, 4 )
				.hero( "you", 3, 2 )
				.caption( "bite" )
				.turn()
				.attack( "queen", 3, 2 )
				.beat()
				.hurt( "you", 16 ).buff( "you", Poison.class )
				.turn()
				.show( "egg" )
				.caption( "egg" )
				.turn()
				.attack( "queen", 3, 2 ).beat().hurt( "you", 14 )
				.turn()
				.attack( "you", 4, 3 )
				.beat()
				.hurt( "egg", 2 ).die( "egg" )
				.caption( "break" )
				.turn()
				.attack( "queen", 3, 2 ).beat().hurt( "you", 13 )
				.turn()
				.attack( "you", 4, 2 )
				.beat()
				.hurt( "queen", 18 )
				.caption( "flee" )
				.turn()
				.move( "queen", 5, 1 )
				.move( "spider", 5, 3 )
				.turn()
				.move( "queen", 6, 1 )
				.move( "spider", 4, 3 )
				.turn()
				.attack( "spider", 3, 2 ).beat().hurt( "you", 5 )
				.turns( 2 );
	}

	// ------------------------------------------------------------------ the caves

	//A brute at 0 health does not die (Brute.isAlive): it rages behind a shield of half its 40 health
	//plus 4, 24, and hits for 30 to 70 instead of 15 to 40. The shield drains 4 a turn
	//(BruteRage.act), so 6 turns after it rages it falls. Round a block of rock, cutting its corners,
	//the brute is never beside you at the end of your move: it walks every turn and never swings.
	private static void bruteRage( DemoBook book ){
		Demo d = book.demo( "caves_f1" ).under( "don't die at 0 health" )
				.stage( Demo.Region.CAVES,
						"#######",
						".......",
						"..###..",
						"....,.." )
				.mob( "brute", Brute.class, 1, 2 )
				.hero( "you", 2, 1 )
				.turn()
				.attack( "you", 1, 2 )
				.beat()
				.hurt( "brute", 12 )
				.beat()
				.buff( "brute", Brute.BruteRage.class )
				.caption( "rage" )
				.turn()
				//stay beside it, and it hits twice as hard
				.attack( "brute", 2, 1 )
				.beat()
				.hurt( "you", 48 ).hurt( "brute", 4 );
		//then keep going round the rock, one step ahead of it, its shield draining
		int[] you = { 3,1, 4,1, 5,2, 4,3, 3,3 };
		int[] brute = { 2,1, 3,1, 4,1, 5,2, 4,3 };
		for (int i = 0; i < you.length; i += 2){
			d.turn()
					.move( "you", you[i], you[i + 1] ).move( "brute", brute[i], brute[i + 1] )
					.beat().hurt( "brute", 4 );
			if (i == 0) d.caption( "drain" );
		}
		d.beat()
				.die( "brute" )
				.caption( "gone" )
				.turns( 2 );
	}

	//A spinner aims its web along the way you are walking (Spinner.webPos: from your last tile through
	//this one) and spreads it over that tile and the two beside it (Spinner.shootWeb, flown there as
	//SpinnerSprite.zap's MAGIC_MISSILE bolt); then it waits 10 turns to web again. Its bite (12 to
	//26) poisons half the time, and then it backs off (FLEEING).
	private static void spinnerWebs( DemoBook book ){
		book.demo( "caves_f2" ).under( "They web the tile you are moving toward" )
				.stage( Demo.Region.CAVES,
						"#######",
						".......",
						"...,...",
						".......",
						"....,.." )
				.mob( "spinner", Spinner.class, 6, 2 )
				.hero( "you", 0, 2 )
				.caption( "web" )
				.turn()
				.move( "you", 1, 2 )
				.beat()
				.zap( "spinner", 2, 2 ).bolt( "spinner", MagicMissile.MAGIC_MISSILE, 2, 2 )
				.beat()
				.mark( Demo.Mark.HINT, 2, 1, 2, 2, 2, 3 ).burst( GuideEffect.WEB, 2, 1, 2, 2, 2, 3 )
				.turn()
				.move( "you", 1, 3 )
				.move( "spinner", 5, 2 )
				.caption( "turn" )
				.turn()
				.move( "you", 2, 4 )
				.move( "spinner", 4, 3 )
				.turn()
				.move( "spinner", 3, 4 )
				.turn()
				.attack( "spinner", 2, 4 )
				.beat()
				.hurt( "you", 18 ).buff( "you", Poison.class )
				.caption( "bite" )
				.turn()
				.move( "spinner", 4, 3 )
				.turn()
				.move( "spinner", 5, 2 )
				.turns( 2 );
	}

	//The broken robot's beam (BrokenRobotSprite: a death ray as its attack ends) runs on to the first
	//wall (BrokenRobot.doAttack: Ballistica STOP_SOLID) and hits everyone on it for 10 to 12, a bat in
	//the way too, with purple sparks; it needs 3 turns before the next (attackDelay 3).
	private static final int[] ROBOT_BEAM = { 1,2, 2,2, 3,2, 4,2, 5,2, 6,2 };

	private static void robotBeam( DemoBook book ){
		book.demo( "caves_f3" ).under( "fires a beam every three turns" )
				.stage( Demo.Region.CAVES,
						"#######",
						"..,....",
						"......." )
				.mob( "robot", BrokenRobot.class, 0, 2 )
				.mob( "bat", Bat.class, 2, 2 )
				.hero( "you", 4, 2 )
				.face( "you", 0, 2 )
				.caption( "beam" )
				.turn()
				.attack( "robot", 4, 2 )
				.beat()
				.ray( "robot", Effects.Type.DEATH_RAY, 6, 2 )
				.mark( Demo.Mark.TARGET, ROBOT_BEAM )
				.burst( GuideEffect.ARCANE, 2, 2, 4, 2 )
				.hurt( "bat", 11 ).hurt( "you", 11 )
				.turn()
				.unmark()
				.move( "bat", 3, 2 )
				.caption( "wait" )
				.turn()
				.attack( "bat", 4, 2 ).beat().hurt( "you", 16 )
				.turn()
				.attack( "robot", 4, 2 )
				.beat()
				.ray( "robot", Effects.Type.DEATH_RAY, 6, 2 )
				.mark( Demo.Mark.TARGET, ROBOT_BEAM )
				.burst( GuideEffect.ARCANE, 3, 2, 4, 2 )
				.hurt( "bat", 10 ).hurt( "you", 12 )
				.caption( "beam" )
				.turn()
				.unmark()
				.turn();
	}

	//An active pylon shocks two opposite tiles beside it with lightning, one step clockwise each turn
	//(Pylon.act: CIRCLE8[n] and CIRCLE8[n+4], n+1 after): each tile beside it is struck every 4th
	//turn, so stepping one tile back against the turn every 3 turns keeps you out of it. Hits of 15
	//and more are cut down (Pylon.damage: 30 deals 19); it has 50 health. From the top-left, clockwise:
	private static final int[][] PYLON_SHOCKS = {
			{ 1,1, 3,3 }, { 2,1, 2,3 }, { 3,1, 1,3 }, { 3,2, 1,2 }, { 3,3, 1,1 }, { 2,3, 2,1 }, { 1,3, 3,1 } };

	private static final Demo.Look LIT = ( s, on ) -> {
		if (on) ((PylonSprite) s).activate();
	};

	private static void pylonShocks( DemoBook book ){
		Demo d = book.demo( "caves_f5" ).under( "shocks two opposite adjacent tiles" )
				.stage( Demo.Region.CAVES,
						"#####",
						".....",
						".._..",
						".....",
						"..,.." )
				.mob( "pylon", Pylon.class, 2, 2 )
				.look( "pylon", LIT, true )
				.hero( "you", 1, 2 );
		int[] hits = { 7, 7, 7, 0, 19, 7, 0 };
		int[][] steps = { null, null, null, { 1, 3 }, null, null, { 2, 3 } };
		for (int n = 0; n < PYLON_SHOCKS.length; n++){
			d.turn().unmark();
			if (steps[n] != null){
				d.move( "you", steps[n][0], steps[n][1] ).caption( "step" );
			} else {
				d.attack( "you", 2, 2 ).beat().hurt( "pylon", hits[n] );
				if (n == 0) d.caption( "pylon" );
				if (hits[n] == 19) d.caption( "cut" );
			}
			int[] hit = PYLON_SHOCKS[n];
			d.beat().lightning( "pylon", hit[0], hit[1] ).lightning( "pylon", hit[2], hit[3] )
					.mark( Demo.Mark.TARGET, hit ).burst( GuideEffect.LIGHT, hit );
		}
		d.turn().unmark().turn();
	}

	//Rocks fall in the 7x7 square around where they center (DM300.dropRocks): the center and the
	//8 tiles round it but the safe one always, half the tiles 2 away, a third of those 3 away
	//(Random.Int( distance ) == 0). Centered on (3, 3), the safe tile (2, 2):
	private static final int[] DM300_ROCKS = {
			//the center and its ring
			3,3, 3,2, 4,2, 2,3, 4,3, 2,4, 3,4, 4,4,
			//half of those 2 away
			1,1, 3,1, 5,2, 1,3, 5,4, 1,5, 3,5, 4,5,
			//a third of those 3 away
			0,0, 4,0, 6,1, 0,2, 6,4, 0,5, 2,6, 6,6 };

	//DM-300 slams next to you: you are thrown 2 tiles back (WandOfBlastWave.throwChar, 2 when adjacent),
	//the rocks center where you land, and one tile next to that center is safe. You get one turn
	//(the rocks fall after the hero's next action) to step onto it; each rock deals 6 to 12 and stuns
	//for 3 turns (DM300.FallingRockBuff).
	private static void dm300Rockfall( DemoBook book ){
		book.demo( "boss_dm300" ).under( "knocks you back" )
				.stage( Demo.Region.CAVES,
						".......",
						".......",
						".......",
						".......",
						".......",
						".......",
						"......." )
				.mob( "dm", DM300.class, 3, 6 )
				.hero( "you", 3, 5 )
				.caption( "thrown" )
				.turn().beat()
				//its drill strikes the ground: next to it, you are thrown 2 tiles back
				.anim( "dm", "slam" )
				.beat()
				.knock( "you", 3, 3 )
				.beat()
				//the 7x7 square around where you land is marked, but for one tile next to its center
				.mark( Demo.Mark.TARGET, DM300_ROCKS ).mark( Demo.Mark.SAFE, 2, 2 )
				.caption( "area" )
				.turn()
				.move( "you", 2, 2 )
				.turn()
				//the rocks fall
				.unmark().burst( GuideEffect.ROCKS, DM300_ROCKS )
				.caption( "rocks" )
				.turns( 3 );
	}

	private static final Demo.Look CHARGED = ( s, on ) -> ((DM300Sprite) s).updateChargeState( on );

	//a pylon's broken frame (PylonSprite's die, which would read the pylon's tile), held
	private static final Demo.Look BROKEN = ( s, on ) -> {
		s.paused = on;
		if (on) s.frame( new TextureFilm( s.texture, 10, 20 ).get( 2 ) );
	};

	//Hit down to 200 (and again to 100, DM300.damage: HT/3 steps), DM-300 supercharges: it lights a
	//pylon other than the one nearest you (CavesBossLevel.activatePylon), cannot be hurt
	//(isInvulnerable: its card shows IMMUNE), charges for 3 turns, then moves at double speed (speed x2) but
	//still strikes once a turn, for 15 to 25. The lit pylon has 50 health and takes hits under 15 whole
	//(Pylon.damage); when it falls the charge ends (Pylon.die, DM300.loseSupercharge).
	private static void dm300Supercharge( DemoBook book ){
		book.demo( "boss_dm300" ).under( "supercharges" )
				.stage( Demo.Region.CAVES,
						"#######",
						"._..._.",
						".......",
						"...,...",
						"......." )
				.mob( "dm", DM300.class, 1, 3 )
				.mob( "near", Pylon.class, 1, 1 )
				.mob( "far", Pylon.class, 5, 1 )
				.mob( "lit", Pylon.class, 5, 1 ).look( "lit", LIT, true ).hide( "lit" )
				.hero( "you", 2, 3 )
				.caption( "charge" )
				.turn()
				.attack( "you", 1, 3 )
				.beat()
				.hurt( "dm", 14 )
				.beat()
				.look( "dm", CHARGED, true ).anim( "dm", "charge" )
				.chip( "dm", xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.IMMUNE )
				.hide( "far" ).show( "lit" )
				.turn()
				.attack( "you", 1, 3 )
				.beat()
				.chip( "dm", xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.IMMUNE )
				.caption( "immune" )
				.turn()
				.move( "you", 3, 2 )
				.turn()
				.move( "you", 4, 2 )
				.move( "dm", 2, 3, 3, 3 )
				.caption( "fast" )
				.turn()
				.attack( "you", 5, 1 ).beat().hurt( "lit", 14 )
				.attack( "dm", 4, 2 ).beat().hurt( "you", 21 )
				.turn()
				.attack( "you", 5, 1 ).beat().hurt( "lit", 14 )
				.caption( "run" )
				.attack( "dm", 4, 2 ).beat().hurt( "you", 18 )
				.turn()
				.attack( "you", 5, 1 ).beat().hurt( "lit", 14 )
				.attack( "dm", 4, 2 ).beat().hurt( "you", 23 )
				.turn()
				.attack( "you", 5, 1 ).beat().hurt( "lit", 14 )
				.look( "lit", BROKEN, true )
				.look( "dm", CHARGED, false )
				.caption( "lost" )
				.beat().beat()
				.hide( "lit" )
				.turns( 2 );
	}
}
