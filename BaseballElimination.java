/* *****************************************************************************
 *  Name: Philipp Sick
 *  Date: August 14, 2023
 *  Description: In the baseball elimination problem, there is a division
 *  consisting of n teams. At some point during the season, team i has
 *  w[i] wins, l[i] losses, r[i] remaining games, and g[i][j] games left to
 *  play against team j. A team is mathematically eliminated if it cannot
 *  possibly finish the season in (or tied for) first place. The goal is to
 *  determine exactly which teams are mathematically eliminated. For simplicity,
 *  we assume that no games end in a tie (as is the case in Major League
 *  Baseball) and that there are no rainouts (i.e., every scheduled game is
 *  played).
 *  We create a flow network and solve a maxflow problem in it. In the network,
 *  feasible integral flows correspond to outcomes of the remaining schedule.
 *  There are vertices corresponding to teams (other than team x) and to
 *  remaining divisional games (not involving team x). Intuitively, each unit
 *  of flow in the network corresponds to a remaining game. As it flows through
 *  the network from s to t, it passes from a game vertex, say between teams
 *  i and j, then through one of the team vertices i or j, classifying this
 *  game as being won by that team.
 **************************************************************************** */

import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.StdOut;

import java.util.ArrayList;
import java.util.List;

public class BaseballElimination {
    private int number;
    private int[][] games;
    private int[] wins;
    private int[] losses;
    private int[] remaining;
    private List<String> eliminated;
    private List<String> teams;
    private List<String>[] certificates;

    public BaseballElimination(
            String filename) {
        In file = new In(filename);

        number = file.readInt(); // Number of teams
        games = new int[number][number]; // Games remaining for each opponent
        wins = new int[number]; // Number of wins for each team
        losses = new int[number]; // Number of losses for each team
        remaining = new int[number]; // Games remaining for each team
        teams = new ArrayList<String>(); // List of team names
        certificates = (List<String>[]) new List[number]; // Opponents responsible for elimination
        eliminated = new ArrayList<String>(); // List of eliminated teams
        int maxWins = 0; // Largest current win total

        // Initialize variables from input file
        for (int line = 0; line < number; line++) {
            teams.add(file.readString());

            wins[line] = file.readInt();
            if (wins[line] > wins[maxWins]) {
                maxWins = line;
            }

            losses[line] = file.readInt();
            remaining[line] = file.readInt();
            for (int against = 0; against < number; against++) {
                games[line][against] = file.readInt();
            }
            certificates[line] = new ArrayList<String>();
        }

        // Check for trivial elimination
        for (int i = 0; i < number; i++) {
            for (int j = 0; j < number; j++) {
                if (j != i) {
                    if (wins[i] + remaining[i] < wins[j]) {
                        eliminated.add(teams.get(i));
                        if (!certificates[i].contains(teams.get(j)))
                            certificates[i].add(teams.get(j));
                    }
                }
            }
        }


        int comb = (number - 2) * (number - 1) / 2; // Combinations of matchups
        int v = 1 + number + comb; // Number of vertices in flow network
        int e = 3 * comb + number - 1; // Number of edges in flow network

        for (int t = 0; t < number; t++) {
            // If team is not winningest team or already eliminated, check
            if (t != maxWins && !eliminated.contains(teams.get(t))) {

                FlowNetwork fn = new FlowNetwork(v); // Flow network

                // Add vertices and edges representing games to flow network
                int vert = 1;
                for (int i = 0; i < number - 1; i++) {
                    for (int j = i + 1; j < number; j++) {
                        if (i != t && j != t) {
                            FlowEdge edge = new FlowEdge(0, vert, games[i][j]);
                            fn.addEdge(edge);
                            vert++;
                        }
                    }
                }

                // Add vertices and edges representing teams to flow network
                int g = 1;
                for (int i = comb + 1; i < comb + number - 1; i++) {
                    for (int j = i + 1; j < comb + number; j++) {
                        FlowEdge edge1 = new FlowEdge(g, i, Double.POSITIVE_INFINITY);
                        fn.addEdge(edge1);
                        FlowEdge edge2 = new FlowEdge(g, j, Double.POSITIVE_INFINITY);
                        fn.addEdge(edge2);
                        g++;
                    }
                }

                // Add t vertex and number of winnable games to flow network
                int vt = 0;
                for (int i = 0; i < number; i++) {
                    if (i != t) {
                        FlowEdge edge = new FlowEdge(vt + comb + 1, comb + number,
                                                     wins[t] + remaining[t] - wins[i]);
                        fn.addEdge(edge);
                        vt++;
                    }
                }

                FordFulkerson ff = new FordFulkerson(fn, 0, v - 1);
                for (FlowEdge fe : fn.adj(0)) {
                    if (fe.flow() != fe.capacity()) {
                        if (!eliminated.contains(teams.get(t))) eliminated.add(teams.get(t));
                        break;
                    }
                }

                if (!eliminated.isEmpty()) {
                    int c = 1;
                    for (int i = 0; i < number; i++) {
                        if (i != t) {
                            if (ff.inCut(c + comb)) {
                                certificates[t].add(teams.get(i));
                            }
                            c++;
                        }
                    }
                }
            }
        }
    }

    // Number of teams
    public int numberOfTeams() {
        return number;
    }

    // All teams
    public Iterable<String> teams() {
        return teams;
    }

    // Number of wins for given team
    public int wins(String team) {
        if (!teams.contains(team)) throw new IllegalArgumentException();
        int t = teams.indexOf(team);
        return wins[t];
    }

    // Number of losses for given team
    public int losses(String team) {
        if (!teams.contains(team)) throw new IllegalArgumentException();
        int t = teams.indexOf(team);
        return losses[t];
    }

    // Number of remaining games for given team
    public int remaining(String team) {
        if (!teams.contains(team)) throw new IllegalArgumentException();
        int t = teams.indexOf(team);
        return remaining[t];
    }

    // Number of remaining games between team1 and team2
    public int against(String team1,
                       String team2) {
        if (!teams.contains(team1) || !teams.contains(team2)) throw new IllegalArgumentException();
        int t1 = teams.indexOf(team1);
        int t2 = teams.indexOf(team2);
        return games[t1][t2];
    }

    public boolean isEliminated(String team) {
        if (!teams.contains(team)) throw new IllegalArgumentException();
        return eliminated.contains(team);
    }

    // Subset R of teams that eliminates given team; null if not eliminated
    public Iterable<String> certificateOfElimination(
            String
                    team) {
        if (!teams.contains(team)) throw new IllegalArgumentException();
        if (isEliminated(team)) {
            return certificates[teams.indexOf(team)];
        }
        else {
            return null;
        }
    }


    public static void main(String[] args) {
        BaseballElimination division = new BaseballElimination(args[0]);
        for (String team : division.teams()) {
            if (division.isEliminated(team)) {
                StdOut.print(team + " is eliminated by the subset R = { ");
                for (String t : division.certificateOfElimination(team)) {
                    StdOut.print(t + " ");
                }
                StdOut.println("}");
            }
            else {
                StdOut.println(team + " is not eliminated");
            }
        }
    }
}
