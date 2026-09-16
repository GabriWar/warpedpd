package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ElementalOrbit;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/** Layered pixel cores, halos and fading trails. Animation never consumes gameplay RNG. */
public class ElementalOrbitFX extends Group {
    private final Char owner;
    private final ElementalOrbit state;
    private final Orb[] orbs = new Orb[ElementalOrbit.MAX_ORBS];
    private float time, trailTime;
    private int flare;
    public ElementalOrbitFX(Char owner, ElementalOrbit state){
        this.owner = owner; this.state = state;
        for (int i = 0; i < orbs.length; i++) add(orbs[i] = new Orb());
    }
    public static int color(int element, int heat){
        int[] fire = {0xFF4522, 0xFF792A, 0xFF9C32, 0xFFB642};
        int[] ice = {0x388AFF, 0x55BEFF, 0x57CFFF, 0x79DDFF};
        return (element == 0 ? fire : ice)[Math.max(0, Math.min(3, heat))];
    }
    @Override public void update(){
        super.update();
        if (owner.sprite == null || !owner.sprite.exists){ killAndErase(); return; }
        visible = owner.sprite.visible;
        time += Game.elapsed; trailTime += Game.elapsed;
        PointF center = owner.sprite.center();
        for (int i = 0; i < orbs.length; i++){
            Orb orb = orbs[i]; orb.visible = i < state.count();
            if (!orb.visible) continue;
            double a = time * 1.8 + i * Math.PI * 2 / state.count();
            float x = center.x + (float)Math.cos(a) * (12 + i % 2);
            float y = center.y - 3 + (float)Math.sin(a) * 6;
            orb.place(x, y, state.element(i), state.heat(i), time);
            if (trailTime >= .15f && visible) add(new Spark(x, y, color(state.element(i), state.heat(i)), -1, -3, .25f));
        }
        if (trailTime >= .15f) trailTime = 0;
        if (flare != state.flareSerial){
            flare = state.flareSerial;
            for (int i = 0; i < state.count(); i++) burst(this, orbs[i].px, orbs[i].py, state.element(i), 3, 4);
        }
    }
    private static Image mote(int frame){
        Image image=new Image("effects/skill_motes.png");
        image.frame(new com.watabou.noosa.TextureFilm(image.texture,4,4).get(frame));return image;
    }
    private static Image orbLayer(int frame){
        Image image=new Image("effects/elemental_orbs.png");
        image.frame(new com.watabou.noosa.TextureFilm(image.texture,12,12).get(frame));return image;
    }
    private static class Orb extends Group {
        final Image halo = orbLayer(0);
        final Image shell = orbLayer(1);
        final Image core = orbLayer(3);
        final com.watabou.noosa.TextureFilm film=new com.watabou.noosa.TextureFilm(shell.texture,12,12);
        float px, py;
        Orb(){ add(halo); add(shell); add(core); }
        void place(float x, float y, int element, int heat, float time){
            px=x; py=y;
            Image[] images={halo,shell,core};
            float pulse = 1 + .09f * (float)Math.sin(time * 7);
            halo.scale.set((1.15f+heat*.08f)*pulse); shell.scale.set(.7f+heat*.15f); core.scale.set(.8f+heat*.08f);
            shell.frame(film.get(element==0?1:2));
            halo.hardlight(color(element,heat)); halo.alpha(.7f);
            shell.hardlight(color(element,heat)); shell.angle = 0;
            core.hardlight(color(element,heat)); core.alpha(.65f+heat*.1f);
            for(Image im:images){ im.x=x-im.width()/2; im.y=y-im.height()/2; im.origin.set(im.width/2,im.height/2); }
        }
    }
    private static class Trail extends Image {
        float left=.28f;
        Trail(float x,float y,int element,int heat){
            copy(orbLayer(element==0?1:2));hardlight(ElementalOrbitFX.color(element,heat));
            scale.set(.6f+heat*.1f);this.x=x-width()/2;this.y=y-height()/2;alpha(.45f);
        }
        @Override public void update(){
            super.update();left-=Game.elapsed;alpha(Math.max(0,left/.28f)*.45f);
            if(left<=0)killAndErase();
        }
    }
    private static class Spark extends Image {
        float life, left;
        Spark(float x,float y,int color,float dx,float dy,float life){
            copy(mote(3)); hardlight(color);
            this.x=x; this.y=y; speed.set(dx,dy); this.life=this.left=life;
        }
        @Override public void update(){ super.update(); left-=Game.elapsed; alpha(Math.max(0,left/life)); if(left<=0)killAndErase(); }
    }
    public static void burst(Group group,float x,float y,int element,int heat,int count){
        for(int i=0;i<count;i++){
            double a=i*Math.PI*2/count;
            float speed=7+(i%3)*3+heat;
            group.add(new Spark(x,y,color(element,heat),(float)Math.cos(a)*speed,(float)Math.sin(a)*speed,.2f+(i%3)*.04f));
        }
    }
    public static void launch(Char owner,int cell,int element,int heat,Callback impact){
        if(owner.sprite==null || owner.sprite.parent==null){ impact.call(); return; }
        Group layer=owner.sprite.parent;
        PointF from=owner.sprite.center(), to=DungeonTilemap.tileCenterToWorld(cell);
        layer.add(new Group(){
            final Orb orb=new Orb(); float elapsed, trail;
            final float duration=Math.max(.15f, Math.min(.5f, PointF.distance(from,to)/160f));
            { add(orb); }
            @Override public void update(){
                super.update(); elapsed+=Game.elapsed; trail+=Game.elapsed;
                float t=Math.min(1,elapsed/duration), x=from.x+(to.x-from.x)*t, y=from.y+(to.y-from.y)*t;
                orb.place(x,y,element,heat,elapsed);
                if(trail>.025f){
                    trail=0;layer.add(new Trail(x,y,element,heat));burst(layer,x,y,element,heat,2);
                }
                if(t>=1){ burst(layer,to.x,to.y,element,heat,4+heat*2); killAndErase(); impact.call(); }
            }
        });
    }
}
