package search;

import core.Shift;

public class Objective {

    public static ObjectiveFunction totalLength() {
        return (shifts) -> {
            double sum = 0.0;
            for (Shift s : shifts) {
                if (s != null) sum += s.totalTime;
            }
            return sum;
        };
    }

    public static ObjectiveFunction balancedObj(double lambdaL, double lambdaC) {
        return (shifts) -> {
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
        };
    }
}