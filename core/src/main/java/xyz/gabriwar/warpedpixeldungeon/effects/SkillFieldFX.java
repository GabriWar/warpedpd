package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.*;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/** Small transparent floor sprites, pulsing warnings and sparse rising motes. */
public class SkillFieldFX extends Group {
    private final SkillField field;
    private final Image[] marks;
    private float time;
    public SkillFieldFX(SkillField field){
        this.field=field;marks=new Image[field.cells.length];
        for(int i=0;i<marks.length;i++){
            Image mark=new Image("effects/skill_fields.png");
            mark.frame(new TextureFilm(mark.texture,16,16).get(field.kind));
            mark.point(DungeonTilemap.tileToWorld(field.cells[i]));
            marks[i]=mark;add(mark);
        }
    }
    @Override public void update(){
        super.update();time+=Game.elapsed;
        if(!field.sameFloor()){visible=false;return;}
        for(int i=0;i<marks.length;i++){
            marks[i].visible=field.cells[i]>=0 && field.cells[i]<Dungeon.level.length() && Dungeon.level.heroFOV[field.cells[i]];
            marks[i].alpha(.65f+.25f*(float)Math.sin(time*4+i*.6f));
        }
    }
}
