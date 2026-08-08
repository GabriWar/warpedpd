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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.CrabSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MirrorSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkeletonSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class SummonedPet extends NPC {

	public enum PET_TYPES {
		RAT("Rat"), CRAB("Crab"), SKELETON("Skeleton"), SPECIAL("Special");

		public String type;
		PET_TYPES(String type){ this.type = type; }

		public String getName(){
			return "Summoned " + type;
		}

		public int getHealth(int level){
			switch (this){
				case RAT:      return 7 + level;
				case CRAB:     return 10 + 2 * level;
				case SKELETON: return 15 + 3 * level;
			}
			return 1;
		}

		public int getDamage(int level){
			switch (this){
				case RAT:      return Random.NormalIntRange(1, 5) + level;
				case CRAB:     return Random.NormalIntRange(2, 7) + level;
				case SKELETON: return Random.NormalIntRange(3, 10) + level;
			}
			return 1;
		}

		public int getDefence(int level){
			switch (this){
				case RAT:      return level;
				case CRAB:     return 2 * level;
				case SKELETON: return 3 * level;
			}
			return 1;
		}

		public Class<? extends CharSprite> getSprite(){
			switch (this){
				case RAT:      return RatSprite.class;
				case CRAB:     return CrabSprite.class;
				case SKELETON: return SkeletonSprite.class;
			}
			return RatSprite.class;
		}
	}

	public static final int DEGRADE_RATE = 15;

	public static int summonedPets = 0;

	public PET_TYPES petType = PET_TYPES.RAT;

	public String name = null;

	public int degradeCounter = 1;

	private int level = 0;

	//SPECIAL pets have no PET_TYPES stat line, so their summoner hands them one
	private int minDamage = -1;
	private int maxDamage = -1;
	private int defence   = -1;

	{
		spriteClass = RatSprite.class;
		alignment = Alignment.ALLY;
		intelligentAlly = true;
		state = WANDERING;
		viewDistance = 4;
		HP = HT = 7;
	}

	public SummonedPet(){
		super();
		summonedPets++;
	}

	public SummonedPet( PET_TYPES type ){
		this();
		petType = type;
		spriteClass = type.getSprite();
		name = type.getName();
	}

	public SummonedPet( Class<? extends CharSprite> sprite ){
		this();
		petType = PET_TYPES.SPECIAL;
		spriteClass = sprite;
	}

	public void spawn( int level ){
		this.level = level;
		HT = petType.getHealth( level );
		HP = HT;
		defenseSkill = 3 + level;
	}

	public void setLevel( int level ){
		this.level = level;
	}

	public void setStats( int minDamage, int maxDamage, int defence ){
		this.minDamage = minDamage;
		this.maxDamage = maxDamage;
		this.defence = defence;
	}

	@Override
	protected boolean act(){
		//summons wither away with time - they are borrowed life
		if (degradeCounter++ % DEGRADE_RATE == 0){
			damage(1, this);
			if (!isAlive()) return true;
		}
		return super.act();
	}

	@Override
	public int attackSkill( Char target ){
		return 10 + level * 3;
	}

	@Override
	public int damageRoll(){
		if (maxDamage >= 0) return Random.NormalIntRange( minDamage, maxDamage );
		return petType.getDamage( level );
	}

	@Override
	public int drRoll(){
		return super.drRoll() + Random.NormalIntRange(0, defence >= 0 ? defence : petType.getDefence( level ));
	}

	@Override
	public String name(){
		return name != null ? name : super.name();
	}

	@Override
	public void die( Object cause ){
		summonedPets = Math.max(0, summonedPets - 1);
		super.die( cause );
	}

	@Override
	public String description(){
		return "A summoned creature bound to its master's will. It slowly withers away as the magic that binds it fades.";
	}

	private static final String PET_SPRITE = "petsprite";
	private static final String PET_TYPE  = "pettype";
	private static final String PET_LEVEL = "petlevel";
	private static final String PET_NAME  = "petname";
	private static final String PET_MIN_DMG = "petmindmg";
	private static final String PET_MAX_DMG = "petmaxdmg";
	private static final String PET_DEFENCE = "petdefence";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( PET_TYPE, petType );
		bundle.put( PET_LEVEL, level );
		if (name != null) bundle.put( PET_NAME, name );
		bundle.put( PET_MIN_DMG, minDamage );
		bundle.put( PET_MAX_DMG, maxDamage );
		bundle.put( PET_DEFENCE, defence );
		//SPECIAL pets pick their own sprite, and Mob does not persist it - without
		//this a shadow clone or guardian spirit comes back as a rat
		if (spriteClass != null) bundle.put( PET_SPRITE, spriteClass.getName() );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		petType = bundle.getEnum( PET_TYPE, PET_TYPES.class );
		level = bundle.getInt( PET_LEVEL );
		if (bundle.contains( PET_NAME )) name = bundle.getString( PET_NAME );
		if (bundle.contains( PET_MAX_DMG )){
			minDamage = bundle.getInt( PET_MIN_DMG );
			maxDamage = bundle.getInt( PET_MAX_DMG );
			defence = bundle.getInt( PET_DEFENCE );
		}
		if (petType != PET_TYPES.SPECIAL){
			spriteClass = petType.getSprite();
		} else if (bundle.contains( PET_SPRITE )){
			Class<?> cls = com.watabou.utils.Reflection.forName( bundle.getString( PET_SPRITE ) );
			if (cls != null) spriteClass = (Class<? extends CharSprite>) cls;
		}
	}
}
