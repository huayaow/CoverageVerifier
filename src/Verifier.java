
public class Verifier {

  /**
   * @param args working mode and arguments
   *
   *  MODE-1: check all txt file in a directory (CCAG format):
   *          args[0] = dir, args[1] = dir_name, args[2] = repetition
   */
  public static void main(String[] args) {
    System.out.println("-------------------------------------------------------");
    System.out.println("                 Coverage Verifier                    ");
    System.out.println("-------------------------------------------------------");

    String in = args[0];
    switch (in) {
      case "dir":
        if (args.length != 3)
          return;
        String root = args[1];
        int bound = Integer.valueOf(args[2]);
        CheckCCAGFiles.constrainedExperiment(root, bound);
        break;
    }
  }

}
