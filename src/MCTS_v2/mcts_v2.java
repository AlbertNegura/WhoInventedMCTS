package MCTS_v2;

import game.Game;
import main.collections.FastArrayList;
import util.AI;
import util.Context;
import util.Move;
import util.Trial;
import utils.AIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

//Playout policies
//utilities

public class mcts_v2 extends AI {

    //-------------------------------------------------------------------------

    /** Our player index */
    protected int player = -1;
    protected String analysisReport;
    protected int lastNumPlayoutActions;

    //-------------------------------------------------------------------------

    /**
     * Constructor
     */
    public mcts_v2()
    {
        this.friendlyName = "MCTS v1";
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
            Node currentNode = SelectionUCT(root);
            // A simulated game is played
            double[] result = PlayOut(currentNode);
            // A node is added
            Expand(currentNode);
            // The result is backpropagated
            Backpropagation(currentNode, result);
            numIterations++;
        }
        Move bestMove = finalMoveSelection(root);

        // Return random move
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

        // Return random move
        return bestMove;

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
            return new Node(currentNode, move, context);
        }

        return SelectBestChild(currentNode);
    }

    private Node SelectBestChild(Node currentNode){
        final double C = 0.04f;

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
                currentNode = new mcts_v2.Node(currentNode, move, context);
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
//            parentNode.unexpandedMoves.remove(parentNode.unexpandedMoves.indexOf(currentNode.moveFromParent));
        }
    }

    // ExampleUCT line 113
    private void Backpropagation(Node currentNode, double[] result) {
        while (currentNode != null){
            currentNode.visitCount++;
            for (int player = 0; player < currentNode.context.game().players().count(); player++) {
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
        double bestValue = Double.MIN_VALUE;

        for (int i = 0; i < root.children.size(); ++i) {
            final Node child = root.children.get(i);
            final int mover = child.context.state().mover();
            final double childValue = child.scoreSums[mover] / child.visitCount;

            if (childValue > bestValue) {
                bestValue = childValue;
                bestChild = child;
            }

        }

        return bestChild.moveFromParent;
    }

    private boolean timeLeft(long stopTime) {
        return System.currentTimeMillis() < stopTime;
    }

    // Get random move
    private Move RandomMove(Game game, Context context) {

        FastArrayList<Move> legalMoves = game.moves(context).moves();

        // If we're playing a simultaneous-move game, some of the legal moves may be
        // for different players. Extract only the ones that we can choose.
        if (!game.isAlternatingMoveGame())
            legalMoves = AIUtils.extractMovesForMover(legalMoves, player);

        final int r = ThreadLocalRandom.current().nextInt(legalMoves.size());
        return legalMoves.get(r);
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
        private final mcts_v2.Node parent;

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
        private final List<mcts_v2.Node> children = new ArrayList<mcts_v2.Node>();

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
        public Node(final mcts_v2.Node parent, final Move moveFromParent, final Context context) {
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
