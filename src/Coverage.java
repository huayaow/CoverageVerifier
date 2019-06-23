import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The method to evaluate the t-way coverage of a given test suite.
 */
public class Coverage {

  private int parameter;
  private int[] value;

  // a list of constraints, each is represented by a conjunction of
  // boolean variables (start from 1)
  private ArrayList<int[]> constraint;

  // relation indicates the index of a given parameter value
  // For example, the mapping for CA(N;t,5,3) is as follows:
  //  p1  p2  p3  p4  p5
  //   1   4   7  10  13
  //   2   5   8  11  14
  //   3   6   9  12  15
  private int[][] relation;

  // constraint handler
  private ConstraintSolver solver ;


  public Coverage(int parameter, int[] value, ArrayList<int[]> constraint) {
    this.parameter = parameter;
    this.value = value;
    this.constraint = constraint;

    // set mapping relationship
    relation = new int[parameter][];
    int start = 1;
    for (int i = 0; i < parameter; i++) {
      relation[i] = new int[value[i]];
      for (int j = 0; j < value[i]; j++, start++) {
        relation[i][j] = start;
      }
    }

    // initialise constraint solver
    solver = new ConstraintSolver();
    solver.init(parameter, value, relation, constraint);
  }


  /**
   * Determine whether there has unfixed parameter values (-1) in the array.
   */
  private boolean isArray(List<int[]> ts) {
    for (int[] tc : ts) {
      if (!solver.isValid(tc))
        return false;

      for (int e : tc) {
        if (e == -1)
          return false;
      }
    }
    return true;
  }

  /**
   * Calculate the t-way combination coverage with the evolve-test-suite manner.
   * @return -1.0, if it is not a covering array, because it has unfixed or invalid rows
   *         otherwise, the double value indicates the t-way coverage obtained
   */
  public double coverageV2(List<int[]> ts, int t_way) {
    if (ts == null || ts.size() == 0)
      return 0;

    if (!isArray(ts))
      return -1.0;

    long total_space   = 0;  // number of all t-way combinations
    long total_covered = 0;  // number of valid combinations that are covered
    long total_invalid = 0;  // number of invalid t-way combinations

    // iterate each parameter combination
    List<int[]> allPC = allCombination(parameter, t_way);
    for (int[] position : allPC) {
      // number of all value combinations of these t parameters
      int len = combineValue(position, value);
      total_space += len ;

      int[] cover = new int[len];

      // remove invalid t-way combinations
      for (int e = 0 ; e < cover.length ; e++) {
        if (!isValidTuple(position, num2val(e, position, t_way, value))) {
          cover[e] = -1;
          total_invalid++;
        }
      }

      // go through t-way combinations in each row of the array
      int[] schema = new int[t_way];
      for (int[] tc : ts) {
        for (int k = 0; k < t_way; k++)
          schema[k] = tc[position[k]];

        int index = val2num(position, schema, t_way, value);
        if (cover[index] == -1) {
          System.out.println(">> " + Arrays.toString(position) + ", " + Arrays.toString(schema) + " <<");
        }
        if (cover[index] == 0) {
          cover[index] = 1;
          total_covered++;
        }

      }
    }

    return (double) total_covered / (double) (total_space - total_invalid);
  }

  public boolean isCoveringArray(List<int[]> ts, int t_way) {
    return coverageV2(ts, t_way) == 1.0;
  }

  /**
   * Determine whether a t-way tuple is constraints satisfying.
   */
  private boolean isValidTuple(final int[] pos, final int[] sch) {
    int[] test = new int[parameter];
    for (int i = 0, j = 0; i < parameter; i++) {
      if (i > pos[pos.length - 1])
        test[i] = -1;
      else if (i == pos[j])
        test[i] = sch[j++];
      else
        test[i] = -1;
    }
    return solver.isValid(test);
  }

  /**
   * Calculate all parameter combinations of C(n, m).
   * allCombination(4, 2) = {{0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}}
   * @param n number of parameters
   * @param m number of chosen parameters
   * @return all parameter combinations, each in a row
   */
  public static ArrayList<int[]> allCombination(int n, int m) {
    ArrayList<int[]> data = new ArrayList<>();
    dfs(data, new int[m], m, 1, n - m + 1, 0);
    return data;
  }

  private static void dfs(List<int[]> data, int[] list, int k_left, int from, int to, int index) {
    if (k_left == 0) {
      data.add(list.clone());
      return;
    }
    for (int i = from; i <= to; ++i ) {
      list[index++] = i - 1;
      dfs(data, list, k_left - 1, i + 1, to + 1, index);
      index -= 1;
    }
  }

  /**
   * Calculate the number of all possible value combinations among a given parameter set.
   * @param position indexes of chosen parameters
   * @param value    number of values of all parameters
   * @return number of value combinations among parameters
   */
  private static int combineValue(final int[] position, final int[] value) {
    int comb = 1;
    for (int k = 0; k < position.length; k++)
      comb = comb * value[position[k]];
    return comb;
  }

  /**
   * Calculate the index of a t-way value combination among
   * given parameters, where index starts at 0.
   *
   * val2num({0, 1}, {1, 2}, 2, {3, 3, 3, 3}) = 5, as the
   * orders of all 3^2 value combinations among parameters {0, 1}
   * are 0 0, 0 1, 0 2, 1 0, 1 1, 1 2, 2 0, 2 1, 2 2
   *
   * @param pos   indexes of chosen parameters
   * @param sch   a value combination
   * @param t     number of chosen parameters
   * @param value number of values of all parameters
   * @return index of sch
   */
  private static int val2num(final int[] pos, final int[] sch, int t, final int[] value) {
    int com = 1;
    int ret = 0;
    for (int k = t - 1; k >= 0; k--) {
      ret += com * sch[k];
      com = com * value[pos[k]];
    }
    return ret;
  }

  /**
   * Calculate the i-th t-way value combination among a given
   * parameter set, where index starts at 0.
   *
   * num2val(4, {1, 2}, 2, {3, 3, 3, 3}) = {1, 1}
   *
   * @param i     index of required value combination
   * @param pos   indexes of chosen parameters
   * @param t     number of chosen parameters
   * @param value number of values of all parameters
   * @return the i-th value combination
   */
  private static int[] num2val(int i, final int[] pos, int t, final int[] value) {
    int[] ret = new int[t];

    int div = 1;
    for (int k = t - 1; k > 0; k--)
      div = div * value[pos[k]];

    for (int k = 0; k < t - 1; k++) {
      ret[k] = i / div;
      i = i - ret[k] * div;
      div = div / value[pos[k + 1]];
    }
    ret[t - 1] = i / div;
    return ret;
  }

}
