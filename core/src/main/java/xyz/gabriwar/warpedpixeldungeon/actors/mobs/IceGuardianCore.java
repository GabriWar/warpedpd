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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IceKey;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.IceGuardianCoreSprite;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.ui.BossHealthBar;
import com.watabou.utils.Random;

//ported from Remixed PD's Ice Caves. The other half of the guardian pair: enormous
//health, but it takes 150 damage every time one of its guardians is destroyed, and
//it drags them all down with it when it finally falls.
public class IceGuardianCore extends Mob {

	{
		spriteClass = IceGuardianCoreSprite.class;

		HP = HT = 1000;
		defenseSkill = 10;

		baseSpeed = 0.5f;

		EXP = 25;
		maxLvl = 30;

		//a set-piece inside the branch, not the branch's boss - that is the demon lord
		properties.add( Property.MINIBOSS );
		properties.add( Property.ICY );
		properties.add( Property.INORGANIC );
		properties.add( Property.IMMOVABLE );

		immunities.add( Paralysis.class );
		immunities.add( ToxicGas.class );
		immunities.add( Terror.class );
		immunities.add( Amok.class );
		immunities.add( Blindness.class );
		immunities.add( MagicalSleep.class );
		immunities.add( Grim.class );


		//no metabolism to disturb: only the extremes reach it
		thermal = Thermal.INSENSATE;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 13, 23 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 26;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 11 );
	}

	public static final int MAX_GUARDIANS = 4;
	private static final int FORM_EVERY = 10;

	//turns until the core shapes another guardian while it is fighting
	private int formIn = FORM_EVERY;

	public static int guardians() {
		int n = 0;
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof IceGuardian && mob.isAlive()) n++;
		}
		return n;
	}

	@Override
	protected boolean act() {
		if (state == HUNTING && paralysed <= 0 && --formIn <= 0) {
			formIn = FORM_EVERY;
			if (guardians() < MAX_GUARDIANS) formGuardian();
		}
		return super.act();
	}

	private void formGuardian() {
		for (int n : PathFinder.NEIGHBOURS8) {
			int cell = pos + n;
			if (!Dungeon.level.passable[cell] || Actor.findChar( cell ) != null) continue;
			IceGuardian guardian = new IceGuardian();
			guardian.pos = cell;
			guardian.state = guardian.HUNTING;
			GameScene.add( guardian );
			CellEmitter.get( cell ).burst( Speck.factory( Speck.LIGHT ), 6 );
			Sample.INSTANCE.play( Assets.Sounds.SHATTER, 0.6f, 1.3f );
			if (Dungeon.level.heroFOV[pos]) GLog.w( Messages.get( this, "form" ) );
			return;
		}
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss( this );
		}
	}

	private static final String FORM_IN = "form_in";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( FORM_IN, formIn );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		if (bundle.contains( FORM_IN )) formIn = bundle.getInt( FORM_IN );
		//older saves left the core passive, where it never fought back
		if (state == PASSIVE) state = WANDERING;
		if (state == HUNTING) BossHealthBar.assignBoss( this );
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (isAlive() && !BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss( this );
		}
		super.damage( dmg, src );
	}

	@Override
	public void die( Object cause ) {
		//without the core the ice holding the guardians together lets go
		for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )) {
			if (mob instanceof IceGuardian && mob.isAlive()) {
				mob.die( cause );
			}
		}

		Dungeon.level.drop( new IceKey( Dungeon.depth ), pos ).sprite.drop();
		Dungeon.level.drop( new WandOfFrost().upgrade(), pos ).sprite.drop();

		super.die( cause );

		yell( Messages.get( this, "die" ) );
	}
}
