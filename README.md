# Who Invented MCTS

Project git for the "Who Invented MCTS?" Masters project from the MSc. Artificial Intelligence programme at Maastricht University. This project ran from 09/2020 to 02/2021.
## Please note step 3 in the getting started section below!


## Ludii-based AMS AI

This project contains instructions and examples for the implementation of 
the Adaptive Multi-stage Sampling algorithm (AMS) for the Ludii General 
Game system. This agent is meant to be loaded locally through the GUI of 
the Ludii application, and watch it play any game supported by Ludii! 

Most of the documentation found in this repository may also be found in
the Ludii User Guide, accessible from the 
[Ludii webpage](http://ludii.games/index.php).

We base this repository and AI on the example provided at [Ludii Tutorials](https://ludiitutorials.readthedocs.io).
We also implement our own versions of Monte-Carlo Tree Search and various
additional enhancements.

## Table of Contents
- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [Example Agents](#example-agents)
- [Citing Information](#citing-information)
- [Background Info](#background-info)
- [Contact Info](#contact-info)
- [Changelog](#changelog)
- [Acknowledgements](#acknowledgements)

## Requirements

As of this time, only Java is supported. The**minimum version of Java** 
required is **Java 8**.

## Getting Started

### AI Development

1. Download [Ludii's JAR file](http://ludii.games/download.php). This is the
JAR file that can also be used to launch the Ludii application.
2. Create a new Java project using your favourite IDE. You can also create a
fork of this [github repository](https://github.com/Ludeme/LudiiExampleAI)
to get started with some example implementations of basic agents.
3. Make sure to add the Ludii's JAR file downloaded in step 1 as a library for
your project.
4. Any agent that you'd like to implement will have to extend the abstract class
`util.AI`. This contains three methods that may be overridden:
	1. `public Move selectAction(final Game game, final Context context, 
	final double maxSeconds, final int maxIterations, final int maxDepth)`.
	It takes a reference to the `game` being played, and the current 
	`context` (which contains, among other data, the current game state) as
	arguments, and should return the next `Move` to be played by the agent. 
	The final three arguments can be used to restrict the agent's processing
	(its search time, or its maximum iteration count or search depth for example).
	2. `public void initAI(final Game game, final int playerID)`. This method can be used
	to perform any initialisation of the AI when the game to be played has been
	determined, but before the initial game state has been generated. 
	3. `public boolean supportsGame(final Game game)`. This method has a default implementation
	to return `true` for any game, but may be overridden to return `false` for games
	that your agent cannot play. For example, it may be unable to play simultaneous-move
	games, and then be implemented to always return `false` for those. Ludii will then
	know not to try to make your AI play such a game.
	4. `public void closeAI()`. This method can be used to perform any cleanup of resources
	when a game has been finished.

### Loading AI in the Ludii Application

In the Ludii application, the dialog in which agent types can be assigned to
players can be opened by clicking one of the player names in the GUI, or by
selecting `Ludii > Preferences...` in the menubar. In addition to a
number of built-in agents, the drop-down menus contain a `From JAR` option.

To load your own custom AI implementation into Ludii, select the `From JAR`
option, and then select the JAR file containing your custom AI's .class file.
A dialog will appear with all the different classes in the selected JAR file
that extend Ludii's `util.AI` abstract class, and you will be required to
choose one of them. Note that this means that it is fine if you have a single
JAR file containing many different, custom AI implementations; they can all be
loaded.

Ludii will attempt to instantiate an agent of the selected class by calling
a zero-arguments constructor of that class. **This will only work correctly
if your class does indeed provide a zero-args constructor, and it will have
to be public as well!.** After loading it as instructed here, the custom AI
can be used to play any games in the Ludii application, just like any other
built-in AI.

**Note:** while the Ludii application is running, it will only load all the
.class files of any selected JAR file once. If you have already selected a
JAR file once, and then re-build your custom JAR file without changing its
filepath, you will have to close and re-open the Ludii application if you
wish to try loading agents from the modified JAR file.

## Implemented Agents

UCB1/UCT Agents:
- [AMS](src/AMSPlaygroung/AMSPlayground.java).
- [AMS-Play-outs](src/AMSPlaygroung/AMS_Rollout_BP.java).
- [AMS-Priority](src/AMSPlaygroung/AMS_Tim.java).
- [AMS-MAST](src/AMSPlaygroung/AMS_Rollout_BP_MAST.java).
- [AMS-NST](src//AMSPlaygroung/AMS_Rollout_BP_NST.java).
- [MCTS](src/mcts/MCTS-Vanilla.java).
- [MCTS-MAST](src/mcts/MCTS-MAST.java).
- [MCTS-NST](src/mcts/MCTS-NST.java).

UCB1-Tuned Agents:
- [AMS](src/AMSPlaygroung/AMSPlayground_Tuned.java).
- [AMS-Play-outs](src/AMSPlaygroung/AMS_Rollout_BP_Tuned.java).
- [AMS-Priority](src/AMSPlaygroung/AMS_Tim_Tuned.java).
- [AMS-MAST](src/AMSPlaygroung/AMS_Rollout_BP_MAST_Tuned.java).
- [AMS-NST](src//AMSPlaygroung/AMS_Rollout_BP_NST_Tuned.java).
- [MCTS](src/mcts/MCTS-Vanilla_Tuned.java).
- [MCTS-MAST](src/mcts/MCTS-MAST_Tuned.java).
- [MCTS-NST](src/mcts/MCTS-NST_Tuned.java).

## Background Info

This repository contains a modification of the Adaptive Multi-stage Sampling
algorithm such that it can be executed within the Ludii General Game System.
An implementation of Monte-Carlo Tree Search and other enhancements (specifically MAST, NST and UCB1-Tuned) are also
contained within this repository. Note that this repository does not contain 
the full Ludii system, or its built-in AI options.

This work is developed using the full Ludii system itself. More info on the 
Ludii General Game project and the system can be found on:

- http://www.ludeme.eu/
- http://ludii.games/

## Contact Info

If there are questions, suggestions, or troubleshooting issues relating
to the algorithms implemented in this repository, please generate a new
Issue on this repository.

For Ludii-based help, see below:

The preferred method for getting help with troubleshooting, suggesting or
requesting additional functionality, or asking other questions about AI
development for Ludii, is [creating new Issues on the github repository](https://github.com/Ludeme/LudiiExampleAI/issues).
Alternatively, the following email address may be used: `ludii(dot)games(at)gmail(dot)com`.


## Acknowledgements

This repository is a fork of the Digital Ludeme Project Example AI, which is part of the Digital Ludeme Project being run by Cameron Browne at Maastricht University's Department of Data Science and Knowledge Engineering. This fork is intended for usage for the Masters Research Project course in the 2020 Autumn Semester as part of the MSc. Artificial Intelligence and MSc. Data Science for Decision Making programmes offered at Maastricht University.
