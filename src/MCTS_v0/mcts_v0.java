package MCTS_v0;

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

        // keep searching until running out of time
        while(timeLeft(stopTime)){
            Node currentNode = root;
            Node lastNode = null;

            // The tree is traversed
            while(currentNode == root || lastNode.children.contains(currentNode)){  // Current node is in search tree
                lastNode = currentNode;
                currentNode = Select(currentNode);
            }
            // A simulated game is played
            float result = PlayOut(currentNode);
            // A node is added
            lastNode = Expand(lastNode, currentNode);
            // The result is backpropagated
            currentNode = lastNode;
            while (currentNode.parent != null){
                Backpropagation(currentNode, result);
                currentNode = currentNode.parent;
            }
        }
        Move bestMove = getBestMove(root);
        System.out.println("timeout");



        // Return random move
        return RandomMove(game, context);

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
