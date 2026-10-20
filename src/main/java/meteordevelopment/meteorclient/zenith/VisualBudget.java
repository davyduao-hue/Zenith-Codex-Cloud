package meteordevelopment.meteorclient.zenith;

/** Shared storage and geometry ceilings; density never overrides these limits. */
public record VisualBudget(int segments,int particles,int bursts,int emission,int skyColumns,int skyRows,int stars,int motes) {
    private static final VisualBudget LOW=new VisualBudget(24,256,16,16,48,16,128,16);
    private static final VisualBudget BALANCED=new VisualBudget(40,768,32,48,96,28,384,36);
    private static final VisualBudget HIGH=new VisualBudget(64,1536,48,88,128,36,768,64);
    public static VisualBudget level(int level) {return level<=0?LOW:level==1?BALANCED:HIGH;}
    public int skyVertices(){return (skyColumns+1)*(skyRows+1);}
}
