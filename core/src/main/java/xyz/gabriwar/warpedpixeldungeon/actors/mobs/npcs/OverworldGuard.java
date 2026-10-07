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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Raider;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuardVariantSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/** A watchful settlement guard: all bark - until bandits raid the settlement (RaidEvent). */
public class OverworldGuard extends NPC {

	{
		spriteClass = GuardVariantSprite.class;
		properties.add( Property.IMMOVABLE );
	}

	public int tint = 0;
	public long homeSector = Long.MIN_VALUE;
	//which of the well's four posts is his by day (VillageRoutine.GUARD_POSTS); by night the
	//same number picks the gate he keeps. -1 in older saves: worked out from where he stands
	public int post = -1;

	//none of this is bundled: worked out again after a load
	private VillageRoutine.Settlement place;
	private boolean noWatch = false;   //his sector is no settlement (a guard spawned by hand)
	private boolean lit = false;       //his torch is burning (CharSprite.State.ILLUMINATED)
	//the cell of this watch, the window and the hour it was found for, and turns until it is
	//looked for again when none could be had
	private int watchCell = -1, watchVersion = -1, watchRetry = 0;
	private boolean watchNight = false;

	public static OverworldGuard random( long homeSector ){
		OverworldGuard g = new OverworldGuard();
		g.tint = Random.Int( 6 );
		g.homeSector = homeSector;
		return g;
	}

	@Override
	public CharSprite sprite() {
		return new GuardVariantSprite( tint );
	}

	@Override
	protected boolean act() {
		OverworldLevel ow = Dungeon.level instanceof OverworldLevel ? (OverworldLevel) Dungeon.level : null;
		if (sprite != null && ow != null && ow.altitude() == 0) keepWatch( ow );
		//a raid on this settlement: the guard strikes a raider beside his post. his blows wear
		//them down but never finish one (Raider.damage) - that is the hero's to do
		if (sprite != null && RaidEvent.ongoing( homeSector )){
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( pos + n );
				if (ch instanceof Raider && ch.isAlive() && ch.HP > 1 && ch.alignment == Alignment.ENEMY){
					enemy = ch;
					return doAttack( ch );
				}
			}
		}
		return super.act();
	}

	@Override
	public int attackSkill( Char target ) {
		return 20;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 3, 7 );
	}

	//finds his settlement once, and his post when an older save did not carry it (from
	//where he stands, world wx, wy)
	private boolean watching( OverworldLevel ow, int wx, int wy ){
		if (place == null){
			if (noWatch) return false;
			VillageRoutine.Settlement s = VillageRoutine.settlement( ow.worldSeed(), homeSector );
			if (s == null || s.gnoll){
				noWatch = true;
				return false;
			}
			place = s;
		}
		if (post < 0) post = VillageRoutine.nearestPost( place, wx, wy );
		return true;
	}

	//the watch: by day at his post by the well, by night at a gate with a torch. he never
	//walks it - a guard is a fixture - so he changes post only where no hero is near enough
	//to see him go. never spends time: act goes on to the ordinary turn
	private void keepWatch( OverworldLevel ow ){
		if (!watching( ow, ow.worldX() + pos % ow.width(), ow.worldY() + pos / ow.width() )) return;
		boolean night = DayNightCycle.isNight();
		if (watchVersion != ow.windowVersion() || night != watchNight){
			watchCell = -1;
			watchRetry = 0;
			watchVersion = ow.windowVersion();
			watchNight = night;
		}
		if (watchCell != -1){
			Char o = Actor.findChar( watchCell );
			if (o != null && o != this) watchCell = -1;   //somebody stands on it: another cell
		}
		if (watchCell == -1){
			if (watchRetry > 0){
				watchRetry--;
			} else {
				watchCell = night ? VillageRoutine.gatePost( ow, place, post, this, false )
						: VillageRoutine.dayPost( ow, place, post, this, false );
				if (watchCell == -1) watchRetry = 12;
			}
		}
		if (watchCell != -1 && watchCell != pos && Actor.findChar( watchCell ) == null
				&& OverworldLevel.heroDistance( ow, pos ) > VillageRoutine.NEAR
				&& OverworldLevel.heroDistance( ow, watchCell ) > VillageRoutine.NEAR){
			move( watchCell, false );
			sprite.place( watchCell );
		}
		boolean torch = night && pos == watchCell;
		if (torch != lit){
			lit = torch;
			if (lit) sprite.add( CharSprite.State.ILLUMINATED );
			else sprite.remove( CharSprite.State.ILLUMINATED );
		}
	}

	/** Comes into the window - spawned, unparked, loaded - having stood at world (wx, wy): set
	 *  straight at his post for the hour, his torch lit by night, before any sprite or turn.
	 *  The window cell, or -1 when no post can be had. See VillageRoutine.arrivalCell. */
	public int arrive( OverworldLevel ow, int wx, int wy ){
		if (ow.altitude() != 0 || !watching( ow, wx, wy )) return -1;
		boolean night = DayNightCycle.isNight();
		//off the board while he looks: a cell from an older window is nobody's post
		int was = pos;
		pos = -1;
		int cell = night ? VillageRoutine.gatePost( ow, place, post, this, true )
				: VillageRoutine.dayPost( ow, place, post, this, true );
		if (cell == -1){
			pos = was;
			return -1;
		}
		watchCell = cell;
		watchVersion = ow.windowVersion();
		watchNight = night;
		watchRetry = 0;
		lit = night;
		return cell;
	}

	@Override
	public void updateSpriteState() {
		super.updateSpriteState();
		//a sprite made afresh (an unpark, a rebuilt scene) takes his torch back up
		if (lit && sprite != null) sprite.add( CharSprite.State.ILLUMINATED );
	}

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}

	@Override
	protected Char chooseEnemy() {
		return null;
	}

	@Override
	public void damage( int dmg, Object src ) {
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	public boolean interact( Char c ) {
		sprite.turnTo( pos, c.pos );
		if (c != Dungeon.hero) return true;
		//a call to arms in a raid, and the village's gratitude for days after the hero beat it off
		if (RaidEvent.ongoing( homeSector )) yell( Messages.get( this, "raid" ) );
		else if (RaidEvent.favoured( homeSector )) yell( Messages.get( this, "grateful" ) );
		else yell( Messages.get( this, "line_" + Random.Int( 3 ) ) );
		return true;
	}

	private static final String TINT = "tint";
	private static final String HOME = "home_sector";
	private static final String POST = "routine_post";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TINT, tint );
		bundle.put( HOME, homeSector );
		bundle.put( POST, post );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		tint = bundle.getInt( TINT );
		homeSector = bundle.getLong( HOME );
		post = bundle.contains( POST ) ? bundle.getInt( POST ) : -1;
	}
}
