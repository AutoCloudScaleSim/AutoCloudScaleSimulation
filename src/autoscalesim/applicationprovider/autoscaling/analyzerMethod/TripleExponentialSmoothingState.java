package autoscalesim.applicationprovider.autoscaling.analyzerMethod;

import java.util.List;

public class TripleExponentialSmoothingState {
    double initialLevel;
    double initialTrend;
    List<Double> initialSeasonalIndices;

    public TripleExponentialSmoothingState(double initialLevel,double initialTrend,List<Double> initialSeasonalIndices){
        this.initialLevel=initialLevel;
        this.initialTrend=initialTrend;
        this.initialSeasonalIndices=initialSeasonalIndices;
    }
}
