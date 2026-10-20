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

package xyz.gabriwar.warpedpixeldungeon.actors.ledger;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.watabou.noosa.Group;
import com.watabou.utils.PathFinder;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * The damage ledger tests' stage: the enchantments' headless rig (stand-in layers, a GPU that says
 * yes, a room all seen, the clock held), no actors, the badges held as all earned (a sneak attack
 * validates and saves none), a sturdy hero whose sprite stands aside and rats on the stage.
 */
final class LedgerStage {

	EnchantFxTest.Rig rig;
	//the hero's sprite stands aside: a mob's sprite stands in for his and is never updated
	private final Group aside = new Group();
	private Object savedGlobal, savedLocal;
	private String savedSeed;

	private static Field badges( String name ) throws Exception {
		Field f = Badges.class.getDeclaredField( name );
		f.setAccessible( true );
		return f;
	}

	void up() throws Exception {
		savedGlobal = badges( "global" ).get( null );
		savedLocal = badges( "local" ).get( null );
		savedSeed = Dungeon.customSeedText;
		badges( "global" ).set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
		badges( "local" ).set( null, new HashSet<Badges.Badge>() );
		Dungeon.customSeedText = "test";
		rig = new EnchantFxTest.Rig();
		rig.up();
		HitCard.reset();
		Actor.clear();
		Dungeon.level.mobs = new LinkedHashSet<>();
		PathFinder.setMapSize( Dungeon.level.width(), Dungeon.level.height() );
		DamageLedger.recording = true;
	}

	void down() throws Exception {
		DamageLedger.recording = true;
		TestParty.leave();
		HitCard.reset();
		Actor.clear();
		Dungeon.hero = null;
		rig.down();
		if (Dungeon.level != null) PathFinder.setMapSize( Dungeon.level.width(), Dungeon.level.height() );
		badges( "global" ).set( null, savedGlobal );
		badges( "local" ).set( null, savedLocal );
		Dungeon.customSeedText = savedSeed;
	}

	/** The hero, sturdy and strong, at a cell. */
	Hero hero( int cell ){
		Hero h = new Hero();
		h.HP = h.HT = 9999;
		h.STR = 20;
		//(an interrupt wants a game scene's cell selector)
		h.damageInterrupt = false;
		h.pos = cell;
		CharSprite s = new RatSprite();
		s.ch = h;
		h.sprite = s;
		aside.add( s );
		s.place( cell );
		s.visible = true;
		Dungeon.hero = h;
		Actor.add( h );
		return h;
	}

	/** A mob standing still at a cell, its sprite a rat's. */
	<M extends Mob> M body( M m, int cell, int hp ){
		m.HP = m.HT = hp;
		m.pos = cell;
		m.state = m.PASSIVE;
		CharSprite s = new RatSprite();
		s.ch = m;
		m.sprite = s;
		rig.stage.add( s );
		s.place( cell );
		s.visible = true;
		Actor.add( m );
		Dungeon.level.mobs.add( m );
		return m;
	}

	Rat rat( int cell, int hp ){
		return body( new Rat(), cell, hp );
	}
}
