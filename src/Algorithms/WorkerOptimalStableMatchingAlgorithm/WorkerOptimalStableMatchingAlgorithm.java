package Algorithms.WorkerOptimalStableMatchingAlgorithm;

import Matching.Matching;
import Matching.MatchingEvaluator;

import java.util.List;

// Based on the "Worker-Optimal Stable Matching Algorithm (WOSMA)" from "Two-sided matching with indifferences",
// by Aytek Erdil and Haluk Ergin. Note that this version of the algorithm omits step 0. This is because we start with
// some existing matching rather than with an empty one; thus the DA-algorithm cannot realistically be run beforehand.
// As a result, despite the algorithm's name, it does *not* guarantee stability!
//
// Note that we model houses as being wholly indifferent [Assumption A1].
//
// Contrary to the original algorithm, we only want fully strict cycles, instead of cycles in which >0 links are strict.
// This is because we assume that families won't move for an equally-good house; they'll want some improvement.
// Thus we want to redefine D^m_f to denote the set of "workers" (families)
// that strictly prefer "firm" (house) f in matching m.
// After all, all families are acceptable to all houses, and there is never another family that some house strictly
// prefers to another.
//
// There are three types of edges in the "Two-labeled graph":
// * w->v   | These we only want when the family's preference is strict; with A1, this means these edges are strict.
// * w->nil | These we also only want when the family strictly prefers some empty house to their current house.
// * nil->w | These do not symbolize any existing desire, and may thus be rendered strict.
// To clarify: We include (w->v)-type and (w->nil)-type edges only when the family's preference is strict.
//
// Thus in our model, all edges are strict. This changes the algorithm slightly: Instead of using a two-labeled graph,
// we use a so-called "strict graph" which is unlabeled; and instead of looking for partially strict cycles, we simply
// look for cycles, which by definition are fully strict. Each cycle that can be found in this way,
// using the original algorithm's methods, represents a valid SWI-chain(/SWI-cycle) that we can carry out.
//
// Note finally that after each realized cycle, we remove all households that were a part of this cycle
// from all future strict graphs. This is because we assume that households who have been part of a realized cycle
// won't want to move again right away, even if we were able to offer them a better house.

public class WorkerOptimalStableMatchingAlgorithm {
    private Matching matching;

    public WorkerOptimalStableMatchingAlgorithm(Matching matching) {
        this.matching = matching;
    }

    public Matching findWorkerOptimalStableMatching() throws Matching.HouseholdLinkedToHouseholdException, Matching.HouseLinkedToMultipleException, Matching.HouseholdLinkedToMultipleException, Matching.HouseLinkedToHouseException, MatchingEvaluator.HouseholdIncomeTooHighException, Matching.HouseholdAlreadyMatchedException, Matching.HouseAlreadyMatchedException, Matching.PreferredNoHouseholdlessHouseException, CycleFinder.FullyExploredVertexDiscoveredException {
//        StrictGraph strictGraph = new StrictGraph(this.matching);
//        List<Integer> strictCycle = strictGraph.findStrictCycle();
//        int i = 1;
//        while (strictCycle != null) {
//            System.out.println("Executing strict cycle " + i);
//            this.matching.effectuateStrictCycle(strictCycle, strictGraph.getNil());
//            strictGraph.update(strictCycle, this.matching);
//            strictCycle = strictGraph.findStrictCycle();
//            i++;
//        }
        // TODO: Update description above to reflect this new part.
        // Brief explanation:
        // Make two-labeled graph, but only execute cycles
        // (1) where at least one edge is strict and
        // (2) all non-strict edges are pointing to households that have moved along a strict edge before.
        // We build up this graph as we go along: First we add only strict edges;
        // then, when a cycle is executed, include the relevant households' non-strict edges in the graph.
        // This way the second condition is always true and needn't be checked.
        int i = 1;
        TwoLabeledGraph twoLabeledGraph = new TwoLabeledGraph(this.matching);
        List<Integer> cycle = twoLabeledGraph.findCycle();
        while (cycle != null) {
            System.out.println("Executing cycle " + i);
            this.matching.effectuateStrictCycle(cycle, twoLabeledGraph.getNil());
            twoLabeledGraph.update(cycle, this.matching);
            cycle = twoLabeledGraph.findCycle();
            i++;
        }

        return this.matching;
    }


}
