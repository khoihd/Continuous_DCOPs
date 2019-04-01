package utilities;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import agent.DcopAgent;
import static agent.DcopInfo.*;
import function.Interval;
import function.multivariate.MultivariateQuadFunction;

public class Utilities {	
  public static String headerLine = "ID" + "\t" + "Alg" + "\t" + "noAgent" + "\t" + "Time (ms)" + "\t" + "Utility" + "\t"
      + "RootArgMax";
  	
  public static void writeToFile(DcopAgent agent) {    
    String alg = algTypes[DcopAgent.getAlgorithm()];
    
    String newFileName;
        
    if (DcopAgent.getAlgorithm() == APPROX_DPOP) {
      newFileName = "alg=" + alg + "_d=" + DcopAgent.getNoAgent() + "_domainSize=" + agent.getDomainSize() + "_numPoints="
          + DcopAgent.getNumberOfPoints() + "_numApproxAgents=" + agent.getNumberOfApproxAgents() + ".txt";
    } else {
      newFileName = "alg=" + alg + "_d=" + DcopAgent.getNoAgent() + "_domainSize=" + agent.getDomainSize() + "_numPoints="
          + DcopAgent.getNumberOfPoints() + "_gradientIteration=" + DcopAgent.getGradientIteration() + "_gradientRate="
          + DcopAgent.GRADIENT_SCALING_FACTOR + ".txt";  
    }
    
    if (DcopAgent.getInstanceID() == 0) {
      writeHeaderLineToFile(newFileName);
    }
    
    DecimalFormat df = new DecimalFormat("##.##");
    df.setRoundingMode(RoundingMode.DOWN);
    String line = null;
    
    line = "\n" + DcopAgent.getInstanceID() + "\t" + alg + "\t" + DcopAgent.getNoAgent() + "\t" + agent.getSimulatedTime() / 1000000.0 + "\t"
        + df.format(agent.getAggregatedUtility()) + "\t" + agent.getChosenValue();
    
    writeToFile(line, newFileName);
  }
	
	
	/**
	 * Code checked
	 * @param sortedValues
	 * @return list of sorted merged intervals
	 */
	public static List<Interval> createSortedInterval(Set<Double> sortedValues) {
    List<Double> sortedValueList = new ArrayList<>(sortedValues);
    List<Interval> sortedInterval = new ArrayList<>();
  
    for (int i = 0; i < sortedValueList.size() - 1; i++) {
      sortedInterval.add(new Interval(sortedValueList.get(i), sortedValueList.get(i + 1)));
    }
    
    return sortedInterval;
  }
	
  /**
   * @param func1 QuadraticUnaryFunction
   * @param func2 QuadraticUnaryFunction
   * @return a list of sorted intervals
   */
  public static Set<Double> solveUnaryQuadForValues(MultivariateQuadFunction func1, MultivariateQuadFunction func2, boolean isGettingSmallerInterval) {
    TreeSet<Double> valueIntervalSet = new TreeSet<>();

    func1.checkSameSelfAgent(func2);
    Interval intervalOfTheResult = null;
    
    if (!isGettingSmallerInterval) {
//      func1.checkSameSelfInterval(func2);
      intervalOfTheResult = func1.getCritFuncIntervalMap().get(func1.getOwner());
    }
    else {
      intervalOfTheResult = func1.getCritFuncIntervalMap().get(func1.getOwner()).intersectInterval(func2.getCritFuncIntervalMap().get(func2.getOwner()));
      if (null == intervalOfTheResult)
        return valueIntervalSet;
    }
    
    MultivariateQuadFunction diffFunc1Func2 = new MultivariateQuadFunction(func1.getA() - func2.getA(),
        func1.getB() - func2.getB(), func1.getC() - func2.getC(), func1.getOwner(), intervalOfTheResult);
    
    valueIntervalSet.addAll(diffFunc1Func2.solveForRootsInsideInterval());
    
    return valueIntervalSet;
  }
  
  public static double roundDouble(double value) {
    return new BigDecimal(value).setScale(10, RoundingMode.DOWN).stripTrailingZeros().doubleValue();
  }
	
  public static void writeHeaderLineToFile(String outputFile) {
    byte data[] = headerLine.getBytes();
    Path p = Paths.get(outputFile);
  
    try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(p, CREATE, APPEND))) {
      out.write(data, 0, data.length);
      out.flush();
      out.close();
    } catch (IOException x) {
      System.err.println(x);
    } 
	}
	
	public static void writeToFile(String line, String fileName) { 
	  byte data[] = line.getBytes();
	  Path p = Paths.get(fileName);

	  try (OutputStream out = new BufferedOutputStream(
	  Files.newOutputStream(p, CREATE, APPEND))) {
	  out.write(data, 0, data.length);
	  out.flush();
	  out.close();
	  } catch (IOException x) {
	  System.err.println(x);
	  }
	}
}
