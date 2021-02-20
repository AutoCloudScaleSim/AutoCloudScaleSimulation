/*
 * Title:        AutoScaleSim Toolkit
 * Description:  AutoScaleSim (Auto-Scaling Simulation) Toolkit for Modeling and Simulation
 *               of Autonomous Systems for Web Applications in Cloud
 *
 * Copyright (c) 2018, Islamic Azad University, Jahrom, Iran
 *
 * Authors: Mohammad Sadegh Aslanpour, Adel Nadjaran Toosi, Javid Taheri
 * 
 */
package autoscalesim.applicationprovider.autoscaling;

import autoscalesim.applicationprovider.autoscaling.analyzerMethod.TripleExponentialSmoothing;
import autoscalesim.applicationprovider.autoscaling.analyzerMethod.TripleExponentialSmoothingConstant;
import autoscalesim.applicationprovider.autoscaling.analyzerMethod.TripleExponentialSmoothingConstantTuning;
import autoscalesim.applicationprovider.autoscaling.knowledgebase.AnalyzerHistory;
import autoscalesim.applicationprovider.ApplicationProvider;
import static autoscalesim.applicationprovider.ApplicationProvider.getMonitor;
import autoscalesim.applicationprovider.autoscaling.knowledgebase.MonitorEndUserHistory;
import autoscalesim.log.DateTime;
import autoscalesim.applicationprovider.autoscaling.knowledgebase.MonitorSLAHistory;
import autoscalesim.applicationprovider.autoscaling.knowledgebase.MonitorVmHistory;
import static autoscalesim.log.ExperimentalResult.error;
import static autoscalesim.log.ExperimentalResult.errorChecker;;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cloudbus.cloudsim.Log;
/**
 * Analyzer class is the second phase of auto-scaling, where the monitored parameters are analyzed.
 * Its Inputs are a collection of monitored parameters regarding resources, SLA and user's behavior
 * Its action is to use simple or complex methods to analyze all parameters.
 * Its outputs are a more accurate value for each parameter.
 */

class DoubleExpState{
    double level;
    double trend;
    DoubleExpState(){
        level=Double.MIN_VALUE;
        trend=Double.MIN_VALUE;
    }
}

public class Analyzer {
    
    private ArrayList<AnalyzerHistory> historyList;
    
    private String[] analysisMethod;
    private final int timeWindow;
    private final double[] sESAlpha;
    private double beta=0.3;
    private int tesStartLimit=2*1440;
    private double[] oldSESOutput={
            Double.MIN_VALUE,
            Double.MIN_VALUE,
            Double.MIN_VALUE,
            Double.MIN_VALUE,
            Double.MIN_VALUE,
            Double.MIN_VALUE,
            Double.MIN_VALUE,
            Double.MIN_VALUE,
            Double.MIN_VALUE,
            Double.MIN_VALUE

    };

    private DoubleExpState[] oldDESState={
            new DoubleExpState(),
            new DoubleExpState(),
            new DoubleExpState(),
            new DoubleExpState(),
            new DoubleExpState(),
            new DoubleExpState(),
            new DoubleExpState(),
            new DoubleExpState(),
            new DoubleExpState(),
            new DoubleExpState()
    };

    private TripleExponentialSmoothingConstant[] TES_CONSTANTS= {
            new TripleExponentialSmoothingConstant(false), //ANLZ_CPUUtil
            new TripleExponentialSmoothingConstant(false) , //ANLZ_VMCount
            new TripleExponentialSmoothingConstant(false) , //ANLZ_Throughput
            new TripleExponentialSmoothingConstant(false) , //ANLZ_ResponseTime
            new TripleExponentialSmoothingConstant(false) , //ANLZ_DelayTime
            new TripleExponentialSmoothingConstant(false) , //ANLZ_SLAVCount
            new TripleExponentialSmoothingConstant(false) , //ANLZ_SLAVPercentage
            new TripleExponentialSmoothingConstant(false) , //ANLZ_SLAVTime
            new TripleExponentialSmoothingConstant(false) , //ANLZ_FailedCloudlet
            new TripleExponentialSmoothingConstant(false) , //ANLZ_FutureWorkload
    };


    private int TES_PERIOD=1440;
    private int TES_nPREDICTIONS=30;


    /**
     * 
     * @param analysisMethod
     * @param timeWindow
     * @param sESAlpha 
     */
    public Analyzer(String[]analysisMethod, 
                                            int timeWindow,  
                                            double sESAlpha[]
                                            ){

        setAnalysisMethod(analysisMethod);
        
        setHistoryList(new ArrayList<AnalyzerHistory>());
        
        this.timeWindow = timeWindow;
        this.sESAlpha = sESAlpha;
        
    }
    
   /**
    * Analyzing effective parameters
    */
    public void doAnalysis(){
/* initialing Analysis Parameters */
        
        // RESOURCE-AWARE
        double cpuUtilization = 0; 
        double vmCount = 0;
        double throughput = 0;
        
         // SLA-AWARE
        double responseTime = 0;
        double delayTime = 0; 
        double  slavCount = 0;
        double slavPercentage = 0;
        double slavTime = 0;
        double failedCloudlet = 0;
        
        // User-Aware
        double futureWorkload = 0;

        // Env. parameters have been set already.
        
/* calculation of analysis parameters */

        // RESOURCE-AWARE
        cpuUtilization = ANLZ_CPUUtil(); 
        vmCount = ANLZ_VMCount();
        throughput = ANLZ_Throughput();
        
        // SLA-AWARE
        responseTime = ANLZ_ResponseTime();
        delayTime = ANLZ_DelayTime();
        
        slavCount = ANLZ_SLAVCount();
        slavPercentage = ANLZ_SLAVPercentage();
        slavTime = ANLZ_SLAVTime();
        failedCloudlet = ANLZ_FailedCloudlet();
        
        // USER-BEHAVIOR-AWARE
        futureWorkload = ANLZ_FutureWorkload();
        
        // Environment-Aware
            // History class set these parameters itself.
        
        /* SAVE analysis results to history */
        AnalyzerHistory analyzerHistory = new AnalyzerHistory(
                                                             cpuUtilization
                                                            , vmCount
                                                            , throughput
                
                                                            , responseTime
                                                            , delayTime
                                                            , slavCount
                                                            , slavPercentage
                                                            , slavTime
                                                            , failedCloudlet
                
                                                            , futureWorkload
                                                            );
        getHistoryList().add(analyzerHistory);
    }
    
    /**
     * Analyzing CPU utilization
     * @return 
     */
    private double ANLZ_CPUUtil(){
        double analyzedCPUUtilization = -1;
        // Get VM monitor history
        ArrayList<MonitorVmHistory> tmpVmHistoryList = getMonitor().getVmHistoryList();
        int sizeVmHistory = tmpVmHistoryList.size();
        // Set the latest monitored Cpu utilization item
        double parameter = tmpVmHistoryList.get(sizeVmHistory - 1).getCpuUtilizationByAllTier();
        // Set a list of monitored Cpu utilization
        int window = timeWindow;
        if (sizeVmHistory < window)
            window = sizeVmHistory;
        
        double parameterList[] = new double[window];
        double tesHistory[] = new double[window];
        for(int i = 0; i< window;i++){
            parameterList[i] = tmpVmHistoryList.get(sizeVmHistory - 1 - i).getCpuUtilizationByAllTier();
            tesHistory[i] = tmpVmHistoryList.get(sizeVmHistory-window + i).getCpuUtilizationByAllTier();
        }
        
                     
        //Index 0 of AnalysisMethod variable indicates the analyzing method for analyzing 'CPU utilization'    
        switch(getAnalysisMethod()[0]){
            // Simple
            case "SIMPLE": 
                analyzedCPUUtilization = parameter;
                break;
                
            // Moving Average
            case "COMPLEX_MA": 
                    analyzedCPUUtilization = calculateMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average
            case "COMPLEX_WMA": 
                    analyzedCPUUtilization = calculateWeightedMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average (weighting by Fibonacci numbers)
            case "COMPLEX_WMAfibo": 
                analyzedCPUUtilization = calculateWeightedMovingAverageFibonacci(parameterList);
                break;
                
            // Single exponential smoothing
            case "COMPLEX_SES":
                analyzedCPUUtilization = calculateSingleExponentialSmoothing(parameter, oldSESOutput[0], sESAlpha[0]);
                oldSESOutput[0] = analyzedCPUUtilization;
                break;
            default:
                analyzedCPUUtilization=TripleExponentialSmoothingVariant(AnalyzerParameter.CPUUtil,tesHistory,parameter);
        }
        
        return analyzedCPUUtilization;
    }
    
    /**
     * Analyzing VMs count
     * @return 
     */
    private double ANLZ_VMCount(){
        double analyzedVmCount = -1;
        // Get VM monitor history
        ArrayList<MonitorVmHistory> tmpVmHistoryList = getMonitor().getVmHistoryList();
        int sizeVmHistory = tmpVmHistoryList.size();
        // Set the latest monitored VM count item
        double parameter = tmpVmHistoryList.get(sizeVmHistory - 1).getVms();
        // Set a list of monitored VM count
        int window = timeWindow;
        if (sizeVmHistory < window)
            window = sizeVmHistory;
        
        double parameterList[] = new double[window];
        double tesHistory[] = new double[window];
        for(int i = 0; i< window;i++){
            parameterList[i] = tmpVmHistoryList.get(sizeVmHistory - 1 - i).getVms();
            tesHistory[i] = tmpVmHistoryList.get(sizeVmHistory-window + i).getVms();
        }
        
                     
        //Index 1 of AnalysisMethod variable indicates the analyzing method for analyzing 'VM count'    
        switch(getAnalysisMethod()[1]){
            // Simple
            case "SIMPLE": 
                analyzedVmCount = parameter;
                break;
                
            // Moving Average
            case "COMPLEX_MA": 
                    analyzedVmCount = calculateMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average
            case "COMPLEX_WMA": 
                    analyzedVmCount = calculateWeightedMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average (weighting by Fibonacci numbers)
            case "COMPLEX_WMAfibo": 
                analyzedVmCount = calculateWeightedMovingAverageFibonacci(parameterList);
                break;
                
            // Single exponential smoothing
            case "COMPLEX_SES":
                analyzedVmCount = calculateSingleExponentialSmoothing(parameter, oldSESOutput[1], sESAlpha[1]);
                oldSESOutput[1] = analyzedVmCount;
                break;
            default:
                analyzedVmCount=TripleExponentialSmoothingVariant(AnalyzerParameter.VMCount,tesHistory,parameter);
        }
        
        return analyzedVmCount;
    }
    
    /**
     * Analyzing Throughput
     * @return 
     */
    private double ANLZ_Throughput(){
        double analyzedThroughput = -1;
        // Get VM monitor history
        ArrayList<MonitorVmHistory> tmpVmHistoryList = getMonitor().getVmHistoryList();
        int sizeVmHistory = tmpVmHistoryList.size();
        // Set the latest monitored Throughout item
        double parameter = tmpVmHistoryList.get(sizeVmHistory - 1).getThroughputFinishedCloudletsAllTiers();
        // Set a list of monitored Throughout
        int window = timeWindow;
        if (sizeVmHistory < window)
            window = sizeVmHistory;
        
        double parameterList[] = new double[window];
        double tesHistory[] = new double[window];
        for(int i = 0; i< window;i++){
            parameterList[i] = tmpVmHistoryList.get(sizeVmHistory - 1 - i).getThroughputFinishedCloudletsAllTiers();
            tesHistory[i] = tmpVmHistoryList.get(sizeVmHistory-window + i).getThroughputFinishedCloudletsAllTiers();
        }
        
                     
        //Index 2 of AnalysisMethod variable indicates the analyzing method for analyzing 'Throughout'    
        switch(getAnalysisMethod()[2]){
            // Simple
            case "SIMPLE": 
                analyzedThroughput = parameter;
                break;
                
            // Moving Average
            case "COMPLEX_MA": 
                    analyzedThroughput = calculateMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average
            case "COMPLEX_WMA": 
                    analyzedThroughput = calculateWeightedMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average (weighting by Fibonacci numbers)
            case "COMPLEX_WMAfibo": 
                analyzedThroughput = calculateWeightedMovingAverageFibonacci(parameterList);
                break;
                
            // Single exponential smoothing
            case "COMPLEX_SES":
                analyzedThroughput = calculateSingleExponentialSmoothing(parameter, oldSESOutput[2], sESAlpha[2]);
                oldSESOutput[2] = analyzedThroughput;
                break;
            default:
                analyzedThroughput=TripleExponentialSmoothingVariant(AnalyzerParameter.Throughput,tesHistory,parameter);
        }
        return analyzedThroughput;
    }
    
    /**
     * Analyzing Response Time
     * @return 
     */
    private double ANLZ_ResponseTime(){
        double analyzedResponseTime = -1;
        
        // Get SLA monitor history
        ArrayList<MonitorSLAHistory> tmpSLAHistoryList = getMonitor().getSLAHistoryList();
        int sizeSLAHistory = tmpSLAHistoryList.size();
        // Set the latest monitored Response Time item
        double parameter = tmpSLAHistoryList.get(sizeSLAHistory - 1).getAvgResponseTimePerAllTiers();
        // Set a list of monitored Response Time
        int window = timeWindow;
        if (sizeSLAHistory < window)
            window = sizeSLAHistory;
        
        double parameterList[] = new double[window];
        double tesHistory[] = new double[window];
        for(int i = 0; i< window;i++){
            parameterList[i] = tmpSLAHistoryList.get(sizeSLAHistory - 1 - i).getAvgResponseTimePerAllTiers();
            tesHistory[i] = tmpSLAHistoryList.get(sizeSLAHistory-window + i).getAvgResponseTimePerAllTiers();
        }
        
                     
        //Index 3 of AnalysisMethod variable indicates the analyzing method for analyzing 'Response Time'    
        switch(getAnalysisMethod()[3]){
            // Simple
            case "SIMPLE": 
                analyzedResponseTime = parameter;
                break;
                
            // Moving Average
            case "COMPLEX_MA": 
                    analyzedResponseTime = calculateMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average
            case "COMPLEX_WMA": 
                    analyzedResponseTime = calculateWeightedMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average (weighting by Fibonacci numbers)
            case "COMPLEX_WMAfibo": 
                analyzedResponseTime = calculateWeightedMovingAverageFibonacci(parameterList);
                break;
                
            // Single exponential smoothing
            case "COMPLEX_SES":
                analyzedResponseTime = calculateSingleExponentialSmoothing(parameter, oldSESOutput[3], sESAlpha[3]);
                oldSESOutput[3] = analyzedResponseTime;
                break;
            default:
                analyzedResponseTime=TripleExponentialSmoothingVariant(AnalyzerParameter.ResponseTime,tesHistory,parameter);
        }
        return analyzedResponseTime;
    }
    
    /**
     * Analyzing Delay Time
     * @return 
     */
    private double ANLZ_DelayTime(){
        double analyzedDelayTime = -1;
        
        // Get SLA monitor history
        ArrayList<MonitorSLAHistory> tmpSLAHistoryList = getMonitor().getSLAHistoryList();
        int sizeSLAHistory = tmpSLAHistoryList.size();
        // Set the latest monitored Delay Time item
        double parameter = tmpSLAHistoryList.get(sizeSLAHistory - 1).getAvgDelayTimePerAllTiers();
        // Set a list of monitored Delay Time
        int window = timeWindow;
        if (sizeSLAHistory < window)
            window = sizeSLAHistory;
        
        double parameterList[] = new double[window];
        double tesHistory[] = new double[window];
        for(int i = 0; i< window;i++){
            parameterList[i] = tmpSLAHistoryList.get(sizeSLAHistory - 1 - i).getAvgDelayTimePerAllTiers();
            tesHistory[i] = tmpSLAHistoryList.get(sizeSLAHistory-window + i).getAvgDelayTimePerAllTiers();
        }
        
                     
        //Index 4 of AnalysisMethod variable indicates the analyzing method for analyzing 'Delay Time'    
        switch(getAnalysisMethod()[4]){
            // Simple
            case "SIMPLE": 
                analyzedDelayTime = parameter;
                break;
                
            // Moving Average
            case "COMPLEX_MA": 
                    analyzedDelayTime = calculateMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average
            case "COMPLEX_WMA": 
                    analyzedDelayTime = calculateWeightedMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average (weighting by Fibonacci numbers)
            case "COMPLEX_WMAfibo": 
                analyzedDelayTime = calculateWeightedMovingAverageFibonacci(parameterList);
                break;
                
            // Single exponential smoothing
            case "COMPLEX_SES":
                analyzedDelayTime = calculateSingleExponentialSmoothing(parameter, oldSESOutput[4], sESAlpha[4]);
                oldSESOutput[4] = analyzedDelayTime;
                break;
            default:
                analyzedDelayTime=TripleExponentialSmoothingVariant(AnalyzerParameter.DelayTime,tesHistory,parameter);
        }
        return analyzedDelayTime;
    }
    
    /**
     * Analyzing SLA violation count
     * @return 
     */
    private double ANLZ_SLAVCount(){
        double analyzedSLAVCount = -1;
        
        // Get SLA monitor history
        ArrayList<MonitorSLAHistory> tmpSLAHistoryList = getMonitor().getSLAHistoryList();
        int sizeSLAHistory = tmpSLAHistoryList.size();
        // Set the latest monitored SLA Violation count item
        double parameter = tmpSLAHistoryList.get(sizeSLAHistory - 1).getSlavNumberByAllTier();
        // Set a list of monitored SLA Violation Count
        int window = timeWindow;
        if (sizeSLAHistory < window)
            window = sizeSLAHistory;
        
        double parameterList[] = new double[window];
        double tesHistory[] = new double[window];
        for(int i = 0; i< window;i++){
            parameterList[i] = tmpSLAHistoryList.get(sizeSLAHistory - 1 - i).getSlavNumberByAllTier();
            tesHistory[i] = tmpSLAHistoryList.get(sizeSLAHistory-window + i).getSlavNumberByAllTier();
        }
        
                     
        //Index 5 of AnalysisMethod variable indicates the analyzing method for analyzing 'SLA Violation Count'    
        switch(getAnalysisMethod()[5]){
            // Simple
            case "SIMPLE": 
                analyzedSLAVCount = parameter;
                break;
                
            // Moving Average
            case "COMPLEX_MA": 
                    analyzedSLAVCount = calculateMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average
            case "COMPLEX_WMA": 
                    analyzedSLAVCount = calculateWeightedMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average (weighting by Fibonacci numbers)
            case "COMPLEX_WMAfibo": 
                analyzedSLAVCount = calculateWeightedMovingAverageFibonacci(parameterList);
                break;
                
            // Single exponential smoothing
            case "COMPLEX_SES":
                analyzedSLAVCount = calculateSingleExponentialSmoothing(parameter, oldSESOutput[5], sESAlpha[5]);
                oldSESOutput[5] = analyzedSLAVCount;
                break;
            default:
                analyzedSLAVCount=TripleExponentialSmoothingVariant(AnalyzerParameter.SLAVCount,tesHistory,parameter);
        }
        
        return analyzedSLAVCount;
    }
    
    /**
     * Analyzing SLA violation percentage
     * @return 
     */
    private double ANLZ_SLAVPercentage(){
        double analyzedSLAVPercentage = -1;
                      
        // Get SLA monitor history
        ArrayList<MonitorSLAHistory> tmpSLAHistoryList = getMonitor().getSLAHistoryList();
        int sizeSLAHistory = tmpSLAHistoryList.size();
        // Set the latest monitored SLA Violation Percent item
        double parameter = tmpSLAHistoryList.get(sizeSLAHistory - 1).getSlavPercent();
        // Set a list of monitored SLA Violation Percent
        int window = timeWindow;
        if (sizeSLAHistory < window)
            window = sizeSLAHistory;
        
        double parameterList[] = new double[window];
        double tesHistory[] = new double[window];
        for(int i = 0; i< window;i++){
            parameterList[i] = tmpSLAHistoryList.get(sizeSLAHistory - 1 - i).getSlavPercent();
            tesHistory[i] = tmpSLAHistoryList.get(sizeSLAHistory-window + i).getSlavPercent();
        }
        
                     
        //Index 6 of AnalysisMethod variable indicates the analyzing method for analyzing 'SLA Violation Percent'    
        switch(getAnalysisMethod()[6]){
            // Simple
            case "SIMPLE": 
                analyzedSLAVPercentage = parameter;
                break;
                
            // Moving Average
            case "COMPLEX_MA": 
                    analyzedSLAVPercentage = calculateMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average
            case "COMPLEX_WMA": 
                    analyzedSLAVPercentage = calculateWeightedMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average (weighting by Fibonacci numbers)
            case "COMPLEX_WMAfibo": 
                analyzedSLAVPercentage = calculateWeightedMovingAverageFibonacci(parameterList);
                break;
                
            // Single exponential smoothing
            case "COMPLEX_SES":
                analyzedSLAVPercentage = calculateSingleExponentialSmoothing(parameter, oldSESOutput[6], sESAlpha[6]);
                oldSESOutput[6] = analyzedSLAVPercentage;
                break;
            default:
                analyzedSLAVPercentage=TripleExponentialSmoothingVariant(AnalyzerParameter.SLAVPercentage,tesHistory,parameter);
        }
        return analyzedSLAVPercentage;
    }
    
    /**
     * Analyzing SLA violation time (second)
     * @return 
     */
    private double ANLZ_SLAVTime(){
        double analyzedSLAVTime = -1;
        
        // Get SLA monitor history
        ArrayList<MonitorSLAHistory> tmpSLAHistoryList = getMonitor().getSLAHistoryList();
        int sizeSLAHistory = tmpSLAHistoryList.size();
        // Set the latest monitored SLA Violation Time item
        double parameter = tmpSLAHistoryList.get(sizeSLAHistory - 1).getSlavSecondByAlltier();
        // Set a list of monitored SLA Violation Time
        int window = timeWindow;
        if (sizeSLAHistory < window)
            window = sizeSLAHistory;
        
        double parameterList[] = new double[window];
        double tesHistory[] = new double[window];
        for(int i = 0; i< window;i++){
            parameterList[i] = tmpSLAHistoryList.get(sizeSLAHistory - 1 - i).getSlavSecondByAlltier();
            tesHistory[i] = tmpSLAHistoryList.get(sizeSLAHistory-window + i).getSlavSecondByAlltier();
        }
        
                     
        //Index 7 of AnalysisMethod variable indicates the analyzing method for analyzing 'SLA Violation Time'    
        switch(getAnalysisMethod()[7]){
            // Simple
            case "SIMPLE": 
                analyzedSLAVTime = parameter;
                break;
                
            // Moving Average
            case "COMPLEX_MA": 
                    analyzedSLAVTime = calculateMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average
            case "COMPLEX_WMA": 
                    analyzedSLAVTime = calculateWeightedMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average (weighting by Fibonacci numbers)
            case "COMPLEX_WMAfibo": 
                analyzedSLAVTime = calculateWeightedMovingAverageFibonacci(parameterList);
                break;
                
            // Single exponential smoothing
            case "COMPLEX_SES":
                analyzedSLAVTime = calculateSingleExponentialSmoothing(parameter, oldSESOutput[7], sESAlpha[7]);
                oldSESOutput[7] = analyzedSLAVTime;
                break;
            default:
                analyzedSLAVTime=TripleExponentialSmoothingVariant(AnalyzerParameter.SLAVTime,tesHistory,parameter);
        }
        
        return analyzedSLAVTime;
    }
    
    /**
     * Analyzing Failed cloudlets
     * @return 
     */
    private double ANLZ_FailedCloudlet(){
        double analyzedFailedCloudlet = -1;
        
        // Get SLA monitor history
        ArrayList<MonitorSLAHistory> tmpSLAHistoryList = getMonitor().getSLAHistoryList();
        int sizeSLAHistory = tmpSLAHistoryList.size();
        // Set the latest monitored Failed Cloudlet item
        double parameter = tmpSLAHistoryList.get(sizeSLAHistory - 1).getCloudletFailedCounter();
        // Set a list of monitored Failed Cloudlet
        int window = timeWindow;
        if (sizeSLAHistory < window)
            window = sizeSLAHistory;
        
        double parameterList[] = new double[window];
        double tesHistory[] = new double[window];
        for(int i = 0; i< window;i++){
            parameterList[i] = tmpSLAHistoryList.get(sizeSLAHistory - 1 - i).getCloudletFailedCounter();
            tesHistory[i] = tmpSLAHistoryList.get(sizeSLAHistory-window + i).getCloudletFailedCounter();
        }
        
                     
        //Index 8 of AnalysisMethod variable indicates the analyzing method for analyzing 'Failed Cloudlet'    
        switch(getAnalysisMethod()[8]){
            // Simple
            case "SIMPLE": 
                analyzedFailedCloudlet = parameter;
                break;
                
            // Moving Average
            case "COMPLEX_MA": 
                    analyzedFailedCloudlet = calculateMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average
            case "COMPLEX_WMA": 
                    analyzedFailedCloudlet = calculateWeightedMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average (weighting by Fibonacci numbers)
            case "COMPLEX_WMAfibo": 
                analyzedFailedCloudlet = calculateWeightedMovingAverageFibonacci(parameterList);
                break;
                
            // Single exponential smoothing
            case "COMPLEX_SES":
                analyzedFailedCloudlet = calculateSingleExponentialSmoothing(parameter, oldSESOutput[8], sESAlpha[8]);
                oldSESOutput[8] = analyzedFailedCloudlet;
                break;
            default:
                analyzedFailedCloudlet=TripleExponentialSmoothingVariant(AnalyzerParameter.FailedCloudlet,tesHistory,parameter);
        }
        
        return analyzedFailedCloudlet;
    }
    
        
    /**
     * Analyzing Workload
     * @return 
     */
    private double ANLZ_FutureWorkload(){
        double analyzedFutureWorkload = -1;
        
        // Get EndUser monitor history
        ArrayList<MonitorEndUserHistory> tmpEndUserHistoryList = getMonitor().getEndUserHistoryList();
        int sizeEndUserHistory = tmpEndUserHistoryList.size();
        // Set the latest monitored Workload item
        double parameter = tmpEndUserHistoryList.get(sizeEndUserHistory - 1).getRequestsPerAllTier();

        // Set a list of monitored Workload
        int window = timeWindow;
        if (sizeEndUserHistory < window)
            window = sizeEndUserHistory;
        
        double parameterList[] = new double[window];
        double tesHistory[] = new double[window];
        for(int i = 0; i< window;i++){
            parameterList[i] = tmpEndUserHistoryList.get(sizeEndUserHistory - 1 - i).getRequestsPerAllTier();
            tesHistory[i] = tmpEndUserHistoryList.get(sizeEndUserHistory-window + i).getRequestsPerAllTier();
        }


        //Index 9 of AnalysisMethod variable indicates the analyzing method for analyzing 'Future Workload'
        switch(getAnalysisMethod()[9]){
            //Simple
            case "SIMPLE": 
                analyzedFutureWorkload = parameter;
            // Moving Average
            case "COMPLEX_MA": 
                    analyzedFutureWorkload = calculateMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average
            case "COMPLEX_WMA": 
                    analyzedFutureWorkload = calculateWeightedMovingAverage(parameterList);
                break;
                
            // Weighted Moving Average (weighting by Fibonacci numbers)
            case "COMPLEX_WMAfibo": 
                analyzedFutureWorkload = calculateWeightedMovingAverageFibonacci(parameterList);
                break;
                
            // Single exponential smoothing
            case "COMPLEX_SES":
                analyzedFutureWorkload = calculateSingleExponentialSmoothing(parameter, oldSESOutput[9], sESAlpha[9]);
                oldSESOutput[9] = analyzedFutureWorkload;
                break;
            default:
                analyzedFutureWorkload=TripleExponentialSmoothingVariant(AnalyzerParameter.FutureWorkload,tesHistory,parameter);
        }
        return analyzedFutureWorkload;
    }
    
    /**
     * Analyzes the indicated parameter by Moving Average method
     * @param history
     * @return 
     */

    private double TripleExponentialSmoothingVariant(AnalyzerParameter analyzerParameter,double[] history, double parameter){
        ArrayList<Double> data=new ArrayList<>();
        for(double d: history){
            data.add(d+0.0000001);
        }
        double analyzedValue = -1;
        int index=analyzerParameter.getIndex();

        // adapting
        if(history.length >= this.tesStartLimit){
            TES_CONSTANTS[index]=new TripleExponentialSmoothingConstantTuning(TES_CONSTANTS[index]).getBestFittedConstant(data,TES_PERIOD);
        }
        TripleExponentialSmoothingConstant tesConst= TES_CONSTANTS[index];

        switch (getAnalysisMethod()[index]){
            case "TES":
                if(history.length < this.tesStartLimit){
                    analyzedValue = calculateDoubleExponentialSmoothing(parameter,oldDESState[index],sESAlpha[index],beta);
                    oldSESOutput[index] = analyzedValue;
                }
                else
                    analyzedValue =calculateTripleExponentialSmoothing(data,tesConst,TES_PERIOD,TES_nPREDICTIONS);
                break;
            case "TES_UPPERBOUND":
                if(history.length < this.tesStartLimit){
                    analyzedValue = calculateDoubleExponentialSmoothing(parameter,oldDESState[index],sESAlpha[index],beta);
                    oldSESOutput[index] = analyzedValue;
                }
                else
                    analyzedValue =calculateTripleExponentialSmoothingUsingUpperBound(data,tesConst,TES_PERIOD,TES_nPREDICTIONS);
                break;
            case "AVERAGE_TES":
                if(history.length < this.tesStartLimit){
                    analyzedValue = calculateDoubleExponentialSmoothing(parameter,oldDESState[index],sESAlpha[index],beta);
                    oldSESOutput[index] = analyzedValue;
                }
                else
                    analyzedValue =calculateAverageTripleExponentialSmoothing(data,tesConst,TES_PERIOD,TES_nPREDICTIONS);
                break;
            case "AVERAGE_TES_UPPERBOUND":
                if(history.length < this.tesStartLimit){
                    analyzedValue = calculateDoubleExponentialSmoothing(parameter,oldDESState[index],sESAlpha[index],beta);
                    oldSESOutput[index] = analyzedValue;
                }
                else
                    analyzedValue =calculateAverageTripleExponentialSmoothingUsingUpperBound(data,tesConst, TES_PERIOD,TES_nPREDICTIONS);
                break;
            case "DES":
                analyzedValue = calculateDoubleExponentialSmoothing(parameter,oldDESState[index],sESAlpha[index],beta);
                break;
            default:
                errorChecker = true;
                error += "Error in Analyzer class- resource aware analayzer - Future Workload analysis method not found";
                Log.printLine("Error in Analyzer class- resource aware analayzer - Future Workload analysis method not found");
        }
//        double[] parameterList,TripleExponentialSmoothingConstant tesConst,int period,int nPredictions
        return  analyzedValue;
    }


    private double calculateMovingAverage(double[] parameterList){
        double sumMovingAverage = 0;
        for(int i = 0; i < parameterList.length; i++){
            sumMovingAverage += parameterList[i];
        }
        return sumMovingAverage / parameterList.length;
    }
    
    /**
     * Analyzes the indicated parameter by Weighted Moving average method
     * @param parameterList
     * @return 
     */
    private double calculateWeightedMovingAverage(double[] parameterList){
        double sumWeightedItem = 0;
        double sumWeight = 0;
        int weight = 1;

        for(int i = 0; i < parameterList.length; i++){
            sumWeightedItem += parameterList[i] * weight;  

            sumWeight += weight;
            weight++;
        }
        return sumWeightedItem / sumWeight;
    }
    
    /**
     * Analyzes the indicated parameter by Weighted Moving average method.
     * This method uses Fibonacci technique.
     * @param parameterList
     * @return 
     */
    private double calculateWeightedMovingAverageFibonacci(double[] parameterList){
        double sumWeightedItems = 0;
        double sumWeight = 0;
        int weight;
        int fibo1 = 0; 
        int fibo2 = 1;
        for(int i = 0; i < parameterList.length; i++){
            weight = fibo1 + fibo2;
            sumWeightedItems += parameterList[i] * weight;
            sumWeight += weight;
            fibo1 = fibo2; fibo2 = weight;                
        }
        return sumWeightedItems / sumWeight;
    }
    
    /**
     * Analyzes the indicated parameter by Single Exponential Parameter method
     * @param parameter
     * @param oldSESOutput
     * @param alpha
     * @return 
     */
    private double calculateSingleExponentialSmoothing(double parameter, double oldSESOutput, double alpha){
        if (oldSESOutput == Double.MIN_VALUE) 
            oldSESOutput = parameter;
        
        return (alpha * parameter) + ((1 - alpha) * oldSESOutput);
    }

    private double calculateDoubleExponentialSmoothing(double parameter, DoubleExpState oldDoubleExpState, double alpha,double beta){
        if (oldDoubleExpState.level == Double.MIN_VALUE)
            oldDoubleExpState.level = parameter;
        if (oldDoubleExpState.trend == Double.MIN_VALUE)
            oldDoubleExpState.trend = parameter;

        double newLevel = alpha*parameter + (1-alpha)*(oldDoubleExpState.level-oldDoubleExpState.trend);
        double newTrend = beta * (newLevel-oldDoubleExpState.level) + (1-beta)*oldDoubleExpState.trend;
        oldDoubleExpState.level = newLevel;
        oldDoubleExpState.trend = newTrend;
        return newLevel+newTrend;
    }

    private double calculateTripleExponentialSmoothing(List<Double> data,TripleExponentialSmoothingConstant tesConst,int period,int nPredictions){

        boolean debug=false;
        TripleExponentialSmoothing tes=new TripleExponentialSmoothing();
        ArrayList<Double> predictions= (ArrayList<Double>) tes.forecast(data,tesConst,period,nPredictions,debug);
        return predictions.get(data.size()+15);
    }

    private double calculateTripleExponentialSmoothingUsingUpperBound(List<Double> data,TripleExponentialSmoothingConstant tesConst,int period,int nPredictions){
        boolean debug=false;
        TripleExponentialSmoothing tes=new TripleExponentialSmoothing();
        ArrayList<Double> predictions= (ArrayList<Double>) tes.forecast(data,tesConst,period,nPredictions,debug);

        return tes.predictedUpperBound.get(data.size()+nPredictions-5);
    }

    private double calculateAverageTripleExponentialSmoothingUsingUpperBound(List<Double> data,TripleExponentialSmoothingConstant tesConst,int period,int nPredictions){
        boolean debug=false;
        TripleExponentialSmoothing tes=new TripleExponentialSmoothing();
        ArrayList<Double> predictions= (ArrayList<Double>) tes.forecast(data,tesConst,period,nPredictions,debug);

        ArrayList<Double> predictedUpperbound=new ArrayList<>();
        for(int i=data.size();i<data.size()+nPredictions;i++){
            predictedUpperbound.add(tes.predictedUpperBound.get(i));
        }
        double prediction=calculateFibonacciWeightedMovingAverage(predictedUpperbound);
        return  prediction;

    }

    private double calculateAverageTripleExponentialSmoothing(List<Double> data,TripleExponentialSmoothingConstant tesConst,int period,int nPredictions){
        boolean debug=false;
        TripleExponentialSmoothing tes=new TripleExponentialSmoothing();
        ArrayList<Double> predictions= (ArrayList<Double>) tes.forecast(data,tesConst,period,nPredictions,debug);

        ArrayList<Double> predictedValues=new ArrayList<>();
        for(int i=data.size();i<data.size()+nPredictions;i++){
            predictedValues.add(predictions.get(i));
        }
        double prediction=calculateFibonacciWeightedMovingAverage(predictedValues);
        return  prediction;

    }

    private double calculateAverage(List<Double> data){
        double sumWeightedItems = 0;
        double sumWeight = 0;
        int weight;
        int length=data.size();

        for(int i = 0; i < length; i++){
            weight=1;
            sumWeightedItems += data.get(i) * weight;
            sumWeight += weight;
        }
        return sumWeightedItems / sumWeight;
    }

    private double calculateFibonacciWeightedMovingAverage(List<Double> data){
        double sumWeightedItems = 0;
        double sumWeight = 0;
        int weight;
        int length=data.size();

        ArrayList<Integer> fibonacci=new ArrayList<>();
        int fibo1 = 0;
        int fibo2 = 1;
        fibonacci.add(fibo1);
        fibonacci.add(fibo2);
        for(int i = 0; i < length; i++){
            int fib3=fibo1+fibo2;
            fibo1=fibo2;
            fibo2=fib3;
            fibonacci.add(fib3);
        }
        for(int i = 0; i < length; i++){
            weight=fibonacci.get(length-i-1);
            sumWeightedItems += data.get(i) * weight;
            sumWeight += weight;
        }
        return sumWeightedItems / sumWeight;
    }

    /**
     * Gets analyzing method
     * @return 
     */
    private String[] getAnalysisMethod(){
        return analysisMethod;
    }
    
    /**
     * Sets Analyzing method
     * @param analysisMethod 
     */
    private void setAnalysisMethod(String[] analysisMethod){
        this.analysisMethod = analysisMethod;
    }
    
    /**
     * Gets the History of analyzing phase
     * @return 
     */
    public ArrayList<AnalyzerHistory> getHistoryList(){
        return historyList;
    }
    
    /**
     * Sets the History of analyzing phase
     * @param historyList 
     */
    private void setHistoryList(ArrayList<AnalyzerHistory> historyList){
        this.historyList = historyList;
    }
    
    /**
     * Returns the size of History for analyzing phase
     * @return 
     */
    public int sizeHistory(){
        return getHistoryList().size();
    }
    
    /**
     * Returns the latest record of analyzing history
     * @return 
     */
    public AnalyzerHistory latestHistoryRec(){
        return getHistoryList().get(sizeHistory()-1);
    }

    public enum AnalyzerParameter{
        CPUUtil(0),
        VMCount(1),
        Throughput(2),
        ResponseTime(3),
        DelayTime(4),
        SLAVCount(5),
        SLAVPercentage(6),
        SLAVTime(7),
        FailedCloudlet(8),
        FutureWorkload(9);
        int index;
        AnalyzerParameter(int i) {
            index = i;
        }
        int getIndex(){
         return index;
        }
    }

}

