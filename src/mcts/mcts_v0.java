package mcts;

import game.Game;
import main.collections.FastArrayList;
import util.AI;
import util.Context;
import util.Move;
import utils.AIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class mcts_v0 extends AI {

    //-------------------------------------------------------------------------

    /** Our player index */
    protected int player = -1;

    //-------------------------------------------------------------------------

    /**
     * Constructor
     */
    public mcts_v0()
    {
        this.friendlyName = "MCTS v0";
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
            Node currentNode = Select(root);
            // A simulated game is played
            double[] result = PlayOut(currentNode);
            // A node is added
            Expand(currentNode);
            // The result is backpropagated
            Backpropagation(currentNode, result);
            numIterations++;
        }
        Move bestMove = getBestMove(root);
        System.out.println("timeout");

        // Return random move
        return bestMove;
    }

    // RANDOM SELECTION STRATEGY
    private Node Select(Node currentNode) {

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
                currentNode = new mcts_v0.Node(currentNode, move, context);
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

    // ExampleUCT finalMoveSelection method
    private Move getBestMove(Node root) {
        mcts_v0.Node bestChild = null;
        int bestVisitCount = Integer.MIN_VALUE;
        int numBestFound = 0;

        final int numChildren = root.children.size();

        for (int i = 0; i < numChildren; ++i) {
            final mcts_v0.Node child = root.children.get(i);
            final int visitCount = child.visitCount;

            if (visitCount > bestVisitCount) {
                bestVisitCount = visitCount;
                bestChild = child;
                numBestFound = 1;
            }
            else if (visitCount == bestVisitCount && ThreadLocalRandom.current().nextInt() % ++numBestFound == 0) {
                // this case implements random tie-breaking
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
        private final mcts_v0.Node parent;

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
        private final List<mcts_v0.Node> children = new ArrayList<mcts_v0.Node>();

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
        public Node(final mcts_v0.Node parent, final Move moveFromParent, final Context context) {
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
