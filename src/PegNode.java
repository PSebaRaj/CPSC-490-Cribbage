public class PegNode extends CFRNode {
    private boolean sample = true;
    public PegNode(byte numActions) {
        super(numActions);
    }

    public PegNode(byte numActions, boolean sample) {
        super(numActions);
        this.sample = sample;
    }

    public int getAction(boolean sample) {
        if (sample) {
//            System.out.println("Sampling!");
            return sampleBestAction();
        }
        return getBestAction();
    }
}
