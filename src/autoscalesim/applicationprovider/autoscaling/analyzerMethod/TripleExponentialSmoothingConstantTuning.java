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

    public TripleExponentialSmoothingConstant getBestFittedConstant(List<Double> pSeries,int period){
        TripleExponentialSmoothing tes=new TripleExponentialSmoothing();
        int nPred=0; // we are not predicting future now
        int seriesLength=pSeries.size();
        List<Double> series= pSeries.subList(seriesLength-Math.min(seriesLength,3*period),seriesLength);
        // forcast for current constant
        List<Double> oldConstantsPredictions =tes.forecast(series,oldConstants,period,nPred,false);
        double oldMSE=getMeanSquaredError(series,oldConstantsPredictions);

        oldConstants.selectTesConstRandomly();
        oldConstants.selectBiasRandomly();
        // Can we guess better constants-
        TripleExponentialSmoothingConstant upperConsts=oldConstants.getUpperValueOfSelectedConstant();
        List<Double> upperConstantsPredictions =tes.forecast(series, upperConsts,period,nPred,false);
        double upperMSE=getMeanSquaredError(series,upperConstantsPredictions);

        TripleExponentialSmoothingConstant lowerConsts=oldConstants.getLowerValueOfSelectedConstant();
        List<Double> lowerConstPredictions =tes.forecast(series, lowerConsts,period,nPred,false);
        double lowerMSE=getMeanSquaredError(series,lowerConstPredictions);

        if(upperMSE<lowerMSE){
            if(oldMSE<upperMSE){
                return oldConstants;
            }else{
                return upperConsts;
            }
        }else{
            if(oldMSE<lowerMSE){
                return oldConstants;
            }else{
                return lowerConsts;
            }
        }

    }

}
