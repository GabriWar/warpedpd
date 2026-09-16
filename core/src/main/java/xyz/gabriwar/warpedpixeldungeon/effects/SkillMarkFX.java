package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.*;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/** Status silhouettes: orbiting shields, charged guardian halo, frost shards and physical chains. */
public class SkillMarkFX extends Group {
    private final SkillInteractions.Mark state;
    private final Image[] sprites=new Image[12];
    private float time;
    private boolean focusedIcon;
    public SkillMarkFX(SkillInteractions.Mark state){
        this.state=state;
        for(int i=0;i<sprites.length;i++){
            Image image;
            if(state.kind==SkillInteractions.Mark.CHAIN||state.kind==SkillInteractions.Mark.CUT)image=Effects.get(Effects.Type.ROPE);
            else {
                image=new Image("effects/skill_motes.png");
                image.frame(new TextureFilm(image.texture,4,4).get(i%4));
            }
            sprites[i]=image;add(image);
        }
    }
    @Override public void update(){
        super.update();time+=Game.elapsed;
        if(state.target==null||state.target.sprite==null||!state.target.sprite.exists){killAndErase();return;}
        visible=state.target.sprite.visible;
        PointF center=state.target.sprite.center();
        boolean orbit=state.kind==SkillInteractions.Mark.SHARDS||state.kind==SkillInteractions.Mark.GUARD;
        boolean linked=state.kind==SkillInteractions.Mark.CUT;
        xyz.gabriwar.warpedpixeldungeon.actors.Actor other=linked?xyz.gabriwar.warpedpixeldungeon.actors.Actor.findById(state.other):null;
        if(linked&&(!(other instanceof xyz.gabriwar.warpedpixeldungeon.actors.Char)
                ||!((xyz.gabriwar.warpedpixeldungeon.actors.Char)other).isAlive()
                ||((xyz.gabriwar.warpedpixeldungeon.actors.Char)other).sprite==null
                ||!((xyz.gabriwar.warpedpixeldungeon.actors.Char)other).sprite.visible
                ||state.target.id()>other.id())){visible=false;return;}
        if(linked&&other==state.target){
            if(!focusedIcon){sprites[0].copy(xyz.gabriwar.warpedpixeldungeon.ui.Icons.get(xyz.gabriwar.warpedpixeldungeon.ui.Icons.TARGET));focusedIcon=true;}
            for(int i=0;i<sprites.length;i++)sprites[i].visible=i==0;
            sprites[0].x=center.x-sprites[0].width()/2;sprites[0].y=center.y-sprites[0].height()/2;
            sprites[0].hardlight(0xD66AAB);sprites[0].alpha(.7f);return;
        }
        boolean chain=state.kind==SkillInteractions.Mark.CHAIN||linked;
        int count=chain?12:orbit?Math.min(3,Math.max(1,(state.power+5)/6)):2;
        int color=state.kind==SkillInteractions.Mark.FROST?0xA4E9FF:state.kind==SkillInteractions.Mark.CUT?0xD94969:
                state.kind==SkillInteractions.Mark.STAGGER?0xCE9765:state.kind==SkillInteractions.Mark.GUARD?0xCCDEFF:0xFFE7A4;
        for(int i=0;i<sprites.length;i++){
            Image image=sprites[i];image.visible=i<count;
            if(!image.visible)continue;
            if(chain){
                if(!linked&&!SkillInteractions.valid(state.cell)){image.visible=false;continue;}
                PointF anchor=linked?((xyz.gabriwar.warpedpixeldungeon.actors.Char)other).sprite.center():DungeonTilemap.tileCenterToWorld(state.cell);float t=(i+1f)/count;
                image.x=center.x+(anchor.x-center.x)*t;image.y=center.y+(anchor.y-center.y)*t;
                if(linked)image.hardlight(0xD66AAB);
                image.angle=(float)Math.toDegrees(Math.atan2(anchor.y-center.y,anchor.x-center.x))+90;image.alpha(.85f);
            }else{
                float angle=time*1.7f+i*(float)Math.PI*2/count;
                float radius=orbit?6:3;
                image.scale.set(1);image.hardlight(color);
                image.x=center.x+(float)Math.cos(angle)*radius-image.width()/2;
                image.y=(orbit?center.y:state.target.sprite.y-2)+(float)Math.sin(angle)*(orbit?5:2)-image.height()/2;
                image.angle=0;image.alpha(.45f+.15f*(float)Math.sin(time*5+i));
            }
        }
    }
}
