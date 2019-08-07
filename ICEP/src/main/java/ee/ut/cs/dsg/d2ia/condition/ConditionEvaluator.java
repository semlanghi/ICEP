package ee.ut.cs.dsg.d2ia.condition;

import ee.ut.cs.dsg.d2ia.event.RawEvent;
import ee.ut.cs.dsg.d2ia.generator.MyIterativeCondition;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.Serializable;

public class ConditionEvaluator<S extends RawEvent>  implements Serializable {

   // private Condition condition;

//    public ConditionEvaluator(Condition condition) // we might not need this constructor
//    {
//        this.condition = condition;
//    }
    //This method can be changed to static
    private int count;
    private double sum;
    private double min;
    private double max;
    private double first;
    private double last;

    public boolean evaluateCondition(AbsoluteCondition condition, S s) throws Exception {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        String conditionString = condition.parse(0,0,0,0,0,0,s.getValue());


        boolean result = (boolean) engine.eval(conditionString);
        return result;
    }
    public void resetStats()
    {
        count=0;
        sum = 0d;
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        first = -1d;
        last = -1d;
    }
    public boolean evaluateRelativeCondition(RelativeCondition condition, Iterable<S> prevMatches, S s) throws Exception {

        // update intermediate results
        if (prevMatches != null) {
            resetStats();
            for (S ss : prevMatches) //Iterables preserve order
            {
                sum += ss.getValue();
                count++;
                min = Double.min(min, ss.getValue());
                max = Double.max(max, ss.getValue());
                if (first == Double.MIN_VALUE)
                    first = ss.getValue();

                last = ss.getValue();
            }
            // we have to add the current element

        }
        else
        {
            if (first == -1) {
                first = s.getValue();
                last = s.getValue();
                min = s.getValue();
                max = s.getValue();
            }
        }
        sum += s.getValue();
        count++;


        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        String conditionString;// = condition.toString().split("Relative")[1].trim();

        Object relativeLHS = condition.getRelativeLHS();
        Object relativeRHS = condition.getRelativeRHS();
        Operator relativeOperator = condition.getRelativeOperator();
        double relativeLHSDouble = Double.MIN_VALUE, relativeRHSDouble = Double.MIN_VALUE;
        Operand relativeLHSOperand = null, relativeRHSOperand = null;


        if (relativeLHS instanceof String) {
            relativeLHSDouble = Double.parseDouble(((String) relativeLHS));
        }

        if (relativeRHS instanceof String) {
            relativeRHSDouble = Double.parseDouble(((String) relativeRHS));
        }

        if (relativeLHS instanceof Operand) {
            relativeLHSOperand = (Operand) relativeLHS;
        }

        if (relativeRHS instanceof Operand) {
            relativeRHSOperand = (Operand) relativeRHS;
        }


//        if ((relativeRHSOperand != null && relativeRHSOperand != Operand.Constant)
//                || (relativeLHSOperand != null && relativeLHSOperand != Operand.Constant)) {


//        }
        String lhsConditionString = null;
        String rhsConditionString = null;
        if (relativeLHS instanceof AbsoluteCondition) {
            lhsConditionString = ((AbsoluteCondition) relativeLHS).parse(first, last, min, max, sum, count, s.getValue());
        }
        if (relativeRHS instanceof AbsoluteCondition) {
            rhsConditionString = ((AbsoluteCondition) relativeRHS).parse(first, last, min, max, sum, count, s.getValue());
        }
        // now we can evaluate the condition
        conditionString = "";
        if (relativeLHSDouble != Double.MIN_VALUE) {
            conditionString += relativeLHSDouble;
        } else if (relativeLHSOperand != null) {
            if (relativeLHSOperand == Operand.Average) {
                conditionString += (sum / count);
            } else if (relativeLHSOperand == Operand.Sum) {
                conditionString += sum;
            } else if (relativeLHSOperand == Operand.First) {
                conditionString += first;
            } else if (relativeLHSOperand == Operand.Last) {
                conditionString += last;
            } else if (relativeLHSOperand == Operand.Max) {
                conditionString += max;
            } else if (relativeLHSOperand == Operand.Min) {
                conditionString += min;
            } else if (relativeLHSOperand == Operand.Value) {
                conditionString += s.getValue();
            }

        } else
            conditionString += lhsConditionString;

        conditionString += " " + relativeOperator.toString();

        if (relativeRHSDouble != Double.MIN_VALUE) {
            conditionString += relativeRHSDouble;
        } else if (relativeRHSOperand != null) {
            if (relativeRHSOperand == Operand.Average) {
                conditionString += (sum / count);
            } else if (relativeRHSOperand == Operand.Sum) {
                conditionString += sum;
            } else if (relativeRHSOperand == Operand.First) {
                conditionString += first;
            } else if (relativeRHSOperand == Operand.Last) {
                conditionString += last;
            } else if (relativeRHSOperand == Operand.Max) {
                conditionString += max;
            } else if (relativeRHSOperand == Operand.Min) {
                conditionString += min;
            } else if (relativeRHSOperand == Operand.Value) {
                conditionString += s.getValue();
            }

        } else
            conditionString += rhsConditionString;
        boolean result = (boolean) engine.eval(conditionString);

        min = Math.min(min, s.getValue());
        max = Math.max(max, s.getValue());
        last = s.getValue();
        return result;
    }
}
