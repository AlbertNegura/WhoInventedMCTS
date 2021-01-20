package experiments;

import AMSPlayground.*;
import Group12.Group12AI;
import game.Game;
import game.types.state.GameType;
import main.collections.FastArrayList;
import mcts.*;
import util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A simple tutorial that demonstrates a variety of useful methods provided
 * by the Ludii general game system.
 *
 * @author Dennis Soemers
 */
public class ThreadedExperiments {

    public static void main(final String[] args) throws ExecutionException, InterruptedException {
        // one of the games is "Amazons.lud". Let's load it
        ArrayList<String> games = new ArrayList<String>();
//        games.add("Breakthrough.lud");
        games.add("Skirmish (GDL).lud");
//        games.add("Tic-Tac-Chess.lud");
//        games.add("Reversi.lud");
//        games.add("Hnefatafl.lud");
//        games.add("Nine Holes.lud");
//        games.add("Chinese Checkers.lud");
//        games.add("Latin Square.lud");
//        games.add("Sudoku.lud");
//        games.add("Magic Hexagon.lud");

        ArrayList<Group12AI> mctsPlayers = new ArrayList<>();
        mctsPlayers.add(new AMS_Rollout_BP());
        mctsPlayers.add(new MCTS_Vanilla(0.4));
//        mctsPlayers.add(new MCTS_Vanilla(0.4));
//        mctsPlayers.add(new MCTS_Vanilla(0.4));
//        mctsPlayers.add(new MCTS_Vanilla(0.4));
//        mctsPlayers.add(new MCTS_Vanilla(0.4));
//        mctsPlayers.add(new MCTS_Vanilla(0.4));
//        mctsPlayers.add(new MCTS_Vanilla(0.4));
//        mctsPlayers.add(new MCTS_MAST(0.4));
//        mctsPlayers.add(new MCTS_MAST(0.4));
//        mctsPlayers.add(new MCTS_MAST(0.4));
//        mctsPlayers.add(new MCTS_MAST(0.4));
//        mctsPlayers.add(new MCTS_MAST(0.4));
//        mctsPlayers.add(new MCTS_MAST(0.4));
//        mctsPlayers.add(new MCTS_MAST(0.4));
//        mctsPlayers.add(new MCTS_MAST(0.4));
//        mctsPlayers.add(new MCTS_MAST(0.4));
//        mctsPlayers.add(new MCTS_MAST(0.4));
        mctsPlayers.add(new MCTS_NSTv2());
        mctsPlayers.add(new MCTS_NSTv2());
        mctsPlayers.add(new MCTS_NSTv2());
//        mctsPlayers.add(new MCTS_NSTv2(0.4));
//        mctsPlayers.add(new MCTS_NSTv2(0.4));
//        mctsPlayers.add(new MCTS_NSTv2(0.4));
//        mctsPlayers.add(new MCTS_NSTv2(0.4));
//        mctsPlayers.add(new MCTS_NSTv2(0.4));
//        mctsPlayers.add(new MCTS_NSTv2(0.4));
//        mctsPlayers.add(new MCTS_NSTv2(0.4));
//        mctsPlayers.add(new MCTS_Vanilla_Tuned());
//        mctsPlayers.add(new MCTS_Vanilla_Tuned());
//        mctsPlayers.add(new MCTS_Vanilla_Tuned());
//        mctsPlayers.add(new MCTS_Vanilla_Tuned());
//        mctsPlayers.add(new MCTS_Vanilla_Tuned());
//        mctsPlayers.add(new MCTS_Vanilla_Tuned());
//        mctsPlayers.add(new MCTS_Vanilla_Tuned());
//        mctsPlayers.add(new MCTS_Vanilla_Tuned());
//        mctsPlayers.add(new MCTS_Vanilla_Tuned());
//        mctsPlayers.add(new MCTS_Vanilla_Tuned());
//        mctsPlayers.add(new MCTS_MAST_Tuned(0.4));
//        mctsPlayers.add(new MCTS_MAST_Tuned(0.4));
//        mctsPlayers.add(new MCTS_MAST_Tuned(0.4));
//        mctsPlayers.add(new MCTS_MAST_Tuned(0.4));
//        mctsPlayers.add(new MCTS_MAST_Tuned(0.4));
//        mctsPlayers.add(new MCTS_MAST_Tuned(0.4));
//        mctsPlayers.add(new MCTS_MAST_Tuned(0.4));
//        mctsPlayers.add(new MCTS_MAST_Tuned(0.4));
//        mctsPlayers.add(new MCTS_MAST_Tuned(0.4));
//        mctsPlayers.add(new MCTS_MAST_Tuned(0.4));
//        mctsPlayers.add(new MCTS_NSTv2_Tuned());
//        mctsPlayers.add(new MCTS_NSTv2_Tuned());
//        mctsPlayers.add(new MCTS_NSTv2_Tuned());
//        mctsPlayers.add(new MCTS_NSTv2_Tuned());
//        mctsPlayers.add(new MCTS_NSTv2_Tuned());
//        mctsPlayers.add(new MCTS_NSTv2_Tuned());
//        mctsPlayers.add(new MCTS_NSTv2_Tuned());
//        mctsPlayers.add(new MCTS_NSTv2_Tuned());
//        mctsPlayers.add(new MCTS_NSTv2_Tuned());
//        mctsPlayers.add(new MCTS_NSTv2_Tuned());
//        mctsPlayers.add(new AMSPlayground());
//        mctsPlayers.add(new AMS_Rollout_BP());
//        mctsPlayers.add(new AMS_Rollout_BP_MAST());
//        mctsPlayers.add(new AMS_Rollout_BP_NST());
//        mctsPlayers.add(new AMSPlayground_Tuned());
//        mctsPlayers.add(new AMS_Rollout_BP_Tuned());
//        mctsPlayers.add(new AMS_Rollout_BP_MAST_Tuned());
//        mctsPlayers.add(new AMS_Rollout_BP_NST_Tuned());
//        mctsPlayers.add(new AMS_Tim_Tuned());

        ArrayList<Group12AI> amsPlayers = new ArrayList<>();
        amsPlayers.add(new MCTS_MAST());
        //amsPlayers.add(new AMSPlayground());
//        amsPlayers.add(new AMS_Rollout_BP());
        amsPlayers.add(new AMS_Rollout_BP_MAST());
//        amsPlayers.add(new AMS_Rollout_BP_NST());
//        amsPlayers.add(new AMS_Tim());
//        amsPlayers.add(new AMSPlayground_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_MAST_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_NST_Tuned());
//        amsPlayers.add(new AMS_Tim_Tuned());
        //amsPlayers.add(new AMSPlayground());
//        amsPlayers.add(new AMS_Rollout_BP());
        amsPlayers.add(new AMS_Rollout_BP_MAST());
        amsPlayers.add(new AMS_Rollout_BP_NST());
//        amsPlayers.add(new AMS_Tim());
//        amsPlayers.add(new AMSPlayground_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_MAST_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_NST_Tuned());
//        amsPlayers.add(new AMS_Tim_Tuned());
        //amsPlayers.add(new AMSPlayground());
//        amsPlayers.add(new AMS_Rollout_BP());
//        amsPlayers.add(new AMS_Rollout_BP_MAST());
        amsPlayers.add(new AMS_Rollout_BP_NST());
//        amsPlayers.add(new AMS_Tim());
//        amsPlayers.add(new AMSPlayground_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_MAST_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_NST_Tuned());
//        amsPlayers.add(new AMS_Tim_Tuned());
//        amsPlayers.add(new AMSPlayground());
//        amsPlayers.add(new AMS_Rollout_BP());
//        amsPlayers.add(new AMS_Rollout_BP_MAST());
//        amsPlayers.add(new AMS_Rollout_BP_NST());
//        amsPlayers.add(new AMS_Tim());
//        amsPlayers.add(new AMSPlayground_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_MAST_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_NST_Tuned());
//        amsPlayers.add(new AMS_Tim_Tuned());
//        amsPlayers.add(new AMSPlayground());
//        amsPlayers.add(new AMS_Rollout_BP());
//        amsPlayers.add(new AMS_Rollout_BP_MAST());
//        amsPlayers.add(new AMS_Rollout_BP_NST());
//        amsPlayers.add(new AMS_Tim());
//        amsPlayers.add(new AMSPlayground_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_MAST_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_NST_Tuned());
//        amsPlayers.add(new AMS_Tim_Tuned());
//        amsPlayers.add(new AMSPlayground());
//        amsPlayers.add(new AMS_Rollout_BP());
//        amsPlayers.add(new AMS_Rollout_BP_MAST());
//        amsPlayers.add(new AMS_Rollout_BP_NST());
//        amsPlayers.add(new AMS_Tim());
//        amsPlayers.add(new AMSPlayground_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_MAST_Tuned());
//        amsPlayers.add(new AMS_Rollout_BP_NST_Tuned());
//        amsPlayers.add(new AMS_Tim_Tuned());

        final ExecutorService executor = Executors.newFixedThreadPool(mctsPlayers.size()*games.size());
        int numSimulations = 2;
        final List<Future<ArrayList<String>>> futures = new ArrayList<>();

        for(int j = 0; j < mctsPlayers.size()*games.size(); j++) {
            int finalJ = j;
            for (int g = 0; g < games.size(); g++) {

                int finalG = g;
                futures.add(executor.submit(() -> {
                    ArrayList<String> toPrint = new ArrayList<>();


                    Game game = GameLoader.loadGameFromName(games.get(finalG));
                    //for (int pl = 0; pl < mctsPlayers.size(); pl++) {

                    // the game's "stateFlags" contain properties of the game that may be
                    // important for some AI algorithms to know about
                    final long stateFlags = game.stateFlags();

                    // for example, we may like to know whether our game has stochastic elements
                    final boolean isStochastic = ((stateFlags & GameType.Stochastic) != 0L);

                    final int numPlayers = game.players().count();

                    // to be able to play the game, we need to instantiate "Trial" and "Context" objects
                    Trial trial = new Trial(game);
                    Context context = new Context(game, trial);


                    //---------------------------------------------------------------------

                    // now we're going to have a look at playing a few full games, using AI

                    final List<Group12AI> agents = new ArrayList<>();
                    agents.add(null);    // insert null at index 0, because player indices start at 1
                    // first, let's instantiate some agents
                    for (int q = 0; q < 2; q++) {

                        if (q == 0) {
                            for (int p = 1; p <= numPlayers; ++p) {
                                if (p % 2 != 0) {
                                    // for half the agents, we'll use the Example Random AI from this repo
                                    agents.add(mctsPlayers.get(finalJ));
                                } else {
                                    // for the other half of the agents, we'll use our example UCT agent
                                    agents.add(amsPlayers.get(finalJ));
                                }
                            }
                        } else {
                            for (int p = 1; p <= numPlayers; ++p) {
                                if (p % 2 != 0) {
                                    // for half the agents, we'll use the Example Random AI from this repo
                                    agents.add(amsPlayers.get(finalJ));
                                } else {
                                    // for the other half of the agents, we'll use our example UCT agent
                                    agents.add(mctsPlayers.get(finalJ));
                                }
                            }
                        }


                        // number of games we'd like to play
                        final int numGames = 50;
                        int[] results = new int[numSimulations];
                        long[] times = new long[numSimulations];
                        int[] iterations = new int[numSimulations];
                        int[] selectedActions = new int[numSimulations];

                        // NOTE: in our following loop through number of games, the different
                        // agents are always assigned the same player number. For example,
                        // Player 1 will always be legacy.random, Player 2 always UCT, Player 3
                        // always legacy.random, etc.
                        //
                        // For a fair comparison of playing strength, agent assignments to
                        // player numbers should rotate through all possible permutations,
                        // to correct for possible first-mover-advantages or disadvantages, etc.
                        for (int i = 0; i < numGames; ++i) {
                            // (re)start our game
                            game.start(context);

                            int depth = 0;
                            int maxBranching1 = -1;
                            int maxBranching2 = -1;
                            ArrayList<Integer> branching1 = new ArrayList<>();
                            ArrayList<Integer> branching2 = new ArrayList<>();

                            // (re)initialise our agents
                            for (int p = 1; p < agents.size(); ++p) {
                                agents.get(p).initAI(game, p);
                            }

                            // keep going until the game is over
                            while (!context.trial().over()) {
                                // figure out which player is to move
                                final int mover = context.state().mover();

                                // retrieve mover from list of agents
                                final AI agent = agents.get(mover);

                                // ask agent to select a move
                                // we'll give them a search time limit of 0.2 seconds per decision
                                // IMPORTANT: pass a copy of the context, not the context object directly
                                long startTime = System.currentTimeMillis();
                                FastArrayList<Move> possibleMoves = game.moves(context).moves();

                                depth++;
                                if (mover == 1) {
                                    branching1.add(game.moves(context).moves().size());
                                    for (int b = 0; b < branching1.size(); b++) {
                                        if (maxBranching1 < branching1.get(b)) {
                                            maxBranching1 = branching1.get(b);
                                        }
                                    }
                                }
                                if (mover == 2) {
                                    branching2.add(game.moves(context).moves().size());
                                    for (int b = 0; b < branching2.size(); b++) {
                                        if (maxBranching2 < branching2.get(b)) {
                                            maxBranching2 = branching2.get(b);
                                        }
                                    }
                                }
                                long st = System.currentTimeMillis();

                                final Move move = agent.selectAction
                                        (
                                                game,
                                                new Context(context),
                                                1,
                                                -1,
                                                2
                                        );
                                long selectionTime = System.currentTimeMillis() - st;
                                if (mover == 1) {
                                    //                    System.out.print(selectionTime);
                                    times[0] += selectionTime;
                                    selectedActions[0] += 1;
                                    iterations[0] += agents.get(1).getIterations();
                                }
                                if (mover == 2) {
                                    //                    System.out.print(", " + selectionTime + "\n");
                                    times[1] += selectionTime;
                                    selectedActions[1] += 1;
                                    iterations[1] += agents.get(2).getIterations();
                                }
                                // apply the chosen move
                                game.apply(context, move);
                            }
                            int branching1sum = 0;
                            int branching2sum = 0;
                            for (int b = 0; b < branching1.size(); b++) {
                                branching1sum += branching1.get(b);
                            }
                            for (int b = 0; b < branching2.size(); b++) {
                                branching2sum += branching2.get(b);
                            }
                            //            System.out.println();
                            //            System.out.println("depth: " + depth);
                            //            System.out.println("max branching player 1: " + maxBranching1);
                            //            System.out.println("average branching player 1: " + branching1sum/branching1.size());
                            //            System.out.println("max branching player 2: " + maxBranching2);
                            //            System.out.println("average branching player 2: " + branching2sum/branching2.size());

                            // let's see who won
                            if (i == 0) {
                                System.out.println(game.name() + ":\n" + games.get(finalG) + ": " + agents.get(1).friendlyName + " vs " + agents.get(2).friendlyName + "\n" + "Game " + i + ": " + context.trial().status());
                            }
                            if (context.trial().status().winner() == 1)
                                results[0]++;
                            else if (context.trial().status().winner() == 2)
                                results[1]++;
                        }
                        System.out.println(games.get(finalG) + ": " + agents.get(1).friendlyName + " vs " + agents.get(2).friendlyName + "\n" +
                                "average selection times = " +
                                times[0] / selectedActions[0] + "/" +
                                times[1] / selectedActions[1] +
                                "\naverage number of iterations = " +
                                iterations[0] / selectedActions[0] + "/" +
                                iterations[1] / selectedActions[1] +
                                "\nwinning rate = " +
                                ((float) results[0] * 100f) / (float) numGames + "/" +
                                ((float) results[1] * 100f) / (float) numGames + "/" +
                                ((float) (numGames - (results[0] + results[1])) * 100f) / (float) numGames + "\n");
                    }

                    //}
                    return toPrint;}));}}
        for ( Future<ArrayList<String>> f : futures ) {
            // ArrayList<String> aResult = f.get(); // Will block until the result of the task is available.
            // Optionally do something with the result...
            // for(String r : aResult)
            //    System.out.println(r);
        }

        executor.shutdown();
    }

}
