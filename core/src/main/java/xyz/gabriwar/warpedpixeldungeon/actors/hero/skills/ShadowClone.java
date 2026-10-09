/*
 * Warped Pixel Dungeon, Copyright (C) 2026 Gabriel Duarte Guerra.
 * Rope animation/item pulling adapted from Summoning Pixel Dungeon Reincarnated
 * Ropes.java, Copyright (C) 2023-2026 Trashbox Bobylev, GPL-3.0-or-later.
 */
package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;

import java.util.*;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.*;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.*;
import xyz.gabriwar.warpedpixeldungeon.items.*;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.*;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;

/** Legacy class and A3 tag retained to preserve learned Shadow Clone levels in saves. */
public class ShadowClone extends ActiveSkill3 {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }

    {name="Grappling Hook";castText="Come on!";tier=3;image=67;mana=6;}
    @Override public boolean toggleable(){return false;}
    /** The strike after an enemy pull. */
    public static final float DAMAGE_MULTIPLIER=1.5f;
    @Override public ArrayList<String> actions(Hero hero){
        ArrayList<String> out=new ArrayList<>();if(level>0&&hero.MP>=getManaCost())out.add(AC_CAST);return out;
    }
    @Override public void execute(Hero hero,String action){
        if(AC_CAST.equals(action)&&level>0&&hero.MP>=getManaCost())new Plan(hero).choose();
    }
    /** Exact clear ray and a safe free tile next to the target; no wall/chasm landing. */
    public int landing(Hero hero,int from,Char enemy){
        if(enemy==null||!enemy.isAlive()||enemy.alignment!=Char.Alignment.ENEMY
                ||!Dungeon.level.heroFOV[enemy.pos])return -1;
        Ballistica line=new Ballistica(from,enemy.pos,Ballistica.PROJECTILE);
        if(line.collisionPos!=enemy.pos||line.dist<1)return -1;
        int cell=line.path.get(line.dist-1);
        if(!Dungeon.level.passable[cell]||Dungeon.level.pit[cell]||Dungeon.level.solid[cell]
                || (Actor.findChar(cell)!=null&&Actor.findChar(cell)!=hero)
                ||(hero.properties().contains(Char.Property.LARGE)&&!Dungeon.level.openSpace[cell]))return -1;
        return cell;
    }
    /** Hook a wall (or, at +3, any visible tile), without passing through characters or other walls. */
    public int anchorLanding(Hero hero,int from,int target){
        if(hero.rooted||!SkillInteractions.valid(from)||!SkillInteractions.valid(target)||target==from
                ||!Dungeon.level.heroFOV[target]||(level<MAX_LEVEL&&!Dungeon.level.solid[target]))return -1;
        Ballistica ray=new Ballistica(from,target,Ballistica.STOP_TARGET);
        int landing=target;
        for(int i=1;i<=ray.dist;i++){
            int c=ray.path.get(i);
            Char occupant=Actor.findChar(c);
            if(occupant!=null&&occupant!=hero)return -1;
            if(Dungeon.level.solid[c]){
                if(c!=target)return -1;
                landing=ray.path.get(i-1);
            }
        }
        if(landing==from||!Dungeon.level.passable[landing]||Dungeon.level.pit[landing]||Dungeon.level.solid[landing]
                ||(Actor.findChar(landing)!=null&&Actor.findChar(landing)!=hero)
                ||(hero.properties().contains(Char.Property.LARGE)&&!Dungeon.level.openSpace[landing]))return -1;
        return landing;
    }
    private class Plan {
        final Hero hero;final ArrayList<Integer> cells=new ArrayList<>();final ArrayList<Char> enemies=new ArrayList<>();final ArrayList<Boolean> anchors=new ArrayList<>();
        int projected,depth,branch;
        boolean waiting;
        final com.watabou.noosa.Group marks=new com.watabou.noosa.Group();
        void clearMarks(){marks.killAndErase();}
        void mark(int cell){
            if(hero.sprite==null||hero.sprite.parent==null)return;
            if(marks.parent==null)hero.sprite.parent.add(marks);
            com.watabou.noosa.Image icon=xyz.gabriwar.warpedpixeldungeon.ui.Icons.get(xyz.gabriwar.warpedpixeldungeon.ui.Icons.TARGET);
            icon.point(xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.tileToWorld(cell));
            icon.hardlight(0xA6DBCF);icon.alpha(.7f);marks.add(icon);
        }
        Plan(Hero hero){this.hero=hero;projected=hero.pos;depth=Dungeon.depth;branch=Dungeon.branch;}
        void retry(){
            // CellSelector resets to the idle listener after onSelect returns.
            com.watabou.noosa.Game.runOnRenderThread(()->{
                if(hero.isAlive()&&depth==Dungeon.depth&&branch==Dungeon.branch)choose();
            });
        }
        void choose(){
            GameScene.selectCell(new CellSelector.Listener(){
                @Override public String prompt(){return Messages.get(ShadowClone.class,level>=MAX_LEVEL?"prompt_any":"prompt",cells.size()+1,level);}
                @Override public void onSelect(Integer cell){
                    if(cell==null){if(waiting)clearMarks();waiting=false;return;}
                    waiting=false;
                    if(depth!=Dungeon.depth||branch!=Dungeon.branch){clearMarks();return;}
                    if(!SkillInteractions.valid(cell)||!Dungeon.level.heroFOV[cell]){retry();return;}
                    Char enemy=Actor.findChar(cell);
                    boolean anchor=false;
                    if(enemy!=null&&enemy.alignment==Char.Alignment.ENEMY){
                        int land=landing(hero,projected,enemy);
                        if(hero.rooted||land<0||enemies.contains(enemy)){GLog.w(Messages.get(ShadowClone.class,"no_target"));retry();return;}
                        projected=land;
                    }else{
                        enemy=null;
                        int land=anchorLanding(hero,projected,cell);
                        if(land<0){GLog.w(Messages.get(ShadowClone.class,"no_target"));retry();return;}
                        anchor=true;projected=land;
                    }
                    cells.add(cell);enemies.add(enemy);anchors.add(anchor);mark(cell);
                    if(cells.size()>=level){start();return;}
                    GameScene.show(new WndOptions(name(),Messages.get(ShadowClone.class,"selected",cells.size(),level),
                            Messages.get(ShadowClone.class,"launch"),Messages.get(ShadowClone.class,"add")){
                        @Override protected void onSelect(int index){if(index==0)start();else if(index==1)choose();}
                        @Override public void onBackPressed(){clearMarks();super.onBackPressed();}
                    });
                }
            });
            waiting=true;
        }
        void start(){
            if(cells.isEmpty()||hero.MP<getManaCost()||!hero.isAlive()||depth!=Dungeon.depth||branch!=Dungeon.branch){clearMarks();return;}
            hero.MP-=getManaCost();hero.heroSkills.lastUsed=ShadowClone.this;Invisibility.dispel();castTextYell();hero.busy();pull(0);
        }
        void pull(int index){
            if(index>=cells.size()||!hero.isAlive()||depth!=Dungeon.depth||branch!=Dungeon.branch){finish();return;}
            Char enemy=enemies.get(index);boolean anchor=anchors.get(index);int cell=enemy==null?cells.get(index):enemy.pos;
            int land=anchor?anchorLanding(hero,hero.pos,cell):landing(hero,hero.pos,enemy);
            if(hero.rooted||land<0){pull(index+1);return;}
            final int from=hero.pos;
            //the rope whips out, a note higher each throw of the chain
            SpatialSound.play(Assets.Sounds.MISS,hero,1f,1.1f+0.1f*index);
            SpatialSound.play(Assets.Sounds.CHAINS,hero,0.7f,1.2f+0.1f*index);
            hero.sprite.parent.add(new Chains(hero.sprite.center(),xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.raisedTileCenterToWorld(cell),Effects.Type.ROPE,()->{
                if(anchor){
                    if(depth!=Dungeon.depth||branch!=Dungeon.branch||!hero.isAlive()
                            ||anchorLanding(hero,hero.pos,cell)!=land){pull(index+1);return;}
                    hero.sprite.jump(from,land,()->{
                        if(depth==Dungeon.depth&&branch==Dungeon.branch&&hero.isAlive()){
                            hero.move(land,false);hero.sprite.place(land);Dungeon.observe();GameScene.updateFog();
                            CellEmitter.bottom(land).burst(Speck.factory(Speck.DUST),4);
                            SpatialSound.play(Assets.Sounds.STURDY,land,0.6f,1.2f);
                        }
                        pull(index+1);
                    });
                }else{
                    if(!enemy.isAlive()||hero.rooted||landing(hero,hero.pos,enemy)!=land){pull(index+1);return;}
                    hero.sprite.jump(from,land,()->{
                        hero.move(land,false);hero.sprite.place(land);Dungeon.observe();GameScene.updateFog();
                        CellEmitter.bottom(land).burst(Speck.factory(Speck.DUST),3);
                        if(hero.isAlive()&&enemy.isAlive()&&Dungeon.level.adjacent(hero.pos,enemy.pos)){
                            hero.sprite.turnTo(hero.pos,enemy.pos);
                            KindOfWeapon weapon=hero.belongings.weapon();
                            int damage=Math.round((weapon==null?RingOfForce.damageRoll(hero):weapon.damageRoll(hero))*DAMAGE_MULTIPLIER);
                            if(weapon!=null)damage=weapon.proc(hero,enemy,damage);
                            damage=hero.heroSkills.allOnHit(enemy,damage,false);
                            enemy.damage(damage,hero);SkillFX.flash(enemy);Wound.hit(enemy);
                            SpatialSound.play(Assets.Sounds.HIT_STAB,enemy,1f,1f+0.1f*index);
                        }
                        pull(index+1);
                    });
                }
            }));
        }
        void finish(){clearMarks();hero.spendAndNext(TIME_TO_USE);}
    }
    @Override public int getManaCost(){return (int)Math.ceil(mana*(1+.5*level));}
    @Override protected boolean upgrade(){return true;}
}
