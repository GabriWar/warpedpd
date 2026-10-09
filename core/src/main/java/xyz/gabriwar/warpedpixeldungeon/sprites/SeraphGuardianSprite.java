package xyz.gabriwar.warpedpixeldungeon.sprites;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.noosa.TextureFilm;
public class SeraphGuardianSprite extends MobSprite {
    public SeraphGuardianSprite(){
        texture("sprites/seraph_guardian.png");TextureFilm f=new TextureFilm(texture,16,12);
        idle=new Animation(1,true);idle.frames(f,0);
        run=new Animation(1,true);run.frames(f,0);
        attack=new Animation(10,false);attack.frames(f,2,3,0);
        die=new Animation(8,false);die.frames(f,3,4,5);
        scale.set(1f);
        play(idle);
    }
    private float floatTime;
    @Override public void update(){super.update();floatTime+=com.watabou.noosa.Game.elapsed;}
    @Override public void draw(){
        float baseY=y, baseShadowOffset=shadowOffset;
        y=Math.round(baseY)-2+Math.round((float)Math.sin(floatTime*1.8f)*1.5f);
        // Only the eye bobs: cancel its displacement in the ground projection.
        shadowOffset=baseShadowOffset+(baseY-y)/scale.y;
        try{super.draw();}finally{y=baseY;shadowOffset=baseShadowOffset;}
    }
    public void laser(int cell){
        if(parent==null||!visible)return;
        parent.add(new xyz.gabriwar.warpedpixeldungeon.effects.Beam.LightRay(center(),
                xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.raisedTileCenterToWorld(cell)));
        SpatialSound.play(xyz.gabriwar.warpedpixeldungeon.Assets.Sounds.RAY,ch,.6f,1.2f);
    }
    @Override public int blood(){return 0xFFFFE9AF;}
}
