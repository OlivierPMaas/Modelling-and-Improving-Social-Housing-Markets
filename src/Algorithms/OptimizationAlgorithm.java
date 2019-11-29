package Algorithms;

import HousingMarket.House.House;
import HousingMarket.HouseAndHouseholdIDs;
import HousingMarket.Household.Household;
import Matching.Matching;
import Matching.MatchingEvaluator;
import TreeNode.TreeNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;

public class OptimizationAlgorithm {
    private Matching matching;

    public OptimizationAlgorithm(Matching matching) {
        this.matching = matching;
    }

    public Result optimizeAvailables()
            throws Matching.HouseholdLinkedToHouseholdException,
            Matching.HouseholdLinkedToMultipleException,
            Matching.HouseLinkedToHouseException,
            Matching.HouseLinkedToMultipleException,
            MatchingEvaluator.HouseholdIncomeTooHighException,
            Matching.HouseholdAlreadyMatchedException,
            Matching.HouseAlreadyMatchedException {
        return optimizeAll(this.matching.getHouseholdlessHouses(),
                this.matching.getHouselessHouseholds());

    }
    public Result optimizeAll(ArrayList<House> houses, ArrayList<Household> households)
            throws Matching.HouseholdLinkedToMultipleException,
            Matching.HouseholdLinkedToHouseholdException,
            Matching.HouseLinkedToMultipleException,
            Matching.HouseLinkedToHouseException,
            MatchingEvaluator.HouseholdIncomeTooHighException,
            Matching.HouseAlreadyMatchedException,
            Matching.HouseholdAlreadyMatchedException {

        MatchingEvaluator evaluator = new MatchingEvaluator(matching);
        float oldResult = evaluator.evaluateTotal(true);
        System.out.print("\n");
        if (houses.size() == 0 || households.size() == 0) {
            System.out.println("Either there were no houseless households, or no householdless houses. Algorithm made no changes.");
            Result result = new Result(-1, -1, 0, 0, 0);
            return result;
        }
        // Dissolve connections.
        for (House house : houses) {
            Household household = matching.getHouseholdFromHouse(house);
            if (household != null) {
                matching.disconnect(house, household);
            }
        }
        for (Household household : households) {
            House house = matching.getHouseFromHousehold(household);
            if (house != null) {
                matching.disconnect(house, household);
            }
        }

        // Say we have a bipartite graph with n1 + n2 vertices.
        // We want to get as high a total fit-quality as possible.
        // Since our fit-values cannot be negative, this means that we'll at least want
        // to maximize the amount of edges in the final graph.
        // However, each vertex may have at most one incident edge.
        // The final bipartite graph will thus have L = min(n1, n2) edges.
        // This means that, if M = max(n1, n2), we have M!/(M-L)! different possible configurations
        // of the final graph (since for each vertex on the smaller side, there will be
        // first M options, next M-1, and so forth, until all edges have been set.)
        // We can enumerate over each of these by looking at the side with the fewest vertices,
        // and create an iterative tree of choices.

        int L;
        int M;
        SourcesSide side;
        // create enumeration of all possibilities - recursively!
        ArrayList<Integer> sources = new ArrayList<Integer>();
        HashSet<Integer> unclaimedTargets = new HashSet<Integer>();

        if (houses.size() <= households.size()) {
            L = houses.size();
            M = households.size();
            side = SourcesSide.HOUSES;
            for (House house : houses) {
                sources.add(house.getID());
            }
            for (Household household : households) {
                unclaimedTargets.add(household.getID());
            }
        }
        else {
            L = households.size();
            M = houses.size();
            side = SourcesSide.HOUSEHOLDS;
            for (Household household : households) {
                sources.add(household.getID());
            }
            for (House house : houses) {
                unclaimedTargets.add(house.getID());
            }
        }

        TreeNode<HouseAndHouseholdIDs> emptyPossibilitiesRoot = new TreeNode<HouseAndHouseholdIDs>(
                new HouseAndHouseholdIDs(-1, -1));

        TreeNode<HouseAndHouseholdIDs> filledPossibilitiesRoot
                = recursivelyEnumeratePossibilities(emptyPossibilitiesRoot,unclaimedTargets,sources, side);



        // try each possibility
        Matching bestMatching = recursivelyTryPossibilities(matching, filledPossibilitiesRoot, sources);
        float newResult = new MatchingEvaluator(bestMatching).evaluateTotal(true);
        System.out.print("\n");

        // compare scores
        if (newResult < oldResult) {
            // TODO: We got here once...
            System.err.println("Error! The best-found matching was worse than the given one.");
        }
        String text = (side == SourcesSide.HOUSES) ? " houses" : " households";
        float percentage = (side == SourcesSide.HOUSES) ? (float) L/matching.getHouses().size() : (float) L/matching.getHouseholds().size();
        System.out.println("Old score was: " + oldResult + ". New score is: " + newResult + ".\n" +
                "Thus the given matching was improved by " + (newResult - oldResult)/oldResult * 100 + "%.\n" +
                "Note that there were " + L + " (= " + percentage * 100 + "%) " + text + " that we could rewire.");
        Result result = new Result(oldResult, newResult, (newResult - oldResult)/oldResult * 100, L, percentage*100);
        return result;

    }

    private TreeNode<HouseAndHouseholdIDs> recursivelyEnumeratePossibilities(TreeNode<HouseAndHouseholdIDs> root,
                                                                             HashSet<Integer> unclaimedTargets,
                                                                             ArrayList<Integer> sources, SourcesSide side) {
        for (int t : unclaimedTargets) {
            HashSet<Integer> newUnclaimedTargets = new HashSet<Integer>(unclaimedTargets);
            newUnclaimedTargets.remove(t);
            TreeNode<HouseAndHouseholdIDs> node = auxiliaryRecursivelyEnumeratePossibilities(0, t, sources,
                    newUnclaimedTargets, side);
            root.addChild(node);
        }
        return root;
    }

    private TreeNode<HouseAndHouseholdIDs> auxiliaryRecursivelyEnumeratePossibilities(int currentSourceIndex, int currentTarget,
                                                                                      ArrayList<Integer> sources, HashSet<Integer> unclaimedTargets, SourcesSide side) {
        HouseAndHouseholdIDs current;
        if (side == SourcesSide.HOUSES) {
            current = new HouseAndHouseholdIDs(sources.get(currentSourceIndex), currentTarget);
        } else { // side = SourcesSide.HOUSEHOLDS.
            current = new HouseAndHouseholdIDs(currentTarget, sources.get(currentSourceIndex)); }

        TreeNode<HouseAndHouseholdIDs> currentNode = new TreeNode<HouseAndHouseholdIDs>(current);

        if(currentSourceIndex == sources.size() - 1) {
            return currentNode;
        }
        else if(currentSourceIndex < sources.size() - 1) {
            for (int newChoice : unclaimedTargets) {
                HashSet<Integer> newUnclaimedTargets = new HashSet<Integer>(unclaimedTargets);
                newUnclaimedTargets.remove(newChoice);
                TreeNode<HouseAndHouseholdIDs> child = auxiliaryRecursivelyEnumeratePossibilities(currentSourceIndex+1,
                        newChoice, sources, newUnclaimedTargets, side);
                currentNode.addChild(child);
            }
        }

        return currentNode;
    }

    private Matching recursivelyTryPossibilities(Matching matching, TreeNode<HouseAndHouseholdIDs> root, ArrayList<Integer> sources)
            throws MatchingEvaluator.HouseholdIncomeTooHighException,
            Matching.HouseholdLinkedToMultipleException,
            Matching.HouseholdLinkedToHouseholdException,
            Matching.HouseAlreadyMatchedException,
            Matching.HouseholdAlreadyMatchedException {
        float highScore = (float) 0.0;
        Matching currentMatching = (Matching) deepClone(matching);
        Matching bestMatching = (Matching) deepClone(matching);
        for (TreeNode<HouseAndHouseholdIDs> child : root.getChildren()) {
            Matching bestChildMatching = auxiliaryRecursivelyTryPossibilities(currentMatching, highScore, child,
                    sources);
            float childScore = new MatchingEvaluator(bestChildMatching).evaluateTotal(false);
            if (childScore > highScore) {
                highScore = childScore;
                bestMatching = bestChildMatching;
            }
        }
        return bestMatching;
    }

    private Matching auxiliaryRecursivelyTryPossibilities(Matching matching, float highScore, TreeNode<HouseAndHouseholdIDs> currentNode,
                                                          ArrayList<Integer> sources)
            throws Matching.HouseholdAlreadyMatchedException,
            Matching.HouseAlreadyMatchedException,
            MatchingEvaluator.HouseholdIncomeTooHighException,
            Matching.HouseholdLinkedToMultipleException,
            Matching.HouseholdLinkedToHouseholdException {
        HouseAndHouseholdIDs data = currentNode.getData();
        Matching modifiedMatching = (Matching) deepClone(matching);
        modifiedMatching.connect(modifiedMatching.getHouse(data.getHouseID()), modifiedMatching.getHousehold(data.getHouseholdID()));
        Matching bestMatching = modifiedMatching;
        if (currentNode.hasChildren()) {
            for (TreeNode<HouseAndHouseholdIDs> child : currentNode.getChildren()) {
                Matching bestChildMatching = auxiliaryRecursivelyTryPossibilities(modifiedMatching, highScore, child,
                        sources);
                float childScore = new MatchingEvaluator(bestChildMatching).evaluateTotal(false);
                if (childScore > highScore) {
                    highScore = childScore;
                    bestMatching = bestChildMatching;
                }
            }
        }
        return bestMatching;
    }



//    private Matching auxiliaryRecursivelyTryPossibilities2(Matching matching, float highScore, TreeNode<HouseAndHouseholdLong> currentNode,
//                                                 ArrayList<AtomicLong> sources, SourcesSide side)
//            throws Matching.HouseholdAlreadyMatchedException,
//            Matching.HouseAlreadyMatchedException,
//            Matching.HouseholdLinkedToMultipleException,
//            Matching.HouseholdLinkedToHouseholdException,
//            MatchingEvaluator.HouseholdIncomeTooHighException {
//        // We have a tree enumerating all possible combinations of edges.
//        // In the beginning, no possible edge has been realized.
//        // We note that since the minimum fit-value is 0,
//        // given a candidate matching, its score will never be lowered if we realize one of its edges.
//        // For simplicity's sake, let's suppose that side = 0.
//        // We perform a breadth-first search of this tree.
//        // Base step: At some base node _leaf_, we first check which node we're at,
//        // and next we check which node it should connect to. We then make this choice of connection,
//        // and return this new matching. By definition, this new matching is the best matching at this stage.
//        // Recursion step: At some node _n_, we first check which node we're at.
//        // We then call the recursion formula to find, for each of n's children, the best matching that could be gotten
//        // at said child node in the tree. We then pick the best of those matchings, and return it.
//        // This completes the recursion. We end up with the best matching that could be found.
//
//        if (!currentNode.hasChildren()) {
//            if (currentIndex.equals(0)) {
//                System.out.println("Possibilities-tree was empty.");
//                return matching;
//            } else {
//                Matching newMatching = (Matching) deepClone(matching);
//                if (side.equals(0)) {
//                    newMatching.connect(newMatching.getHouseholdlessHouse(currentIndex-1),
//                            newMatching.getHouselessHousehold(currentNode.getData()-1));
//                }
//                if (side.equals(1)) {
//                    newMatching.connect(newMatching.getHouseholdlessHouse(currentNode.getData()-1),
//                            newMatching.getHouselessHousehold(currentIndex-1));
//                }
//                return newMatching;
//            }
//        }
//        else {
//            Matching currentMatching = (Matching) deepClone(matching);
//            Matching bestCurrentMatching = (Matching) deepClone(matching);
//            float highestCurrentScore = highScore;
//            for (TreeNode<Integer> child : currentNode.getChildren()) {
//                Matching bestFoundMatching = recursivelyTryPossibilities(currentMatching, highestCurrentScore, child, currentIndex+1, side);
//                float bestFoundScore = new MatchingEvaluator(bestFoundMatching).evaluateTotal(false);
//                if (bestFoundScore > highestCurrentScore) {
//                    highestCurrentScore = bestFoundScore;
//                    bestCurrentMatching = bestFoundMatching;
//                }
//            }
//            // TODO: Should this go inside the above loop? After all, that way the scorer could take the changes made at
//            //  each step into account. Not sure if those changes would change the score in such a way that they'd need
//            //  to be taken into account early, though.
//            //The problem is that I'm rewiring houses and households based on a changing houselesshouseholds list; thus sometimes we get out of bounds.
//            //The solution is to stop using indices to refer to the n'th house and household in the householdlesshouses and houselesshouseholds lists,
//            //and instead use indices that refer to houses' and households' positions within the larger matching list.
//            //Or better yet: Give houses unique identifier (so that later when we dynamically add houses, we won't have a problem).
//            // This gives us a new problem: We can no longer go through a tree with an implicit index.
//            // So we'll need to instead find a way to get each treenode to carry both its source house/household and its target household/house.
//            // We can't do this using a HouseAndHousehold class, however, because the deep copy usage breaks house/household identities.
//            // So... unique identifiers, here we go!
//            if(!currentIndex.equals(0)) {
//                try {
//                    if (side.equals(0)) {
//                        bestCurrentMatching.connect(bestCurrentMatching.getHouseholdlessHouse(currentIndex - 1),
//                                bestCurrentMatching.getHouselessHousehold(currentNode.getData() - 1));
//                    }
//                    if (side.equals(1)) {
//                        bestCurrentMatching.connect(bestCurrentMatching.getHouseholdlessHouse(currentNode.getData() - 1),
//                                bestCurrentMatching.getHouselessHousehold(currentIndex - 1));
//                    }
//                } catch(IndexOutOfBoundsException e) {
//                    System.out.println("We're here!");
//                    throw new IndexOutOfBoundsException(e.getMessage());
//                }
//            }
//            return bestCurrentMatching;
//        }
//    }

    public static Object deepClone(Object object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
