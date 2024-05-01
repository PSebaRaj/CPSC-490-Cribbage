# CPSC-490-Cribbage

For Project Proposal, Final Report, and Poster, see [here](https://zoo.cs.yale.edu/classes/cs490/23-24b/sebaraj.patrick.pds36/) (requires Yale Network / VPN).

This project is built upon the work previously done to build a [counterfactual regret minimization agent for cribbage](https://github.com/msolonko/CSEC-491-Cribbage/tree/master).

## Compiling
Compile through IntelliJ's build function. The `.iml` file is provided, and we compilation in two forms:

### Compiling to Run Natively
You may build the agent utilizing IntelliJ's native 'Build Project' function. This will enable you to train the agents (separately or simultaneously) and play against the agent on your local machine. Simultaneous training is initiated through the `CFRTrainTogether` class, and playing against any combination of agents is initiated through the `Play` class.

### Compiling Artifacts
You may build artifacts, which capture different functionalities of the project, through IntelliJ's 'Build Artifacts' function. This will enable you to run the agents on a remote server, as well as train the agents on a remote server, through moving these artifact `.jar`s onto such servers. The artifacts are built in the following manner:
- `CFRTrainTogether` artifact trains both the discard and pegging agents simultaneously, with or without positional play.
- `Play` artifact allows you to play against any combination of agents

## Training 
To train both the discard agent and the pegging agent together *without positional play*, run:
```bash
java CFRTrainTogether.jar
```

To continue positional play training, check out the `positional-play` branch, change the starting score in `CFRTrainTogether` to whichever greater game state you intend to train on, and run:
```bash
java CFRTrainTogether.jar
```
Then utilize the `CalculateExpectedValue` class with the output of this training (thrownodes and pegnodes) to calculate (through simulation) the expected value of the greater game state and store the result in `GamePositionList` object.

## Running
Utilize the `Play` class to play against any combination of agents available in this codebase. If you have selected CFR agent (thrower or pegger), please have the appropriate thrownodes and/or pegnodes ready to provide to the agent, stored in the project root folder.
