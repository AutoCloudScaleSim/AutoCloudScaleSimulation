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
    public static List<Double> forecast(List<Double> y, double alpha, double beta,
                                        double gamma, int period, int m, boolean debug) {

        validateArguments(y, alpha, beta, gamma, period, m);
        int seasons = y.size() / period;

        /**
        *calculate initial values
        */
        double a0 = calculateInitialLevel(y);
        double b0 = calculateInitialTrend(y, period);
        List<Double> initialSeasonalIndices = calculateSeasonalIndices(y, period,
                seasons);

//        if (debug) {
//            System.out.println(String.format(
//                    "Total observations: %d, Seasons %d, Periods %d", y.size(),
//                    seasons, period));
//            System.out.println("Initial level value a0: " + a0);
//            System.out.println("Initial trend value b0: " + b0);
//            printArray("Seasonal Indices: ", initialSeasonalIndices);
//        }

        List<Double> forecast = calculateHoltWinters(y, a0, b0, alpha, beta, gamma,
                initialSeasonalIndices, period, m, debug);

//        if (debug) {
//            printArray("Forecast", forecast);
//        }

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
    private static void validateArguments(List<Double> y, double alpha, double beta,
                                          double gamma, int period, int m) {
        if (y == null) {
            throw new IllegalArgumentException("Value of y should be not null");
        }

        if(m <= 0){
            throw new IllegalArgumentException("Value of m must be greater than 0.");
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
     * @param y
     * @param a0
     * @param b0
     * @param alpha
     * @param beta
     * @param gamma
     * @param initialSeasonalIndices
     * @param period
     * @param m
     * @param debug
     * @return - Forecast for m periods.
     */
    private static List<Double> calculateHoltWinters(List<Double> y, double a0, double b0,
                                                     double alpha, double beta, double gamma,
                                                     List<Double> initialSeasonalIndices, int period, int m, boolean debug) {

        List<Double> level = new ArrayList<Double>(y.size());
        while(level.size()<y.size()) level.add(0.0);

        List<Double> trend = new ArrayList<Double>(y.size());
        while(trend.size()<y.size()) trend.add(0.0);

        List<Double> seasonality = new ArrayList<Double>(y.size());
        while(seasonality.size()<y.size()) seasonality.add(0.0);

        List<Double> predictions = new ArrayList<Double>(y.size() + m);
        while(predictions.size()<y.size() + m) predictions.add(0.0);

        // Initialize base values
        level.add(1, a0);
        trend.add(1, b0);

        for (int i = 0; i < period; i++) {
            seasonality.set(i,initialSeasonalIndices.get(i));
        }

        // Start calculations
        for (int i = 2; i < y.size(); i++) {

            // Calculate overall smoothing
            if ((i - period) >= 0) {
                level.set(i, alpha * y.get(i) - seasonality.get(i - period) + (1.0 - alpha)
                        * (level.get(i - 1) + trend.get(i - 1)));
            } else {
                level.set(i, alpha * y.get(i) + (1.0 - alpha) * (level.get(i - 1) + trend.get(i - 1)));
            }

            // Calculate trend smoothing
            trend.set(i, beta * (level.get(i) - level.get(i - 1)) + (1 - beta) * trend.get(i - 1));

            // Calculate seasonal smoothing
            if ((i - period) >= 0) {
                seasonality.set(i, gamma * y.get(i) - level.get(i) + (1.0 - gamma) * seasonality.get(i - period));
            }

            // Calculate forecast
            if (((i + m) >= period)) {
                predictions.set(i+m, (level.get(i) + m * trend.get(i)) * seasonality.get(i - period + m));
            }

//            if (debug) {
//                System.out.println(String.format(
//                        "i = %d, y = %d, S = %f, Bt = %f, It = %f, F = %f", i,
//                        Math.round(y.get(i)), level.get(i), trend.get(i), seasonality.get(i), predictions.get(i)));
//            }
        }

        return predictions;
    }

    /**
     *
     * @return - Initial Level value i.e. St[1]
     */
    private static double calculateInitialLevel(List<Double> y) {
        return y.get(0);
    }

    /**
     *
     * @return - Initial trend - Bt[1]
     */
    private static double calculateInitialTrend(List<Double> y, int period) {

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
    private static List<Double> calculateSeasonalIndices(List<Double> y, int period,
                                                         int seasons) {

        double[] seasonalAverage = new double[seasons];
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