package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.*;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/** Visual-only timelines. Combat never waits for particles or animation callbacks. */
public class SkillSpectacleFX extends Image {
    public static final int ROCK=0,HAMMER=1,SHADOW=2,SHURIKEN=3,RAPTOR=4,JAW=5,SABER=6,LANCE=7,WINGS=8,SPIRE=9,THORN=10,SLASH=11;
    private final PointF from,to;
    private final int kind,cell,depth,branch;
    private final float delay,duration;
    private float elapsed;
    private SkillSpectacleFX(int kind,int fromCell,int toCell,float delay,float duration){
        super("effects/skill_spectacles.png");
        frame(new TextureFilm(texture,12,12).get(kind));
        this.kind=kind;cell=toCell;this.delay=delay;this.duration=duration;
        depth=Dungeon.depth;branch=Dungeon.branch;
        from=DungeonTilemap.tileCenterToWorld(fromCell);to=DungeonTilemap.tileCenterToWorld(toCell);
        scale.set(1);
        if(kind==ROCK){texture("effects/skill_fields.png");frame(new TextureFilm(texture,16,16).get(0));scale.set(1);}
        origin.set(0,0);visible=false;
    }
    public static void show(int kind,int cell){fly(kind,cell,cell,0,.65f);}
    public static void fly(int kind,int from,int to,float delay,float duration){
        if(!SkillInteractions.valid(from)||!SkillInteractions.valid(to)||Dungeon.hero==null||Dungeon.hero.sprite==null
                ||Dungeon.hero.sprite.parent==null||!Dungeon.level.heroFOV[to])return;
        int active=0;
        for(Gizmo child:Dungeon.hero.sprite.parent.membersView())if(child instanceof SkillSpectacleFX&&++active>=96)return;
        Dungeon.hero.sprite.parent.add(new SkillSpectacleFX(kind,from,to,delay,duration));
    }
    @Override public void update(){
        super.update();elapsed+=Game.elapsed;
        if(depth!=Dungeon.depth||branch!=Dungeon.branch||elapsed>=delay+duration){killAndErase();destroy();return;}
        if(elapsed<delay){visible=false;return;}
        visible=Dungeon.level.heroFOV[cell];float p=Math.min(1,(elapsed-delay)/duration);
        x=Math.round(from.x+(to.x-from.x)*p)-6;y=Math.round(from.y+(to.y-from.y)*p)-8;
        if(kind==ROCK){x=Math.round(to.x)-8;y=Math.round(to.y)-8;alpha(Math.min(1,(1-p)*3)*.65f);}
        else if(kind==THORN||kind==LANCE){y+=Math.round(6*(1-Math.min(1,p*4)));alpha(Math.min(1,p*5)*Math.min(1,(1-p)*4));}
        else if(kind==HAMMER||kind==SPIRE){y-=Math.round(14*(1-p)*(1-p));alpha(Math.min(1,p*7)*Math.min(1,(1-p)*5));}
        else if(kind==SHURIKEN||kind==SABER){angle=p*360;alpha(Math.min(1,(1-p)*5));}
        else if(kind==WINGS||kind==RAPTOR){y-=Math.round(Math.sin(p*Math.PI)*4);alpha(Math.min(1,p*6)*Math.min(1,(1-p)*5));}
        else {alpha(Math.min(1,p*7)*Math.min(1,(1-p)*5));}
    }
}
