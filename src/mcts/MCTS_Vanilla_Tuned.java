package mcts;

import Group12.Group12AI;
import game.Game;
import main.collections.FastArrayList;
import util.AI;
import util.Context;
import util.Move;
import utils.AIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MCTS_Vanilla_Tuned extends Group12AI {

    //-------------------------------------------------------------------------

    /** Our player index */
    protected int player = -1;
    protected String analysisReport;
    protected int lastNumPlayoutActions;
    protected int iterations = 0;
    //-------------------------------------------------------------------------

    /**
     * Constructor
     */
    public MCTS_Vanilla_Tuned()
    {
        this.friendlyName = "MCTS Vanilla UCB1 Tuned";
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
        Move bestMove = finalMoveSelection(root);
        updateIterations(numIterations);
        // Return best move to play from root
        return bestMove;
    }

    private Move MCTSMaxN(Game game, Context context, double maxSeconds, int maxIterations, int maxDepth, int startDepth) {
        // NOT CURRENTLY SUPPORTED - STILL TRYING TO FIGURE OUT HOW MAXN SEARCH TREES WOULD WORK WITH LUDII
        // initialize Monte-Carlo Tree
        Node root = new Node(null, null, context);

        // calculate time to stop search in milliseconds
        final long stopTime = (maxSeconds > 0.0) ? System.currentTimeMillis() + (long) (maxSeconds * 1000L) : Long.MAX_VALUE;
        final int maxIts = (maxIterations >= 0) ? maxIterations : Integer.MAX_VALUE;
        final int numPlayers = game.players().count();

        int numIterations = 0;
        // keep searching until running out of time (ExampleUCT)
        while(numIterations < maxIts && 					// Respect iteration limit
                System.currentTimeMillis() < stopTime && 	// Respect time limit
                !wantsInterrupt								// Respect GUI user clicking the pause button
        ){
            Node currentNode = RandomSelection(root);
            // A simulated game is played
            double[] result = PlayOut(currentNode);
            // A node is added
            Expand(currentNode);
            // The result is backpropagated
            Backpropagation(currentNode, result);
            numIterations++;
        }
        Move bestMove = finalMoveSelection(root);
        System.out.println("timeout");

        // Return legacy.random move
        return bestMove;

    }

    private Node Selection(Node currentNode){
//        Node current = currentNode;
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

        Node bestChild = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        final double parentLog = Math.log(currentNode.visitCount);
        final double twoParentLog = 2.0 * Math.log(Math.max(1, currentNode.visitCount));

        final int mover = currentNode.context.state().mover();

        for(int i = 0; i < currentNode.children.size(); i++){
            final Node child = currentNode.children.get(i);
//            final double childValue = child.scoreSums[player] / child.visitCount;
            final double childValue = child.scoreSums[mover] / child.visitCount;
            final double variance = childValue * (1-childValue);
            final double ucbValue = childValue + Math.sqrt((parentLog / child.visitCount) * Math.min(.25, variance + Math.sqrt(2 * parentLog/ child.visitCount)));

//            final double exploit = child.scoreSums[mover] / child.visitCount;
//            final double explore = Math.sqrt(twoParentLog / child.visitCount);
//            final double ucb1Value = exploit + explore;
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
                currentNode = new MCTS_Vanilla_Tuned.Node(currentNode, move, context);
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

    // ExampleUCT line 89
    private double[] PlayOut(Node currentNode) {
        Context contextEnd = currentNode.context;
        Game game = contextEnd.game();
        if (!contextEnd.trial().over())
        {
            // Run a playout if we don't already have a terminal game state in node
            contextEnd = new Context(contextEnd);
            game.playout
                    (
                            contextEnd,
                            null,
                            -1.0,
                            null,
                            null,
                            0,
                            -1,
                            0.f,
                            ThreadLocalRandom.current()
                    );
        }
        // This computes utilities for all players at the of the playout,
        // which will all be values in [-1.0, 1.0]
        return AIUtils.utilities(contextEnd);
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


    public int getNumPlayoutActions() {
        return this.lastNumPlayoutActions;
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
        private final MCTS_Vanilla_Tuned.Node parent;

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
        private final List<MCTS_Vanilla_Tuned.Node> children = new ArrayList<MCTS_Vanilla_Tuned.Node>();

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
        public Node(final MCTS_Vanilla_Tuned.Node parent, final Move moveFromParent, final Context context) {
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
