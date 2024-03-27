# CPSC-490-Cribbage

Project Proposal: []()

Project Report: []()

This project is built upon the work previously done to build a [counterfactual regret minimization agent for cribbage](https://github.com/msolonko/CSEC-491-Cribbage/tree/master).

## Compiling 
To compile on the Zoo, run the following command:
```bash
javac -cp ".:/<path-to-repository>/CPSC-490-Cribbage/lib/*" -Xlint:deprecation *.java
```

For example, 
```bash
javac -cp ".:/home/accts/pds36/CPSC-490-Cribbage/lib/*" -Xlint:deprecation *.java
```

## Training 
To train both the discard agent and the pegging agent together, run:
```bash
java CFRTrainTogether
```

Notes: currently training on frog.zoo.cs.yale.edu

## Running