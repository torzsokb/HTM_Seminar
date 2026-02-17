package search;

import core.Shift;

import java.util.List;

public class Objective {

    public static final class BalancedObj implements ObjectiveFunction {
        public final double lambdaL;
        public final double lambdaC;

        public BalancedObj(double lambdaL, double lambdaC) {
            this.lambdaL = lambdaL;
            this.lambdaC = lambdaC;
        }

        @Override
        public double shifts(List<Shift> shifts) {
            int m = (shifts == null) ? 0 : shifts.size();
            if (m == 0) return 0.0;

            double sumL = 0.0, sumL2 = 0.0;
            double sumC = 0.0, sumC2 = 0.0;

            for (Shift s : shifts) {
                if (s == null) continue;

                double L = s.totalTime;
                double C = s.serviceTime;

                sumL += L;
                sumL2 += L * L;

                sumC += C;
                sumC2 += C * C;
            }

            double sseL = sumL2 - (sumL * sumL) / m;
            double sseC = sumC2 - (sumC * sumC) / m;

            return sumL + lambdaL * sseL + lambdaC * sseC;
        }
    }

    public static ObjectiveFunction balancedObj(double lambdaL, double lambdaC) {
        return new BalancedObj(lambdaL, lambdaC);
    }

    public static ObjectiveFunction totalLength() {
        return new BalancedObj(0.0, 0.0);
    }

}