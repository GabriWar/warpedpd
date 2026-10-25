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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownLedger;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.FishingFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * A line out in the water from the hero's rod (FishingRod): where it went in (a world cell of the
 * slice it was cast on), from where (the hero's world cell then), into what (FishingWaters.Kind),
 * on what bait, and its turns: WAITING, the bobber riding the water until the bite comes
 * (FishingRules.waitTurns, worked out at the cast); a NIBBLE the turn before; then the BITE, which
 * holds FishingRules.BITE_TURNS turns - reel in within them (FishingRod's REEL) and what took the
 * hook (FishingRules.roll, drawn as it bites, from the hour and the weather then) is landed; else it
 * is gone with the bait. The bait went at the cast, whatever comes of it.
 *
 * The hero rests while it waits (FishingRod sets him resting); the bite stops that as anything does
 * (Hero.interrupt), and so does any monster coming into view. Should he step off his cell, go to
 * another slice, part with his rod or the water under it turn to ice or land, the line is pulled in.
 *
 * Its picture (FishingFx: the rod in his hands, the line, the bobber, a hole in the ice) is drawn on
 * the render thread from the volatile state below; how it ended plays out there (End).
 */
public class FishingLine extends Buff {

	/** What the bobber shows. */
	public enum State { WAITING, NIBBLE, BITE }

	/** How the line came in, as its picture plays it out. */
	public enum End { NONE, LANDED, EMPTY, ESCAPED, PULLED }

	//the slice it was cast on (its depth), the hero's world cell then and the bobber's
	private int depth = -1;
	private int heroX, heroY, x, y;
	private FishingWaters.Kind water = FishingWaters.Kind.LAKE;
	private Bait bait = Bait.WORM;
	private boolean ice;
	private int wait;
	//turns the fish on it holds on yet: 0 while none bites
	private int bite;
	private FishingRules.Catch hooked;

	//the picture's (read on the render thread)
	public volatile State state = State.WAITING;
	public volatile End end = End.NONE;
	//cast just now: the bobber flies out (one loaded from a save is simply there)
	public volatile boolean fresh;
	//what was landed, as its picture flies to the hero (an item's image), -1 for none
	public volatile int landed = -1;

	{
		type = buffType.NEUTRAL;
	}

	/** Casts a line from the hero to a window cell of a slice: it waits for its bite from the next turn. */
	public static FishingLine cast( Hero hero, OverworldLevel ow, int cell, FishingWaters.Kind water, Bait bait ){
		FishingLine line = new FishingLine();
		int w = ow.width();
		line.depth = Dungeon.depth;
		line.heroX = ow.worldX() + hero.pos % w;
		line.heroY = ow.worldY() + hero.pos / w;
		line.x = ow.worldX() + cell % w;
		line.y = ow.worldY() + cell / w;
		line.water = water;
		line.bait = bait;
		line.ice = FishingWaters.ice( ow.map[cell] );
		line.wait = FishingRules.waitTurns( FishingRules.keenness( water, bait, phase(), season(), sky() ), Random.Float() );
		line.fresh = true;
		if (line.attachTo( hero )) line.spend( TICK );
		return line;
	}

	private static DayNightCycle.Phase phase(){
		return DayNightCycle.phase();
	}

	private static GameCalendar.Season season(){
		return GameCalendar.season();
	}

	//under the rock there is no weather: the caves' pools ask none of it (FishingRules.weight)
	private static VillageRoutine.Weather sky(){
		return VillageRoutine.weather();
	}

	/** Through a hole in the ice. */
	public boolean ice(){
		return ice;
	}

	/** Where the bobber is from the hero, in cells. */
	public int dx(){
		return x - heroX;
	}

	public int dy(){
		return y - heroY;
	}

	/** Its picture may show on this slice (the one it was cast on; not a house's inside at its depth). */
	public boolean onThisSlice(){
		return Dungeon.depth == depth && Dungeon.level instanceof OverworldLevel;
	}

	/** The bobber's window cell on the slice the line is on, -1 when it is not on the window. */
	public int bobberCell(){
		if (!onThisSlice()) return -1;
		return ((OverworldLevel) Dungeon.level).localCell( x, y );
	}

	//still out: the hero alive on the cell he cast from on the same slice, his rod still with him
	//(dropped, sold or stolen, there is nothing to reel in with), the water still water
	private boolean valid(){
		Hero hero = (Hero) target;
		if (hero == null || !hero.isAlive() || !onThisSlice()) return false;
		if (hero.belongings.getItem( FishingRod.class ) == null) return false;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		int w = ow.width();
		if (ow.worldX() + hero.pos % w != heroX || ow.worldY() + hero.pos / w != heroY) return false;
		int c = ow.localCell( x, y );
		if (c == -1) return false;
		int t = ow.map[c];
		return ice ? FishingWaters.ice( t ) : FishingWaters.openWater( t );
	}

	@Override
	public boolean act(){
		if (!valid()){
			if (target instanceof Hero && ((Hero) target).isAlive()) GLog.i( Messages.get( FishingLine.class, "pulled" ) );
			finish( End.PULLED );
			return true;
		}
		if (bite > 0){
			if (--bite <= 0){
				GLog.w( Messages.get( FishingLine.class, "escaped" ) );
				finish( End.ESCAPED );
				return true;
			}
		} else if (--wait <= 0){
			bite( (Hero) target );
		} else if (wait == 1){
			state = State.NIBBLE;
		}
		spend( TICK );
		return true;
	}

	//it bites: what took the hook is drawn now, from the hour and the weather now; the hero is roused
	private void bite( Hero hero ){
		hooked = FishingRules.roll( water, bait, phase(), season(), sky() );
		if (hooked == null){
			//nothing lives in this water at all: the bait drifts off
			GLog.i( Messages.get( FishingLine.class, "nothing" ) );
			finish( End.EMPTY );
			return;
		}
		bite = FishingRules.BITE_TURNS;
		state = State.BITE;
		GLog.p( Messages.get( FishingLine.class, "bite" ) );
		int c = bobberCell();
		if (c >= 0) SpatialSound.play( Assets.Sounds.WATER, c, 0.7f, FxRandom.Float( 1.2f, 1.4f ) );
		//he was resting, watching the bobber: up he gets
		if (hero.resting) hero.interrupt();
	}

	/**
	 * The rod's REEL (render thread): the hero pulls the line in, a turn; with a fish on, what took the
	 * hook is landed - into the pack, or at his feet when it is full - else the hook comes up empty.
	 */
	public void reel( final Hero hero ){
		final FishingRules.Catch got = bite > 0 ? hooked : null;
		final End how = got != null ? End.LANDED : End.EMPTY;
		final Item item = got != null ? catchOf( got ) : null;
		landed = item != null ? item.image() : got != null ? ItemSpriteSheet.GOLD : -1;
		//the picture reels in at once, as the hero pulls
		end = how;
		int c = bobberCell();
		hero.busy();
		SpatialSound.play( Assets.Sounds.ROPE, hero, 0.5f, FxRandom.Float( 1.3f, 1.5f ) );
		if (got != null && c >= 0) SpatialSound.play( Assets.Sounds.WATER, c, 0.9f, FxRandom.Float( 0.9f, 1.1f ) );
		hero.sprite.operate( c >= 0 ? c : hero.pos, () -> {
			if (got != null){
				land( hero, got, item );
			} else {
				GLog.i( Messages.get( FishingLine.class, "empty" ) );
			}
			finish( how );
			hero.spendAndNext( 1f );
		} );
	}

	//what a catch is as an item: a fish, a boot, a bottle; null for a purse (its gold is spilled)
	private static Item catchOf( FishingRules.Catch c ){
		if (c.fish != null) return new CaughtFish( c.fish );
		switch (c.junk){
			case BOOT:   return new OldBoot();
			case BOTTLE: return new MessageBottle();
			default:     return null;
		}
	}

	//the catch comes to the hero: into his pack, or at his feet; a purse's coins spill at his feet
	private static void land( Hero hero, FishingRules.Catch c, Item item ){
		if (item == null){
			int gold = TownLedger.scaled( Random.IntRange( 15, 40 ) );
			drop( new Gold( gold ), hero );
			//one purse in four held a runestone too
			if (Random.Int( 4 ) == 0){
				Item stone = Generator.random( Generator.Category.STONE );
				if (stone != null) drop( stone, hero );
			}
			GLog.p( Messages.get( FishingLine.class, "purse", gold ) );
			return;
		}
		String key = c.fish != null ? "landed" : c.junk == FishingRules.Junk.BOOT ? "boot" : "bottle";
		GLog.p( Messages.get( FishingLine.class, key, item.name() ) );
		if (!item.collect( hero.belongings.backpack )) drop( item, hero );
	}

	private static void drop( Item item, Hero hero ){
		Heap h = Dungeon.level.drop( item, hero.pos );
		if (h.sprite != null) h.sprite.drop();
	}

	//how it came in: the picture plays it out, and the line is gone
	private void finish( End how ){
		end = how;
		detach();
	}

	@Override
	public void fx( boolean on ){
		if (on) FishingFx.show( this );
		else FishingFx.hide( this );
	}

	private static final String DEPTH = "depth", HERO_X = "hero_x", HERO_Y = "hero_y", X = "x", Y = "y",
			WATER = "water", BAIT = "bait", ICE = "ice", WAIT = "wait", BITE = "bite",
			HOOKED_FISH = "hooked_fish", HOOKED_JUNK = "hooked_junk";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( DEPTH, depth );
		bundle.put( HERO_X, heroX );
		bundle.put( HERO_Y, heroY );
		bundle.put( X, x );
		bundle.put( Y, y );
		bundle.put( WATER, water.name() );
		bundle.put( BAIT, bait.name() );
		bundle.put( ICE, ice );
		bundle.put( WAIT, wait );
		bundle.put( BITE, bite );
		if (hooked != null){
			if (hooked.fish != null) bundle.put( HOOKED_FISH, hooked.fish.name() );
			else bundle.put( HOOKED_JUNK, hooked.junk.name() );
		}
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		depth = bundle.getInt( DEPTH );
		heroX = bundle.getInt( HERO_X );
		heroY = bundle.getInt( HERO_Y );
		x = bundle.getInt( X );
		y = bundle.getInt( Y );
		water = named( FishingWaters.Kind.values(), bundle.getString( WATER ), FishingWaters.Kind.LAKE );
		bait = named( Bait.values(), bundle.getString( BAIT ), Bait.WORM );
		ice = bundle.getBoolean( ICE );
		wait = bundle.getInt( WAIT );
		bite = bundle.getInt( BITE );
		hooked = null;
		if (bundle.contains( HOOKED_FISH )){
			hooked = FishingRules.Catch.of( FishSpecies.named( bundle.getString( HOOKED_FISH ) ) );
		} else if (bundle.contains( HOOKED_JUNK )){
			hooked = FishingRules.Catch.of( named( FishingRules.Junk.values(), bundle.getString( HOOKED_JUNK ), FishingRules.Junk.BOOT ) );
		}
		if (bite > 0 && hooked == null) bite = 0;
		state = bite > 0 ? State.BITE : wait <= 1 ? State.NIBBLE : State.WAITING;
		fresh = false;
	}

	private static <E extends Enum<E>> E named( E[] values, String name, E otherwise ){
		for (E e : values) if (e.name().equals( name )) return e;
		return otherwise;
	}
}
