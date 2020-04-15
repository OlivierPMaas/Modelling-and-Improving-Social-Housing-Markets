package Algorithms.SimpleImprovement;

import HousingMarket.House.House;
import HousingMarket.Household.Household;
import HousingMarket.HousingMarketVertex;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

public class ImprovementMCPMA {

    private ImprovementGraph improvementGraph;
    private SimpleGraph<HousingMarketVertex, DefaultEdge> matchGraph;

    public ImprovementMCPMA(ImprovementGraph improvementGraph) throws UnequalSidesException {
        this.improvementGraph = improvementGraph;
        if (improvementGraph.getHouses().size() + improvementGraph.getDummyHouses().size()
        != improvementGraph.getHouseholds().size() + improvementGraph.getDummyHouseholds().size()) {
            throw new UnequalSidesException("Error: Improvement graph does not contain equal amount of houses and households.");
        }
        // Create matchGraph
        matchGraph = new SimpleGraph<HousingMarketVertex, DefaultEdge>(DefaultEdge.class);
        for (House house : improvementGraph.getHouses()) {
            matchGraph.addVertex(house);
        }
        for (Household household : improvementGraph.getHouseholds()) {
            matchGraph.addVertex(household);
        }
        for (DummyHouse dummyHouse : improvementGraph.getDummyHouses()) {
            matchGraph.addVertex(dummyHouse);
        }
        for (DummyHousehold dummyHousehold : improvementGraph.getDummyHouseholds()) {
            matchGraph.addVertex(dummyHousehold);
        }
        // Note that matchGraph starts without any matches; hence no edges are added.
    }

    // Find optimal matching.
    public SimpleGraph<HousingMarketVertex, DefaultEdge> findOptimalMatching(boolean print) throws ImprovementPrices.AlreadyInitiatedException, ResidualImprovementGraph.MatchGraphNotEmptyException, ResidualImprovementGraph.PathEdgeNotInResidualImprovementGraphException {
        ImprovementPrices improvementPrices = new ImprovementPrices(improvementGraph, matchGraph);
        improvementPrices.setInitialPrices();
        int i = 0;
        boolean shouldContinue = matchGraph.edgeSet().size() != improvementGraph.getHouseholds().size() + improvementGraph.getDummyHouseholds().size();
        while (shouldContinue) {
            if(print) { System.out.println("Augmenting path " + i); }
            GraphPath<Integer, DefaultWeightedEdge> augmentingPath = improvementPrices.getResidualImprovementGraph().findAugmentingPath();
            // TODO: Check if indeed this path may be null with non-maximal matching!
            if (augmentingPath == null) {
                shouldContinue = false;
            } else {
                this.matchGraph = improvementPrices.augmentMatchGraphAndUpdateAll(augmentingPath);
                i++;
                shouldContinue = matchGraph.edgeSet().size() != improvementGraph.getHouseholds().size() + improvementGraph.getDummyHouseholds().size();
            }
        }
        return matchGraph;
    }

    public class UnequalSidesException extends Exception {
        public UnequalSidesException(String errorMessage) {
            super(errorMessage);
        }
    }

}
