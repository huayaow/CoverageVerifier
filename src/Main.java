import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) throws IOException {
    System.out.println("-------------------------------------------------------");
    System.out.println("                 Coverage Evaluator                    ");
    System.out.println("-------------------------------------------------------");
    System.out.println("3 - constrained CT experiment data");
    System.out.print("> ");

    Scanner sc = new Scanner(System.in);
    int in = sc.nextInt();
    switch (in) {
      case 3:
        System.out.print("the name of data directory: ");
        String root = sc.next();
        System.out.print("the maximum number of examined arrays for each file: ");
        int bound = sc.nextInt();
        System.out.println();
        constrainedExperiment(root, bound);
        break;
    }
  }


  /**
   * Check the t-way coverage of all files in the data directory.
   * @param path the root path of data directory
   * @param bound the maximum number of examined arrays for each file
   */
  private static void constrainedExperiment(String path, int bound) {
    String MODEL_PATH = "benchmark/%s.model";
    String CONSTRAINT_PATH = "benchmark/%s.constraints";

    // get all files
    List<File> files = new ArrayList<>();
    File root = new File(path);
    func(root, files);
    System.out.println("Check " + files.size() + " files in the " + path + " directory ...");

    List<File> problematic = new ArrayList<>();   // number of problematic files
    List<File> missing = new ArrayList<>();       // number of files that do not have 30 arrays

    int count = 0;
    for (File file : files) {
      // split to get model name
      String[] str = file.toPath().toString().split("/");
      int s1 = str[2].indexOf("_");
      int s2 = str[2].lastIndexOf("_");
      String algorithm = str[1];
      String handler = str[2].substring(0, s1);
      String name = str[2].substring(s1 + 1, s2);

      System.out.print("#" + count + " | " + algorithm + " + " + handler + " + " + name);
      count++;

      // read the model file
      CASAFileReader M = new CASAFileReader(String.format(MODEL_PATH, name), String.format(CONSTRAINT_PATH, name));
      CoverageEvaluator CE = new CoverageEvaluator(M.parameter, M.value, M.constraint);

      // read each of the covering array
      List<List<int[]>> all = allArrays(file);
      System.out.print(" | check " + bound + " (" + all.size() + ") arrays");

      // whether the file contains 30 arrays
      if (all.size() < 30 && !algorithm.equals("IPO"))
        missing.add(file);

      List<Integer> uncovered = new ArrayList<>();
      List<String> info = new ArrayList<>();
      int index = 0;
      for (List<int[]> array : all) {
        if (index == bound)
          break;

        // conduct coverage check
        double cov = CE.coverageV2(array, 2);
        if (cov != 1.0) {
          uncovered.add(index);
          if (cov == -1)
            info.add("unfixed or invalid rows");
          else
            info.add("not full coverage");
        }
        index += 1;
      }

      if (uncovered.size() == 0)
        System.out.print("\t \u2705\n");
      else {
        System.out.print("\t \u274C\n");
        for (int x = 0 ; x < uncovered.size() ; x++)
          System.out.println("    \u2717 array " + uncovered.get(x) + ": " + info.get(x));
        problematic.add(file);
      }
    }

    if (problematic.size() == 0)
      System.out.println("\nSeems all good \u1F60");
    else {
      System.out.println("\nThe following files are problematic:");
      problematic.forEach(System.out::println);
    }

  }

  private static void func(File file, List<File> files){
    File[] fs = file.listFiles();
    for (File f : fs) {
      if (f.isDirectory())
        func(f, files);
      if (f.isFile() && !f.getName().equals(".DS_Store"))
        files.add(f);
    }
  }

  /**
   * Get all test suites in the given file.
   */
  private static List<List<int[]>> allArrays(File file) {
    List<List<int[]>> all = new ArrayList<>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line = br.readLine();
      while (line != null) {
        // covering array head
        String[] str = line.trim().split(" ");
        double size = Double.valueOf(str[3]);
        double time = Double.valueOf(str[7]);

        // covering array body
        List<int[]> ca = new ArrayList<>();
        for (int i = 0 ; i < (int) size ; i++ ) {
          line = br.readLine();
          str = line.split(" ");
          int[] tc = new int[str.length];
          for (int k = 0 ; k < tc.length ; k++)
            tc[k] = Integer.valueOf(str[k]);
          ca.add(tc);
        }
        all.add(ca);

        line = br.readLine();
        line = br.readLine();
      }

    } catch (IOException e) {
      System.err.println(e.getMessage());
    }

    return all;
  }

}
