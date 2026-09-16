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

	//how long any summon lasts before it fades: one in-game day
	public static final int LIFETIME = xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.FULL_CYCLE;

	private int lifeLeft = LIFETIME;

	//summons always grow with their master: max health is a share of the hero's own, damage
	//follows the hero's damage, and accuracy and evasion keep pace with the hero's own growth
	private float healthShare = 0f;
	//damage as a share of the hero's average hit
	private float damageShare = 0f;

	public void setDamageShare( float share ){
		damageShare = share;
	}

	private static int heroBonus(){
		return Dungeon.hero != null ? Math.max( 0, Dungeon.hero.lvl - 1 ) : 0;
	}

	/** max health as a fraction of the hero's max health, kept in step for as long as it lives */
	public void setHealthShare( float share ){
		healthShare = share;
		if (Dungeon.hero != null){
			HT = HP = Math.max( 1, Math.round( share * Dungeon.hero.HT ) );
		}
	}

	private static float pick( float[] shares, int level ){
		return shares[Math.max( 0, Math.min( shares.length - 1, level - 1 ) )];
	}

	//max health follows the hero's; a rise also heals the new health in
	private void scaleWithHero(){
		if (healthShare <= 0 || Dungeon.hero == null) return;
		int target = Math.max( 1, Math.round( healthShare * Dungeon.hero.HT ) );
		if (target != HT){
			int delta = target - HT;
			HT = target;
			HP = Math.min( HT, HP + Math.max( 0, delta ) );
		}
	}

	public int lifeLeft(){
		return lifeLeft;
	}

	/** Compatibility mirror only. Limits always recount the live floor instead of trusting it. */
    @Deprecated public static int summonedPets = 0;

    public static int activeCount(){
        int count=0;
        if(Dungeon.level!=null&&Dungeon.level.mobs!=null)for(Mob mob:Dungeon.level.mobs)
            if(mob instanceof SummonedPet&&mob.isAlive()&&mob.alignment==Alignment.ALLY)count++;
        summonedPets=count;
        return count;
    }

	public PET_TYPES petType = PET_TYPES.RAT;

	public String name = null;


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
		defenseSkill = 3 + level;
		switch (petType){
			case RAT:      setHealthShare( pick( new float[]{ 0.08f, 0.10f, 0.12f }, level ) ); setDamageShare( pick( new float[]{ 0.25f, 0.30f, 0.35f }, level ) ); break;
			case CRAB:     setHealthShare( pick( new float[]{ 0.20f, 0.30f, 0.40f }, level ) ); setDamageShare( pick( new float[]{ 0.35f, 0.40f, 0.45f }, level ) ); break;
			case SKELETON: setHealthShare( pick( new float[]{ 0.50f, 0.60f, 0.70f }, level ) ); setDamageShare( pick( new float[]{ 0.50f, 0.60f, 0.70f }, level ) ); break;
			default:       HT = HP = petType.getHealth( level ); break;
		}
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
        if(guardianEye())viewDistance=6;
		scaleWithHero();
		//borrowed life: a summon lasts one in-game day at full strength, then fades
		if (--lifeLeft <= 0){
			die( this );
			return true;
		}
		return super.act();
	}

    @Override
    public void damage( int dmg, Object src ){
        if(guardianEye()&&dmg>0&&isAlive()&&Dungeon.hero!=null){
            xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.Mark guard=
                    xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.get(this,
                    xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.Mark.GUARD);
            if(guard!=null)guard.power=Math.min(xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.ofHealth(Dungeon.hero.HT,0.3f),guard.power+dmg);
        }
        super.damage(dmg,src);
    }
    private boolean guardianEye(){
        return spriteClass==xyz.gabriwar.warpedpixeldungeon.sprites.SeraphGuardianSprite.class;
    }
    @Override protected boolean canAttack(Char enemy){
        if(!guardianEye())return super.canAttack(enemy);
        return enemy!=null&&enemy.isAlive()&&Dungeon.level.distance(pos,enemy.pos)<=6
                &&new xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica(pos,enemy.pos,
                xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica.MAGIC_BOLT).collisionPos==enemy.pos;
    }
    public static class GuardianLaser {}
    @Override protected boolean doAttack(Char enemy){
        if(!guardianEye())return super.doAttack(enemy);
        spend(attackDelay());
        if(hit(this,enemy,true))enemy.damage(damageRoll(),GuardianLaser.class);
        final int cell=enemy.pos;
        if(sprite instanceof xyz.gabriwar.warpedpixeldungeon.sprites.SeraphGuardianSprite){
            final xyz.gabriwar.warpedpixeldungeon.sprites.SeraphGuardianSprite eye=
                    (xyz.gabriwar.warpedpixeldungeon.sprites.SeraphGuardianSprite)sprite;
            com.watabou.noosa.Game.runOnRenderThread(()->{if(eye.exists)eye.laser(cell);});
        }
        return true;
    }

	@Override
	public int attackSkill( Char target ){
		return 10 + level * 3 + heroBonus();
	}

	@Override
	public int defenseSkill( Char enemy ){
		return super.defenseSkill( enemy ) + heroBonus() / 2;
	}

	@Override
	public int damageRoll(){
		if (damageShare > 0){
			float hit = xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.heroAverageHit() * damageShare;
			return Math.max( 1, Random.NormalIntRange( Math.round( hit * 0.7f ), Math.round( hit * 1.3f ) ) );
		}
		if (maxDamage >= 0) return Random.NormalIntRange( minDamage, maxDamage );
		return petType.getDamage( level );
	}

	@Override
	public int drRoll(){
		int block;
		if (defence >= 0){
			block = Math.round( defence * xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.heroPower() );
		} else {
			float perLevel = petType == PET_TYPES.RAT ? 0.02f : 0.05f;
			block = Math.round( HT * perLevel * Math.max( 1, Math.min( 3, level ) ) );
		}
		return super.drRoll() + Random.NormalIntRange( 0, block );
	}

	@Override
	public String name(){
		return name != null ? name : super.name();
	}

	@Override
	public void die( Object cause ){
		super.die( cause );
	}

	@Override
	public String description(){
		return "A summoned creature bound to its master's will. Its strength is drawn from its master's, and it holds together for " + lifeLeft + " more turns before the magic that binds it fades.";
	}

	private static final String PET_LIFE   = "petlife";
	private static final String PET_SHARE  = "petshare";
	private static final String PET_DMG_SHARE = "petdmgshare";
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
		bundle.put( PET_LIFE, lifeLeft );
		bundle.put( PET_SHARE, healthShare );
		bundle.put( PET_DMG_SHARE, damageShare );
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
		//older saves carried no timer: they get a fresh day
		lifeLeft = bundle.contains( PET_LIFE ) ? bundle.getInt( PET_LIFE ) : LIFETIME;
		healthShare = bundle.contains( PET_SHARE ) ? bundle.getFloat( PET_SHARE ) : 0f;
		damageShare = bundle.contains( PET_DMG_SHARE ) ? bundle.getFloat( PET_DMG_SHARE ) : 0f;
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
