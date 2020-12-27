package autoscalesim.applicationprovider.autoscaling.analyzerMethod;

import java.util.List;

public class TripleExponentialSmoothingConstantTuning {

    private TripleExponentialSmoothingConstant oldConstants;
    public TripleExponentialSmoothingConstantTuning(TripleExponentialSmoothingConstant constant){
        oldConstants=constant;
    }
    public TripleExponentialSmoothingConstantTuning(){
        oldConstants=new TripleExponentialSmoothingConstant(false);
    }

    public double getMeanSquaredError(List<Double> series,List<Double> predictions){
        int seriesSize=series.size();
        double squaredErrorSum=0;
        for(int index=0;index<seriesSize;index++){
            double squaredError=(series.get(index)-predictions.get(index));
            squaredErrorSum+=(squaredError*squaredError);
        }

        double mse=squaredErrorSum/seriesSize;
        return mse;
    }

    public TripleExponentialSmoothingConstant getBestFittedConstant(List<Double> series,int period){
        TripleExponentialSmoothing tes=new TripleExponentialSmoothing();
        int nPred=0; // we are not predicting future now
        List<Double> oldConstantsPredictions =tes.forecast(series,oldConstants,period,0,false);

        TripleExponentialSmoothingConstant randomConstants=new TripleExponentialSmoothingConstant(true);
        List<Double> randomConstantsPredictions =tes.forecast(series, randomConstants,period,0,false);

        double oldMSE=getMeanSquaredError(series,oldConstantsPredictions);
        double newMSE=getMeanSquaredError(series,randomConstantsPredictions);
        if(oldMSE<newMSE){
            return  oldConstants;
        }else {
            return randomConstants;
        }

    }
}
