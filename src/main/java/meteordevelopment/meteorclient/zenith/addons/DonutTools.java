package meteordevelopment.meteorclient.zenith.addons;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.zenith.ZenithMenuScreen;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.*;
import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Original Zenith tools. No Glazed or Prestige implementation or assets are included. */
public final class DonutTools {
    public static final Category CATEGORY = new Category("Zenith Donut");
    public static void install() {
        Modules.get().add(new ChunkFinder()); Modules.get().add(new Region());
        Modules.get().add(new Rtper(false)); Modules.get().add(new OriginRtper());
        Modules.get().add(new Panel()); Modules.get().add(new FakeBan());
    }
    public static int regionStart(int block) { return DonutMath.regionStart(block); }
    public static boolean withinOrigin(double x,double z,double radius) { return DonutMath.withinOrigin(x,z,radius); }
    private abstract static class Tool extends Module {
        final SettingGroup general=settings.getDefaultGroup();
        Tool(String name,String description){super(CATEGORY,"zenith-"+name,description);}
        Setting<Integer> integer(String name,String description,int value,int min,int max){return general.add(new IntSetting.Builder().name(name).description(description).defaultValue(value).range(min,max).sliderRange(min,max).build());}
        Setting<Boolean> flag(String name,String description,boolean value){return general.add(new BoolSetting.Builder().name(name).description(description).defaultValue(value).build());}
    }
    public static final class ChunkFinder extends Tool {
        private final Setting<Integer> radius=integer("scan-radius","Loaded chunk radius. Never requests new chunks.",6,1,12);
        private final Setting<Integer> minimum=integer("minimum-storage","Minimum storage block entities before a chunk is highlighted.",3,1,100);
        private final Setting<Boolean> notices=flag("notifications","Notify once for newly detected storage chunks.",true);
        private final Map<Long,Integer> found=new LinkedHashMap<>();
        private final Setting<Boolean> structures=flag("crafted-block-clues","Also scan loaded underground blocks for concentrations of crafted stone and obsidian. Natural structures can trigger this.",true);
        private final Setting<Integer> blockBudget=integer("blocks-per-tick","Maximum block samples per tick.",4096,256,8192);
        private final Setting<Integer> craftedMinimum=integer("minimum-crafted-blocks","Crafted underground blocks needed for a second clue.",16,1,256);
        private final Map<Long,Integer> crafted=new LinkedHashMap<>();
        private int scanCursor,offset,score;private long scanning=Long.MIN_VALUE;
        private int cursor,ticks; private String dimension="";
        ChunkFinder(){super("chunk-finder","Highlights storage-rich loaded chunks. A clue to builds, not proof of a player base or chunk age.");}
        public int detections(){var keys=new HashSet<>(found.keySet());keys.addAll(crafted.keySet());return keys.size();}
        @Override public void onActivate(){clear();}
        @Override public void onDeactivate(){clear();}
        private void clear(){found.clear();crafted.clear();cursor=ticks=scanCursor=offset=score=0;scanning=Long.MIN_VALUE;dimension="";}
        @EventHandler private void tick(TickEvent.Post event){
            String current=mc.level.dimension().identifier().toString();
            if(!current.equals(dimension)){clear();dimension=current;}
            scanBlocks();
            if(++ticks%2!=0)return;
            ChunkPos center=mc.player.chunkPosition();int r=radius.get(),width=r*2+1;
            // Four existing chunks per sampling tick, with a strict storage-entry ceiling.
            for(int i=0;i<4;i++){
                int index=cursor++%(width*width),x=center.x()+index%width-r,z=center.z()+index/width-r;
                LevelChunk chunk=mc.level.getChunkSource().getChunk(x,z,false);long key=ChunkPos.pack(x,z);
                if(chunk==null){found.remove(key);continue;}
                int count=0,visited=0;
                for(BlockEntity be:chunk.getBlockEntities().values()){
                    if(++visited>4096)break;
                    if(be instanceof ChestBlockEntity||be instanceof BarrelBlockEntity||be instanceof ShulkerBoxBlockEntity||be instanceof HopperBlockEntity)count++;
                }
                if(count>=minimum.get()){
                    boolean fresh=!found.containsKey(key);found.put(key,count);
                    if(fresh&&notices.get())info("Storage clue: chunk %d, %d (%d containers)",x,z,count);
                }else found.remove(key);
            }
            crafted.keySet().removeIf(key->Math.abs(ChunkPos.getX(key)-center.x())>r||Math.abs(ChunkPos.getZ(key)-center.z())>r);
            found.keySet().removeIf(key->Math.abs(ChunkPos.getX(key)-center.x())>r||Math.abs(ChunkPos.getZ(key)-center.z())>r);
        }
        private void scanBlocks(){
            if(!structures.get()){crafted.clear();return;}
            int r=radius.get(),width=r*2+1;var center=mc.player.chunkPosition();
            if(scanning==Long.MIN_VALUE){int index=scanCursor++%(width*width);scanning=ChunkPos.pack(center.x()+index%width-r,center.z()+index/width-r);offset=score=0;}
            int cx=ChunkPos.getX(scanning),cz=ChunkPos.getZ(scanning);
            LevelChunk chunk=mc.level.getChunkSource().getChunk(cx,cz,false);
            if(chunk==null||Math.abs(cx-center.x())>r||Math.abs(cz-center.z())>r){crafted.remove(scanning);scanning=Long.MIN_VALUE;return;}
            int floor=mc.level.getMinY(),ceiling=Math.min(32,mc.level.getMaxY()),end=Math.max(0,(ceiling-floor)*256);
            var pos=new net.minecraft.core.BlockPos.MutableBlockPos();
            for(int n=0;n<blockBudget.get()&&offset<end;n++,offset++){
                pos.set(cx*16+(offset&15),floor+(offset>>8),cz*16+((offset>>4)&15));
                var b=chunk.getBlockState(pos).getBlock();
                if(b==net.minecraft.world.level.block.Blocks.COBBLED_DEEPSLATE||b==net.minecraft.world.level.block.Blocks.DEEPSLATE_BRICKS||b==net.minecraft.world.level.block.Blocks.DEEPSLATE_TILES||b==net.minecraft.world.level.block.Blocks.OBSIDIAN)score++;
            }
            if(offset>=end){if(score>=craftedMinimum.get())crafted.put(scanning,score);else crafted.remove(scanning);scanning=Long.MIN_VALUE;}
        }
        @EventHandler private void render(Render3DEvent e){
            double y=mc.player.getY();var keys=new HashSet<>(found.keySet());keys.addAll(crafted.keySet());
            for(long key:keys){
                int x=ChunkPos.getX(key)*16,z=ChunkPos.getZ(key)*16;
                e.renderer.box(x,y-1,z,x+16,y+2,z+16,new Color(177,103,255,12),new Color(196,143,255,210),ShapeMode.Both,0);
            }
        }
    }
    public static final class Region extends Tool {
        private final Setting<Boolean> map=flag("region-map","Show a local region grid with coordinates and heading. These are Minecraft region coordinates, not server rank labels.",true);
        private final Setting<Integer> mapX=integer("map-x","Map left position in screen pixels.",20,0,7680);
        private final Setting<Integer> mapY=integer("map-y","Map top position in screen pixels.",380,0,4320);
        private final Setting<Integer> cell=integer("map-cell-size","Region map cell size in pixels.",54,30,120);
        private meteordevelopment.meteorclient.systems.hud.HudRenderer hud;
        @EventHandler private void overlay(meteordevelopment.meteorclient.events.render.Render2DEvent e){
            if(!map.get()||mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden)return;
            if(hud==null)hud=meteordevelopment.meteorclient.systems.hud.HudRenderer.independent();
            hud.begin(e.graphics);int x=mapX.get(),y=mapY.get(),c=cell.get(),rx=Math.floorDiv(mc.player.blockPosition().getX(),512),rz=Math.floorDiv(mc.player.blockPosition().getZ(),512);
            for(int row=0;row<3;row++)for(int col=0;col<3;col++){
                meteordevelopment.meteorclient.zenith.ZenithDraw.rounded(hud,x+col*c,y+row*c,c-2,c-2,6,new Color(35,20,52,215));
                hud.text((rx+col-1)+","+(rz+row-1),x+col*c+5,y+row*c+7,new Color(204,173,242),false,.65);
            }
            double px=x+c+(mc.player.getX()-regionStart(mc.player.blockPosition().getX()))/512*c,pz=y+c+(mc.player.getZ()-regionStart(mc.player.blockPosition().getZ()))/512*c,a=Math.toRadians(mc.player.getYRot());
            meteordevelopment.meteorclient.zenith.ZenithDraw.line(hud,px,pz,px-Math.sin(a)*13,pz+Math.cos(a)*13,3,new Color(106,245,205));
            hud.text("Region "+rx+", "+rz,x,y+3*c+4,new Color(208,184,240),true,.8);hud.end();
        }
        private final Setting<Boolean> region=flag("region-boundary","Also outline the current 512 by 512 block region.",true);
        Region(){super("chunk-region","Current 16-block chunk and 512-block region boundaries, including negative coordinates.");}
        @EventHandler private void render(Render3DEvent e){
            var p=mc.player.blockPosition();double y=mc.player.getY()+.05;
            int x=Math.floorDiv(p.getX(),16)*16,z=Math.floorDiv(p.getZ(),16)*16;
            e.renderer.box(x,y,z,x+16,y+3,z+16,new Color(0,0,0,0),new Color(192,133,255,220),ShapeMode.Lines,0);
            if(region.get())e.renderer.box(regionStart(p.getX()),y,regionStart(p.getZ()),regionStart(p.getX())+512,y+8,regionStart(p.getZ())+512,new Color(0,0,0,0),new Color(105,231,207,190),ShapeMode.Lines,0);
        }
    }
    public static class Rtper extends Tool {
        public final Setting<String> command=general.add(new StringSetting.Builder().name("command").description("Server command without slash. Configure for the server's current RTP menu/command.").defaultValue("rtp").build());
        private final Setting<Integer> cooldown=integer("interval-seconds","Minimum time between requests; respect server cooldowns.",45,15,600);
        private final Setting<Integer> limit=integer("maximum-attempts","Stop after this many requests.",10,1,100);
        private final Setting<Integer> target=integer("origin-radius","Stop within this horizontal distance of 0,0.",1000,0,100000);
        private final Setting<Boolean> pause=flag("pause-in-menus","Pause while a menu is open. RTP menus are operated manually.",true);
        private final boolean seekOrigin; private int attempts; private long next;private double best;
        Rtper(boolean seek){super(seek?"rtp-base-finder":"rtper",seek?"Retries server RTP until inside your 0,0 radius or the attempt limit. Random server results cannot be forced.":"Sends a configurable RTP command at a bounded interval. No automatic menu clicking.");seekOrigin=seek;}
        @Override public void onActivate(){attempts=0;next=0;best=Double.POSITIVE_INFINITY;for(Module m:Modules.get().getAll())if(m instanceof Rtper&&m!=this)m.disable();}
        @Override public void onDeactivate(){attempts=0;next=0;}
        @EventHandler private void tick(TickEvent.Post e){
            if(mc.hasSingleplayerServer()){warning("RTP requires a multiplayer server.");disable();return;}
            double distance=Math.hypot(mc.player.getX(),mc.player.getZ());
            if(distance<best){best=distance;}
            if(seekOrigin&&withinOrigin(mc.player.getX(),mc.player.getZ(),target.get())){info("Origin target reached: %.0f blocks away.",distance);disable();return;}
            if(pause.get()&&mc.gui.screen()!=null)return;
            long now=System.nanoTime();if(now<next)return;
            if(attempts>=limit.get()){info("RTP limit reached. Closest observed distance: %.0f blocks.",best);disable();return;}
            String text=command.get().strip();if(text.startsWith("/"))text=text.substring(1);
            if(text.isBlank()||text.contains("\n")||text.contains("\r")){error("Set a valid single RTP command.");disable();return;}
            mc.player.connection.sendCommand(text);attempts++;next=now+cooldown.get()*1_000_000_000L;
            info("RTP request %d/%d. Current distance: %.0f blocks.",attempts,limit.get(),distance);
        }
    }
    public static final class OriginRtper extends Rtper { OriginRtper(){super(true);} }
    public static final class Panel extends Tool {
        Panel(){super("donut-panel","Open the purple Donut quick-settings sidebar. Bind this module to a key.");runInMainMenu=true;}
        @Override public void onActivate(){mc.gui.setScreen(new QuickPanel(mc.gui.screen()));disable();}
    }
    public static final class FakeBan extends Tool {
        final Setting<String> reason=general.add(new StringSetting.Builder().name("reason").description("Text shown in the local preview.").defaultValue("You have been banned from this server.").build());
        FakeBan(){super("fake-ban-screen","Local ban-screen preview. Does not disconnect you or change server state.");runInMainMenu=true;}
        @Override public void onActivate(){mc.gui.setScreen(new BanPreview(mc.gui.screen(),reason.get()));disable();}
    }
    public static final class QuickPanel extends Screen {
        private final Screen previous;private final long opened=System.nanoTime();
        public QuickPanel(Screen previous){super(Component.literal("Zenith Donut"));this.previous=previous;}
        @Override protected void init(){
            String[] ids={"fullbright","zenith-chunk-finder","zenith-chunk-region","zenith-rtper","zenith-rtp-base-finder","auto-totem","stash-finder","zenith-fake-ban-screen"};
            int x=Math.max(6,width-244),y=54,row=Math.max(12,Math.min(25,(height-76)/9));
            for(String id:ids){Module module=Modules.get().get(id);if(module==null)continue;
                addRenderableWidget(new ZenithMenuScreen.ZenithButton(x,y,184,row-2,module.title+(module.isActive()?" ON":" OFF"),b->{module.toggle();b.setMessage(Component.literal(module.title+(module.isActive()?" ON":" OFF")));}));
                addRenderableWidget(new ZenithMenuScreen.ZenithButton(x+188,y,32,row-2,"...",b->mc.gui.setScreen(GuiThemes.get().moduleScreen(module))));y+=row;
            }
            addRenderableWidget(new ZenithMenuScreen.ZenithButton(x,y+5,220,20,"Close",b->onClose()));
        }
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
            double t=Math.min(1,(System.nanoTime()-opened)/250_000_000.0);int left=Math.max(0,width-256);
            g.fillGradient(left,0,width,height,((int)(235*t)<<24)|0x1d102e,((int)(245*t)<<24)|0x090711);
            g.fill(left,0,left+2,height,0xffb577ff);g.text(font,"ZENITH / DONUT",left+14,18,0xffe3c9ff,false);
            g.text(font,"Toggle   /   ... settings",left+14,34,0xffb7a1ce,false);
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable r)r.extractRenderState(g,mx,my,delta);
        }
        @Override public boolean isPauseScreen(){return false;}
        @Override public void onClose(){mc.gui.setScreen(previous);}
    }
    public static final class BanPreview extends Screen {
        private final Screen previous;private final String reason;
        public BanPreview(Screen previous,String reason){super(Component.literal("Disconnected"));this.previous=previous;this.reason=reason;}
        @Override protected void init(){addRenderableWidget(Button.builder(Component.literal("Return"),b->onClose()).bounds(width/2-100,height/2+40,200,20).build());}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
            ZenithMenuScreen.background(g,width,height,0);ZenithMenuScreen.centered(g,"Disconnected",width/2,height/2-55,0xffff8888,1);
            g.text(font,Component.literal(reason),width/2-font.width(reason)/2,height/2-15,0xffeeeeee);
            ZenithMenuScreen.centered(g,"Local preview · connection unchanged",width/2,height/2+15,0xffa693bb,.8f);
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable r)r.extractRenderState(g,mx,my,delta);
        }
        @Override public boolean isPauseScreen(){return false;}
        @Override public void onClose(){mc.gui.setScreen(previous);}
    }
}
