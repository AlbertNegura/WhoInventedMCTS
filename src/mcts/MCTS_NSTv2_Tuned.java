package mcts;

import Group12.Group12AI;
import game.Game;
import main.collections.FastArrayList;
import util.AI;
import util.Context;
import util.Move;
import utils.AIUtils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MCTS_NSTv2_Tuned extends Group12AI {

    //-------------------------------------------------------------------------

    /** Our player index */
    protected int player = -1;
    protected String analysisReport;
    protected int lastNumPlayoutActions;

    protected Hashtable<Integer, Gram> grams;

    protected final int MAX_GRAMS = 3;
    protected final int MIN_VISITS = 7;
    protected final double eps = 0;
    protected final double decayFactor = 0.9;
    protected double playedMoves = 0;
    protected int iterations = 0;

    //-------------------------------------------------------------------------

    /**
     * Constructor
     */
    public MCTS_NSTv2_Tuned()
    {
        this.friendlyName = "MCTS NST UCB1-Tuned";
        this.analysisReport = null;
    }

    //-------------------------------------------------------------------------

    @Override
    public Move selectAction(Game game, Context context, double maxSeconds, int maxIterations, int maxDepth) {
        // get the action by calling Monte-Carlo tree search
        return MCTS(game, context, maxSeconds, maxIterations, maxDepth);
    }

    private Move MCTS(Game game, Context context, double maxSeconds, int maxIterations, int maxDepth) {
        // initialize Monte-Carlo Tree
        Node root = new Node(null, null, context);
        grams = new Hashtable<>();

        // calculate time to stop search in milliseconds
        final long stopTime = (maxSeconds > 0.0) ? System.currentTimeMillis() + (long) (maxSeconds * 1000L) : Long.MAX_VALUE;
        final int maxIts = (maxIterations >= 0) ? maxIterations : Integer.MAX_VALUE;

        int numIterations = 0;
        resetIterations();
        // keep searching until running out of time (ExampleUCT)
        while(numIterations < maxIts && 					// Respect iteration limit
                System.currentTimeMillis() < stopTime && 	// Respect time limit
                !wantsInterrupt								// Respect GUI user clicking the pause button
        ){
            Node selectedNode = Selection(root);
            // A simulated game is played
            double[] result = PlayOut(selectedNode);
            // The result is backpropagated
            Backpropagation(selectedNode, result);
            numIterations++;
        }
        ++playedMoves;
        Move bestMove = finalMoveSelection(root);
        updateIterations(numIterations);
        // Return best move to play from root
        return bestMove;
    }


    private Node Selection(Node currentNode){
//        Node current = currentNode;
        // Traverse tree
        List<Move> history = new ArrayList<>();
        while (true) {
            if (currentNode.context.trial().over()) {
                // We've reached a terminal state
                break;
            }

            currentNode = SelectionUCT(currentNode, history);

            if (currentNode.visitCount == 0) {
                // We've expanded a new node, time for playout!
                break;
            }
            history.add(currentNode.moveFromParent);
        }
        return currentNode;
    }

    private Node SelectionUCT(Node currentNode, List<Move> history) {
        // If there is any unexpanded move from the current node...
        if (!currentNode.unexpandedMoves.isEmpty())
        {
            // create a copy of context
            final Context context = new Context(currentNode.context);

            FastArrayList<Move> unexpandedMoves = currentNode.unexpandedMoves;

            int bestMoveIndex = -1;
            final double p = ThreadLocalRandom.current().nextDouble(1d);
            if (p <= eps){   // Explore
                bestMoveIndex = ThreadLocalRandom.current().nextInt(unexpandedMoves.size());
            }

            else {          // Exploit
                double bestScore = Double.NEGATIVE_INFINITY;
                int numBestFound = 0;

                for (int m = 0; m < unexpandedMoves.size(); m++) {
                    Move evaluatingMove = unexpandedMoves.get(m);
                    final int mover = context.state().mover();

                    ArrayList<Double> gramScoreSums = new ArrayList<>();

                    Gram currentGram = grams.get(evaluatingMove.hashCode());

                    if(currentGram != null){
                        gramScoreSums = getGramsScoreSums(currentGram, gramScoreSums, history, history.size(), 1, mover);
                    }

                    else{
                        gramScoreSums.add(Double.MAX_VALUE);
                    }

                    double moveScore = 0;

                    for (int i = 0; i < gramScoreSums.size(); i++){
                        moveScore += gramScoreSums.get(i);
                    }

                    moveScore = moveScore / gramScoreSums.size();

                    if (moveScore > bestScore) {
                        bestScore = moveScore;
                        bestMoveIndex = m;
                        numBestFound = 1;
                    } else if (moveScore == bestScore &&
                            ThreadLocalRandom.current().nextInt() % ++numBestFound == 0) {
                        bestMoveIndex = m;
                    }
                }
            }

            final Move move = currentNode.unexpandedMoves.remove(bestMoveIndex);

            // apply the move
            context.game().apply(context, move);

            // create new node and return it
            // This is EXPANSION already.
            return new Node(currentNode, move, context);
        }

        return BestChild(currentNode);
    }

    private Node BestChild(Node currentNode){

        Node bestChild = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        final double parentLog = Math.log(currentNode.visitCount);

        final int mover = currentNode.context.state().mover();

        for(int i = 0; i < currentNode.children.size(); i++){
            final Node child = currentNode.children.get(i);
            final double childValue = child.scoreSums[mover] / child.visitCount;
            final double variance = childValue * (1-childValue);
            final double ucbValue = childValue + Math.sqrt(parentLog / child.visitCount * Math.min(.25, variance + Math.sqrt(2 * parentLog/ child.visitCount)));

            if(ucbValue > bestValue) {
                bestValue = ucbValue;
                bestChild = child;
            }
            if (bestChild == null){
                bestValue = ucbValue;
                bestChild = child;
            }
        }

        return bestChild;
    }

    // RANDOM SELECTION STRATEGY
    private Node RandomSelection(Node currentNode) {

        // Traverse tree
        while (true) {
            if (currentNode.context.trial().over()) {
                // We've reached a terminal state
                break;
            }

            if (!currentNode.unexpandedMoves.isEmpty()) {
                // randomly select an unexpanded move
                final Move move = currentNode.unexpandedMoves.remove(
                        ThreadLocalRandom.current().nextInt(currentNode.unexpandedMoves.size()));

                // create a copy of context
                final Context context = new Context(currentNode.context);

                // apply the move
                context.game().apply(context, move);

                // create new node and return it
                currentNode = new MCTS_NSTv2_Tuned.Node(currentNode, move, context);
            }
            else if(!currentNode.children.isEmpty()){
                // randomly select a children node
                currentNode = currentNode.children.get(
                        ThreadLocalRandom.current().nextInt(currentNode.children.size()));
            }

            if (currentNode.visitCount == 0) {
                // We've expanded a new node, time for playout!
                break;
            }
        }

        return currentNode;
    }

    private FastArrayList<Move> getLegalMoves(Game game, Context context){
        FastArrayList<Move> legalMoves = game.moves(context).moves();

        // If we're playing a simultaneous-move game, some of the legal moves may be
        // for different players. Extract only the ones that we can choose.
        if (!game.isAlternatingMoveGame())
            legalMoves = AIUtils.extractMovesForMover(legalMoves, player);
        return legalMoves;
    }

    // ExampleUCT line 89
    private double[] PlayOut(Node currentNode) {
        Context contextEnd = currentNode.context;
        Game game = contextEnd.game();

        List<Move> history = new ArrayList<>();

        while (!contextEnd.trial().over()){
            contextEnd = new Context(contextEnd);

            FastArrayList<Move> legalMoves = getLegalMoves(game, contextEnd);

            Move bestMove = null;
            final double p = ThreadLocalRandom.current().nextDouble(1d);
            if (p <= eps){   // Explore
                final int r = ThreadLocalRandom.current().nextInt(legalMoves.size());
                bestMove = legalMoves.get(r);
            }

            else {          // Exploit
                double bestScore = Double.NEGATIVE_INFINITY;
                int numBestFound = 0;

                for (int m = 0; m < legalMoves.size(); m++) {
                    Move evaluatingMove = legalMoves.get(m);
                    final int mover = contextEnd.state().mover();

                    ArrayList<Double> gramScoreSums = new ArrayList<>();

                    Gram currentGram = grams.get(evaluatingMove.hashCode());

                    if(currentGram != null){
                        gramScoreSums = getGramsScoreSums(currentGram, gramScoreSums, history, history.size(), 1, mover);
                    }

                    else{
                        gramScoreSums.add(Double.MAX_VALUE);
                    }

                    double moveScore = 0;

                    for (int i = 0; i < gramScoreSums.size(); i++){
                        moveScore += gramScoreSums.get(i);
                    }

                    moveScore = moveScore / gramScoreSums.size();

                    if (moveScore > bestScore) {
                        bestScore = moveScore;
                        bestMove = evaluatingMove;
                        numBestFound = 1;
                    } else if (moveScore == bestScore &&
                            ThreadLocalRandom.current().nextInt() % ++numBestFound == 0) {
                        bestMove = evaluatingMove;
                    }
                }
            }

            history.add(bestMove);
            game.apply(contextEnd, bestMove);
        }

        double[] results = AIUtils.utilities(contextEnd);

        for (int i = 0; i < history.size(); ++i){
            Gram currentGram = this.grams.get(history.get(i).hashCode());
            if (currentGram == null){
                addGram(null, history, i, 1, results);
            }

            else {
                updateGramsScoreSums(currentGram, history, i, 1, results);
            }
        }

        // This computes utilities for all players at the of the playout,
        // which will all be values in [-1.0, 1.0]
        return results;
    }

    private Gram addGram(Gram nextGram, List<Move> history, int currentMove, int gram, double[] results){
        Gram newGram = new Gram(history.get(currentMove), nextGram , results);

        if (nextGram == null){
            grams.put(history.get(currentMove).hashCode(), newGram);
        }
        else {
            nextGram.previousGram.put(history.get(currentMove).hashCode(), newGram);
        }

        if (currentMove > 0 && gram <= MAX_GRAMS){
            addGram(newGram, history, currentMove-1, ++gram, results);
        }

        return newGram;
    }

    private void updateGramsScoreSums(Gram currentGram, List<Move> history, int currentMove, int gram, double[] results){
        currentGram.UpdateScoreSums(results);

        if (currentMove > 0 && gram <= MAX_GRAMS){
            Gram previousGram = currentGram.previousGram.get(history.get(currentMove-1).hashCode());
            if(previousGram == null){
                addGram(currentGram, history, --currentMove, ++gram, results);
            }
            else {
                updateGramsScoreSums(previousGram, history, --currentMove, ++gram, results);
            }
        }
    }

    private ArrayList<Double> getGramsScoreSums(Gram currentGram, ArrayList<Double> gramScoreSums, List<Move> history, int currentMove, int gram, int mover){
        ArrayList<Double> gramScores = gramScoreSums;
        gramScores.add(currentGram.MoverScoreSums(mover));
        if (currentMove > 0 && gram <= MAX_GRAMS){
            Gram previousGram = currentGram.previousGram.get(history.get(currentMove-1).hashCode());
            if (previousGram != null && previousGram.visitCount >= MIN_VISITS){
                gramScores = getGramsScoreSums(previousGram, gramScores, history, currentMove-1, gram+1, mover);
            }
        }
        return gramScores;
    }

    private void Expand(Node currentNode) {
        Node parentNode = currentNode.parent;
        if (parentNode != null){
            parentNode.children.add(currentNode);
        }
    }

    // ExampleUCT line 113
    private void Backpropagation(Node currentNode, double[] result) {
        final int playersCount = currentNode.context.game().players().count();
        while (currentNode != null){
            currentNode.visitCount += 1;
            for (int player = 0; player <= playersCount; player++) {
                currentNode.scoreSums[player] += result[player];
            }
            currentNode = currentNode.parent;
        }
    }

    /**
     * Final move selection implementing "Max child" strategy
     * where the max child is the child that has the highest value
     * @param root
     * @return
     */
    private Move finalMoveSelection(Node root) {
        Node bestChild = null;
        final int mover = root.context.state().mover();
        double bestValue = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < root.children.size(); ++i) {
            final Node child = root.children.get(i);
            final double childValue = child.scoreSums[mover] / child.visitCount;

            if (childValue > bestValue) {
                bestValue = childValue;
                bestChild = child;
            }
            if (bestChild == null){
                bestValue = childValue;
                bestChild = child;
            }
        }

        return bestChild.moveFromParent;
    }

    @Override
    public void initAI(final Game game, final int playerID)
    {
        this.player = playerID;
        this.analysisReport = null;
    }

    @Override
    public boolean supportsGame(final Game game) {
        return !game.isStochasticGame() && !game.hiddenInformation() && game.isAlternatingMoveGame();
    }

    public int getIterations(){
        return this.iterations;
    }

    protected void updateIterations(int iterations){
        this.iterations += iterations;
    }

    protected void resetIterations(){
        this.iterations = 0;
    }

    public String generateAnalysisReport() {
        return this.analysisReport==null ? "No analysis generated" : this.analysisReport;
    }

    //-------------------------------------------------------------------------

    private static class Gram{
        private final Hashtable<Integer, Gram> previousGram;

        private int visitCount;

        private double[] scoreSums;

        public Gram(final Move move, final Gram nextGram, final double[] scoreSums){
            this.previousGram = new Hashtable<>();
            this.visitCount = 1;
            this.scoreSums = scoreSums;

            if (nextGram != null){
                nextGram.previousGram.put(move.hashCode(), this);
            }
        }

        public double MoverScoreSums(int mover){
            return scoreSums[mover] / visitCount;
        }

        public void UpdateScoreSums(final double[] scoreSums){
            visitCount += 1;
            for (int i = 0; i < scoreSums.length; ++i){
                this.scoreSums[i] += scoreSums[i];
            }
        }
    }

    /**
     * Inner class for nodes used by MCTS,
     *
     * @author Dennis Soemers
     */
    private static class Node {
        /**
         * Our parent node
         */
        private final MCTS_NSTv2_Tuned.Node parent;

        /**
         * The move that led from parent to this node
         */
        private final Move moveFromParent;

        /**
         * This objects contains the game state for this node (this is why we don't support stochastic games)
         */
        private final Context context;

        /**
         * Visit count for this node
         */
        private int visitCount = 0;

        /**
         * For every player, sum of utilities / scores backpropagated through this node
         */
        private final double[] scoreSums;

        /**
         * Child nodes
         */
        private final List<MCTS_NSTv2_Tuned.Node> children = new ArrayList<MCTS_NSTv2_Tuned.Node>();

        /**
         * List of moves for which we did not yet create a child node
         */
        private final FastArrayList<Move> unexpandedMoves;

        /**
         * Constructor
         *
         * @param parent
         * @param moveFromParent
         * @param context
         */
        public Node(final MCTS_NSTv2_Tuned.Node parent, final Move moveFromParent, final Context context) {
            this.parent = parent;
            this.moveFromParent = moveFromParent;
            this.context = context;
            final Game game = context.game();
            scoreSums = new double[game.players().count() + 1];

            // For simplicity, we just take ALL legal moves.
            // This means we do not support simultaneous-move games.
            unexpandedMoves = new FastArrayList<Move>(game.moves(context).moves());

            if (parent != null)
                parent.children.add(this);
        }

    }

    //-------------------------------------------------------------------------
}
