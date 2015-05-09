/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.mcts.mlps;

import ai.*;
import ai.core.AI;
import ai.core.InterruptibleAIWithComputationBudget;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleEvaluationFunction;
import java.util.Random;
import rts.GameState;
import rts.PlayerAction;

/**
 *
 * @author santi
 */
public class ContinuingMLPSMCTS extends InterruptibleAIWithComputationBudget {
    public static int DEBUG = 0;
    public EvaluationFunction ef = null;
       
    Random r = new Random();
    public AI randomAI = new RandomBiasedAI();
    long max_actions_so_far = 0;
    
    GameState gs_to_start_from = null;
    MLPSNode tree = null;
    int current_iteration = 0;
            
    public int MAXSIMULATIONTIME = 1024;
    public int MAX_TREE_DEPTH = 10;

    int player;
    
    double C = 0.05;
    
    // statistics:
    public long total_runs = 0;
    public long total_cycles_executed = 0;
    public long total_actions_issued = 0;
    public long total_time = 0;
    
    
    public ContinuingMLPSMCTS(int available_time, int max_playouts, int lookahead, int max_depth, 
                               double a_C,
                               AI policy, EvaluationFunction a_ef) {
        super(available_time,max_playouts);
        MAXSIMULATIONTIME = lookahead;
        randomAI = policy;
        MAX_TREE_DEPTH = max_depth;
        C = a_C;
        ef = a_ef;
    }    


    public void reset() {
        tree = null;
        gs_to_start_from = null;
        total_runs = 0;
        total_cycles_executed = 0;
        total_actions_issued = 0;
        total_time = 0;
        current_iteration = 0;
    }    
        
    
    public AI clone() {
        return new ContinuingMLPSMCTS(MAX_TIME, MAX_ITERATIONS, MAXSIMULATIONTIME, MAX_TREE_DEPTH, C, randomAI, ef);
    }    
    
    
    public void startNewComputation(int a_player, GameState gs) throws Exception {
        player = a_player;
        current_iteration = 0;
        float evaluation_bound = ef.upperBound(gs);
        tree = new MLPSNode(player, 1-player, gs, null, evaluation_bound, current_iteration++);
        
        max_actions_so_far = Math.max(tree.moveGenerator.getSize(),max_actions_so_far);
        gs_to_start_from = gs;        
    }    
    
    
    public void resetSearch() {
        if (DEBUG>=2) System.out.println("Resetting search...");
        tree = null;
        gs_to_start_from = null;
    }
    

    public void computeDuringOneGameFrame() throws Exception {        
        if (DEBUG>=2) System.out.println("Search...");
        long start = System.currentTimeMillis();
        long end = start;
        long count = 0;
        while(true) {
            if (!iteration(player)) break;
            count++;
            end = System.currentTimeMillis();
            if (MAX_TIME>=0 && (end - start)>=MAX_TIME) break; 
            if (MAX_ITERATIONS>=0 && count>=MAX_ITERATIONS) break;             
        }
//        System.out.println("HL: " + count + " time: " + (System.currentTimeMillis() - start) + " (" + available_time + "," + max_playouts + ")");
        total_time += (end - start);
        total_cycles_executed++;
    }
    
    
    public boolean iteration(int player) throws Exception {
        MLPSNode leaf = tree.selectLeaf(player, 1-player, C, MAX_TREE_DEPTH, current_iteration++);

        if (leaf!=null) {            
            GameState gs2 = leaf.gs.clone();
            simulate(gs2, gs2.getTime() + MAXSIMULATIONTIME);

            int time = gs2.getTime() - gs_to_start_from.getTime();
            double evaluation = ef.evaluate(player, 1-player, gs2)*Math.pow(0.99,time/10.0);

            leaf.propagateEvaluation((float)evaluation,null);            

            total_runs++;
            
        } else {
            // no actions to choose from :)
            System.err.println(this.getClass().getSimpleName() + ": claims there are no more leafs to explore...");
            return false;
        }
        return true;
    }
    
    public PlayerAction getBestActionSoFar() {
        int idx = getMostVisitedActionIdx();
        if (idx==-1) {
            if (DEBUG>=1) System.out.println("ContinuingNaiveMCTS no children selected. Returning an empty asction");
            return new PlayerAction();
        }
        if (DEBUG>=2) tree.showNode(0,1,ef);
        if (DEBUG>=1) {
            MLPSNode best = (MLPSNode) tree.children.get(idx);
            System.out.println("ContinuingMLPSMCTS selected children " + tree.actions.get(idx) + " explored " + best.visit_count + " Avg evaluation: " + (best.accum_evaluation/((double)best.visit_count)));
        }
        return tree.actions.get(idx);
    }
    
    
    public int getMostVisitedActionIdx() {
        total_actions_issued++;
            
        int bestIdx = -1;
        MLPSNode best = null;
        if (DEBUG>=2) {
//            for(Player p:gs_to_start_from.getPlayers()) {
//                System.out.println("Resources P" + p.getID() + ": " + p.getResources());
//            }
            System.out.println("Number of playouts: " + tree.visit_count);
            tree.printUnitActionTable();
        }
        for(int i = 0;i<tree.children.size();i++) {
            MLPSNode child = (MLPSNode)tree.children.get(i);
            if (DEBUG>=2) {
                System.out.println("child " + tree.actions.get(i) + " explored " + child.visit_count + " Avg evaluation: " + (child.accum_evaluation/((double)child.visit_count)));
            }
//            if (best == null || (child.accum_evaluation/child.visit_count)>(best.accum_evaluation/best.visit_count)) {
            if (best == null || child.visit_count>best.visit_count) {
                best = child;
                bestIdx = i;
            }
        }
        
        return bestIdx;
    }
    
    
    public int getHighestEvaluationActionIdx() {
        total_actions_issued++;
            
        int bestIdx = -1;
        MLPSNode best = null;
        if (DEBUG>=2) {
//            for(Player p:gs_to_start_from.getPlayers()) {
//                System.out.println("Resources P" + p.getID() + ": " + p.getResources());
//            }
            System.out.println("Number of playouts: " + tree.visit_count);
            tree.printUnitActionTable();
        }
        for(int i = 0;i<tree.children.size();i++) {
            MLPSNode child = (MLPSNode)tree.children.get(i);
            if (DEBUG>=2) {
                System.out.println("child " + tree.actions.get(i) + " explored " + child.visit_count + " Avg evaluation: " + (child.accum_evaluation/((double)child.visit_count)));
            }
//            if (best == null || (child.accum_evaluation/child.visit_count)>(best.accum_evaluation/best.visit_count)) {
            if (best == null || (child.accum_evaluation/((double)child.visit_count))>(best.accum_evaluation/((double)best.visit_count))) {
                best = child;
                bestIdx = i;
            }
        }
        
        return bestIdx;
    }
    
        
    public void simulate(GameState gs, int time) throws Exception {
        boolean gameover = false;

        do{
            if (gs.isComplete()) {
                gameover = gs.cycle();
            } else {
                gs.issue(randomAI.getAction(0, gs));
                gs.issue(randomAI.getAction(1, gs));
            }
        }while(!gameover && gs.getTime()<time);   
    }
    
    public MLPSNode getTree() {
        return tree;
    }
    
    public GameState getGameStateToStartFrom() {
        return gs_to_start_from;
    }
    
    
    public String toString() {
        return "ContinuingMLPSMCTS(" + MAXSIMULATIONTIME + "," + MAX_ITERATIONS + "," + MAX_TREE_DEPTH + "," + C + ")";
    }
    
    public String statisticsString() {
        return "Total runs: " + total_runs + 
               ", runs per action: " + (total_runs/(float)total_actions_issued) + 
               ", runs per cycle: " + (total_runs/(float)total_cycles_executed) + 
               ", averate time per cycle: " + (total_time/(float)total_cycles_executed) + 
               ", max branching factor: " + max_actions_so_far;
    }
    
}