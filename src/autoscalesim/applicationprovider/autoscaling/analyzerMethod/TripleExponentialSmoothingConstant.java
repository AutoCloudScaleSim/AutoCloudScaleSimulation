package autoscalesim.applicationprovider.autoscaling.analyzerMethod;

import java.util.Random;

public class TripleExponentialSmoothingConstant {
    public double alpha;
    public double beta;
    public double gamma;
    public double[] tesConsts;
    public int selectedConst;
    public double bias;
    Random random = new Random();

    public TripleExponentialSmoothingConstant(double alpha, double beta, double gamma){

        init(alpha,beta,gamma);
    }

    public TripleExponentialSmoothingConstant(boolean randomize){
        if(randomize){
            init(getRandomValue(),getRandomValue(),getRandomValue());
        }else{
            init(0.0,0.0,0.0);
        }
    }

    private void init(double alpha, double beta, double gamma){
        this.alpha=alpha;
        this.beta=beta;
        this.gamma=gamma;
        tesConsts= new double[]{alpha, beta, gamma};
        selectedConst=0;
        bias=0.0;

    }

    public void selectTesConstRandomly(){
        selectedConst=random.nextInt(3);
    }

    public void selectBiasRandomly(){
        bias=getRandomValue(0.02);
    }

    public TripleExponentialSmoothingConstant getUpperValueOfSelectedConstant(){
        tesConsts[selectedConst]=getRandomUpperValue(tesConsts[selectedConst]);
        return new TripleExponentialSmoothingConstant(tesConsts[0],tesConsts[1],tesConsts[2]);
    }

    public TripleExponentialSmoothingConstant getLowerValueOfSelectedConstant(){
        tesConsts[selectedConst]=getRandomLowerValue(tesConsts[selectedConst]);
        return new TripleExponentialSmoothingConstant(tesConsts[0],tesConsts[1],tesConsts[2]);
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
        double valueToAdd=bias;
        pConst+=valueToAdd;
        if(pConst>1.0){
            pConst=1.0;
        }
        return pConst;
    }

    private double getRandomLowerValue(double pConst){
        double valueToSubtract=bias;
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
