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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Corruption;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.FestivalEvent;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.FestivalChampionSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * A village's champion at its festival (levels/overworld/FestivalEvent): he takes on all comers in
 * a friendly bout, the festival's contest. A word with him tells the terms; a second within
 * ASK_TURNS starts it. He comes out matched to his challenger (fitTo: health, aim, guard and blows
 * of the hero's level), fights him and only him, and nobody falls: the first brought down to a
 * third of his health (YIELD) yields. Beaten, he hands over the festival's prize, once a festival
 * (FestivalEvent.championYields); the winner, he lets the challenger catch his breath and try again.
 * A challenger who walks off (WALK_OFF), or the festival's end, calls it off. Out of a bout no blow,
 * no bolt and no spell touches him, nor will anything turn him; a tap is a word, not a blow.
 */
public class FestivalChampion extends NPC {

	{
		spriteClass = FestivalChampionSprite.class;

		//a bout's sides are not for magic to change
		immunities.add( Corruption.class );
		immunities.add( Charm.class );
		immunities.add( Amok.class );
	}

	/** Turns a second word may come within to start the bout. */
	public static final int ASK_TURNS = 30;
	/** Cells: a challenger further off has walked away from it. */
	public static final int WALK_OFF = 10;
	/** The share of his health either side yields at. */
	public static final float YIELD = 1f / 3f;

	//the festival he stands for (WorldEvents id), the turn it ends, and his village (packed sector)
	public long festival = 0;
	public int endTurn = 0;
	public long village = Long.MIN_VALUE;

	//the bout: the hero he wrestles (actor id, -1 for none) and the health that hero is stopped
	//at, his own match to him (fitTo), and who asked last and when. none of it saved: a bout
	//broken off by a save is no bout
	private int challenger = -1, stopAt = 0, match = 1, askedBy = -1;
	private float askedAt = Float.NEGATIVE_INFINITY;

	public static FestivalChampion of( long festival, int endTurn, long village ){
		FestivalChampion c = new FestivalChampion();
		c.festival = festival;
		c.endTurn = endTurn;
		c.village = village;
		return c;
	}

	public boolean inBout(){
		return challenger != -1;
	}

	//a word between bouts; a blow during one
	@Override
	public boolean heroShouldInteract(){
		return !inBout();
	}

	@Override
	public boolean isInvulnerable( Class effect ){
		return !inBout() || super.isInvulnerable( effect );
	}

	@Override
	public boolean interact( Char c ){
		if (!(c instanceof Hero)) return true;
		Hero h = (Hero) c;
		if (sprite != null) sprite.turnTo( pos, h.pos );
		if (inBout()) return true;
		if (FestivalEvent.prizeTaken( this )){
			say( h, Messages.get( this, "beaten" ) );
		} else if (h.HP <= Math.round( h.HT * YIELD ) + 1){
			say( h, Messages.get( this, "rest" ) );
		} else if (askedBy == h.id() && Actor.now() - askedAt <= ASK_TURNS){
			begin( h );
		} else {
			askedBy = h.id();
			askedAt = Actor.now();
			say( h, Messages.get( this, "terms" ) );
		}
		return true;
	}

	//the bout begins: matched to the challenger, at full strength, and facing him
	private void begin( Hero h ){
		fitTo( h );
		challenger = h.id();
		stopAt = Math.max( 1, Math.round( h.HT * YIELD ) );
		askedBy = -1;
		state = HUNTING;
		enemySeen = true;
		aggro( h );
		target = h.pos;
		if (sprite != null) sprite.showAlert();
		say( h, Messages.get( this, "begin" ) );
		FestivalEvent.boutBegins( this, h );
	}

	/** His match to a hero: health, aim, guard and blows of that hero's level. */
	public void fitTo( Hero h ){
		match = Math.max( 1, h.lvl );
		HT = HP = 20 + 6 * match;
		defenseSkill = 4 + match;
	}

	@Override
	public int attackSkill( Char target ){
		return 10 + match;
	}

	@Override
	public int damageRoll(){
		return Random.NormalIntRange( 1 + match / 3, 4 + 2 * match / 3 );
	}

	@Override
	public int drRoll(){
		return super.drRoll() + Random.NormalIntRange( 0, match / 4 );
	}

	//he wrestles his challenger and nobody else
	@Override
	protected Char chooseEnemy(){
		if (!inBout()) return null;
		Char ch = (Char) Actor.findById( challenger );
		return ch != null && ch.isAlive() ? ch : null;
	}

	@Override
	protected boolean act(){
		if (inBout()){
			Char ch = (Char) Actor.findById( challenger );
			if (ch == null || !ch.isAlive() || Dungeon.level.distance( pos, ch.pos ) > WALK_OFF
					|| !FestivalEvent.stillOn( this )){
				end( ch, null );
			}
		}
		return super.act();
	}

	//his blow never takes the challenger below the third he yields at: there it ends the bout
	@Override
	public int attackProc( Char enemy, int damage ){
		damage = super.attackProc( enemy, damage );
		if (inBout() && enemy.id() == challenger && enemy.HP - damage <= stopAt){
			damage = Math.max( 0, enemy.HP - stopAt );
			end( enemy, false );
		}
		return damage;
	}

	//brought down to a third of his health, he yields
	@Override
	public void damage( int dmg, Object src ){
		if (!inBout() || !isAlive()) return;
		int floor = Math.max( 1, Math.round( HT * YIELD ) );
		if (HP - dmg > floor){
			super.damage( dmg, src );
			return;
		}
		super.damage( Math.max( 0, HP - floor ), src );
		end( (Char) Actor.findById( challenger ), true );
	}

	//the bout is over: the challenger won (he yields), he won, or neither (called off)
	private void end( Char ch, Boolean challengerWon ){
		challenger = -1;
		enemy = null;
		state = PASSIVE;
		HP = HT;
		if (!(ch instanceof Hero)) return;
		Hero h = (Hero) ch;
		if (challengerWon == null){
			say( h, Messages.get( this, "off" ) );
		} else if (challengerWon){
			say( h, Messages.get( this, "yield" ) );
			FestivalEvent.championYields( this, h );
		} else {
			say( h, Messages.get( this, "won" ) );
			FestivalEvent.championWins( this, h );
		}
	}

	//a line of his to a hero: spoken aloud to the host's, written in a co-op guest's log
	private void say( Hero h, String line ){
		if (h == Dungeon.hero || !h.isRemote) yell( line );
		else NetManager.heroLog( h, Messages.titleCase( name() ) + ": \"" + line + "\"" );
	}

	private static final String FESTIVAL = "festival";
	private static final String END_TURN = "end_turn";
	private static final String VILLAGE  = "village";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( FESTIVAL, festival );
		bundle.put( END_TURN, endTurn );
		bundle.put( VILLAGE, village );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		festival = bundle.contains( FESTIVAL ) ? bundle.getLong( FESTIVAL ) : 0;
		//no known end: sent home at the next look
		endTurn = bundle.contains( END_TURN ) ? bundle.getInt( END_TURN ) : 0;
		village = bundle.contains( VILLAGE ) ? bundle.getLong( VILLAGE ) : Long.MIN_VALUE;
		state = PASSIVE;
	}
}
