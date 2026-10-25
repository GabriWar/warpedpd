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

package xyz.gabriwar.warpedpixeldungeon.homestead.mounts;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Horse;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

/**
 * The hero on horseback (Mounts): the horse he rides, taken off the level and carried here whole
 * (its health, coat and saddle; saved with him), drawn under him by his sprite (CharSprite's
 * RIDING state, effects/fx/combat/RiddenHorse). It heals a point every Horse.REGEN turns, and a
 * rider found indoors or off the riding slices (a teleport, a push) is set down at once, his horse
 * beside him. Its icon shows the horse's health.
 *
 * It also keeps the world cell he rode in from, for a horse left at a threshold (Mounts.leavingLevel).
 */
public class Riding extends Buff {

	{
		type = buffType.POSITIVE;
		announced = false;
	}

	private Horse horse;
	private float healing;
	//its health fell to a third: the hero was told
	private boolean warned;
	//the world cell of his last step on horseback (Integer.MIN_VALUE: none)
	private int fromX = Integer.MIN_VALUE, fromY = Integer.MIN_VALUE;

	/** The horse he rides; null once it is set down. */
	public Horse horse(){
		return horse;
	}

	/** The horse goes under him. */
	void carry( Horse h ){
		horse = h;
		healing = 0;
		warned = h.HP * 3 <= h.HT;
		fromX = fromY = Integer.MIN_VALUE;
		if (target != null && target.sprite != null) fx( true );
	}

	/** The horse comes out from under him (it is the caller's to put back on a level), and this goes. */
	Horse takeHorse(){
		Horse h = horse;
		horse = null;
		detach();
		return h;
	}

	/** He stepped off a cell on horseback. */
	void cameFrom( Level level, int cell ){
		if (!(level instanceof OverworldLevel) || cell < 0 || cell >= level.length()) return;
		OverworldLevel ow = (OverworldLevel) level;
		fromX = ow.worldX + cell % ow.width();
		fromY = ow.worldY + cell / ow.width();
	}

	/** The cell of the level he last stepped off on horseback, -1 for none it holds. */
	int cameFrom( Level level ){
		if (fromX == Integer.MIN_VALUE || !(level instanceof OverworldLevel)) return -1;
		return ((OverworldLevel) level).localCell( fromX, fromY );
	}

	/** A share of a blow reaches the horse (Mounts.shareBlow): it falls when its health runs out
	 *  (Mounts.horseFalls), and he is told once when it is badly hurt. */
	void hurt( int dmg, Char by ){
		if (horse == null) return;
		horse.HP -= dmg;
		if (horse.HP <= 0){
			horse.HP = 0;
			Mounts.horseFalls( (Hero) target, by );
		} else if (!warned && horse.HP * 3 <= horse.HT){
			warned = true;
			if (target == Dungeon.hero) GLog.w( Messages.get( this, "hurt", horse.name() ) );
		}
	}

	@Override
	public boolean act(){
		if (horse == null){
			detach();
			return true;
		}
		if (!target.isAlive()){
			spend( TICK );
			return true;
		}
		healing += TICK;
		while (healing >= Horse.REGEN){
			healing -= Horse.REGEN;
			if (horse.HP < horse.HT) horse.HP++;
		}
		if (horse.HP * 3 > horse.HT) warned = false;
		spend( TICK );
		//set down indoors or off the riding slices (a teleport, a push): the horse stands beside him
		if (target instanceof Hero && !Mounts.rideable( Dungeon.level, target.pos )) Mounts.setDownHere( (Hero) target );
		return true;
	}

	@Override
	public int icon(){
		return BuffIndicator.RIDING;
	}

	@Override
	public float iconFadePercent(){
		return horse == null ? 0 : Math.max( 0f, 1f - horse.HP / (float) horse.HT );
	}

	@Override
	public String iconTextDisplay(){
		return horse == null ? "" : Integer.toString( horse.HP );
	}

	@Override
	public String desc(){
		if (horse == null) return Messages.get( this, "desc_none" );
		return Messages.get( this, "desc", horse.name(), horse.HP, horse.HT );
	}

	@Override
	public void fx( boolean on ){
		CharSprite s = target == null ? null : target.sprite;
		if (s == null) return;
		if (on && horse != null){
			s.rideCoat = horse.coat;
			s.add( CharSprite.State.RIDING );
		} else {
			s.remove( CharSprite.State.RIDING );
		}
	}

	//taken off him with the horse still under him (a revival sweeps his buffs away): it stands beside him
	@Override
	public void detach(){
		Horse left = horse;
		horse = null;
		super.detach();
		if (left != null && left.isAlive() && target instanceof Hero) Mounts.standBeside( (Hero) target, left );
	}

	private static final String HORSE = "horse", HEALING = "healing", WARNED = "warned", FROM_X = "from_x", FROM_Y = "from_y";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		if (horse != null) bundle.put( HORSE, horse );
		bundle.put( HEALING, healing );
		bundle.put( WARNED, warned );
		bundle.put( FROM_X, fromX );
		bundle.put( FROM_Y, fromY );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		horse = bundle.contains( HORSE ) ? (Horse) bundle.get( HORSE ) : null;
		healing = bundle.getFloat( HEALING );
		warned = bundle.getBoolean( WARNED );
		fromX = bundle.contains( FROM_X ) ? bundle.getInt( FROM_X ) : Integer.MIN_VALUE;
		fromY = bundle.contains( FROM_Y ) ? bundle.getInt( FROM_Y ) : Integer.MIN_VALUE;
	}
}
