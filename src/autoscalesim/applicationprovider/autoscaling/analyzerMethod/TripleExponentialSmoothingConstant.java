package autoscalesim.applicationprovider.autoscaling.analyzerMethod;

public class TripleExponentialSmoothingConstant {
    public double alpha;
    public double beta;
    public double gamma;

    public TripleExponentialSmoothingConstant(boolean randomize){
        if(randomize){
            init(getRandomValue(),getRandomValue(),getRandomValue());
        }else{
            init(0.40,0.0,0.60);
        }
    }
    public TripleExponentialSmoothingConstant(double alpha, double beta, double gamma){
        init(alpha,beta,gamma);
    }

    // 3 decimal point
    public double getRandomValue(){
        double value=Math.random();
        //return (double)((int)(value*1000))/1000;
        return  value;
    }
    private void init(double alpha, double beta, double gamma){
        this.alpha=alpha;
        this.beta=beta;
        this.gamma=gamma;
    }
}
