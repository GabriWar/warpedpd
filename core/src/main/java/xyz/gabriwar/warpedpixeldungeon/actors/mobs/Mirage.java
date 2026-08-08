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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.WraithSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

/**
 * A hallucination conjured by an exhausted or starving mind. It looks and
 * moves exactly like a monster native to the floor, but it is not there:
 * any hit dissolves it, and its own attacks pass through harmlessly,
 * revealing the trick.
 */
public class Mirage extends Mob {

	{
		spriteClass = WraithSprite.class;

		HP = HT = 1;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -1;

		state = HUNTING;
	}

	//the mob this hallucination pretends to be
	private Class<? extends Mob> disguise = null;

	//hallucinations fade on their own after a while
	private int ttl = 60;

	private void disguiseAs( Class<? extends Mob> cls ){
		Mob template = Reflection.newInstance( cls );
		if (template != null){
			disguise = cls;
			spriteClass = template.spriteClass;
		}
	}

	@Override
	public String name() {
		if (disguise != null){
			return Messages.get( disguise, "name" );
		}
		return super.name();
	}

	@Override
	public String description() {
		if (disguise != null){
			return Messages.get( disguise, "desc" );
		}
		return super.description();
	}

	@Override
	protected boolean act() {
		if (--ttl <= 0 || Dungeon.hero == null || !Dungeon.hero.isAlive()){
			dissipate( false );
			return true;
		}
		return super.act();
	}

	@Override
	public int damageRoll() {
		return 0;
	}

	@Override
	public int attackSkill( Char target ) {
		return 100;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		//its strike passes right through - the illusion breaks
		GLog.w( Messages.get( this, "reveal", name() ) );
		dissipate( true );
		return 0;
	}

	@Override
	public void damage( int dmg, Object src ) {
		//any hit dispels it
		GLog.i( Messages.get( this, "dissipate", name() ) );
		dissipate( true );
	}

	private void dissipate( boolean visible ){
		if (!isAlive()) return;
		if (visible && sprite != null && Dungeon.level.heroFOV[pos]){
			CellEmitter.get( pos ).burst( ShadowParticle.UP, 5 );
		}
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	//no loot, no exp, no kill credit: it was never really there
	@Override
	public void rollToDropLoot() {}

	//spawned from the hunger tick when the hero's mind starts to slip.
	//exhaustion or starvation alone: rare. both at once: frequent
	public static void trySpawn( Hero hero ){
		if (Dungeon.level == null || Dungeon.bossLevel()) return;

		Sleepiness sl = hero.buff( Sleepiness.class );
		Hunger hu = hero.buff( Hunger.class );
		boolean tired    = sl != null && sl.isDrowsy();
		boolean starving = hu != null && hu.isStarving();
		if (!tired && !starving) return;

		if (Random.Int( tired && starving ? 20 : 60 ) != 0) return;

		int mirages = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Mirage) mirages++;
		}
		if (mirages >= 2) return;

		//disguised as something native to this floor
		Mob template = Dungeon.level.createMob();
		if (template == null) return;

		Mirage mirage = new Mirage();
		mirage.disguiseAs( template.getClass() );
		mirage.pos = Dungeon.level.randomRespawnCell( mirage );
		if (mirage.pos == -1) return;

		xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.add( mirage );
		mirage.beckon( hero.pos );
	}

	private static final String DISGUISE = "disguise";
	private static final String TTL      = "ttl";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		if (disguise != null) bundle.put( DISGUISE, disguise );
		bundle.put( TTL, ttl );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		if (bundle.contains( DISGUISE )){
			disguiseAs( (Class<? extends Mob>) bundle.getClass( DISGUISE ) );
		}
		ttl = bundle.getInt( TTL );
	}
}
