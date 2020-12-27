package autoscalesim.applicationprovider.autoscaling.analyzerMethod;
import java.util.List;
import java.util.ArrayList;
/**
 * Triple Exponential smoothing is Equation:
 *
 * level[i] = alpha * y[i] - seasonality[i - period] + (1.0 - alpha) * (level[i - 1] + trend[i - 1])
 * trend[i] = beta * (level[i] - level[i - 1]) + (1 - beta) * trend[i - 1]
 * seasonality[i] = gamma * y[i] - level[i] + (1.0 - gamma) * seasonality[i - period]
 * Future[i + m] = (level[i] + (m * trend[i])) * seasonality[i - period + m]
 *
 *
 * @author navdeepgill
 *
 */
public class TripleExponentialSmoothing {
    public ArrayList<Double> predictedUpperBound=new ArrayList<>();

    /**
     * Calculates the initial values and
     * returns the forecast for the future m periods.
     *
     * @param y - Time series data.
     * @param alpha - Exponential smoothing coefficients for level, trend,
     *            seasonal components.
     * @param beta - Exponential smoothing coefficients for level, trend,
     *            seasonal components.
     * @param gamma - Exponential smoothing coefficients for level, trend,
     *            seasonal components.
     * @param period - A complete season's data consists of L periods. And we need
     *            to estimate the trend factor from one period to the next. To
     *            accomplish this, it is advisable to use two complete seasons;
     *            that is, 2L periods.
     * @param m - Extrapolated future data points.
     *          - 4 quarterly,
     *          - 7 weekly,
     *          - 12 monthly
     *
     * @param debug - Print debug values.
     *
     */

    public List<Double> forecast(List<Double> y,TripleExponentialSmoothingConstant tesConst, int period, int m, boolean debug) {

        double alpha= tesConst.alpha;
        double beta= tesConst.beta;
        double gamma= tesConst.gamma;

        validateArguments(y, alpha, beta, gamma, period, m);
        int seasons = y.size() / period;

        /**
        *calculate initial values
        */
        double a0 = calculateInitialLevel(y);
        double b0 = calculateInitialTrend(y, period);
        List<Double> initialSeasonalIndices = calculateSeasonalIndices(y, period,
                seasons);

        if (debug) {
            System.out.println(String.format(
                    "Total observations: %d, Seasons %d, Periods %d", y.size(),
                    seasons, period));
            System.out.println("Initial level value a0: " + a0);
            System.out.println("Initial trend value b0: " + b0);
            System.out.println("Seasonal Indices: "+ initialSeasonalIndices);
        }

        List<Double> forecast = calculateHoltWinters(y, a0, b0, alpha, beta, gamma,
                initialSeasonalIndices, period, m,1.96, debug);

        if (debug) {
            System.out.println("Forecast"+ forecast);
        }

        return forecast;
    }

    /**
     * Validate input.
     *
     * @param y
     * @param alpha
     * @param beta
     * @param gamma
     * @param m
     */
    private void validateArguments(List<Double> y, double alpha, double beta,
                                          double gamma, int period, int m) {
        if (y == null) {
            throw new IllegalArgumentException("Value of y should be not null");
        }

        if(m > period){
            throw new IllegalArgumentException("Value of m must be <= period.");
        }

        if((alpha < 0.0) || (alpha > 1.0)){
            throw new IllegalArgumentException("Value of Alpha should satisfy 0.0 <= alpha <= 1.0");
        }

        if((beta < 0.0) || (beta > 1.0)){
            throw new IllegalArgumentException("Value of Beta should satisfy 0.0 <= beta <= 1.0");
        }

        if((gamma < 0.0) || (gamma > 1.0)){
            throw new IllegalArgumentException("Value of Gamma should satisfy 0.0 <= gamma <= 1.0");
        }
    }

    /**
     * This method realizes the Holt-Winters equations.
     *
     * @param series
     * @param a0
     * @param b0
     * @param alpha
     * @param beta
     * @param gamma
     * @param seasonals
     * @param period
     * @param nPreds
     * @param debug
     * @return - Forecast for m periods.
     */
    public List<Double> calculateHoltWinters(List<Double> series, double a0, double b0,
                                                    double alpha, double beta, double gamma,
                                                    List<Double> seasonals, int period, int nPreds,double scalingFactor, boolean debug){

        ArrayList<Double> predictions=new ArrayList<>();
        ArrayList<Double> levels=new ArrayList<>();
        ArrayList<Double> trends=new ArrayList<>();
        ArrayList<Double> seasons=new ArrayList<>();
        ArrayList<Double> predictedDeviations=new ArrayList<>();
        ArrayList<Double> upperBound=new ArrayList<>();

        double level=0.0,trend=0.0;
        for(int i =0;i<series.size()+nPreds;i++){
            double prediction,deviations;
            // initializations
            if(i==0){
                level=a0;
                trend=b0;
                prediction=series.get(0);
                deviations=0.0;

                predictions.add(prediction);
                levels.add(level);
                trends.add(trend);
                seasons.add(seasonals.get(i%period));
                predictedDeviations.add(deviations);
                upperBound.add(calculateUpperbound(prediction,deviations,scalingFactor));
                continue;
            }
            // training
            else if(i<series.size()){
                double val=series.get(i);
                double lastLevel=level,lastTrend=trend;
                level=alpha*(val-seasonals.get(i%period)) + (1-alpha)*(lastLevel+lastTrend);
                trend = beta * (level-lastLevel) + (1-beta)*lastTrend;
                double seasonal = gamma*(val-level) + (1-gamma)*seasonals.get(i%period);
                prediction=level+trend+seasonal;
                deviations=calculatePredictedDeviations(val,prediction,predictedDeviations.get(i-1),gamma);

                seasonals.set(i%period,seasonal);
                predictions.add(prediction);
                predictedDeviations.add(deviations);
            }
            // predicting
            else{
                int m=i-series.size()+1;
                prediction=level+m*trend+seasonals.get(i%period);
                deviations=predictedDeviations.get(predictedDeviations.size()-1)*1.01;
                predictions.add(prediction);
                predictedDeviations.add(deviations);
            }
            double ub=calculateUpperbound(prediction,deviations,scalingFactor);
            upperBound.add(ub);

        }
        predictedUpperBound=upperBound;
        return  predictions;
    }

    /**
     * @return - calculate predictedDeviations
     */
    private double calculatePredictedDeviations(double actualValue,double predictedValue,double lastPredictedDeviations,double gamma){
        return  gamma*Math.abs(actualValue-predictedValue)+(1-gamma)*lastPredictedDeviations;
    }

    /**
     * @return - Calculate upperbound of predictions
     */
    private double calculateUpperbound(double predictedValue,double predictedDeviations,double scalingFactor){
        return predictedValue+scalingFactor*predictedDeviations;
    }

    /**
     *
     * @return - Initial Level value i.e. St[1]
     */
    private double calculateInitialLevel(List<Double> y) {
        return y.get(0);
    }

    /**
     *
     * @return - Initial trend - Bt[1]
     */
    private double calculateInitialTrend(List<Double> y, int period) {
        if(y.size()<2*period){
            return  0.0;
        }

        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += (y.get(period + i) - y.get(i));
        }

        return sum / (period * period);
    }

    /**
     *
     * @return - Seasonal Indices.
     */
    private List<Double> calculateSeasonalIndices(List<Double> y, int period,
                                                         int seasons) {

        double[] seasonalAverage = new double[seasons]; // every season average=[av_s]*season
        double[] seasonalIndices = new double[period];

        double[] averagedObservations = new double[y.size()];

        for (int i = 0; i < seasons; i++) {
            for (int j = 0; j < period; j++) {
                seasonalAverage[i] += y.get((i * period) + j);
            }
            seasonalAverage[i] /= period;
        }

        for (int i = 0; i < seasons; i++) {
            for (int j = 0; j < period; j++) {
                averagedObservations[(i * period) + j] = y.get((i * period) + j)
                        / seasonalAverage[i];
            }
        }

        for (int i = 0; i < period; i++) {
            for (int j = 0; j < seasons; j++) {
                seasonalIndices[i] += averagedObservations[(j * period) + i];
            }
            seasonalIndices[i] /= seasons;
        }

        ArrayList<Double> list = new ArrayList<Double>(seasonalIndices.length);
        for(double d : seasonalIndices) list.add(d);
        return list;
    }

}