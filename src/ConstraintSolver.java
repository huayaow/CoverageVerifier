import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Use the sat4j solver (http://www.sat4j.org) as the constraint handling technique.
 * Current version uses a basic SAT (boolean) encoding to model constraints.
 */
public class ConstraintSolver {

  /*
   *  To use the sat4j solver, each parameter value must be mapped
   *  into an integer value, which starts from 1.
   *
   *  For example, the mapping for CA(N;t,5,3) is as follows:
   *  p1  p2  p3  p4  p5
   *   1   4   7  10  13
   *   2   5   8  11  14
   *   3   6   9  12  15
   *
   *  A constraint is represented as a disjunction of literals.
   *  For example, the followings give the representations of
   *  two forbidden combinations.
   *  {0,  -1, 0, -1, -1} as [-1, -7]
   *  {-1, -1, 2,  0,  1} as [-9, -10, -14]
   */

  private int[][] relation;
  private Vector<int[]> basicConstraint;  // at-least & at-most constraints
  private Vector<int[]> hardConstraint;   // user specified constraints

  private ISolver solver;     // the SAT solver

  public ConstraintSolver() {
    basicConstraint = new Vector<>();
    hardConstraint = new Vector<>();
  }

  /**
   * Initialize a constraint solver.
   */
  public void init(int parameter, final int[] value, final int[][] relation, List<int[]> constraint) {
    basicConstraint = new Vector<>();
    hardConstraint = new Vector<>();
    this.relation = relation;

    // set at-least constraint
    for (int i = 0; i < parameter; i++) {
      basicConstraint.add(relation[i]);
    }

    // set at-most constraint
    for (int i = 0; i < parameter; i++) {
      for (int[] row : CoverageEvaluator.allCombination(value[i], 2)) {
        int[] tp = {0 - relation[i][row[0]], 0 - relation[i][row[1]]};
        basicConstraint.add(tp);
      }
    }

    // set hard constraints
    if (constraint != null)
      hardConstraint.addAll(constraint);

    // maximum number of variable and number of clauses
    int MAXVAR = relation[parameter-1][value[parameter-1]-1];
    int NBCLAUSES = basicConstraint.size() + hardConstraint.size();

    // initialize solver
    solver = SolverFactory.newDefault();
    solver.newVar(MAXVAR);
    solver.setExpectedNumberOfClauses(NBCLAUSES);

    try {
      for (int[] clause : basicConstraint)
        solver.addClause(new VecInt(clause));
      for (int[] clause : hardConstraint)
        solver.addClause(new VecInt(clause));
    } catch (ContradictionException e) {
      System.err.println("ConstraintSolver Contradiction Error: " + e.getMessage());
    }
  }

  /**
   * Determine whether a given complete or partial test case is
   * constraints satisfiable. Any free parameters are assigned
   * to value -1.
   * @param test a complete or partial test case
   */
  public boolean isValid(final int[] test) {
    if (hardConstraint.size() == 0)
      return true;

    // transfer test to clause representation
    ArrayList<Integer> list = new ArrayList<>();
    for (int i = 0; i < test.length; i++) {
      if (test[i] != -1)
        list.add(relation[i][test[i]]);
    }
    int[] clause = list.stream().mapToInt(i -> i).toArray();

    // determine validity
    boolean satisfiable = false;
    try {
      VecInt c = new VecInt(clause);
      IProblem problem = solver;
      satisfiable = problem.isSatisfiable(c);
    } catch (TimeoutException e) {
      System.err.println("ConstraintSolver Timeout Error: " + e.getMessage());
    }
    return satisfiable;
  }

}
