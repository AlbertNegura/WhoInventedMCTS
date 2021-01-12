package mcts.legacy;

import game.Game;
import main.collections.FastArrayList;
import util.AI;
import util.Context;
import util.Move;
import utils.AIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MCTS_NST extends AI {

    //-------------------------------------------------------------------------

    /** Our player index */
    protected int player = -1;
    protected String analysisReport;
    protected ArrayList<Sequence> Gram1;
    protected ArrayList<Sequence> Gram2;
    protected ArrayList<Sequence> Gram3;

    protected final double eps = 0.99;
    protected final double decayFactor = 0.9;
    protected double playedMoves = 0;

    //-------------------------------------------------------------------------

    /**
     * Constructor
     */
    public MCTS_NST()
    {
        this.friendlyName = "MCTS NST";
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

        // calculate time to stop search in milliseconds
        final long stopTime = (maxSeconds > 0.0) ? System.currentTimeMillis() + (long) (maxSeconds * 1000L) : Long.MAX_VALUE;
        final int maxIts = (maxIterations >= 0) ? maxIterations : Integer.MAX_VALUE;

        int numIterations = 0;
        // keep searching until running out of time (ExampleUCT)
        while(numIterations < maxIts && 					// Respect iteration limit
                System.currentTimeMillis() < stopTime && 	// Respect time limit
                !wantsInterrupt								// Respect GUI user clicking the pause button
        ){
            Node selectedNode = Selection(root);
            // A simulated game is played
            double[] result = PlayOut(selectedNode, "nst");
            // The result is backpropagated
            Backpropagation(selectedNode, result);
            numIterations++;
        }
        Move bestMove = finalMoveSelection(root);

        ++playedMoves;
//        DiscountNGrams();
        // Return best move to play from root
        return bestMove;
    }

//    private Move MCTSMaxN(Game game, Context context, double maxSeconds, int maxIterations, int maxDepth, int startDepth) {
//        // NOT CURRENTLY SUPPORTED - STILL TRYING TO FIGURE OUT HOW MAXN SEARCH TREES WOULD WORK WITH LUDII
//        // initialize Monte-Carlo Tree
//        Node root = new Node(null, null, context);
//
//        // calculate time to stop search in milliseconds
//        final long stopTime = (maxSeconds > 0.0) ? System.currentTimeMillis() + (long) (maxSeconds * 1000L) : Long.MAX_VALUE;
//        final int maxIts = (maxIterations >= 0) ? maxIterations : Integer.MAX_VALUE;
//        final int numPlayers = game.players().count();
//
//        int numIterations = 0;
//        // keep searching until running out of time (ExampleUCT)
//        while(numIterations < maxIts && 					// Respect iteration limit
//                System.currentTimeMillis() < stopTime && 	// Respect time limit
//                !wantsInterrupt								// Respect GUI user clicking the pause button
//        ){
//            Node currentNode = RandomSelection(root);
//            // A simulated game is played
//            double[] result = PlayOut(currentNode);
//            // A node is added
//            Expand(currentNode);
//            // The result is backpropagated
//            Backpropagation(currentNode, result);
//            numIterations++;
//        }
//        Move bestMove = finalMoveSelection(root);
//        System.out.println("timeout");
//
//        // Return legacy.random move
//        return bestMove;
//
//    }

    private Node Selection(Node currentNode){
        // Traverse tree
        while (true) {
            if (currentNode.context.trial().over()) {
                // We've reached a terminal state
                break;
            }

            currentNode = SelectionUCT(currentNode);

            if (currentNode.visitCount == 0) {
                // We've expanded a new node, time for playout!
                break;
            }
        }
        return currentNode;
    }

    private Node SelectionUCT(Node currentNode) {
        // If there is any unexpanded move from the current node...
        if (!currentNode.unexpandedMoves.isEmpty())
        {
            // ... randomly select an unexpanded move
            final Move move = currentNode.unexpandedMoves.remove(
                    ThreadLocalRandom.current().nextInt(currentNode.unexpandedMoves.size()));

            // create a copy of context
            final Context context = new Context(currentNode.context);

            // apply the move
            context.game().apply(context, move);

            // create new node and return it
            // This is EXPANSION already.
            return new Node(currentNode, move, context);
        }

        return BestChild(currentNode);
    }

    private Node BestChild(Node currentNode){
        final double C = 0.4f;

        Node bestChild = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        final double parentLog = Math.log(currentNode.visitCount);

        final int mover = currentNode.context.state().mover();

        for(int i = 0; i < currentNode.children.size(); i++){
            final Node child = currentNode.children.get(i);
            final double childValue = child.scoreSums[mover] / child.visitCount;
            final double ucbValue = childValue + C * Math.sqrt(parentLog / child.visitCount);

            if(ucbValue > bestValue) {
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
                currentNode = new MCTS_NST.Node(currentNode, move, context);
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

    private double[] PlayOut(Node currentNode,String strategy){
        if(strategy.equals("legacy/random")){
            Context contextEnd = currentNode.context;
            Game game = contextEnd.game();

            int count = 0;
            while (!contextEnd.trial().over() || count > 10000){
                count++;
                contextEnd = new Context(contextEnd);

                FastArrayList<Move> legalMoves = getLegalMoves(game, contextEnd);

                final int r = ThreadLocalRandom.current().nextInt(legalMoves.size());
                Move selectedMove = legalMoves.get(r);

                game.apply(contextEnd, selectedMove);
            }

            // This computes utilities for all players at the of the playout,
            // which will all be values in [-1.0, 1.0]
            return AIUtils.utilities(contextEnd);
        }

        if (strategy.equals("nst")){
            Context contextEnd = currentNode.context;
            Game game = contextEnd.game();

            // Get history of moves from playout game.
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

                        double scoreGram1 = Double.MAX_VALUE;
                        double scoreGram2 = 0.0d;
                        double scoreGram3 = 0.0d;
                        int foundGrams = 1;

                        for (int j = 0; j < Gram1.size(); j++) {
                            if (Gram1.get(j).moves.get(0) == evaluatingMove) {
                                scoreGram1 = (Gram1.get(j).scoreSums[mover] * Math.pow(decayFactor, playedMoves)) / Gram1.get(j).visitCount;
                                break;
                            }
                        }

                        if (history.size() - 1 >= 0) {
                            for (int j = 0; j < Gram2.size(); j++) {
                                if (Gram2.get(j).visitCount > 10 &&
                                        Gram2.get(j).moves.get(1) == evaluatingMove &&
                                        Gram2.get(j).moves.get(0) == history.get(history.size() - 1)) {
                                    scoreGram2 = (Gram1.get(j).scoreSums[mover] * Math.pow(decayFactor, playedMoves)) / Gram2.get(j).visitCount;
                                    ++foundGrams;
                                    break;
                                }
                            }
                        }

                        if (history.size() - 2 >= 0) {
                            for (int j = 0; j < Gram3.size(); j++) {
                                if (Gram3.get(j).visitCount > 10 &&
                                        Gram3.get(j).moves.get(2) == evaluatingMove &&
                                        Gram3.get(j).moves.get(1) == history.get(history.size() - 1) &&
                                        Gram3.get(j).moves.get(0) == history.get(history.size() - 2)) {
                                    scoreGram3 = (Gram1.get(j).scoreSums[mover] * Math.pow(decayFactor, playedMoves)) / Gram3.get(j).visitCount;
                                    ++foundGrams;
                                    break;
                                }
                            }
                        }

                        double moveScore = (scoreGram1 + scoreGram2 + scoreGram3) / foundGrams;

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
//            System.out.println("COMPLET PLAYOUT");

            double[] result = AIUtils.utilities(contextEnd);
            final int playersCount = currentNode.context.game().players().count()+1;

            // Extract the sequences that appeared in the simulated tree.
            // Traverse history of moves from last to first
            for(int i = history.size()-1; i >= 0; i--){
                boolean exists = false;
                List<Move> movesSequence = new ArrayList<>();
                movesSequence.add(history.get(i));
                // Find the played move in the Gram1 list
                for(int j = 0; j < Gram1.size(); j++){
                    if (Gram1.get(j).moves == movesSequence){
                        for (int player = 1; player < playersCount; player++) {
                            Gram1.get(j).scoreSums[player] += result[player];
                        }
                        Gram1.get(j).Visit();

                        exists = true;
                        break;
                    }
                }

                // If the sequence was not stored in the Gram1 list...
                if(!exists){
                    // ... add the sequence...
                    Gram1.add(new Sequence(movesSequence, playersCount));
                }

                if(i - 1 >= 0) {
                    movesSequence = new ArrayList<>();
                    movesSequence.add(history.get(i - 1));
                    movesSequence.add(history.get(i));
                    // If the sequence was stored in Gram1 there might be also in Gram2
                    if(exists){
                        exists = false;
                        for (int j = 0; j < Gram2.size(); j++) {
                            if (Gram2.get(j).moves.get(1) == history.get(i) &&
                                    Gram2.get(j).moves.get(0) == history.get(i-1)) {
                                for (int player = 1; player < playersCount; player++) {
                                    Gram2.get(j).scoreSums[player] += result[player];
                                }
                                Gram2.get(j).Visit();

                                exists = true;
                                break;
                            }
                        }
                    }
                    // If the sequence was not stored in the Gram2 list...
                    if (!exists) {
                        // ... add the sequence.
                        Gram2.add(new Sequence(movesSequence, playersCount));
                    }

                    if(i - 2 >= 0){
                        movesSequence = new ArrayList<>();
                        movesSequence.add(history.get(i-2));
                        movesSequence.add(history.get(i-1));
                        movesSequence.add(history.get(i));
                        // If the sequence was stored in Gram2 there might be also in Gram3
                        if (exists){
                            exists = false;
                            for(int j = 0; j < Gram3.size(); j++){
                                if (Gram3.get(j).moves.get(2) == history.get(i) &&
                                        Gram3.get(j).moves.get(1) == history.get(i-1) &&
                                        Gram3.get(j).moves.get(0) == history.get(i-2)){
                                    for (int player = 1; player < playersCount; player++) {
                                        Gram3.get(j).scoreSums[player] += result[player];
                                    }
                                    Gram3.get(j).Visit();
                                    exists = true;
                                    break;
                                }
                            }
                        }

                        // If the sequence was not stored in the Gram2 list...
                        if(!exists){
                            // ... add the sequence...
                            Gram3.add(new Sequence(movesSequence, playersCount));
                        }
                    }
                }
            }
            return AIUtils.utilities(contextEnd);
        }

        // This is a dummy return.
        System.out.println("invalid strategy");
        return AIUtils.utilities(currentNode.context);
    }
    
    public void DiscountNGrams(){
        float gamma = 0.9f;
        for (int i = 0; i < Gram1.size(); i++){
            for(int player = 0; player < Gram1.get(i).scoreSums.length; player++){
                Gram1.get(i).scoreSums[player] = Gram1.get(i).scoreSums[player] * gamma;
            }
        }
        for (int i = 0; i < Gram2.size(); i++){
            for(int player = 0; player < Gram2.get(i).scoreSums.length; player++){
                Gram2.get(i).scoreSums[player] = Gram2.get(i).scoreSums[player] * gamma;
            }        }
        for (int i = 0; i < Gram3.size(); i++){
            for(int player = 0; player < Gram3.get(i).scoreSums.length; player++) {
                Gram3.get(i).scoreSums[player] = Gram3.get(i).scoreSums[player] * gamma;
            }
        }
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
        }

        return bestChild.moveFromParent;
    }

    @Override
    public void initAI(final Game game, final int playerID)
    {
        this.player = playerID;
        this.analysisReport = null;

        this.Gram1 = new ArrayList<>();
        this.Gram2 = new ArrayList<>();
        this.Gram3 = new ArrayList<>();

        this.playedMoves = 0;
    }

    @Override
    public boolean supportsGame(final Game game) {
        return !game.isStochasticGame() && !game.hiddenInformation() && game.isAlternatingMoveGame();
    }


    public String generateAnalysisReport() {
        return this.analysisReport==null ? "No analysis generated" : this.analysisReport;
    }

    //-------------------------------------------------------------------------

    private static class Sequence{
        public int visitCount;
        public List<Move> moves;
        public double[] scoreSums;

        public Sequence(List<Move> moves, int players){
            this.moves = moves;
            this.visitCount = 1;
            this.scoreSums = new double[players];
        }

        public void Visit(){ ++visitCount; }
    }

    //-------------------------------------------------------------------------

    /**
     * Inner class for nodes used by MCTS,
     *
     * @author Dennis Soemers
     */
    private static class Node {
        /**
         * Our parent node
         */
        private final MCTS_NST.Node parent;

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
        private final List<MCTS_NST.Node> children = new ArrayList<MCTS_NST.Node>();

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
        public Node(final MCTS_NST.Node parent, final Move moveFromParent, final Context context) {
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
