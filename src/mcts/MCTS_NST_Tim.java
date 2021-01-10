package mcts;

import game.Game;
import game.rules.play.moves.Moves;
import game.rules.play.moves.nonDecision.effect.requirement.Do;
import main.collections.FastArrayList;
import util.AI;
import util.Context;
import util.Move;
import util.Trial;
import utils.AIUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MCTS_NST_Tim extends AI {

    //-------------------------------------------------------------------------

    /**
     * Our player index
     */
    protected int player = -1;
    protected String analysisReport;
    protected ArrayList<Sequence> Gram1;
    protected ArrayList<Sequence> Gram2;
    protected ArrayList<Sequence> Gram3;
    protected Map hashMap = new HashMap();


    protected final double eps = 0.9;
    protected final double decayFactor = 0.9;
    protected double playedMoves = 0;

    //-------------------------------------------------------------------------

    /**
     * Constructor
     */
    public MCTS_NST_Tim() {
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
        while (numIterations < maxIts &&                    // Respect iteration limit
                System.currentTimeMillis() < stopTime &&    // Respect time limit
                !wantsInterrupt                                // Respect GUI user clicking the pause button
        ) {
            Node selectedNode = Selection(root);
            // A simulated game is played
            double[] result = PlayOut(selectedNode, "nst");
            // The result is backpropagated
            Backpropagation(selectedNode, result);
            numIterations++;
        }
        Move bestMove = finalMoveSelection(root);
        hashMap = new HashMap();
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
//        // Return random move
//        return bestMove;
//
//    }

    private Node Selection(Node currentNode) {
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
        if (!currentNode.unexpandedMoves.isEmpty()) {
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

    private Node BestChild(Node currentNode) {
        final double C = 0.4f;

        Node bestChild = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        final double parentLog = Math.log(currentNode.visitCount);

        final int mover = currentNode.context.state().mover();

        for (int i = 0; i < currentNode.children.size(); i++) {
            final Node child = currentNode.children.get(i);
            final double childValue = child.scoreSums[mover] / child.visitCount;
            final double ucbValue = childValue + C * Math.sqrt(parentLog / child.visitCount);

            if (ucbValue > bestValue) {
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
                currentNode = new MCTS_NST_Tim.Node(currentNode, move, context);
            } else if (!currentNode.children.isEmpty()) {
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

    private FastArrayList<Move> getLegalMoves(Game game, Context context) {
        FastArrayList<Move> legalMoves = game.moves(context).moves();

        // If we're playing a simultaneous-move game, some of the legal moves may be
        // for different players. Extract only the ones that we can choose.
        if (!game.isAlternatingMoveGame())
            legalMoves = AIUtils.extractMovesForMover(legalMoves, player);
        return legalMoves;
    }

    private double[] PlayOut(Node currentNode, String strategy) {
        Context contextEnd = currentNode.context;
        Game game = contextEnd.game();
        ;
        List<Move> history = new ArrayList<>();
        while (!contextEnd.trial().over()) {
            contextEnd = new Context(contextEnd);
            FastArrayList<Move> legalMoves = getLegalMoves(game, contextEnd);
            final int mover = contextEnd.state().mover();
            int numBestFound = 0;

            Move bestMove = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            int bestOneHash = 0;
            int bestTwoHash = 0;
            int bestThreeHash = 0;
            for (int m = 0; m < legalMoves.size(); m++) {
                Move evaluatingMove = legalMoves.get(m);
                double scoreGram1 = 100;
                double scoreGram2 = 0.0d;
                double scoreGram3 = 0.0d;
                int foundGrams = 1;

                int oneGramHash = evaluatingMove.hashCode();
                int twoGramHash = 0;
                int threeGramHash = 0;
                if (history.size() == 1) {
                    twoGramHash = history.get(0).hashCode() ^ evaluatingMove.hashCode();
                } else if (history.size() > 1) {
                    threeGramHash = history.get(history.size() - 2).hashCode() ^ history.get(history.size() - 1).hashCode() ^ evaluatingMove.hashCode();
                    twoGramHash = history.get(history.size() - 1).hashCode() ^ evaluatingMove.hashCode();
                }
                Map hmValues = (Map) hashMap.get(oneGramHash);
                if (hmValues != null) {
                    double[] result = (double[]) hmValues.get("results");
                    int visitCount = (int) hmValues.get("visitCount");
                    if (visitCount > 6) {
                        scoreGram1 = (result[mover]) / visitCount;
                    }
                }
                if (hashMap.containsKey(twoGramHash)) {
                    hmValues = (Map) hashMap.get(twoGramHash);
                    double[] result = (double[]) hmValues.get("results");
                    int visitCount = (int) hmValues.get("visitCount");
                    if (visitCount > 6) {
                        scoreGram2 = (result[mover]) / visitCount;
                        ++foundGrams;
                    }
                }
                if (hashMap.containsKey(threeGramHash)) {
                    hmValues = (Map) hashMap.get(threeGramHash);
                    double[] result = (double[]) hmValues.get("results");
                    int visitCount = (int) hmValues.get("visitCount");
                    if (visitCount > 6) {
                        scoreGram3 = (result[mover]) / visitCount;
                        ++foundGrams;
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
            history.add(bestMove);
            game.apply(contextEnd, bestMove);
        }
        double[] result = AIUtils.utilities(contextEnd);
        final int playersCount = currentNode.context.game().players().count() + 1;

        for (int i = history.size() - 1; i >= 0; i--) {
            int oneGramHash = history.get(i).hashCode();
            Map hmValues = (Map) hashMap.get(oneGramHash);
            if (hmValues == null) {
                Map input = new HashMap();
                input.put("visitCount", 1);
                input.put("results", result);
                hashMap.put(oneGramHash, input);
            } else {
//                Map hmValues = (Map) hashMap.get(oneGramHash);
                int visitCount = (int) hmValues.get("visitCount") + 1;
                double[] scoredResult = (double[]) hmValues.get("results");
                for (int player = 0; player < playersCount; player++) {
                    scoredResult[player] += result[player];
                }
                Map input = new HashMap();
                input.put("visitCount", visitCount);
                input.put("results", scoredResult);
                hashMap.put(oneGramHash, input);
            }
            if (i - 1 >= 0) {
                int twoGramHash = history.get(i - 1).hashCode() ^ history.get(i).hashCode();
                if (!hashMap.containsKey(twoGramHash)) {
                    Map input = new HashMap();
                    input.put("visitCount", 1);
                    input.put("results", result);
                    hashMap.put(twoGramHash, input);
                } else {
                    hmValues = (Map) hashMap.get(twoGramHash);
                    int visitCount = (int) hmValues.get("visitCount") + 1;
                    double[] scoredResult = (double[]) hmValues.get("results");
                    for (int player = 0; player < playersCount; player++) {
                        scoredResult[player] += result[player];
                    }
                    Map input = new HashMap();
                    input.put("visitCount", visitCount);
                    input.put("results", scoredResult);
                    hashMap.put(twoGramHash, input);
                }
            }
            if (i - 2 >= 0) {
                int threeGramHash = history.get(i - 2).hashCode() ^ history.get(i - 1).hashCode() ^ history.get(i).hashCode();
                if (!hashMap.containsKey(threeGramHash)) {
                    Map input = new HashMap();
                    input.put("visitCount", 1);
                    input.put("results", result);
                    hashMap.put(threeGramHash, input);
                } else {
                    hmValues = (Map) hashMap.get(threeGramHash);
                    int visitCount = (int) hmValues.get("visitCount") + 1;
                    double[] scoredResult = (double[]) hmValues.get("results");
                    for (int player = 0; player < playersCount; player++) {
                        scoredResult[player] += result[player];
                    }
                    Map input = new HashMap();
                    input.put("visitCount", visitCount);
                    input.put("results", scoredResult);
                    hashMap.put(threeGramHash, input);
                }
            }
        }

        return AIUtils.utilities(contextEnd);
    }

    public void DiscountNGrams() {
        float gamma = 0.9f;
        for (int i = 0; i < Gram1.size(); i++) {
            for (int player = 0; player < Gram1.get(i).scoreSums.length; player++) {
                Gram1.get(i).scoreSums[player] = Gram1.get(i).scoreSums[player] * gamma;
            }
        }
        for (int i = 0; i < Gram2.size(); i++) {
            for (int player = 0; player < Gram2.get(i).scoreSums.length; player++) {
                Gram2.get(i).scoreSums[player] = Gram2.get(i).scoreSums[player] * gamma;
            }
        }
        for (int i = 0; i < Gram3.size(); i++) {
            for (int player = 0; player < Gram3.get(i).scoreSums.length; player++) {
                Gram3.get(i).scoreSums[player] = Gram3.get(i).scoreSums[player] * gamma;
            }
        }
    }

    private void Expand(Node currentNode) {
        Node parentNode = currentNode.parent;
        if (parentNode != null) {
            parentNode.children.add(currentNode);
        }
    }

    // ExampleUCT line 113
    private void Backpropagation(Node currentNode, double[] result) {
        final int playersCount = currentNode.context.game().players().count();
        while (currentNode != null) {
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
     *
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
    public void initAI(final Game game, final int playerID) {
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
        return this.analysisReport == null ? "No analysis generated" : this.analysisReport;
    }

    //-------------------------------------------------------------------------

    private static class Sequence {
        public int visitCount;
        public List<Move> moves;
        public double[] scoreSums;

        public Sequence(List<Move> moves, int players) {
            this.moves = moves;
            this.visitCount = 1;
            this.scoreSums = new double[players];
        }

        public void Visit() {
            ++visitCount;
        }
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
        private final MCTS_NST_Tim.Node parent;

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
        private final List<MCTS_NST_Tim.Node> children = new ArrayList<MCTS_NST_Tim.Node>();

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
        public Node(final MCTS_NST_Tim.Node parent, final Move moveFromParent, final Context context) {
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
