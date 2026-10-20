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

package xyz.gabriwar.warpedpixeldungeon.items.rarity;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AdultDragonViolet;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoPiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Assassin;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bee;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Beetle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BlueCat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrokenRobot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownBat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Brute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ChaosMage;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ClayGolem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Crab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrabKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfLich;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollArcher;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollGeomancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollSapper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GoldThief;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreyOni;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreyRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Guard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gullin;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HoardSquirrel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.KoboldIcemancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Kupua;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MarketBodyguard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MrDestructo2dot0;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MrDestructo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Necromancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Oni;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Piranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RatBoss;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shaman;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShellCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderBot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderExploding;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderMind;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderQueen;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderServant;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Spinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Squid;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SteelBee;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Swarm;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Thief;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ThiefKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tinkerer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Velocirooster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Warlock;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

/**
 * The kind of foe a SLAYER line is sharpened against, rolled with the line and named in it
 * (Quality.slayer). Read from the foe's properties where the game marks them (the undead, demons,
 * the elements) and from its class where it does not (beasts, humanoids, the built). A foe in two
 * families answers to both, and a line only ever names one. Saved by name: append only.
 */
public enum SlayerFamily {

	UNDEAD, DEMONS, CONSTRUCTS, ELEMENTALS, BEASTS, HUMANOIDS;

	//machines and golems the game does not mark inorganic; subclasses come with them
	private static final Class<?>[] BUILT = {
			ClayGolem.class, BrokenRobot.class, SpiderBot.class, MrDestructo.class, MrDestructo2dot0.class
	};

	private static final Class<?>[] BEAST = {
			Rat.class, GreyRat.class, RatBoss.class, Bat.class, BrownBat.class, BrownWolf.class, GrayWolf.class,
			Bunny.class, Deer.class, Goat.class, Eagle.class, Snake.class, Squid.class, HoardSquirrel.class,
			Velocirooster.class, Yeti.class, Kupua.class, Gullin.class, Crab.class, CrabKing.class, ShellCrab.class,
			Piranha.class, AlbinoPiranha.class, Bee.class, SteelBee.class, Swarm.class, Beetle.class, Spinner.class,
			SpiderGuard.class, SpiderMind.class, SpiderServant.class, SpiderExploding.class, SpiderQueen.class,
			AdultDragonViolet.class
	};

	private static final Class<?>[] HUMANOID = {
			Gnoll.class, GnollArcher.class, GnollGuard.class, GnollSapper.class, GnollGeomancer.class, Brute.class,
			Shaman.class, Thief.class, GoldThief.class, ThiefKing.class, BlueCat.class, Assassin.class, Tengu.class,
			Oni.class, GreyOni.class, Monk.class, Warlock.class, Guard.class, Necromancer.class, DwarfKing.class,
			DwarfLich.class, KoboldIcemancer.class, Tinkerer.class, MarketBodyguard.class, ChaosMage.class
	};

	/** whether a character belongs to this family */
	public boolean has( Char ch ){
		if (ch == null) return false;
		switch (this){
			case UNDEAD:
				return Char.hasProp( ch, Char.Property.UNDEAD );
			case DEMONS:
				return Char.hasProp( ch, Char.Property.DEMONIC );
			case CONSTRUCTS:
				return (Char.hasProp( ch, Char.Property.INORGANIC ) && !Char.hasProp( ch, Char.Property.UNDEAD ))
						|| any( BUILT, ch );
			case ELEMENTALS:
				return Char.hasProp( ch, Char.Property.FIERY ) || Char.hasProp( ch, Char.Property.ICY )
						|| Char.hasProp( ch, Char.Property.ELECTRIC );
			case BEASTS:
				return any( BEAST, ch );
			case HUMANOIDS:
				return any( HUMANOID, ch );
			default:
				return false;
		}
	}

	private static boolean any( Class<?>[] classes, Char ch ){
		for (Class<?> c : classes) if (c.isInstance( ch )) return true;
		return false;
	}

	/** the family as a line names it, "the undead", "beasts" */
	public String title(){
		return Messages.get( this, name().toLowerCase() );
	}
}
