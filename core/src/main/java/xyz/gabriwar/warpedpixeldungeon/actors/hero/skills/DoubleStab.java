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
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.*;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.scenes.*;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Chains;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RogueHuntressAuras;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraBuff;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/** Keeps the A1 tag and class for old saves; replaces the layered double-hit toggle. */
public class DoubleStab extends ActiveSkill1 {
    {name="Shadow Link";castText="Bound together!";image=61;tier=1;mana=5;}
    @Override public boolean toggleable(){return true;}
    @Override public boolean doubleStab(){return false;}
    public Char marked(){return null;}
    public static int sharePercent(int rank){return 20+20*Math.max(1,Math.min(3,rank));}
    public static int focusedPercent(int rank){return rank>=3?70:rank==2?50:35;}
    @Override public void execute(Hero hero,String action){
        super.execute(hero,action);
        if(!active)clearLinks();
        if(hero.sprite==null)return;
        if(AC_ACTIVATE.equals(action)&&active){
            //the shadows gather round him and stay: the aura holds while the stance does
            SpatialSound.play(Assets.Sounds.MELD,hero,1f,1.3f);
            hero.sprite.emitter().burst(ShadowParticle.UP,8);
            StanceAuraBuff.sync( hero, RogueHuntressAuras.ShadowLink.class, true );
        }else if(AC_DEACTIVATE.equals(action)){
            SpatialSound.play(Assets.Sounds.MELD,hero,0.6f,0.8f);
            hero.sprite.emitter().burst(ShadowParticle.MISSILE,5);
            StanceAuraBuff.sync( hero, RogueHuntressAuras.ShadowLink.class, false );
        }
    }
    public Char automaticPartner(Char enemy){
        Char nearest=null;
        for(Char other:Actor.chars())if(other!=enemy&&other.alignment==Char.Alignment.ENEMY&&other.isAlive()
                &&SkillInteractions.valid(other.pos)&&Dungeon.level.heroFOV[other.pos]
                &&Dungeon.level.distance(enemy.pos,other.pos)<=3&&SkillInteractions.clear(enemy.pos,other.pos)){
            if(nearest==null||Dungeon.level.distance(enemy.pos,other.pos)<Dungeon.level.distance(enemy.pos,nearest.pos)
                    ||Dungeon.level.distance(enemy.pos,other.pos)==Dungeon.level.distance(enemy.pos,nearest.pos)&&other.id()<nearest.id())nearest=other;
        }
        return nearest==null?enemy:nearest;
    }
    @Override public int onHitProc(Char enemy,int damage,boolean ranged){
        Hero hero=Dungeon.hero;
        if(!active||level<=0||hero.MP<getManaCost()||enemy==null||!enemy.isAlive()
                ||enemy.alignment!=Char.Alignment.ENEMY||damage<=0){clearLinks();return damage;}
        hero.MP-=getManaCost();
        Char partner=automaticPartner(enemy);
        link(enemy,partner,level);
        bind(enemy,partner);
        return damage;
    }
    /** the bond, seen: a spectral chain shoots from the struck enemy to its partner, shadow bursts
     *  where it bites at each end, and both are called out. Cosmetic only; the marks are already set */
    private static void bind(Char first,Char second){
        if(first.sprite==null||first.sprite.parent==null||!Dungeon.level.heroFOV[first.pos])return;
        SpatialSound.play(Assets.Sounds.CHAINS,first,0.8f,1.4f);
        CellEmitter.center(first.pos).burst(ShadowParticle.CURSE,4);
        if(second==first||second.sprite==null){
            first.sprite.showStatus(CharSprite.NEGATIVE,"Bound");
            return;
        }
        final Char a=first,b=second;
        first.sprite.parent.add(new Chains(first.sprite.center(),second.sprite.destinationCenter(),Effects.Type.ETHEREAL_CHAIN,()->{
            if(b.sprite!=null&&b.isAlive()){
                b.sprite.flash();
                b.sprite.emitter().burst(ShadowParticle.CURSE,4);
                b.sprite.showStatus(CharSprite.NEGATIVE,"Bound");
            }
            if(a.sprite!=null&&a.isAlive())a.sprite.showStatus(CharSprite.NEGATIVE,"Bound");
            SpatialSound.play(Assets.Sounds.CHAINS,b,0.6f,1.7f);
        }));
    }
    public static void clearLinks(){
        for(Char ch:Actor.chars())for(SkillInteractions.Mark mark:ch.buffs(SkillInteractions.Mark.class))
            if(mark.kind==SkillInteractions.Mark.CUT)mark.detach();
    }
    public static void link(Char first,Char second,int rank){
        clearLinks();
        SkillInteractions.Mark a=SkillInteractions.mark(first,SkillInteractions.Mark.CUT,rank,8);
        SkillInteractions.Mark b=SkillInteractions.mark(second,SkillInteractions.Mark.CUT,rank,8);
        a.other=second.id();b.other=first.id();
    }
    @Override public int getManaCost(){return 6;}
    @Override protected boolean upgrade(){return true;}
}
