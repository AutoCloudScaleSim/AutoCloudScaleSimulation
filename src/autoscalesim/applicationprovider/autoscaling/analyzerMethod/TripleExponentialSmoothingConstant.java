package autoscalesim.applicationprovider.autoscaling.analyzerMethod;

import java.util.Random;

public class TripleExponentialSmoothingConstant {
    public double alpha;
    public double beta;
    public double gamma;
    Random random = new Random();

    public TripleExponentialSmoothingConstant(double alpha, double beta, double gamma){

        init(alpha,beta,gamma);
    }

    public TripleExponentialSmoothingConstant(boolean randomize){
        if(randomize){
            init(getRandomValue(),getRandomValue(),getRandomValue());
        }else{
            init(0.40,0.0,0.60);
        }
    }

    private void init(double alpha, double beta, double gamma){
        this.alpha=alpha;
        this.beta=beta;
        this.gamma=gamma;
    }

    public TripleExponentialSmoothingConstant getRandomAdditiveConstant(){

        double _alpha=getRandomUpperOrLower(alpha);
        double _beta=getRandomUpperOrLower(beta);
        double _gamma=getRandomUpperOrLower(gamma);
        return new TripleExponentialSmoothingConstant(_alpha,_beta,_gamma);
    }

    private double getRandomUpperOrLower(double pConst){
        boolean isUpper=getRandomBoolean();
        if(isUpper){
            return getRandomUpperValue(pConst);
        }else{
            return getRandomLowerValue(pConst);
        }
    }

    // 3 decimal point
    private double getRandomValue(){
        double value=random.nextDouble();
        //return (double)((int)(value*1000))/1000;
        return  value;
    }

    private double getRandomValue(double hi){
        double value=random.nextDouble();
        while (value>hi){
            value/=2;
        }
        return  value;
    }

    private double getRandomUpperValue(double pConst){
        double valueToAdd=getRandomValue(0.02);
        pConst+=valueToAdd;
        if(pConst>1.0){
            pConst=1.0;
        }
        return pConst;
    }

    private double getRandomLowerValue(double pConst){
        double valueToSubtract=getRandomValue(0.02);
        pConst-=valueToSubtract;
        if(pConst<0.0){
            pConst=0.0;
        }
        return pConst;
    }

    private boolean getRandomBoolean() {
        return random.nextBoolean();
    }
}
