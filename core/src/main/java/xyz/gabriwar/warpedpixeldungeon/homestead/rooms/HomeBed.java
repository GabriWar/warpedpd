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

package xyz.gabriwar.warpedpixeldungeon.homestead.rooms;

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CampFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Bedroll;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomeBedSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.Locale;

/**
 * A bed the hero built (homestead/pieces/RoomPieces): a bedroll's night (Bedroll - sleep to the next
 * phase or to dawn, the clock moved on, the stomach slowed, the cold, being found) in a better bed.
 *   - A night heals a share of the hero's health by the bed - straw a quarter, wood a third, fur a
 *     half - raised by the room's comfort by up to COMFORT_HEAL more of it, never past MAX_HEAL.
 *   - Nobody finds a sleeper in a sealed room (Bedroll.safe: every door shut, no wall broken).
 *     Elsewhere he may be found as on a bedroll, by the world's own wild, half as likely with a fire
 *     warming the room or a camp fire burning within FIRE_REACH; an alarm bell can rouse him first
 *     (homestead/SleepGuards).
 *   - A fur bed keeps him warm: he feels FUR_WARMTH more, wakes cold only in a room colder than
 *     FUR_COLD, and sleeps off any chill he lay down with.
 *   - Waking from a whole night (to dawn) he is Well Rested (WellRested, longer the more
 *     comfortable the room), and blessed in a shrine (ShrineBlessing). On a real-clock run, whose
 *     sleep takes no time, he cannot lie down again while still well rested.
 * Built, it is the homestead's for good (HomesteadActor); its health is the build piece's, and a
 * fire on its cell burns it down a turn at a time (smoulder), as it does a wooden station.
 */
public class HomeBed extends Bedroll implements HomesteadActor {

	{
		spriteClass = HomeBedSprite.class;
	}

	/** The beds, by what they heal and how they look. */
	public enum Kind {
		STRAW( 1/4f, HomesteadSprites.BED_STRAW ),
		WOOD( 1/3f, HomesteadSprites.BED_WOOD ),
		FUR( 1/2f, HomesteadSprites.BED_FUR );

		/** The share of the sleeper's health a night gives back, comfort aside. */
		public final float heal;
		/** Its frame of sprites/homestead.png. */
		public final int frame;

		Kind( float heal, int frame ){
			this.heal = heal;
			this.frame = frame;
		}
	}

	/** The most of the sleeper's health any night gives back. */
	public static final float MAX_HEAL = 0.5f;
	/** The most the room's comfort adds to a bed's heal, as a share of it (at Comfort.MAX). */
	public static final float COMFORT_HEAL = 0.5f;
	/** How much warmer the sleeper feels under furs (C), and the room's air a fur bed still wakes
	 *  him cold in. */
	public static final float FUR_WARMTH = 6f, FUR_COLD = -10f;
	/** How near a camp fire must burn to keep the night's visitors off as well as a hearth. */
	public static final int FIRE_REACH = 2;

	public Kind kind = Kind.STRAW;

	public static HomeBed of( Kind kind ){
		HomeBed bed = new HomeBed();
		bed.kind = kind;
		return bed;
	}

	/** The share of the sleeper's health a night in a bed of this kind gives back in a room of this
	 *  comfort (0 out of any room). */
	public static float healFraction( Kind kind, int comfort ){
		return Math.min( MAX_HEAL, kind.heal * (1f + COMFORT_HEAL * Comfort.fraction( comfort )) );
	}

	//the room it stands in now, or null
	private Rooms.Room room(){
		return Rooms.at( Dungeon.level, pos );
	}

	/** The comfort of the room it stands in, 0 out of any room. */
	public int comfort(){
		Rooms.Room r = room();
		return r == null ? 0 : Comfort.score( (OverworldLevel) Dungeon.level, r );
	}

	@Override
	protected float healFraction(){
		return healFraction( kind, comfort() );
	}

	//a fire keeping the night off: one warming its room (a lit hearth), a camp fire burning near, or
	//a flame beside it as by a bedroll
	@Override
	protected boolean fireBy(){
		Rooms.Room r = room();
		if (r != null && r.heated()) return true;
		CampFire camp = (CampFire) Dungeon.level.blobs.get( CampFire.class );
		if (camp != null && camp.volume > 0){
			int w = Dungeon.level.width(), x = pos % w, y = pos / w;
			for (int dy = -FIRE_REACH; dy <= FIRE_REACH; dy++){
				for (int dx = -FIRE_REACH; dx <= FIRE_REACH; dx++){
					int nx = x + dx, ny = y + dy;
					if (nx < 0 || ny < 0 || nx >= w || ny >= Dungeon.level.height()) continue;
					if (camp.burning( nx + ny * w )) return true;
				}
			}
		}
		return super.fireBy();
	}

	@Override
	protected boolean wakesCold( Hero hero ){
		if (kind != Kind.FUR) return super.wakesCold( hero );
		return TileTemperature.tileTemp( pos ) < FUR_COLD
				&& TileTemperature.feelsLikeAt( hero.pos, hero ) + FUR_WARMTH < Hypothermia.WARN_TEMP;
	}

	@Override
	protected String restMenu(){
		StringBuilder s = new StringBuilder( Messages.get( HomeBed.class, "menu_heal",
				Math.round( healFraction() * 100 ) ) );
		s.append( ' ' );
		if (safe()) s.append( Messages.get( HomeBed.class, "menu_sealed" ) );
		else s.append( Messages.get( HomeBed.class, fireBy() ? "menu_open_fire" : "menu_open" ) );
		if (kind == Kind.FUR) s.append( ' ' ).append( Messages.get( HomeBed.class, "menu_fur" ) );
		return s.toString();
	}

	//a real-clock run's sleep takes no time: one rested night, then the bed waits until it wears off
	@Override
	public boolean interact( Char c ){
		if (c == Dungeon.hero && Dungeon.isChallenged( Challenges.REAL_CLOCK ) && c.buff( WellRested.class ) != null){
			GLog.w( Messages.get( HomeBed.class, "not_tired" ) );
			return true;
		}
		return super.interact( c );
	}

	@Override
	protected boolean act(){
		smoulder( Dungeon.level );
		//the flames may have taken it
		if (!Dungeon.level.mobs.contains( this )) return true;
		return super.act();
	}

	/** A turn in the flames: a bed on a burning cell loses Homestead.BURN_DAMAGE of its health (the
	 *  homestead's, as a station's) and at none left is gone. Host only. True when it burned. */
	public boolean smoulder( Level level ){
		if (!Homestead.allowedHere( level )) return false;
		Blob fire = level.blobs.get( Fire.class );
		if (fire == null || fire.volume <= 0 || fire.cur == null || fire.cur[pos] <= 0) return false;
		BuildPiece p = Homestead.pieceAt( level, pos );
		return p != null && p.flammable && Homestead.damage( level, pos, Homestead.BURN_DAMAGE, fire );
	}

	@Override
	protected void woke( Hero hero, boolean found, boolean toDawn ){
		//under furs in a room that is not bitter, any chill is slept off
		if (kind == Kind.FUR && !wakesCold( hero )) Buff.detach( hero, Hypothermia.class );
		if (found) return;
		//only a whole night rests him: a nap to the next part of the day only heals (a real-clock
		//run's clock never moves, so any sleep there is the night)
		if (!toDawn && !Dungeon.isChallenged( Challenges.REAL_CLOCK )) return;
		int comfort = comfort();
		WellRested.rest( hero, WellRested.duration( comfort ) );
		Rooms.Room r = room();
		if (r != null && RoomTypes.has( (OverworldLevel) Dungeon.level, r, RoomTypes.SHRINE )){
			Buff.prolong( hero, ShrineBlessing.class, ShrineBlessing.DURATION );
			GLog.p( Messages.get( HomeBed.class, "blessed" ) );
		}
	}

	@Override
	public String name(){
		return Messages.get( HomeBed.class, "name_" + kind.name().toLowerCase( Locale.ENGLISH ) );
	}

	@Override
	public String description(){
		return Messages.get( HomeBed.class, "desc_" + kind.name().toLowerCase( Locale.ENGLISH ) )
				+ "\n\n" + Messages.get( HomeBed.class, "desc_heal", Math.round( healFraction() * 100 ),
						Comfort.levelOf( comfort() ).title() );
	}

	private static final String KIND = "kind";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( KIND, kind.name() );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		kind = bundle.contains( KIND ) ? bundle.getEnum( KIND, Kind.class ) : Kind.STRAW;
	}
}
