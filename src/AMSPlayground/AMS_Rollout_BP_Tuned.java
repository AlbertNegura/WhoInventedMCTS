package AMSPlayground;

import Group12.Group12AI;
import game.Game;
import main.collections.FVector;
import main.collections.FastArrayList;
import metadata.ai.Ai;
import metadata.ai.heuristics.Heuristics;
import metadata.ai.heuristics.terms.HeuristicTerm;
import metadata.ai.heuristics.terms.Material;
import metadata.ai.heuristics.terms.MobilitySimple;
import metadata.ai.heuristics.transformations.HeuristicTransformation;
import metadata.ai.misc.Pair;
import util.AI;
import util.Context;
import util.Move;
import utils.AIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A simple example implementation of a standard UCT approach.
 * <p>
 * Only supports deterministic, alternating-move games.
 *
 * @author Dennis Soemers
 */
public class AMS_Rollout_BP_Tuned extends Group12AI {

    private Heuristics heuristicValueFunction = null;
    private final boolean heuristicsFromMetadata = true;
    private static int recursiveStackDepth = 0;
//    private final int maxStackDepth = 50000;
    private final int maxStackDepth = Integer.MAX_VALUE;
    protected double autoPlaySeconds = 0.0D;
    protected float estimatedRootScore = 0.0F;
    protected float maxHeuristicEval = 0.0F;
    protected float minHeuristicEval = 0.0F;
    protected String analysisReport = null;
    protected FastArrayList<Move> currentRootMoves = null;
    protected Move lastReturnedMove = null;
    protected Context lastSearchedRootContext = null;
    protected FVector rootValueEstimates = null;
    protected int numPlayersInGame = 0;
    protected int iterations = 0;
    //-------------------------------------------------------------------------

    /**
     * Our player index
     */
    protected int player = -1;

    //-------------------------------------------------------------------------

    /**
     * Constructor
     */
    public AMS_Rollout_BP_Tuned() {
        this.friendlyName = "AMS_Rollout_BP UCB1 Tuned";
    }

    //-------------------------------------------------------------------------

    @Override
    public Move selectAction
            (
                    final Game game,
                    final Context context,
                    final double maxSeconds,
                    final int maxIterations,
                    final int maxDepth
            ) {
        // Start out by creating a new root node (no tree reuse in this example)
        final Node root = new Node(null, null, context);

        // We'll respect any limitations on max seconds and max iterations (don't care about max depth)
        final long stopTime = (maxSeconds > 0.0) ? System.currentTimeMillis() + (long) (maxSeconds * 1000L) : Long.MAX_VALUE;
        final int maxIts = (maxIterations >= 0) ? maxIterations : 10000000;

        int iteration = 0;
        double discountFactor = 1.0;

        recursiveStackDepth = 0;
        resetIterations();
        int[] opponents = new int[game.players().size() - 1];
        int idx = 0;
        for (int p = 1; p <= game.players().size(); ++p) {
            if (p != player) {
                opponents[idx++] = p;
            }
        }

        Context copyContext = new Context(context);
        FastArrayList<Move> legalMoves = game.moves(context).moves();
        double[] values = new double[legalMoves.size()];
        int[] actionCount = new int[legalMoves.size()];
        Game copyGame = game;

        //Initialization
        //Execute each action once and get the result of the action for the current player
        for (int i = 0; i < legalMoves.size(); ++i) {
            copyGame.apply(copyContext, legalMoves.get(i));
            actionCount[i] = 1;

            recursiveStackDepth+=1;
            double returnedValue = 0;
            if(recursiveStackDepth < maxStackDepth) {
                returnedValue = AMS(copyGame, copyContext, maxIts, maxDepth - 1, opponents[0], stopTime)[this.player];
            }
            values[i] = returnedValue;
            copyGame = game;
            ++iteration;
            copyContext = new Context(context);
        }
        //loop
        double[] vHatValuesSum = new double[legalMoves.size()];
        for (int i = 0; i < legalMoves.size(); i++) {
            vHatValuesSum[i] = values[i];
        }
        //Estimation of q value and the UCB value
        double[] qValue = new double[legalMoves.size()];
        double[] qValueUCB = new double[legalMoves.size()];
        copyContext = new Context(context);
        int legalMoveSize = iteration;
        //
        while (iteration < legalMoveSize + maxIts &&
                System.currentTimeMillis() < stopTime) {
            for (int i = 0; i < legalMoves.size(); ++i) {
                copyGame.apply(copyContext, legalMoves.get(i));
                qValue[i] = 0 + discountFactor / actionCount[i] * vHatValuesSum[i];
                double var = vHatValuesSum[i] / actionCount[i];
                double variance = var * (1-var);
                qValueUCB[i] = qValue[i] + Math.sqrt((Math.log(iteration)) / actionCount[i] * Math.min(0.25, variance + Math.sqrt(2 * Math.log(iteration)) / actionCount[i]));
                copyContext = new Context(context);
            }
            // Find best action and sample this action once more
            int bestMoveIndex = maxInteger(qValueUCB);
            actionCount[bestMoveIndex] += 1;
            game.apply(copyContext, legalMoves.get(bestMoveIndex));

            recursiveStackDepth+=1;
            double result = 0;
            if(recursiveStackDepth < maxStackDepth) {
                result = AMS(copyGame, copyContext, maxIts, maxDepth - 1, opponents[0], stopTime)[this.player];
            }

            vHatValuesSum[bestMoveIndex] += result;
            ++iteration;
        }

        copyContext = new Context(context);

        //Get final q values to determine action to take
        for (int i = 0; i < legalMoves.size(); ++i) {
            copyGame.apply(copyContext, legalMoves.get(i));

            qValue[i] = 0 + discountFactor / actionCount[i] * vHatValuesSum[i];
            qValueUCB[i] = qValue[i] * actionCount[i] / iteration;
            copyContext = new Context(context);
        }

        int bestMoveIndex = maxInteger(qValueUCB);
        updateIterations(iteration);
        // Return the move we wish to play
        return legalMoves.get(bestMoveIndex);
    }

    public double[] AMS(Game game, Context context, int maxIterations, int depth, int player, long stopTime) {
        Context copyContext = new Context(context);
        final Node root = new Node(null, null, context);
        Node current = root;
        //Mover equals who's turn it is in tree
        final int mover = current.context.state().mover();
        if (depth == 0 || current.context.trial().over()) {
            double[] result = PlayOut(current);
            return result;
        }

        int iteration = 0;
        double discountFactor = 1.0;

        int[] opponents = new int[game.players().size() - 1];
        int idx = 0;
        for (int p = 1; p <= game.players().size(); ++p) {
            if (p != player) {
                opponents[idx++] = p;
            }
        }


        copyContext = new Context(context);
        FastArrayList<Move> legalMoves = game.moves(context).moves();
        double[][] values = new double[game.players().size()][legalMoves.size()];
        int[] actionCount = new int[legalMoves.size()];
        Game copyGame = game;


        //Initialization Get first Estimation
        for (int i = 0; i < legalMoves.size(); ++i) {
            copyGame.apply(copyContext, legalMoves.get(i));
            actionCount[i] = 1;

            recursiveStackDepth+=1;
            double[] returnedValues = new double[game.players().size()];
            if(recursiveStackDepth < maxStackDepth) {
                returnedValues = AMS(copyGame, copyContext, maxIterations, depth - 1, opponents[0], stopTime);
            }

            values = Backpropagation(current, returnedValues, values, i);
            copyGame = game;
            ++iteration;
            copyContext = new Context(context);
        }
        //To get all the values for all players, to backpropagate properly, it becomes a matrix
        double[][] vHatValuesSum = new double[game.players().size()][legalMoves.size()];
        for (int i = 0; i < legalMoves.size(); i++) {
            for (int p = 0; p < game.players().size(); p++) {
                vHatValuesSum[p][i] = values[p][i];
            }

        }
        double[][] qValue = new double[game.players().size()][legalMoves.size()];
        double[][] qValueUCB = new double[game.players().size()][legalMoves.size()];
        copyContext = new Context(context);
        int legalMoveSize = iteration;
        // Get Q value plus UCB value
        while (iteration < legalMoveSize + maxIterations &&
                System.currentTimeMillis() < stopTime) {
            for (int i = 0; i < legalMoves.size(); ++i) {
                copyGame.apply(copyContext, legalMoves.get(i));
                for (int p = 0; p < game.players().size(); p++) {
                    // Reward equals 0
                    qValue[p][i] = 0 + discountFactor / actionCount[i] * vHatValuesSum[p][i];
                    double var = vHatValuesSum[p][i] / actionCount[i];
                    double variance = var * (1-var);
                    qValueUCB[p][i] = qValue[p][i] + Math.sqrt((Math.log(iteration)) / actionCount[i] * Math.min(0.25, variance + Math.sqrt(2 * Math.log(iteration)) / actionCount[i]));
                }
//                float reward = this.heuristicValueFunction.computeValue(copyContext, this.player, 0.01F) - heuristicScore;

                copyContext = new Context(context);
            }
            // Get best action to perform
            int bestMoveIndex = maxInteger(qValueUCB[mover]);
            actionCount[bestMoveIndex] += 1;
            game.apply(copyContext, legalMoves.get(bestMoveIndex));

            recursiveStackDepth+=1;
            double[] returnedValues = new double[game.players().size()];
            if(recursiveStackDepth < maxStackDepth) {
                returnedValues = AMS(game, copyContext, maxIterations, depth - 1, opponents[0], stopTime);
            }

            vHatValuesSum = Backpropagation(current, returnedValues, vHatValuesSum, bestMoveIndex);
            ++iteration;
            copyContext = new Context(context);
        }

        //Calculate weighted averages to return
        double[] estimatedReturnValue = new double[game.players().size()];
        for (int i = 0; i < legalMoves.size(); ++i) {
            for (int p = 0; p < game.players().size(); p++) {
                estimatedReturnValue[p] += ((double) actionCount[i] / (iteration)) * qValue[p][i];
            }
        }
        updateIterations(iterations);
        return estimatedReturnValue;
    }

    // Find highest number
    public int maxInteger(double[] values) {
        double max_value = Integer.MIN_VALUE;
        int bestInt = 0;
        for (int j = 0; j < values.length; ++j) {
            if (values[j] > max_value) {
                max_value = values[j];
                bestInt = j;
            }
        }
        return bestInt;
    }

    // Do MCTS Playout/Rollout, same as MCTS_Vanilla
    private double[] PlayOut(Node currentNode) {
        Context contextEnd = currentNode.context;
        Game game = contextEnd.game();
        if (!contextEnd.trial().over()) {
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

    //Backpropagate results, similar to MCTS_Vanilla backpropagation
    private double[][] Backpropagation(Node currentNode, double[] result, double[][] values, int action) {

        final int playersCount = currentNode.context.game().players().count();
        double[][] results = values;
        for (int player = 0; player <= playersCount; player++) {
            results[player][action] += result[player];
        }
        return results;
    }


    /**
     * Selects child of the given "current" node according to UCB1 equation.
     * This method also implements the "Expansion" phase of MCTS, and creates
     * a new node if the given current node has unexpanded moves.
     *
     * @param current
     * @return Selected node (if it has 0 visits, it will be a newly-expanded node).
     */
    public static Node select(final Node current) {
        if (!current.unexpandedMoves.isEmpty()) {
            // randomly select an unexpanded move
            final Move move = current.unexpandedMoves.remove(
                    ThreadLocalRandom.current().nextInt(current.unexpandedMoves.size()));

            // create a copy of context
            final Context context = new Context(current.context);

            // apply the move
            context.game().apply(context, move);

            // create new node and return it
            return new Node(current, move, context);
        }

        // use UCB1 equation to select from all children, with legacy.random tie-breaking
        Node bestChild = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        final double twoParentLog = 2.0 * Math.log(Math.max(1, current.visitCount));
        int numBestFound = 0;

        final int numChildren = current.children.size();
        final int mover = current.context.state().mover();

        for (int i = 0; i < numChildren; ++i) {
            final Node child = current.children.get(i);
            final double exploit = child.scoreSums[mover] / child.visitCount;
            final double explore = Math.sqrt(twoParentLog / child.visitCount);

            final double ucb1Value = exploit + explore;

            if (ucb1Value > bestValue) {
                bestValue = ucb1Value;
                bestChild = child;
                numBestFound = 1;
            } else if
            (
                    ucb1Value == bestValue &&
                            ThreadLocalRandom.current().nextInt() % ++numBestFound == 0
            ) {
                // this case implements legacy.random tie-breaking
                bestChild = child;
            }
        }

        return bestChild;
    }

    /**
     * Selects the move we wish to play using the "Robust Child" strategy
     * (meaning that we play the move leading to the child of the root node
     * with the highest visit count).
     *
     * @param rootNode
     * @return
     */
    public static Move finalMoveSelection(final Node rootNode) {
        Node bestChild = null;
        int bestVisitCount = Integer.MIN_VALUE;
        int numBestFound = 0;

        final int numChildren = rootNode.children.size();

        for (int i = 0; i < numChildren; ++i) {
            final Node child = rootNode.children.get(i);
            final int visitCount = child.visitCount;

            if (visitCount > bestVisitCount) {
                bestVisitCount = visitCount;
                bestChild = child;
                numBestFound = 1;
            } else if
            (
                    visitCount == bestVisitCount &&
                            ThreadLocalRandom.current().nextInt() % ++numBestFound == 0
            ) {
                // this case implements legacy.random tie-breaking
                bestChild = child;
            }
        }

        assert bestChild != null;
        return bestChild.moveFromParent;
    }

    @Override
    public void initAI(final Game game, final int playerID) {
        this.player = playerID;
        if (this.heuristicsFromMetadata) {
            Ai aiMetadata = game.metadata().ai();
            if (aiMetadata != null && aiMetadata.heuristics() != null) {
                this.heuristicValueFunction = aiMetadata.heuristics();
            } else {
                this.heuristicValueFunction = new Heuristics(new HeuristicTerm[]{new Material((HeuristicTransformation) null, 1.0F, (Pair[]) null), new MobilitySimple((HeuristicTransformation) null, 0.001F)});
            }
        }

        if (this.heuristicValueFunction != null) {
            this.heuristicValueFunction.init(game);
        }

        this.estimatedRootScore = 0.0F;
        this.maxHeuristicEval = 0.0F;
        this.minHeuristicEval = 0.0F;
        this.analysisReport = null;
        this.currentRootMoves = null;
        this.rootValueEstimates = null;
        this.lastSearchedRootContext = null;
        this.lastReturnedMove = null;
        this.numPlayersInGame = game.players().count();
    }

    @Override
    public boolean supportsGame(final Game game) {
        if (game.isStochasticGame())
            return false;

        if (!game.isAlternatingMoveGame())
            return false;

        return true;
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
    //-------------------------------------------------------------------------

    /**
     * Inner class for nodes used by AMS
     *
     * @author Dennis Soemers
     */
    private static class Node {
        /**
         * Our parent node
         */
        private final Node parent;

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
        private final List<Node> children = new ArrayList<Node>();

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
        public Node(final Node parent, final Move moveFromParent, final Context context) {
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


