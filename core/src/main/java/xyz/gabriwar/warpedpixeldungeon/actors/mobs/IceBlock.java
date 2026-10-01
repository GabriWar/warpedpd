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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFireblast;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.IceBlockSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

/**
 * A block of ice with something frozen inside it, the Frozen Cache's lock. There are two
 * ways in, and the block does not care which:
 *
 *   thaw it    every turn its tile is above THAW_TEMP it melts by the degrees above. It
 *              chills its own tile hard (CHILL a turn), which with the ice under it keeps it
 *              some twenty degrees under the air: nothing short of a hot summer's day melts
 *              it, that does by itself in a hundred-odd turns, fire beside it does in a couple
 *              of dozen - and what is inside comes out whole.
 *   break it   it has hit points. The noise brings the floor running, and a potion inside
 *              shatters with the ice, right there, for whatever that potion does. A scroll
 *              comes out as pulp. Anything sturdier survives.
 *
 * Fire thrown at it is heat, not a hammer: a burn or a fireblast is added to the melt.
 * The tile under a thawed block is left as the puddle it made.
 */
public class IceBlock extends Mob {

	{
		spriteClass = IceBlockSprite.class;

		HP = HT = 20 + WarpedRooms.threat() * 4;
		defenseSkill = 0;
		EXP = 0;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );

		immunities.add( Burning.class );
		immunities.add( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost.class );
		immunities.add( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill.class );
		immunities.add( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis.class );
		immunities.add( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia.class );

		thermal = Thermal.INSENSATE;
	}

	public static final float THAW_TEMP   = 5f;
	public static final float MELT_NEEDED = 300f;
	//laid on its own tile every turn. An open tile sheds most of what is laid on it to the
	//tiles around it (TileTemperature.stepDiffusion), and what is left settles at about two
	//thirds of this: thirteen degrees under the floor's air, and six on the tiles beside it
	private static final float CHILL      = 20f;

	private Item prize;
	private float melt = 0f;

	public void encase( Item item ){
		prize = item;
	}

	/** a block found already going: the share of the thaw that is done, 0..1 */
	public void preMelt( float share ){
		melt = MELT_NEEDED * Math.max( 0f, Math.min( 0.9f, share ) );
	}

	/** which silhouette shows through the ice: 0 long, 1 round, 2 small */
	public int shape(){
		if (prize instanceof Weapon || prize instanceof Wand) return 0;
		if (prize instanceof Potion || prize instanceof Ring
				|| prize instanceof xyz.gabriwar.warpedpixeldungeon.items.artifacts.Artifact
				|| prize instanceof xyz.gabriwar.warpedpixeldungeon.items.armor.Armor) return 1;
		return 2;
	}

	/** how far gone it looks: 0 solid, 1 sweating, 2 slumped */
	public int stage(){
		float share = melt / MELT_NEEDED;
		return share >= 0.66f ? 2 : share >= 0.33f ? 1 : 0;
	}

	@Override
	protected boolean act(){
		if (meltTurn()){
			thaw();
			return true;
		}
		return super.act();
	}

	/** one turn of the tile's temperature on the block; true once it has melted through */
	public boolean meltTurn(){
		int before = stage();
		float temp = TileTemperature.tileTemp( pos );
		//the block is its own cold store
		TileTemperature.depositHeat( pos, -CHILL );

		if (temp > THAW_TEMP){
			melt += temp - THAW_TEMP;
			if (Dungeon.level.heroFOV[pos]) CellEmitter.get( pos ).burst( Speck.factory( Speck.STEAM ), 1 );
		} else if (temp < 0f && melt > 0f){
			melt = Math.max( 0f, melt - 2f );
		}

		if (stage() != before && sprite instanceof IceBlockSprite){
			((IceBlockSprite) sprite).show( shape(), stage() );
		}
		return melt >= MELT_NEEDED;
	}

	public float melt(){
		return melt;
	}

	@Override
	public void damage( int dmg, Object src ){
		if (src instanceof Blob) return;
		if (src instanceof Burning || src instanceof WandOfFireblast){
			melt += dmg * 10f;
			return;
		}
		super.damage( dmg, src );
	}

	@Override
	public boolean add( Buff buff ){
		//nothing sticks to a lump of ice but the heat that ends it
		return false;
	}

	//tapping it swings at it: it is an obstacle, not a conversation
	@Override
	public boolean heroShouldInteract(){
		return false;
	}

	private void thaw(){
		int cell = pos;
		destroy();
		if (sprite != null) sprite.killAndErase();

		if (Dungeon.level.map[cell] == Terrain.FROZEN_WATER){
			Level.set( cell, Terrain.WATER );
			GameScene.updateMap( cell );
		}
		Splash.at( cell, 0xFF9FD4E8, 12 );
		Sample.INSTANCE.play( Assets.Sounds.WATER, 1f, 0.8f );
		if (prize != null){
			Dungeon.level.drop( prize, cell ).sprite.drop();
			if (Dungeon.level.heroFOV[cell]) GLog.p( Messages.get( this, "thawed", prize.name() ) );
			prize = null;
		}
	}

	@Override
	public void die( Object cause ){
		int cell = pos;
		Item inside = prize;
		prize = null;
		super.die( cause );

		Sample.INSTANCE.play( Assets.Sounds.SHATTER );
		//an ice block going to pieces carries: everything on the floor heard that
		for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (mob != this) mob.beckon( cell );
		}

		if (inside == null) return;
		if (inside instanceof Potion){
			GLog.w( Messages.get( this, "broke_potion", inside.name() ) );
			((Potion) inside).shatter( cell );
		} else if (inside instanceof Scroll){
			GLog.w( Messages.get( this, "broke_scroll" ) );
		} else {
			Dungeon.level.drop( inside, cell ).sprite.drop();
			GLog.i( Messages.get( this, "broke_out", inside.name() ) );
		}
	}

	@Override
	public float spawningWeight(){
		return 0f;
	}

	@Override
	public boolean reset(){
		return true;
	}

	@Override
	public String description(){
		return Messages.get( this, "desc" )
				+ "\n\n" + Messages.get( this, "inside_" + shape() )
				+ "\n\n" + Messages.get( this, "stage_" + stage() );
	}

	private static final String PRIZE = "prize";
	private static final String MELT  = "melt";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		if (prize != null) bundle.put( PRIZE, prize );
		bundle.put( MELT, melt );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		prize = bundle.contains( PRIZE ) ? (Item) bundle.get( PRIZE ) : null;
		melt = bundle.getFloat( MELT );
	}
}
