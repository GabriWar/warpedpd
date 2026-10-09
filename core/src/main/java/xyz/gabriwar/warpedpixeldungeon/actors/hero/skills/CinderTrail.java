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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;

import java.util.ArrayList;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.*;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.*;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.ElementalOrbitFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/** Class/tag retained so existing saves keep their purchased skill levels. */
public class CinderTrail extends Skill {
    { tag="D1"; name="Elemental Orbit"; castText="Gather!"; tier=1; image=29; mana=4; }
    private ElementalOrbit orbit(Hero hero){ return hero.buff(ElementalOrbit.class); }
    @Override public ArrayList<String> actions(Hero hero){
        ArrayList<String> out=new ArrayList<>();
        if(level>0 && (orbit(hero)!=null || hero.MP>=getManaCost())) out.add(AC_CAST);
        return out;
    }
    @Override public String quickslotStatus(){
        ElementalOrbit orbit=Dungeon.hero==null?null:orbit(Dungeon.hero);
        return orbit==null?super.quickslotStatus():Integer.toString(orbit.count());
    }
    @Override public void execute(Hero hero,String action){
        if(!AC_CAST.equals(action) || level<=0) return;
        if(orbit(hero)==null){
            refresh(hero);
        }else{
            ElementalOrbit current=orbit(hero);
            ArrayList<String> options=new ArrayList<>();
            ArrayList<Integer> choices=new ArrayList<>();
            for(int element=0;element<2;element++)if(current.count(element)>0){
                options.add(Messages.get(CinderTrail.class,element==0?"fire":"ice",current.count(element)));
                choices.add(element);
            }
            if(hero.MP>=getManaCost()){
                options.add(Messages.get(CinderTrail.class,"refresh",getManaCost()));choices.add(2);
            }
            if(options.isEmpty())return;
            if(options.size()==1&&choices.get(0)!=2){aim(hero,choices.get(0));return;}
            GameScene.show(new xyz.gabriwar.warpedpixeldungeon.windows.WndOptions(name(),
                    Messages.get(CinderTrail.class,"choose_element"),options.toArray(new String[0])){
                @Override protected void onSelect(int index){
                    if(index<0||index>=choices.size())return;
                    int choice=choices.get(index);
                    if(choice==2)refresh(hero);else aim(hero,choice);
                }
            });
        }
        hero.heroSkills.lastUsed=this;
    }
    private void refresh(Hero hero){
        if(!hero.isAlive()||hero.MP<getManaCost())return;
        Buff.affect(hero,ElementalOrbit.class).set(level);
        hero.MP-=getManaCost();hero.heroSkills.lastUsed=this;
        castTextYell();SpatialSound.play(Assets.Sounds.CHARGEUP, hero);
        // The gathering: heat and frost pulled in from either side, a flare of each as they settle into orbit.
        if(hero.sprite!=null){
            hero.sprite.centerEmitter().burst(FlameParticle.FACTORY,3+level);
            hero.sprite.centerEmitter().burst(SnowParticle.RISING_FACTORY,3+level);
            new Flare(6,14).color(0xFFAA66,true).show(hero.sprite,0.5f);
            StaggerFX.after(0.15f,()->{if(hero.sprite!=null)new Flare(6,14).color(0x88DDFF,true).show(hero.sprite,0.5f);});
        }
        hero.spendAndNext(TIME_TO_USE);
    }
    private void aim(Hero hero,int element){
        GameScene.selectCell(new CellSelector.Listener(){
            @Override public String prompt(){return Messages.get(CinderTrail.class,"prompt");}
            @Override public void onSelect(Integer selected){
                ElementalOrbit orbit=orbit(hero);
                if(selected==null || orbit==null || !hero.isAlive())return;
                if(selected<0 || selected>=Dungeon.level.length() || !Dungeon.level.heroFOV[selected]
                        || Dungeon.level.distance(hero.pos,selected)>7){ GLog.w(Messages.get(CinderTrail.class,"no_target")); return; }
                int cell=new Ballistica(hero.pos,selected,Ballistica.PROJECTILE).collisionPos;
                if(cell==hero.pos || Dungeon.level.solid[cell])return;
                int[] shot=orbit.launch(element);
                if(shot==null)return;
                Invisibility.dispel();
                hero.busy();
                hero.sprite.zap(cell);
                ElementalOrbitFX.launch(hero,cell,shot[0],shot[1],()->{
                    explode(hero,cell,shot[0],shot[1],shot[2]);
                    hero.spendAndNext(TIME_TO_USE);
                });
            }
        });
    }
    public static int damage(int level,int heat){ return (4+3*level)*(3+heat)/3; }
    public static void explode(Hero hero,int cell,int element,int heat,int level){
        SpatialSound.play(element==0?Assets.Sounds.BURNING:Assets.Sounds.SHATTER, cell);
        // The burst: a flare at the impact, then flame or frost racing over the ring around it a beat later.
        if(hero.sprite!=null&&hero.sprite.parent!=null&&Dungeon.level.heroFOV[cell]){
            new Flare(6,14).color(element==0?0xFF8418:0xA4E9FF,true).show(hero.sprite.parent,
                    xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(cell),0.5f);
            CellEmitter.center(cell).burst(element==0?FlameParticle.FACTORY:SnowParticle.FACTORY,4+heat);
        }
        com.watabou.noosa.Camera.main.shake(1,0.2f);
        StaggerFX.ring(cell,1,0.08f,(c,r)->CellEmitter.get(c).burst(element==0?FlameParticle.FACTORY:SnowParticle.FACTORY,2));
        for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++){
            int nx=cell%Dungeon.level.width()+x, ny=cell/Dungeon.level.width()+y;
            if(nx<0 || nx>=Dungeon.level.width() || ny<0 || ny>=Dungeon.level.height())continue;
            int c=nx+ny*Dungeon.level.width();
            if(Dungeon.level.solid[c])continue;
            if(new Ballistica(cell,c,Ballistica.STOP_TARGET|Ballistica.STOP_SOLID).collisionPos!=c)continue;
            Char ch=Actor.findChar(c);
            if(ch!=null && ch.alignment==Char.Alignment.ENEMY){
                int damage=Random.NormalIntRange(damage(level,heat)/2,damage(level,heat));
                ch.damage(damage,CinderTrail.class);
                if(ch.isAlive()){
                    if(element==0)Buff.affect(ch,Burning.class).reignite(ch);
                    else Buff.prolong(ch,Chill.class,2+heat);
                }
            }
            // Every impact leaves its element; mature upgraded orbs sustain a stronger patch.
            int strength=2+level+heat;
            GameScene.add(element==0?Blob.seed(c,strength,Fire.class):Blob.seed(c,strength*4,Blizzard.class));
        }
    }
    @Override public int getManaCost(){return (int)Math.ceil(mana*(1+.5*level));}
    @Override protected boolean upgrade(){return true;}
}
