package dk.mmj.circuit;

import dk.mmj.fhe.interfaces.Ciphertext;
import dk.mmj.fhe.interfaces.FHE;
import dk.mmj.fhe.interfaces.PublicKey;

import java.util.List;

/**
 * Builder for a gate.
 * <br/>
 * Note that a gate must have input being EITHER gates, or circuit-input
 */
class GateBuilder {
    private final FHE fhe;
    private final List<CircuitBuilder.Observer> observers;
    private final GateType type;
    private CircuitBuilder[] inputGates;
    private int input;
    private int depth = -1;

    GateBuilder(GateType type, FHE fhe, List<CircuitBuilder.Observer> observers) {
        this.type = type;
        inputGates = new CircuitBuilder[type.inDegree];
        this.fhe = fhe;
        this.observers = observers;
    }

    Gate build(CircuitBuilder.DepthCounter cnt) {
        Gate left = null;
        Gate right = null;
        int leftDepth = 0, rightDepth = 0;
        DepthCounterImpl dc = new DepthCounterImpl();
        if (inputGates.length > 0) {
            left = inputGates[0].gateBuild(dc);
            leftDepth = dc.depth;
        }

        if (inputGates.length > 1) {
            right = inputGates[1].gateBuild(dc);
            rightDepth = dc.depth;
        }

        if(leftDepth > rightDepth && right != null){
            Gate tmp = left;
            left = right;
            right = tmp;
        }

        depth = Math.max(leftDepth, rightDepth);

        switch (type) {
            case NOT:
                cnt.registerDepth(depth);
                return evaluate(type, fhe::not, left);
            case OR:
                cnt.registerDepth(depth + 1);
                return evaluate(type, fhe::or, left, right);
            case AND:
                cnt.registerDepth(depth + 1);
                return evaluate(type, fhe::and, left, right);
            case NAND:
                cnt.registerDepth(depth + 1);
                return evaluate(type, fhe::nand, left, right);
            case XOR:
                cnt.registerDepth(depth + 2);
                return evaluate(type, fhe::xor, left, right);
            case INPUT:
                cnt.registerDepth(depth);
                return (pk, inputArray) -> inputArray[input];
            default:
                throw new RuntimeException("Invalid type");
        }
    }

    private Gate evaluate(GateType type, IndegreeOneFunction func, Gate inputGate) {
        return (pk, input) -> {
            Ciphertext inputValue = inputGate.evaluate(pk, input);
            Ciphertext eval = func.eval(inputValue, pk);
            registerWithObservers(type, inputValue, eval);
            return eval;
        };
    }

    private Gate evaluate(GateType type, IndegreeTwoFunction func, Gate leftGate, Gate rightGate) {
        return (pk, input) -> {
            Ciphertext leftInput = leftGate.evaluate(pk, input);
            Ciphertext rightInput = rightGate.evaluate(pk, input);

            Ciphertext eval = func.eval(leftInput, rightInput, pk);
            registerWithObservers(type, leftInput, rightInput, eval);
            registerWithObservers(type, leftInput, rightInput, func.eval(rightInput, leftInput, pk), "RevInput");
            return eval;
        };
    }

    void setGates(CircuitBuilder... gates) {
        if (type == GateType.INPUT) {
            throw new RuntimeException("Cannot define gates as input, on an input gate");
        }
        if (gates.length != type.inDegree) {
            throw new RuntimeException("Number of gates as input, must match indegree");
        }
        inputGates = gates;
    }

    void setInputIndex(int inputIndex) {
        input = inputIndex;
    }

    /**
     * Registers usage of gate with indegree two, with observers
     *
     * @param type       type of gate
     * @param inputValue input value
     * @param eval       the result of the evaluation
     */
    private void registerWithObservers(GateType type, Ciphertext inputValue, Ciphertext eval) {
        try {
            for (CircuitBuilder.Observer observer : observers) {
                observer.register(type, inputValue, eval);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Registers usage of gate with indegree two, with observers
     *
     * @param type       type of gate
     * @param leftValue  left input gate
     * @param rightValue right input gate
     * @param eval       the result of the evaluation
     */
    private void registerWithObservers(GateType type, Ciphertext leftValue, Ciphertext rightValue, Ciphertext eval) {
        registerWithObservers(type, leftValue, rightValue, eval, "");
    }

    /**
     * Registers usage of gate with indegree two, with observers
     *
     * @param type       type of gate
     * @param leftValue  left input gate
     * @param rightValue right input gate
     * @param eval       the result of the evaluation
     * @param comment    comment to be suffixed
     */
    private void registerWithObservers(GateType type, Ciphertext leftValue, Ciphertext rightValue, Ciphertext eval, String comment) {
        try {
            for (CircuitBuilder.Observer observer : observers) {
                observer.register(type, leftValue, rightValue, eval, comment);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private interface IndegreeTwoFunction {
        Ciphertext eval(Ciphertext left, Ciphertext right, PublicKey pk);
    }

    private interface IndegreeOneFunction {
        Ciphertext eval(Ciphertext input, PublicKey pk);
    }



    private static class DepthCounterImpl implements CircuitBuilder.DepthCounter {
        int depth = 0;

        public void registerDepth(int depth) {
            this.depth = depth;
        }
    }

}


